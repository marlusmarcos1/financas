package com.marlus.financas.creditcard.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

public record CreditCardRequest(
        @NotBlank(message = "Informe o nome do cartão.") String name,
        String issuer,
        @NotNull(message = "Informe o limite do cartão.") @PositiveOrZero(message = "O limite não pode ser negativo.")
                BigDecimal creditLimit,
        @NotNull(message = "Informe o dia de fechamento.") @Min(value = 1, message = "O dia de fechamento deve ser entre 1 e 31.")
                @Max(value = 31, message = "O dia de fechamento deve ser entre 1 e 31.") Short closingDay,
        @NotNull(message = "Informe o dia de vencimento.") @Min(value = 1, message = "O dia de vencimento deve ser entre 1 e 31.")
                @Max(value = 31, message = "O dia de vencimento deve ser entre 1 e 31.") Short dueDay,
        UUID defaultPaymentAccountId,
        String color) {
}
