package com.marlus.financas.invoice.web;

import com.marlus.financas.invoice.domain.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID cardId,
        String referenceMonth,
        LocalDate closingDate,
        LocalDate dueDate,
        InvoiceStatus status,
        BigDecimal total,
        BigDecimal paidAmount,
        LocalDate paidOn,
        UUID paidFromAccountId) {
}
