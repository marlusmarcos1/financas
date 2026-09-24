package com.marlus.financas.category.service;

import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.category.domain.Budget;
import com.marlus.financas.category.domain.Category;
import com.marlus.financas.category.domain.CategoryKind;
import com.marlus.financas.category.repository.BudgetRepository;
import com.marlus.financas.category.repository.CategoryRepository;
import com.marlus.financas.category.web.CategoryBudgetStatusResponse;
import com.marlus.financas.transaction.domain.Transaction;
import com.marlus.financas.transaction.domain.TransactionKind;
import com.marlus.financas.transaction.repository.TransactionRepository;
import com.marlus.financas.transaction.repository.TransactionSpecifications;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orçamento realizado vs. teto por categoria, com indicador verde/amarelo/vermelho (seção 7.3). */
@Service
@Transactional(readOnly = true)
public class BudgetSummaryService {

    private static final BigDecimal YELLOW_THRESHOLD = BigDecimal.valueOf(80);
    private static final BigDecimal RED_THRESHOLD = BigDecimal.valueOf(100);

    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUserProvider currentUserProvider;

    public BudgetSummaryService(
            CategoryRepository categoryRepository,
            BudgetRepository budgetRepository,
            TransactionRepository transactionRepository,
            CurrentUserProvider currentUserProvider) {
        this.categoryRepository = categoryRepository;
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public List<CategoryBudgetStatusResponse> summarize(YearMonth month) {
        UUID userId = currentUserProvider.currentUserId();
        List<Category> expenseCategories = categoryRepository.findAllByUserIdOrderByNameAsc(userId).stream()
                .filter(category -> category.getKind() == CategoryKind.EXPENSE)
                .toList();

        return expenseCategories.stream()
                .map(category -> summarizeCategory(userId, category, month))
                .filter(status -> status.limitAmount() != null)
                .toList();
    }

    private CategoryBudgetStatusResponse summarizeCategory(UUID userId, Category category, YearMonth month) {
        List<Budget> budgets = budgetRepository.findAllByUserIdAndCategoryIdOrderByMonthAsc(userId, category.getId());
        Budget applicable = budgets.stream()
                .filter(budget -> month.toString().equals(budget.getMonth()))
                .findFirst()
                .or(() -> budgets.stream().filter(budget -> budget.getMonth() == null).findFirst())
                .orElse(null);

        if (applicable == null) {
            return new CategoryBudgetStatusResponse(category.getId(), category.getName(), null, null, null, null);
        }

        Specification<Transaction> spec = Specification.where(TransactionSpecifications.belongsToUser(userId))
                .and(TransactionSpecifications.categoryId(category.getId()))
                .and(TransactionSpecifications.kind(TransactionKind.EXPENSE))
                .and(TransactionSpecifications.dateFrom(month.atDay(1)))
                .and(TransactionSpecifications.dateTo(month.atEndOfMonth()));
        BigDecimal spent = transactionRepository.findAll(spec).stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percent = applicable.getLimitAmount().signum() > 0
                ? spent.multiply(BigDecimal.valueOf(100)).divide(applicable.getLimitAmount(), 2, RoundingMode.HALF_EVEN)
                : BigDecimal.ZERO;

        String status;
        if (percent.compareTo(RED_THRESHOLD) > 0) {
            status = "RED";
        } else if (percent.compareTo(YELLOW_THRESHOLD) >= 0) {
            status = "YELLOW";
        } else {
            status = "GREEN";
        }

        return new CategoryBudgetStatusResponse(
                category.getId(), category.getName(), applicable.getLimitAmount(), spent, percent, status);
    }
}
