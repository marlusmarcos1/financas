package com.marlus.financas.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.marlus.financas.simulation.service.MonthCommitmentInput;
import com.marlus.financas.simulation.service.SafePurchaseCalculator;
import com.marlus.financas.simulation.service.SimulationResult;
import com.marlus.financas.simulation.service.SimulationVerdict;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class SafePurchaseCalculatorTest {

    private static MonthCommitmentInput month(int offset, String existingInstallments, String fixedRecurring, String averageVariable) {
        return new MonthCommitmentInput(
                YearMonth.now().plusMonths(offset),
                new BigDecimal(existingInstallments),
                new BigDecimal(fixedRecurring),
                new BigDecimal(averageVariable));
    }

    @Test
    void lowCommitmentAndPositiveBalanceIsSafe() {
        List<MonthCommitmentInput> months = List.of(month(0, "0", "200", "300"), month(1, "0", "200", "300"), month(2, "0", "200", "300"));
        List<BigDecimal> newInstallments = List.of(new BigDecimal("500"), new BigDecimal("500"), new BigDecimal("500"));

        SimulationResult result = SafePurchaseCalculator.simulate(
                new BigDecimal("5000"), new BigDecimal("10"), new BigDecimal("30"), months, newInstallments);

        assertThat(result.verdict()).isEqualTo(SimulationVerdict.SAFE);
        assertThat(result.months().get(0).commitmentPercent()).isEqualByComparingTo("10.00");
        assertThat(result.months().get(0).freeBalance()).isEqualByComparingTo("3500.00");
        // Teto de 30% -> R$1500 de parcelas; saldo livre sem a compra é R$4000 -> restringe em R$1500.
        assertThat(result.maxSafeInstallmentToday()).isEqualByComparingTo("1500.00");
    }

    @Test
    void commitmentAboveThirtyPercentIsWarningWhenBalanceStaysPositive() {
        List<MonthCommitmentInput> months = List.of(month(0, "0", "200", "300"));
        List<BigDecimal> newInstallments = List.of(new BigDecimal("1600")); // 32% de 5000

        SimulationResult result = SafePurchaseCalculator.simulate(
                new BigDecimal("5000"), new BigDecimal("10"), new BigDecimal("30"), months, newInstallments);

        assertThat(result.verdict()).isEqualTo(SimulationVerdict.WARNING);
        assertThat(result.months().get(0).commitmentPercent()).isEqualByComparingTo("32.00");
        assertThat(result.months().get(0).freeBalance()).isEqualByComparingTo("2400.00");
    }

    @Test
    void negativeFreeBalanceIsNotRecommendedEvenWithinCommitmentLimit() {
        // Comprometimento fica em exatos 30% (não dispara o alerta de %), mas o saldo livre fica negativo.
        List<MonthCommitmentInput> months = List.of(month(0, "0", "500", "1500"));
        List<BigDecimal> newInstallments = List.of(new BigDecimal("900"));

        SimulationResult result = SafePurchaseCalculator.simulate(
                new BigDecimal("3000"), new BigDecimal("10"), new BigDecimal("30"), months, newInstallments);

        assertThat(result.months().get(0).commitmentPercent()).isEqualByComparingTo("30.00");
        assertThat(result.months().get(0).freeBalance()).isEqualByComparingTo("-200.00");
        assertThat(result.verdict()).isEqualTo(SimulationVerdict.NOT_RECOMMENDED);
    }

    @Test
    void commitmentAboveFortyPercentIsNotRecommended() {
        List<MonthCommitmentInput> months = List.of(month(0, "0", "0", "0"));
        List<BigDecimal> newInstallments = List.of(new BigDecimal("2100")); // 42% de 5000

        SimulationResult result = SafePurchaseCalculator.simulate(
                new BigDecimal("5000"), new BigDecimal("10"), new BigDecimal("30"), months, newInstallments);

        assertThat(result.verdict()).isEqualTo(SimulationVerdict.NOT_RECOMMENDED);
    }

    @Test
    void overallVerdictReflectsTheWorstMonthInTheHorizon() {
        List<MonthCommitmentInput> months = List.of(
                month(0, "0", "200", "300"), // seguro
                month(1, "0", "200", "300") // vai receber uma parcela que estoura 30%
        );
        List<BigDecimal> newInstallments = List.of(new BigDecimal("500"), new BigDecimal("1600"));

        SimulationResult result = SafePurchaseCalculator.simulate(
                new BigDecimal("5000"), new BigDecimal("10"), new BigDecimal("30"), months, newInstallments);

        assertThat(result.verdict()).isEqualTo(SimulationVerdict.WARNING);
    }

    @Test
    void lowFreeBalanceBelowTenPercentOfIncomeTriggersWarningEvenWithLowCommitmentPercent() {
        // Parcela de apenas 20% da renda (bem abaixo do teto de 30%), mas gastos fixos altos
        // deixam o saldo livre positivo e abaixo de 10% da renda base (R$500).
        List<MonthCommitmentInput> months = List.of(month(0, "0", "3100", "0"));
        List<BigDecimal> newInstallments = List.of(new BigDecimal("1000"));

        SimulationResult result = SafePurchaseCalculator.simulate(
                new BigDecimal("5000"), new BigDecimal("10"), new BigDecimal("30"), months, newInstallments);

        assertThat(result.months().get(0).commitmentPercent()).isEqualByComparingTo("20.00");
        assertThat(result.months().get(0).freeBalance()).isEqualByComparingTo("400.00");
        assertThat(result.verdict()).isEqualTo(SimulationVerdict.WARNING);
    }
}
