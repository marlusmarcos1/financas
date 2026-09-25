package com.marlus.financas.retirement.web;

import java.math.BigDecimal;

public record YearSnapshotResponse(int year, BigDecimal balance) {
}
