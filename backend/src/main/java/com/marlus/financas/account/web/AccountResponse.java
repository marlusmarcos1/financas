package com.marlus.financas.account.web;

import com.marlus.financas.account.domain.AccountPurpose;
import com.marlus.financas.account.domain.AccountType;
import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String name,
        AccountType type,
        String institution,
        BigDecimal initialBalance,
        AccountPurpose purpose,
        boolean archived) {
}
