package com.marlus.financas.investment.web;

import com.marlus.financas.investment.domain.InvestmentTransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvestmentTransactionRequest(
        @NotNull(message = "Informe o tipo da operação.") InvestmentTransactionType type,
        @NotNull(message = "Informe a data.") LocalDate date,
        @PositiveOrZero(message = "Não pode ser negativa.") BigDecimal quantity,
        @PositiveOrZero(message = "Não pode ser negativo.") BigDecimal unitPrice,
        @PositiveOrZero(message = "Não pode ser negativa.") BigDecimal fees,
        @NotNull(message = "Informe o valor.") @PositiveOrZero(message = "Não pode ser negativo.") BigDecimal amount,
        UUID accountId) {
}
