package com.marlus.financas.category.web;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryBudgetStatusResponse(
        UUID categoryId,
        String categoryName,
        BigDecimal limitAmount,
        BigDecimal spent,
        BigDecimal percent,
        String status) {
}
