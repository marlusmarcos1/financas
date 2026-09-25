package com.marlus.financas.investment.web;

import com.marlus.financas.investment.domain.InvestmentAssetClass;
import com.marlus.financas.investment.domain.InvestmentPurpose;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InvestmentAssetResponse(
        UUID id,
        String ticker,
        String name,
        InvestmentAssetClass assetClass,
        String subclass,
        String indexer,
        LocalDate maturityDate,
        BigDecimal currentPrice,
        Instant priceUpdatedAt,
        InvestmentPurpose purpose,
        boolean archived) {
}
