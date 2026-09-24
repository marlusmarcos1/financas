package com.marlus.financas.recurring.web;

import com.marlus.financas.recurring.domain.RecurringFrequency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecurringRuleRequest(
        @NotBlank(message = "Informe a descrição.") String description,
        @NotNull(message = "Informe o valor.") @PositiveOrZero(message = "O valor não pode ser negativo.")
                BigDecimal amount,
        boolean amountIsVariable,
        @NotNull(message = "Informe a frequência.") RecurringFrequency frequency,
        @NotNull(message = "Informe o dia do mês.") @Min(value = 1, message = "Entre 1 e 31.")
                @Max(value = 31, message = "Entre 1 e 31.") Short dayOfMonth,
        @NotNull(message = "Informe a data de início.") LocalDate startDate,
        LocalDate endDate,
        @NotNull(message = "Informe a categoria.") UUID categoryId,
        UUID cardId,
        UUID accountId,
        boolean active) {
}
