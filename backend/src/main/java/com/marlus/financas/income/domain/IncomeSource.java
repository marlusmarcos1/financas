package com.marlus.financas.income.domain;

import com.marlus.financas.common.UserOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "income_source")
public class IncomeSource extends UserOwnedEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncomeSourceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncomeRecurrence recurrence;

    @Column(name = "expected_amount", nullable = false)
    private BigDecimal expectedAmount;

    @Column(name = "pay_day")
    private Short payDay;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "expected_months")
    private Short expectedMonths;

    @Column(name = "tithe_applies", nullable = false)
    private boolean titheApplies;

    @Column(name = "counts_in_base_budget", nullable = false)
    private boolean countsInBaseBudget;

    protected IncomeSource() {
    }

    public IncomeSource(
            UUID id,
            UUID userId,
            String name,
            IncomeSourceType type,
            IncomeRecurrence recurrence,
            BigDecimal expectedAmount,
            Short payDay,
            LocalDate startDate,
            LocalDate endDate,
            Short expectedMonths,
            boolean titheApplies,
            boolean countsInBaseBudget) {
        super(userId);
        this.id = id;
        this.name = name;
        this.type = type;
        this.recurrence = recurrence;
        this.expectedAmount = expectedAmount;
        this.payDay = payDay;
        this.startDate = startDate;
        this.endDate = endDate;
        this.expectedMonths = expectedMonths;
        this.titheApplies = titheApplies;
        this.countsInBaseBudget = countsInBaseBudget;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IncomeSourceType getType() {
        return type;
    }

    public void setType(IncomeSourceType type) {
        this.type = type;
    }

    public IncomeRecurrence getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(IncomeRecurrence recurrence) {
        this.recurrence = recurrence;
    }

    public BigDecimal getExpectedAmount() {
        return expectedAmount;
    }

    public void setExpectedAmount(BigDecimal expectedAmount) {
        this.expectedAmount = expectedAmount;
    }

    public Short getPayDay() {
        return payDay;
    }

    public void setPayDay(Short payDay) {
        this.payDay = payDay;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Short getExpectedMonths() {
        return expectedMonths;
    }

    public void setExpectedMonths(Short expectedMonths) {
        this.expectedMonths = expectedMonths;
    }

    public boolean isTitheApplies() {
        return titheApplies;
    }

    public void setTitheApplies(boolean titheApplies) {
        this.titheApplies = titheApplies;
    }

    public boolean isCountsInBaseBudget() {
        return countsInBaseBudget;
    }

    public void setCountsInBaseBudget(boolean countsInBaseBudget) {
        this.countsInBaseBudget = countsInBaseBudget;
    }
}
