package com.marlus.financas.simulation.web;

import java.math.BigDecimal;

public record CardLimitImpactResponse(BigDecimal creditLimit, BigDecimal availableLimitBefore, BigDecimal availableLimitAfter) {
}
