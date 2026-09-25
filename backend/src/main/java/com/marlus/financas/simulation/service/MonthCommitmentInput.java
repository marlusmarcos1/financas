package com.marlus.financas.simulation.service;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Dados já comprometidos de um mês da projeção, antes de somar a nova parcela simulada.
 * `averageVariableSpend` é a mesma média móvel para todos os meses (calculada uma vez sobre o
 * histórico real).
 */
public record MonthCommitmentInput(
        YearMonth month, BigDecimal existingInstallments, BigDecimal fixedRecurring, BigDecimal averageVariableSpend) {
}
