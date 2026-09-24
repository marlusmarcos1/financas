package com.marlus.financas.invoice.service;

import java.time.LocalDate;
import java.time.YearMonth;

/** Competência, data de fechamento e data de vencimento de uma fatura. */
public record InvoiceCycle(YearMonth referenceMonth, LocalDate closingDate, LocalDate dueDate) {
}
