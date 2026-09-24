package com.marlus.financas.tithe.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TithePaymentRequest(
        @NotNull(message = "Informe o valor pago.") @Positive(message = "O valor pago deve ser maior que zero.")
                BigDecimal amount,
        @NotNull(message = "Informe a data do pagamento.") LocalDate paidOn) {
}
