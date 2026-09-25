package com.marlus.financas.investment.service;

import java.math.BigDecimal;

public record Position(
        BigDecimal quantity,
        BigDecimal averagePrice,
        BigDecimal totalCost,
        BigDecimal totalIncome,
        BigDecimal realizedGain) {
}
