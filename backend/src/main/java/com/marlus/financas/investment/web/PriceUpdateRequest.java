package com.marlus.financas.investment.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record PriceUpdateRequest(
        @NotNull(message = "Informe o preço.") @PositiveOrZero(message = "Não pode ser negativo.") BigDecimal price) {
}
