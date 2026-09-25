package com.marlus.financas.allocation.web;

import com.marlus.financas.investment.domain.InvestmentAssetClass;
import java.math.BigDecimal;

public record AllocationComparisonResponse(
        InvestmentAssetClass assetClass,
        BigDecimal targetPercent,
        BigDecimal currentPercent,
        BigDecimal currentValue,
        BigDecimal differencePercent) {
}
