package com.marlus.financas.retirement.service;

import java.math.BigDecimal;

public record RetirementProjectionResult(BigDecimal finalBalance, BigDecimal totalContributed, BigDecimal interestEarned) {
}
