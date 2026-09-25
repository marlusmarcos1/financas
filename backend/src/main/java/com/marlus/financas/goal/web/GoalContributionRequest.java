package com.marlus.financas.goal.web;

import com.marlus.financas.goal.domain.GoalContributionSource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalContributionRequest(
        @NotNull(message = "Informe a data.") LocalDate date,
        @NotNull(message = "Informe o valor.") @PositiveOrZero(message = "Não pode ser negativo.") BigDecimal amount,
        @NotNull(message = "Informe a origem.") GoalContributionSource source) {
}
