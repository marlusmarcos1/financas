package com.marlus.financas.income.web;

import com.marlus.financas.income.domain.IncomeRecurrence;
import com.marlus.financas.income.domain.IncomeSourceType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record IncomeSourceResponse(
        UUID id,
        String name,
        IncomeSourceType type,
        IncomeRecurrence recurrence,
        BigDecimal expectedAmount,
        Short payDay,
        LocalDate startDate,
        LocalDate endDate,
        Short expectedMonths,
        boolean titheApplies,
        boolean countsInBaseBudget) {
}
