package com.marlus.financas.dataio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marlus.financas.common.UuidV7Generator;
import com.marlus.financas.dataio.csv.Checksums;
import com.marlus.financas.dataio.csv.CsvReader;
import com.marlus.financas.dataio.domain.ImportJob;
import com.marlus.financas.dataio.domain.ImportJobStatus;
import com.marlus.financas.dataio.domain.ImportMode;
import com.marlus.financas.dataio.repository.ImportJobRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Importação de dados em CSV/ZIP (seção 9). Dry-run só valida (nada é escrito); apply roda em uma
 * única transação: primeiro valida tudo, só então (se "Substituir tudo") apaga os dados atuais e
 * grava — qualquer erro de linha cancela a importação inteira sem tocar no banco.
 */
@Service
public class ImportService {

    private final DataIoRegistry registry;
    private final ObjectMapper objectMapper;
    private final ImportJobRepository importJobRepository;

    public ImportService(DataIoRegistry registry, ObjectMapper objectMapper, ImportJobRepository importJobRepository) {
        this.registry = registry;
        this.objectMapper = objectMapper;
        this.importJobRepository = importJobRepository;
    }

    public ImportSummary dryRun(UUID userId, String filename, byte[] zipBytes) {
        Extracted extracted = extractAndValidate(zipBytes);
        Map<String, List<Map<String, String>>> parsed = parseRows(extracted.csvFiles());
        ImportSummary summary = process(userId, parsed, ImportMode.MERGE, false);
        saveJob(userId, filename, ImportMode.MERGE, ImportJobStatus.DRY_RUN, summary);
        return summary;
    }

    @Transactional
    public ImportSummary apply(UUID userId, String filename, byte[] zipBytes, ImportMode mode, String confirmation) {
        if (mode == ImportMode.REPLACE && !"SUBSTITUIR".equals(confirmation)) {
            throw new ImportConfirmationException(
                    "Para substituir todos os dados, digite exatamente \"SUBSTITUIR\" no campo de confirmação.");
        }

        Extracted extracted = extractAndValidate(zipBytes);
        Map<String, List<Map<String, String>>> parsed = parseRows(extracted.csvFiles());

        ImportSummary validation = process(userId, parsed, mode, false);
        if (validation.totalErrors() > 0) {
            saveJob(userId, filename, mode, ImportJobStatus.FAILED, validation);
            return validation;
        }

        if (mode == ImportMode.REPLACE) {
            for (EntityCsvHandler handler : registry.handlersInReverseDependencyOrder()) {
                handler.deleteAllForUser(userId);
            }
        }

        ImportSummary applied = process(userId, parsed, mode, true);
        saveJob(userId, filename, mode, ImportJobStatus.APPLIED, applied);
        return applied;
    }

    public List<ImportJob> jobs(UUID userId) {
        return importJobRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    private ImportSummary process(
            UUID userId, Map<String, List<Map<String, String>>> parsedByFile, ImportMode mode, boolean apply) {
        List<FileSummary> fileSummaries = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();
        int totalCreated = 0;
        int totalUpdated = 0;
        int totalErrors = 0;

        for (EntityCsvHandler handler : registry.handlersInDependencyOrder()) {
            List<Map<String, String>> rows = parsedByFile.getOrDefault(handler.fileName(), List.of());
            int created = 0;
            int updated = 0;
            int fileErrors = 0;
            int line = 2;
            for (Map<String, String> row : rows) {
                RowResult result = handler.importRow(userId, row, line, apply);
                switch (result.outcome()) {
                    case CREATED -> created++;
                    case UPDATED -> updated++;
                    case ERROR -> {
                        fileErrors++;
                        errors.add(new RowError(handler.fileName(), result.line(), result.errorMessage()));
                    }
                }
                line++;
            }
            fileSummaries.add(new FileSummary(handler.fileName(), created, updated, fileErrors));
            totalCreated += created;
            totalUpdated += updated;
            totalErrors += fileErrors;
        }

        return new ImportSummary(apply, mode, fileSummaries, errors, totalCreated, totalUpdated, totalErrors);
    }

    private Extracted extractAndValidate(byte[] zipBytes) {
        Map<String, byte[]> entries = readZipEntries(zipBytes);

        byte[] manifestBytes = entries.get("manifest.json");
        if (manifestBytes == null) {
            throw new ImportFileException("O arquivo não contém manifest.json — não parece ser uma exportação válida.");
        }

        ExportManifest manifest;
        try {
            manifest = objectMapper.readValue(manifestBytes, ExportManifest.class);
        } catch (IOException ex) {
            throw new ImportFileException("Não foi possível ler manifest.json: " + ex.getMessage());
        }

        int fileSchemaVersion = parseSchemaVersion(manifest.schemaVersion());
        int supportedSchemaVersion = parseSchemaVersion(ExportService.SCHEMA_VERSION);
        if (fileSchemaVersion > supportedSchemaVersion) {
            throw new ImportFileException(
                    "Este arquivo foi exportado por uma versão mais nova do app (esquema "
                            + manifest.schemaVersion() + "). Atualize o Finanças do Marlus antes de importar.");
        }

        for (ExportManifest.ManifestFile file : manifest.files()) {
            byte[] content = entries.get(file.name());
            if (content == null) {
                throw new ImportFileException("Arquivo \"" + file.name() + "\" listado no manifesto não foi encontrado no zip.");
            }
            String actualChecksum = Checksums.sha256(content);
            if (!actualChecksum.equalsIgnoreCase(file.sha256())) {
                throw new ImportFileException(
                        "O checksum de \"" + file.name() + "\" não confere — o arquivo pode estar corrompido.");
            }
        }

        return new Extracted(manifest, entries);
    }

    private static int parseSchemaVersion(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new ImportFileException("Versão de esquema inválida no manifesto: " + value);
        }
    }

    private Map<String, List<Map<String, String>>> parseRows(Map<String, byte[]> csvFiles) {
        Map<String, List<Map<String, String>>> parsed = new LinkedHashMap<>();
        for (EntityCsvHandler handler : registry.handlersInDependencyOrder()) {
            byte[] content = csvFiles.get(handler.fileName());
            if (content == null) {
                continue;
            }
            String csv = new String(content, StandardCharsets.UTF_8);
            parsed.put(handler.fileName(), CsvReader.readAsMaps(csv).stream().map(m -> (Map<String, String>) m).toList());
        }
        return parsed;
    }

    private Map<String, byte[]> readZipEntries(byte[] zipBytes) {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    entries.put(entry.getName(), zip.readAllBytes());
                }
                zip.closeEntry();
            }
        } catch (IOException ex) {
            throw new ImportFileException("Não foi possível ler o arquivo zip: " + ex.getMessage());
        }
        return entries;
    }

    private void saveJob(UUID userId, String filename, ImportMode mode, ImportJobStatus status, ImportSummary summary) {
        try {
            String summaryJson = objectMapper.writeValueAsString(summary);
            importJobRepository.save(
                    new ImportJob(UuidV7Generator.generate(), userId, filename, mode, status, summaryJson));
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new UncheckedIOException(new IOException(ex));
        }
    }

    private record Extracted(ExportManifest manifest, Map<String, byte[]> csvFiles) {
    }
}
