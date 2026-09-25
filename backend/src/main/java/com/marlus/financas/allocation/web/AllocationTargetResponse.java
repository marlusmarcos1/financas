package com.marlus.financas.allocation.web;

import com.marlus.financas.investment.domain.InvestmentAssetClass;
import com.marlus.financas.investment.domain.InvestmentPurpose;
import java.math.BigDecimal;
import java.util.UUID;

public record AllocationTargetResponse(
        UUID id, InvestmentPurpose purpose, InvestmentAssetClass assetClass, BigDecimal targetPercent) {
}
