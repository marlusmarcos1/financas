package com.marlus.financas.retirement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.marlus.financas.retirement.service.RetirementProjectionCalculator;
import com.marlus.financas.retirement.service.RetirementProjectionResult;
import com.marlus.financas.retirement.service.RetirementYearSnapshot;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RetirementProjectionCalculatorTest {

    @Test
    void fiveHundredMonthlyForThirtyYearsAtEightPercentMatchesSpecAcceptanceCriterion() {
        // Critério de aceite da Fase 7 (seção 15): R$500/mês por 30 anos a 8% a.a. ≈ R$704 mil nominal.
        RetirementProjectionResult result = RetirementProjectionCalculator.project(
                BigDecimal.ZERO, new BigDecimal("500"), BigDecimal.ZERO, 30, new BigDecimal("0.08"));

        assertThat(result.finalBalance()).isCloseTo(new BigDecimal("704000"), within(new BigDecimal("5000")));
        assertThat(result.totalContributed()).isEqualByComparingTo("180000.00"); // 500 * 360
        assertThat(result.interestEarned()).isCloseTo(new BigDecimal("524000"), within(new BigDecimal("5000")));
    }

    @Test
    void higherReturnRateProducesLargerFinalBalance() {
        RetirementProjectionResult pessimistic = RetirementProjectionCalculator.project(
                BigDecimal.ZERO, new BigDecimal("500"), BigDecimal.ZERO, 30, new BigDecimal("0.06"));
        RetirementProjectionResult optimistic = RetirementProjectionCalculator.project(
                BigDecimal.ZERO, new BigDecimal("500"), BigDecimal.ZERO, 30, new BigDecimal("0.10"));

        assertThat(optimistic.finalBalance()).isGreaterThan(pessimistic.finalBalance());
    }

    @Test
    void annualIncreaseGrowsTotalContributedBeyondFlatContribution() {
        RetirementProjectionResult flat = RetirementProjectionCalculator.project(
                BigDecimal.ZERO, new BigDecimal("500"), BigDecimal.ZERO, 10, new BigDecimal("0.08"));
        RetirementProjectionResult increasing = RetirementProjectionCalculator.project(
                BigDecimal.ZERO, new BigDecimal("500"), new BigDecimal("5"), 10, new BigDecimal("0.08"));

        assertThat(increasing.totalContributed()).isGreaterThan(flat.totalContributed());
        assertThat(increasing.finalBalance()).isGreaterThan(flat.finalBalance());
    }

    @Test
    void existingBalanceCompoundsAlongsideContributions() {
        RetirementProjectionResult withoutBalance = RetirementProjectionCalculator.project(
                BigDecimal.ZERO, new BigDecimal("500"), BigDecimal.ZERO, 5, new BigDecimal("0.08"));
        RetirementProjectionResult withBalance = RetirementProjectionCalculator.project(
                new BigDecimal("50000"), new BigDecimal("500"), BigDecimal.ZERO, 5, new BigDecimal("0.08"));

        assertThat(withBalance.finalBalance()).isGreaterThan(withoutBalance.finalBalance().add(new BigDecimal("50000")));
    }

    @Test
    void realAnnualRateIsLowerThanNominalWhenInflationIsPositive() {
        BigDecimal realRate = RetirementProjectionCalculator.realAnnualRate(new BigDecimal("0.08"), new BigDecimal("0.045"));

        // (1.08 / 1.045) - 1 ~= 0.033493
        assertThat(realRate).isCloseTo(new BigDecimal("0.0335"), within(new BigDecimal("0.001")));
    }

    @Test
    void yearlySnapshotsCoverTheWholeHorizonAndEndAtTheFinalBalance() {
        List<RetirementYearSnapshot> snapshots = RetirementProjectionCalculator.projectYearlyBalances(
                BigDecimal.ZERO, new BigDecimal("500"), BigDecimal.ZERO, 30, new BigDecimal("0.08"));
        RetirementProjectionResult result = RetirementProjectionCalculator.project(
                BigDecimal.ZERO, new BigDecimal("500"), BigDecimal.ZERO, 30, new BigDecimal("0.08"));

        assertThat(snapshots).hasSize(30);
        assertThat(snapshots.get(0).year()).isEqualTo(1);
        assertThat(snapshots.get(29).year()).isEqualTo(30);
        assertThat(snapshots.get(29).balance()).isEqualByComparingTo(result.finalBalance());
    }
}
