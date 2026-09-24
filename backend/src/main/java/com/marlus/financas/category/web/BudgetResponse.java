package com.marlus.financas.category.web;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetResponse(UUID id, UUID categoryId, String month, BigDecimal limitAmount) {
}
