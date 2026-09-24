package com.marlus.financas.invoice.service;

import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.stereotype.Component;

/** Implementação padrão: ciclo de fatura mensal, com clamp de dias em meses mais curtos. */
@Component
public class MonthlyInvoiceCycleCalculator implements InvoiceCycleCalculator {

    @Override
    public InvoiceCycle calculate(LocalDate purchaseDate, short closingDay, short dueDay) {
        YearMonth purchaseMonth = YearMonth.from(purchaseDate);
        LocalDate closingInPurchaseMonth = dayOrLastOfMonth(purchaseMonth, closingDay);

        YearMonth referenceMonth =
                purchaseDate.isAfter(closingInPurchaseMonth) ? purchaseMonth.plusMonths(1) : purchaseMonth;

        LocalDate closingDate = dayOrLastOfMonth(referenceMonth, closingDay);

        YearMonth dueMonth = dueDay < closingDay ? referenceMonth.plusMonths(1) : referenceMonth;
        LocalDate dueDate = dayOrLastOfMonth(dueMonth, dueDay);

        return new InvoiceCycle(referenceMonth, closingDate, dueDate);
    }

    private static LocalDate dayOrLastOfMonth(YearMonth month, int day) {
        return month.atDay(Math.min(day, month.lengthOfMonth()));
    }
}
