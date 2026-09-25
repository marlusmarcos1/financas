package com.marlus.financas.dataio.web;

import com.marlus.financas.dataio.domain.ImportJob;
import com.marlus.financas.dataio.domain.ImportJobStatus;
import com.marlus.financas.dataio.domain.ImportMode;
import java.time.Instant;
import java.util.UUID;

public record ImportJobResponse(
        UUID id, String filename, ImportMode mode, ImportJobStatus status, String summaryJson, Instant createdAt) {

    public static ImportJobResponse from(ImportJob job) {
        return new ImportJobResponse(
                job.getId(), job.getFilename(), job.getMode(), job.getStatus(), job.getSummaryJson(), job.getCreatedAt());
    }
}
