package com.marlus.financas.simulation.service;

import java.math.BigDecimal;
import java.util.List;

public record SimulationResult(SimulationVerdict verdict, BigDecimal maxSafeInstallmentToday, List<MonthSimulation> months) {
}
