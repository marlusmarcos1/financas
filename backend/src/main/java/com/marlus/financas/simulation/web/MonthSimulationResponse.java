package com.marlus.financas.simulation.web;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthSimulationResponse(
        YearMonth month,
        BigDecimal existingInstallments,
        BigDecimal newInstallment,
        BigDecimal fixedRecurring,
        BigDecimal averageVariableSpend,
        BigDecimal titheDue,
        BigDecimal totalCommittedInstallments,
        BigDecimal commitmentPercent,
        BigDecimal freeBalance,
        BigDecimal freeBalanceWithoutPurchase) {
}
