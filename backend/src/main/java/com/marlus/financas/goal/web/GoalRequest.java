package com.marlus.financas.goal.web;

import com.marlus.financas.goal.domain.GoalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GoalRequest(
        @NotBlank(message = "Informe o nome da meta.") String name,
        @NotNull(message = "Informe o tipo.") GoalType type,
        @NotNull(message = "Informe o valor alvo.") @PositiveOrZero(message = "Não pode ser negativo.")
                BigDecimal targetAmount,
        LocalDate targetDate,
        UUID linkedAccountId,
        @PositiveOrZero(message = "Não pode ser negativo.") BigDecimal monthlyContributionPlanned,
        short priority,
        String notes) {
}
