package com.marlus.financas.retirement.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RetirementPlanResponse(
        UUID id,
        BigDecimal monthlyContribution,
        BigDecimal contributionAnnualIncreasePercent,
        LocalDate startDate,
        short horizonYears,
        BigDecimal expectedReturnNominalAnnual,
        BigDecimal expectedInflationAnnual,
        BigDecimal currentBalance) {
}
