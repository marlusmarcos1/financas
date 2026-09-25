package com.marlus.financas.networth.web;

import java.math.BigDecimal;
import java.time.YearMonth;

public record NetWorthPointResponse(
        YearMonth month, BigDecimal accountsTotal, BigDecimal investedCapital, BigDecimal total) {
}
