package com.marlus.financas.dataio.service;

import java.util.List;

public record ImportSummary(
        boolean applied,
        com.marlus.financas.dataio.domain.ImportMode mode,
        List<FileSummary> files,
        List<RowError> errors,
        int totalCreated,
        int totalUpdated,
        int totalErrors) {
}
