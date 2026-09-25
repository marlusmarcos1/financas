package com.marlus.financas.dataio.web;

import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.dataio.domain.ImportMode;
import com.marlus.financas.dataio.service.ImportFileException;
import com.marlus.financas.dataio.service.ImportService;
import com.marlus.financas.dataio.service.ImportSummary;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/data")
public class ImportController {

    private final ImportService importService;
    private final CurrentUserProvider currentUserProvider;

    public ImportController(ImportService importService, CurrentUserProvider currentUserProvider) {
        this.importService = importService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping(value = "/import/dry-run", consumes = "multipart/form-data")
    public ImportSummary dryRun(@RequestPart("file") MultipartFile file) {
        return importService.dryRun(currentUserProvider.currentUserId(), file.getOriginalFilename(), readBytes(file));
    }

    @PostMapping(value = "/import/apply", consumes = "multipart/form-data")
    public ImportSummary apply(
            @RequestPart("file") MultipartFile file,
            @RequestParam("mode") ImportMode mode,
            @RequestParam(value = "confirmation", required = false) String confirmation) {
        return importService.apply(
                currentUserProvider.currentUserId(), file.getOriginalFilename(), readBytes(file), mode, confirmation);
    }

    @GetMapping("/import/jobs")
    public List<ImportJobResponse> jobs() {
        return importService.jobs(currentUserProvider.currentUserId()).stream()
                .map(ImportJobResponse::from)
                .toList();
    }

    private static byte[] readBytes(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ImportFileException("Envie um arquivo .zip exportado pelo Finanças do Marlus.");
        }
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
