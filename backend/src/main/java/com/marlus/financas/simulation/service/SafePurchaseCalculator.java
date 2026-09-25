package com.marlus.financas.simulation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Lógica pura do simulador de compra parcelada segura (seção 7.4). Não acessa banco nem
 * contexto Spring — recebe os dados já projetados e devolve o veredito mês a mês, para poder
 * ser testada com cenários numéricos isolados.
 */
public final class SafePurchaseCalculator {

    private static final BigDecimal WARNING_COMMITMENT_PERCENT = BigDecimal.valueOf(30);
    private static final BigDecimal NOT_RECOMMENDED_COMMITMENT_PERCENT = BigDecimal.valueOf(40);
    private static final BigDecimal WARNING_FREE_BALANCE_FRACTION = BigDecimal.valueOf(0.10);

    private SafePurchaseCalculator() {
    }

    public static SimulationResult simulate(
            BigDecimal baseIncomeMonthly,
            BigDecimal tithePercent,
            BigDecimal installmentLimitPercent,
            List<MonthCommitmentInput> existingCommitments,
            List<BigDecimal> newInstallmentAmounts) {
        if (existingCommitments.size() != newInstallmentAmounts.size()) {
            throw new IllegalArgumentException("Projeção e parcelas simuladas precisam ter o mesmo número de meses.");
        }

        BigDecimal titheDue = percentOf(baseIncomeMonthly, tithePercent);
        BigDecimal warningBalanceThreshold = baseIncomeMonthly.multiply(WARNING_FREE_BALANCE_FRACTION);

        List<MonthSimulation> months = new ArrayList<>();
        boolean anyNotRecommended = false;
        boolean anyWarning = false;

        for (int i = 0; i < existingCommitments.size(); i++) {
            MonthCommitmentInput input = existingCommitments.get(i);
            BigDecimal newInstallment = newInstallmentAmounts.get(i);
            BigDecimal totalInstallments = input.existingInstallments().add(newInstallment);
            BigDecimal commitmentPercent = baseIncomeMonthly.signum() > 0
                    ? totalInstallments
                            .multiply(BigDecimal.valueOf(100))
                            .divide(baseIncomeMonthly, 2, RoundingMode.HALF_EVEN)
                    : BigDecimal.ZERO;
            BigDecimal freeBalance = baseIncomeMonthly
                    .subtract(titheDue)
                    .subtract(input.fixedRecurring())
                    .subtract(totalInstallments)
                    .subtract(input.averageVariableSpend());
            BigDecimal freeBalanceWithoutPurchase = freeBalance.add(newInstallment);

            months.add(new MonthSimulation(
                    input.month(),
                    input.existingInstallments(),
                    newInstallment,
                    input.fixedRecurring(),
                    input.averageVariableSpend(),
                    titheDue,
                    totalInstallments,
                    commitmentPercent,
                    freeBalance,
                    freeBalanceWithoutPurchase));

            if (freeBalance.signum() < 0 || commitmentPercent.compareTo(NOT_RECOMMENDED_COMMITMENT_PERCENT) > 0) {
                anyNotRecommended = true;
            } else if (commitmentPercent.compareTo(installmentLimitPercent) > 0
                    || freeBalance.compareTo(warningBalanceThreshold) < 0) {
                anyWarning = true;
            }
        }

        SimulationVerdict verdict = anyNotRecommended
                ? SimulationVerdict.NOT_RECOMMENDED
                : anyWarning ? SimulationVerdict.WARNING : SimulationVerdict.SAFE;

        BigDecimal maxSafeInstallment =
                maxSafeInstallment(baseIncomeMonthly, titheDue, installmentLimitPercent, existingCommitments);

        return new SimulationResult(verdict, maxSafeInstallment, months);
    }

    /**
     * Maior parcela mensal nova que, somada aos comprometimentos já existentes de cada mês
     * projetado, mantém o comprometimento ≤ limite configurado e o saldo livre ≥ 0 em todos eles.
     */
    private static BigDecimal maxSafeInstallment(
            BigDecimal baseIncomeMonthly,
            BigDecimal titheDue,
            BigDecimal installmentLimitPercent,
            List<MonthCommitmentInput> existingCommitments) {
        BigDecimal limitAmount = percentOf(baseIncomeMonthly, installmentLimitPercent);
        BigDecimal max = null;

        for (MonthCommitmentInput input : existingCommitments) {
            BigDecimal percentConstraint = limitAmount.subtract(input.existingInstallments());
            BigDecimal existingFreeBalance = baseIncomeMonthly
                    .subtract(titheDue)
                    .subtract(input.fixedRecurring())
                    .subtract(input.existingInstallments())
                    .subtract(input.averageVariableSpend());
            BigDecimal monthMax = percentConstraint.min(existingFreeBalance);
            max = max == null ? monthMax : max.min(monthMax);
        }

        return max == null ? BigDecimal.ZERO : max.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal percentOf(BigDecimal base, BigDecimal percent) {
        return base.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN);
    }
}
