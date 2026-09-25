package com.marlus.financas.investment.web;

import com.marlus.financas.investment.domain.InvestmentAssetClass;
import com.marlus.financas.investment.domain.InvestmentPurpose;
import java.math.BigDecimal;
import java.util.UUID;

public record AssetPositionResponse(
        UUID assetId,
        String ticker,
        String name,
        InvestmentAssetClass assetClass,
        InvestmentPurpose purpose,
        BigDecimal quantity,
        BigDecimal averagePrice,
        BigDecimal currentPrice,
        BigDecimal totalCost,
        BigDecimal currentValue,
        BigDecimal unrealizedGain,
        BigDecimal totalIncome,
        BigDecimal yieldOnCost) {
}
