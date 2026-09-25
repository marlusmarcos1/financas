package com.marlus.financas.investment.web;

import com.marlus.financas.investment.domain.InvestmentTransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvestmentTransactionResponse(
        UUID id,
        UUID assetId,
        InvestmentTransactionType type,
        LocalDate date,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal fees,
        BigDecimal amount,
        UUID accountId) {
}
