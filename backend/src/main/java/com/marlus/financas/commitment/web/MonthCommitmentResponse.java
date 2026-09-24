package com.marlus.financas.commitment.web;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record MonthCommitmentResponse(
        YearMonth month,
        BigDecimal installmentsTotal,
        BigDecimal recurringTotal,
        BigDecimal committedTotal,
        List<EndingPlanResponse> endingPlans) {
}
