package com.marlus.financas.recurring.web;

import com.marlus.financas.recurring.domain.RecurringFrequency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecurringRuleResponse(
        UUID id,
        String description,
        BigDecimal amount,
        boolean amountIsVariable,
        RecurringFrequency frequency,
        short dayOfMonth,
        LocalDate startDate,
        LocalDate endDate,
        UUID categoryId,
        UUID cardId,
        UUID accountId,
        boolean active) {
}
