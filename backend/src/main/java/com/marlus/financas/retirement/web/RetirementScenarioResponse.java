package com.marlus.financas.retirement.web;

import java.math.BigDecimal;
import java.util.List;

public record RetirementScenarioResponse(
        String label,
        BigDecimal annualRate,
        BigDecimal finalBalanceNominal,
        BigDecimal totalContributed,
        BigDecimal interestEarned,
        BigDecimal finalBalanceReal,
        List<YearSnapshotResponse> yearlyBalances) {
}
