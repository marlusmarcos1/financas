package com.marlus.financas.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import com.marlus.financas.invoice.service.InvoiceCycle;
import com.marlus.financas.invoice.service.MonthlyInvoiceCycleCalculator;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class InvoiceCycleCalculatorTest {

    private final MonthlyInvoiceCycleCalculator calculator = new MonthlyInvoiceCycleCalculator();

    @Test
    void purchaseBeforeClosingDayGoesToCurrentMonthInvoice() {
        // Fechamento dia 10, compra dia 5 -> fatura do mesmo mês.
        InvoiceCycle cycle = calculator.calculate(LocalDate.of(2026, 3, 5), (short) 10, (short) 20);

        assertThat(cycle.referenceMonth()).isEqualTo(YearMonth.of(2026, 3));
        assertThat(cycle.closingDate()).isEqualTo(LocalDate.of(2026, 3, 10));
    }

    @Test
    void purchaseOnClosingDayGoesToCurrentMonthInvoice() {
        InvoiceCycle cycle = calculator.calculate(LocalDate.of(2026, 3, 10), (short) 10, (short) 20);

        assertThat(cycle.referenceMonth()).isEqualTo(YearMonth.of(2026, 3));
    }

    @Test
    void purchaseAfterClosingDayGoesToNextMonthInvoice() {
        // Fechamento dia 10, compra dia 11 -> fatura do mês seguinte.
        InvoiceCycle cycle = calculator.calculate(LocalDate.of(2026, 3, 11), (short) 10, (short) 20);

        assertThat(cycle.referenceMonth()).isEqualTo(YearMonth.of(2026, 4));
        assertThat(cycle.closingDate()).isEqualTo(LocalDate.of(2026, 4, 10));
    }

    @Test
    void dueDateFallsInSameMonthAsReferenceWhenDueDayIsNotBeforeClosingDay() {
        // Fechamento dia 5, vencimento dia 12 (12 >= 5) -> vencimento na competência.
        InvoiceCycle cycle = calculator.calculate(LocalDate.of(2026, 1, 10), (short) 5, (short) 12);

        assertThat(cycle.referenceMonth()).isEqualTo(YearMonth.of(2026, 2));
        assertThat(cycle.dueDate()).isEqualTo(LocalDate.of(2026, 2, 12));
    }

    @Test
    void dueDateFallsInNextMonthWhenDueDayIsBeforeClosingDay() {
        // Fechamento dia 28, vencimento dia 5 (5 < 28) -> vencimento no mês seguinte à competência.
        InvoiceCycle cycle = calculator.calculate(LocalDate.of(2026, 1, 20), (short) 28, (short) 5);

        assertThat(cycle.referenceMonth()).isEqualTo(YearMonth.of(2026, 1));
        assertThat(cycle.closingDate()).isEqualTo(LocalDate.of(2026, 1, 28));
        assertThat(cycle.dueDate()).isEqualTo(LocalDate.of(2026, 2, 5));
    }

    @Test
    void closingDayIsClampedInShorterMonths() {
        // Fechamento dia 31 em fevereiro (28 dias em 2026, não bissexto) -> cai no dia 28.
        InvoiceCycle cycle = calculator.calculate(LocalDate.of(2026, 2, 1), (short) 31, (short) 10);

        assertThat(cycle.referenceMonth()).isEqualTo(YearMonth.of(2026, 2));
        assertThat(cycle.closingDate()).isEqualTo(LocalDate.of(2026, 2, 28));
    }
}
