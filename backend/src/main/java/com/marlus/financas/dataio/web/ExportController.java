package com.marlus.financas.dataio.web;

import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.dataio.service.ExportService;
import java.time.LocalDate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/data")
public class ExportController {

    private final ExportService exportService;
    private final CurrentUserProvider currentUserProvider;

    public ExportController(ExportService exportService, CurrentUserProvider currentUserProvider) {
        this.exportService = exportService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAll() {
        byte[] zip = exportService.exportAll(currentUserProvider.currentUserId());
        String filename = "financas-export-" + LocalDate.now() + ".zip";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(zip);
    }

    @GetMapping("/export/{fileName}")
    public ResponseEntity<byte[]> exportEntity(@PathVariable String fileName) {
        byte[] csv = exportService.exportEntityCsv(currentUserProvider.currentUserId(), fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(csv);
    }
}
