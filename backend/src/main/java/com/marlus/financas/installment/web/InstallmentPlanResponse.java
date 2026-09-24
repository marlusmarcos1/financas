package com.marlus.financas.installment.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

public record InstallmentPlanResponse(
        UUID id,
        UUID cardId,
        String description,
        LocalDate purchaseDate,
        BigDecimal totalAmount,
        short installmentCount,
        BigDecimal installmentAmount,
        short firstInstallmentNumber,
        YearMonth firstInvoiceMonth,
        YearMonth lastInstallmentMonth,
        BigDecimal interestRateMonthly,
        BigDecimal totalToPay,
        BigDecimal totalInterest,
        BigDecimal effectiveRate,
        UUID categoryId) {
}
