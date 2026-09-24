package com.marlus.financas.category.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record BudgetRequest(
        @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "Competência deve estar no formato AAAA-MM.")
                String month,
        @NotNull(message = "Informe o valor do teto.") @PositiveOrZero(message = "O teto não pode ser negativo.")
                BigDecimal limitAmount) {
}
