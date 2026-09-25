package com.marlus.financas.dataio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marlus.financas.dataio.csv.Checksums;
import com.marlus.financas.dataio.csv.CsvWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;

/** Exportação de dados em CSV/ZIP (seção 9): um arquivo por entidade + {@code manifest.json}. */
@Service
public class ExportService {

    /** Versão do formato exportado — incrementada só quando o layout de colunas muda de forma incompatível. */
    public static final String SCHEMA_VERSION = "1";

    private static final String APP_VERSION = "1.0.0";

    private final DataIoRegistry registry;
    private final ObjectMapper objectMapper;

    public ExportService(DataIoRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    public byte[] exportAll(UUID userId) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            List<ExportManifest.ManifestFile> manifestFiles = new ArrayList<>();
            try (ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
                for (EntityCsvHandler handler : registry.handlersInDependencyOrder()) {
                    byte[] csvBytes = buildCsv(handler, userId);
                    int rows = handler.exportRows(userId).size();
                    zip.putNextEntry(new ZipEntry(handler.fileName()));
                    zip.write(csvBytes);
                    zip.closeEntry();
                    manifestFiles.add(
                            new ExportManifest.ManifestFile(handler.fileName(), rows, Checksums.sha256(csvBytes)));
                }

                ExportManifest manifest =
                        new ExportManifest(SCHEMA_VERSION, Instant.now().toString(), APP_VERSION, manifestFiles);
                byte[] manifestBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest);
                zip.putNextEntry(new ZipEntry("manifest.json"));
                zip.write(manifestBytes);
                zip.closeEntry();
            }
            return buffer.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public byte[] exportEntityCsv(UUID userId, String fileName) {
        EntityCsvHandler handler = registry.byFileName(fileName);
        if (handler == null) {
            throw new com.marlus.financas.common.EntityNotFoundException("Entidade de exportação não encontrada: " + fileName);
        }
        return buildCsv(handler, userId);
    }

    private byte[] buildCsv(EntityCsvHandler handler, UUID userId) {
        String csv = CsvWriter.write(handler.header(), handler.exportRows(userId));
        return csv.getBytes(StandardCharsets.UTF_8);
    }
}
