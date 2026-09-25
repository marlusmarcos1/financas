package com.marlus.financas.retirement.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Projeção de aposentadoria por simulação mês a mês (seção 7.6): saldo cresce à taxa mensal
 * equivalente da taxa anual informada — {@code (1+i_anual)^(1/12) - 1} — e recebe o aporte do
 * mês; o aporte é reajustado ao final de cada ano completo pelo percentual de reajuste anual.
 * Para o valor real, a mesma simulação roda com a taxa real equivalente
 * {@code (1+nominal)/(1+inflação) - 1} no lugar da taxa nominal.
 */
public final class RetirementProjectionCalculator {

    private RetirementProjectionCalculator() {
    }

    public static RetirementProjectionResult project(
            BigDecimal currentBalance,
            BigDecimal monthlyContribution,
            BigDecimal contributionAnnualIncreasePercent,
            int horizonYears,
            BigDecimal annualReturnRate) {
        BigDecimal monthlyRate = monthlyRateFromAnnual(annualReturnRate);
        BigDecimal onePlusIncrease =
                BigDecimal.ONE.add(contributionAnnualIncreasePercent.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_EVEN));

        BigDecimal balance = currentBalance;
        BigDecimal contribution = monthlyContribution;
        BigDecimal totalContributed = BigDecimal.ZERO;

        for (int year = 0; year < horizonYears; year++) {
            for (int month = 0; month < 12; month++) {
                balance = balance.multiply(BigDecimal.ONE.add(monthlyRate)).add(contribution);
                totalContributed = totalContributed.add(contribution);
            }
            contribution = contribution.multiply(onePlusIncrease);
        }

        BigDecimal interestEarned = balance.subtract(currentBalance).subtract(totalContributed);

        return new RetirementProjectionResult(
                balance.setScale(2, RoundingMode.HALF_EVEN),
                totalContributed.setScale(2, RoundingMode.HALF_EVEN),
                interestEarned.setScale(2, RoundingMode.HALF_EVEN));
    }

    /** Mesma simulação, mas retornando o saldo ao final de cada ano — para o gráfico de evolução. */
    public static List<RetirementYearSnapshot> projectYearlyBalances(
            BigDecimal currentBalance,
            BigDecimal monthlyContribution,
            BigDecimal contributionAnnualIncreasePercent,
            int horizonYears,
            BigDecimal annualReturnRate) {
        BigDecimal monthlyRate = monthlyRateFromAnnual(annualReturnRate);
        BigDecimal onePlusIncrease =
                BigDecimal.ONE.add(contributionAnnualIncreasePercent.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_EVEN));

        BigDecimal balance = currentBalance;
        BigDecimal contribution = monthlyContribution;
        List<RetirementYearSnapshot> snapshots = new ArrayList<>();

        for (int year = 0; year < horizonYears; year++) {
            for (int month = 0; month < 12; month++) {
                balance = balance.multiply(BigDecimal.ONE.add(monthlyRate)).add(contribution);
            }
            snapshots.add(new RetirementYearSnapshot(year + 1, balance.setScale(2, RoundingMode.HALF_EVEN)));
            contribution = contribution.multiply(onePlusIncrease);
        }
        return snapshots;
    }

    /** Taxa real equivalente: (1+nominal)/(1+inflação) - 1. */
    public static BigDecimal realAnnualRate(BigDecimal nominalAnnualRate, BigDecimal inflationAnnualRate) {
        return BigDecimal.ONE
                .add(nominalAnnualRate)
                .divide(BigDecimal.ONE.add(inflationAnnualRate), 10, RoundingMode.HALF_EVEN)
                .subtract(BigDecimal.ONE);
    }

    private static BigDecimal monthlyRateFromAnnual(BigDecimal annualRate) {
        double monthly = Math.pow(1 + annualRate.doubleValue(), 1.0 / 12) - 1;
        return BigDecimal.valueOf(monthly);
    }
}
