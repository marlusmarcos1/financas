package com.marlus.financas.tithe.web;

import com.marlus.financas.tithe.domain.TitheStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TitheLedgerResponse(
        UUID id,
        String referenceMonth,
        BigDecimal baseAmount,
        BigDecimal percent,
        BigDecimal dueAmount,
        BigDecimal paidAmount,
        LocalDate paidOn,
        TitheStatus status) {
}
