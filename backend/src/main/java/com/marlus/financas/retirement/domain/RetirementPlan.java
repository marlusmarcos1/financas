package com.marlus.financas.retirement.domain;

import com.marlus.financas.common.UserOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "retirement_plan")
public class RetirementPlan extends UserOwnedEntity {

    @Id
    private UUID id;

    @Column(name = "monthly_contribution", nullable = false)
    private BigDecimal monthlyContribution;

    @Column(name = "contribution_annual_increase_percent", nullable = false)
    private BigDecimal contributionAnnualIncreasePercent;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "horizon_years", nullable = false)
    private short horizonYears;

    @Column(name = "expected_return_nominal_annual", nullable = false)
    private BigDecimal expectedReturnNominalAnnual;

    @Column(name = "expected_inflation_annual", nullable = false)
    private BigDecimal expectedInflationAnnual;

    @Column(name = "current_balance", nullable = false)
    private BigDecimal currentBalance;

    protected RetirementPlan() {
    }

    public RetirementPlan(
            UUID id,
            UUID userId,
            BigDecimal monthlyContribution,
            BigDecimal contributionAnnualIncreasePercent,
            LocalDate startDate,
            short horizonYears,
            BigDecimal expectedReturnNominalAnnual,
            BigDecimal expectedInflationAnnual,
            BigDecimal currentBalance) {
        super(userId);
        this.id = id;
        this.monthlyContribution = monthlyContribution;
        this.contributionAnnualIncreasePercent = contributionAnnualIncreasePercent;
        this.startDate = startDate;
        this.horizonYears = horizonYears;
        this.expectedReturnNominalAnnual = expectedReturnNominalAnnual;
        this.expectedInflationAnnual = expectedInflationAnnual;
        this.currentBalance = currentBalance;
    }

    public UUID getId() {
        return id;
    }

    public BigDecimal getMonthlyContribution() {
        return monthlyContribution;
    }

    public BigDecimal getContributionAnnualIncreasePercent() {
        return contributionAnnualIncreasePercent;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public short getHorizonYears() {
        return horizonYears;
    }

    public BigDecimal getExpectedReturnNominalAnnual() {
        return expectedReturnNominalAnnual;
    }

    public BigDecimal getExpectedInflationAnnual() {
        return expectedInflationAnnual;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void update(
            BigDecimal monthlyContribution,
            BigDecimal contributionAnnualIncreasePercent,
            LocalDate startDate,
            short horizonYears,
            BigDecimal expectedReturnNominalAnnual,
            BigDecimal expectedInflationAnnual,
            BigDecimal currentBalance) {
        this.monthlyContribution = monthlyContribution;
        this.contributionAnnualIncreasePercent = contributionAnnualIncreasePercent;
        this.startDate = startDate;
        this.horizonYears = horizonYears;
        this.expectedReturnNominalAnnual = expectedReturnNominalAnnual;
        this.expectedInflationAnnual = expectedInflationAnnual;
        this.currentBalance = currentBalance;
    }
}
