package com.marlus.financas.income.web;

import com.marlus.financas.income.domain.IncomeRecurrence;
import com.marlus.financas.income.domain.IncomeSourceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record IncomeSourceRequest(
        @NotBlank(message = "Informe o nome da fonte de receita.") String name,
        @NotNull(message = "Informe o tipo.") IncomeSourceType type,
        @NotNull(message = "Informe a recorrência.") IncomeRecurrence recurrence,
        @NotNull(message = "Informe o valor esperado.") @PositiveOrZero(message = "Não pode ser negativo.")
                BigDecimal expectedAmount,
        @Min(value = 1, message = "Entre 1 e 31.") @Max(value = 31, message = "Entre 1 e 31.") Short payDay,
        LocalDate startDate,
        LocalDate endDate,
        Short expectedMonths,
        boolean titheApplies,
        boolean countsInBaseBudget) {
}
