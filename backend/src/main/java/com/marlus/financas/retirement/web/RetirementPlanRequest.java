package com.marlus.financas.retirement.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RetirementPlanRequest(
        @NotNull(message = "Informe o aporte mensal.") @PositiveOrZero(message = "Não pode ser negativo.")
                BigDecimal monthlyContribution,
        @NotNull(message = "Informe o reajuste anual do aporte.") @PositiveOrZero(message = "Não pode ser negativo.")
                BigDecimal contributionAnnualIncreasePercent,
        @NotNull(message = "Informe a data de início.") LocalDate startDate,
        @NotNull(message = "Informe o horizonte em anos.") @Min(value = 1, message = "Pelo menos 1 ano.")
                Short horizonYears,
        @NotNull(message = "Informe o retorno nominal esperado.") BigDecimal expectedReturnNominalAnnual,
        @NotNull(message = "Informe a inflação esperada.") BigDecimal expectedInflationAnnual,
        @NotNull(message = "Informe o saldo atual.") @PositiveOrZero(message = "Não pode ser negativo.")
                BigDecimal currentBalance) {
}
