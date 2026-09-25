package com.marlus.financas.goal.web;

import com.marlus.financas.goal.domain.GoalType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GoalProgressResponse(
        UUID id,
        String name,
        GoalType type,
        BigDecimal targetAmount,
        LocalDate targetDate,
        UUID linkedAccountId,
        BigDecimal monthlyContributionPlanned,
        short priority,
        String notes,
        BigDecimal currentAmount,
        BigDecimal amountRemaining,
        Integer monthsRemaining,
        BigDecimal requiredMonthlyContribution,
        String warning) {
}
