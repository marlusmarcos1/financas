package com.marlus.financas.networth.web;

import java.math.BigDecimal;

public record EmergencyReserveResponse(
        BigDecimal currentReserve,
        BigDecimal averageEssentialMonthlyExpense,
        int emergencyMonthsTarget,
        BigDecimal targetAmount,
        BigDecimal monthsOfCoverage) {
}
