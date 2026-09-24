package com.marlus.financas.category.service;

import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.category.domain.Budget;
import com.marlus.financas.category.repository.BudgetRepository;
import com.marlus.financas.category.web.BudgetRequest;
import com.marlus.financas.common.EntityNotFoundException;
import com.marlus.financas.common.UuidV7Generator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryService categoryService;
    private final CurrentUserProvider currentUserProvider;

    public BudgetService(
            BudgetRepository budgetRepository, CategoryService categoryService, CurrentUserProvider currentUserProvider) {
        this.budgetRepository = budgetRepository;
        this.categoryService = categoryService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<Budget> findAllByCategory(UUID categoryId) {
        categoryService.findById(categoryId);
        return budgetRepository.findAllByUserIdAndCategoryIdOrderByMonthAsc(
                currentUserProvider.currentUserId(), categoryId);
    }

    public Budget create(UUID categoryId, BudgetRequest request) {
        categoryService.findById(categoryId);
        UUID userId = currentUserProvider.currentUserId();
        Budget budget = new Budget(UuidV7Generator.generate(), userId, categoryId, request.month(), request.limitAmount());
        return budgetRepository.save(budget);
    }

    public Budget update(UUID categoryId, UUID id, BudgetRequest request) {
        Budget budget = findByIdAndCategory(categoryId, id);
        budget.setLimitAmount(request.limitAmount());
        return budget;
    }

    public void delete(UUID categoryId, UUID id) {
        budgetRepository.delete(findByIdAndCategory(categoryId, id));
    }

    private Budget findByIdAndCategory(UUID categoryId, UUID id) {
        Budget budget = budgetRepository
                .findByIdAndUserId(id, currentUserProvider.currentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Orçamento não encontrado."));
        if (!budget.getCategoryId().equals(categoryId)) {
            throw new EntityNotFoundException("Orçamento não encontrado.");
        }
        return budget;
    }
}
