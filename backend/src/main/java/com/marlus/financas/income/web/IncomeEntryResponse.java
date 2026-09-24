package com.marlus.financas.income.web;

import com.marlus.financas.income.domain.IncomeEntryStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record IncomeEntryResponse(
        UUID id,
        UUID sourceId,
        UUID accountId,
        String referenceMonth,
        LocalDate receivedOn,
        BigDecimal amount,
        IncomeEntryStatus status) {
}
