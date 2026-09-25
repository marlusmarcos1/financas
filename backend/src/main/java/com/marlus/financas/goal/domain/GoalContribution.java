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
@Table(name = "goal_contribution")
public class GoalContribution extends UserOwnedEntity {

    @Id
    private UUID id;

    @Column(name = "goal_id", nullable = false)
    private UUID goalId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GoalContributionSource source;

    protected GoalContribution() {
    }

    public GoalContribution(UUID id, UUID userId, UUID goalId, LocalDate date, BigDecimal amount, GoalContributionSource source) {
        super(userId);
        this.id = id;
        this.goalId = goalId;
        this.date = date;
        this.amount = amount;
        this.source = source;
    }

    public UUID getId() {
        return id;
    }

    public UUID getGoalId() {
        return goalId;
    }

    public LocalDate getDate() {
        return date;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public GoalContributionSource getSource() {
        return source;
    }
}
