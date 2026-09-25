package com.marlus.financas.goal.web;

import com.marlus.financas.goal.domain.GoalContributionSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GoalContributionResponse(UUID id, UUID goalId, LocalDate date, BigDecimal amount, GoalContributionSource source) {
}
