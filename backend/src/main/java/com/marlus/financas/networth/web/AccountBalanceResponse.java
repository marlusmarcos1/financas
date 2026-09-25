package com.marlus.financas.networth.web;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountBalanceResponse(UUID accountId, String name, String purpose, BigDecimal balance) {
}
