package com.marlus.financas.account.web;

import com.marlus.financas.account.domain.AccountPurpose;
import com.marlus.financas.account.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AccountRequest(
        @NotBlank(message = "Informe o nome da conta.") String name,
        @NotNull(message = "Informe o tipo da conta.") AccountType type,
        String institution,
        @NotNull(message = "Informe o saldo inicial.") BigDecimal initialBalance,
        @NotNull(message = "Informe a finalidade da conta.") AccountPurpose purpose) {
}
