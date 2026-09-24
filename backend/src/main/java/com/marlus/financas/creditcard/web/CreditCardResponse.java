package com.marlus.financas.creditcard.web;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditCardResponse(
        UUID id,
        String name,
        String issuer,
        BigDecimal creditLimit,
        short closingDay,
        short dueDay,
        UUID defaultPaymentAccountId,
        String color,
        boolean archived) {
}
