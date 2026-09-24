package com.marlus.financas.dashboard.web;

import com.marlus.financas.category.web.CategoryBudgetStatusResponse;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record DashboardResponse(
        YearMonth month,
        BigDecimal baseIncomeExpected,
        BigDecimal baseIncomeReceived,
        BigDecimal extrasReceived,
        BigDecimal totalExpenses,
        BigDecimal titheDue,
        BigDecimal titheOutstanding,
        BigDecimal surplus,
        List<CategoryBudgetStatusResponse> budgets) {
}
