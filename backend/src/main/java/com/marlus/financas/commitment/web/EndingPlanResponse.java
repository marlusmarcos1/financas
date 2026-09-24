package com.marlus.financas.commitment.web;

import java.math.BigDecimal;
import java.util.UUID;

public record EndingPlanResponse(UUID planId, String description, BigDecimal monthlyAmount) {
}
