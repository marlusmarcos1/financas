package com.marlus.financas.recurring.domain;

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
@Table(name = "recurring_rule")
public class RecurringRule extends UserOwnedEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 160)
    private String description;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "amount_is_variable", nullable = false)
    private boolean amountIsVariable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RecurringFrequency frequency;

    @Column(name = "day_of_month", nullable = false)
    private short dayOfMonth;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "card_id")
    private UUID cardId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(nullable = false)
    private boolean active;

    protected RecurringRule() {
    }

    public RecurringRule(
            UUID id,
            UUID userId,
            String description,
            BigDecimal amount,
            boolean amountIsVariable,
            RecurringFrequency frequency,
            short dayOfMonth,
            LocalDate startDate,
            LocalDate endDate,
            UUID categoryId,
            UUID cardId,
            UUID accountId) {
        super(userId);
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.amountIsVariable = amountIsVariable;
        this.frequency = frequency;
        this.dayOfMonth = dayOfMonth;
        this.startDate = startDate;
        this.endDate = endDate;
        this.categoryId = categoryId;
        this.cardId = cardId;
        this.accountId = accountId;
        this.active = true;
    }

    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public boolean isAmountIsVariable() {
        return amountIsVariable;
    }

    public void setAmountIsVariable(boolean amountIsVariable) {
        this.amountIsVariable = amountIsVariable;
    }

    public RecurringFrequency getFrequency() {
        return frequency;
    }

    public void setFrequency(RecurringFrequency frequency) {
        this.frequency = frequency;
    }

    public short getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(short dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
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

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public UUID getCardId() {
        return cardId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setPaymentMethod(UUID cardId, UUID accountId) {
        this.cardId = cardId;
        this.accountId = accountId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
