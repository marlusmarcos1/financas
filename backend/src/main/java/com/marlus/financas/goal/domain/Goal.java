package com.marlus.financas.goal.domain;

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
@Table(name = "goal")
public class Goal extends UserOwnedEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GoalType type;

    @Column(name = "target_amount", nullable = false)
    private BigDecimal targetAmount;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "linked_account_id")
    private UUID linkedAccountId;

    @Column(name = "monthly_contribution_planned", nullable = false)
    private BigDecimal monthlyContributionPlanned;

    @Column(nullable = false)
    private short priority;

    @Column(length = 500)
    private String notes;

    protected Goal() {
    }

    public Goal(
            UUID id,
            UUID userId,
            String name,
            GoalType type,
            BigDecimal targetAmount,
            LocalDate targetDate,
            UUID linkedAccountId,
            BigDecimal monthlyContributionPlanned,
            short priority,
            String notes) {
        super(userId);
        this.id = id;
        this.name = name;
        this.type = type;
        this.targetAmount = targetAmount;
        this.targetDate = targetDate;
        this.linkedAccountId = linkedAccountId;
        this.monthlyContributionPlanned = monthlyContributionPlanned;
        this.priority = priority;
        this.notes = notes;
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

    public GoalType getType() {
        return type;
    }

    public void setType(GoalType type) {
        this.type = type;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public UUID getLinkedAccountId() {
        return linkedAccountId;
    }

    public void setLinkedAccountId(UUID linkedAccountId) {
        this.linkedAccountId = linkedAccountId;
    }

    public BigDecimal getMonthlyContributionPlanned() {
        return monthlyContributionPlanned;
    }

    public void setMonthlyContributionPlanned(BigDecimal monthlyContributionPlanned) {
        this.monthlyContributionPlanned = monthlyContributionPlanned;
    }

    public short getPriority() {
        return priority;
    }

    public void setPriority(short priority) {
        this.priority = priority;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
