package com.marlus.financas.transaction.web;

import com.marlus.financas.transaction.domain.TransactionKind;
import com.marlus.financas.transaction.domain.TransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionKind kind,
        String description,
        BigDecimal amount,
        LocalDate date,
        UUID categoryId,
        UUID accountId,
        UUID cardId,
        UUID invoiceId,
        TransactionStatus status,
        UUID installmentPlanId,
        Short installmentNumber,
        UUID recurringRuleId,
        String notes) {
}
