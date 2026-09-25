package com.marlus.financas.networth.web;

import java.math.BigDecimal;
import java.util.List;

public record NetWorthResponse(
        BigDecimal accountsTotal, BigDecimal investmentsTotal, BigDecimal netWorth, List<AccountBalanceResponse> accounts) {
}
