package com.marlus.financas.simulation.web;

import com.marlus.financas.simulation.service.SimulationVerdict;
import java.math.BigDecimal;
import java.util.List;

public record SimulationResponse(
        SimulationVerdict verdict,
        BigDecimal installmentAmount,
        BigDecimal maxSafeInstallmentToday,
        CardLimitImpactResponse cardLimitImpact,
        List<MonthSimulationResponse> months,
        List<String> suggestions) {
}
