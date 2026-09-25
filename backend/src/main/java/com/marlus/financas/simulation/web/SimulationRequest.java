package com.marlus.financas.simulation.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SimulationRequest(
        @NotBlank(message = "Informe a descrição.") String description,
        @NotNull(message = "Informe o valor total.") @Positive(message = "Deve ser maior que zero.")
                BigDecimal totalAmount,
        @NotNull(message = "Informe o número de parcelas.") @Min(value = 1, message = "Pelo menos 1 parcela.")
                Short installmentCount,
        @NotNull(message = "Informe o cartão.") UUID cardId,
        @NotNull(message = "Informe a data da compra.") LocalDate purchaseDate,
        @PositiveOrZero(message = "Não pode ser negativo.") BigDecimal interestRateMonthly) {
}
