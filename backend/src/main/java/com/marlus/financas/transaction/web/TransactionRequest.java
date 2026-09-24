package com.marlus.financas.transaction.web;

import com.marlus.financas.transaction.domain.TransactionKind;
import com.marlus.financas.transaction.domain.TransactionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionRequest(
        @NotNull(message = "Informe o tipo do lançamento.") TransactionKind kind,
        @NotBlank(message = "Informe a descrição.") String description,
        @NotNull(message = "Informe o valor.") @PositiveOrZero(message = "O valor não pode ser negativo.")
                BigDecimal amount,
        @NotNull(message = "Informe a data.") LocalDate date,
        UUID categoryId,
        UUID accountId,
        UUID cardId,
        TransactionStatus status,
        String notes) {
}
