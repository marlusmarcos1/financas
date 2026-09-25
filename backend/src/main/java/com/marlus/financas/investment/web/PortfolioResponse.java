package com.marlus.financas.investment.web;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioResponse(List<AssetPositionResponse> positions, BigDecimal totalCurrentValue, BigDecimal totalCost) {
}
