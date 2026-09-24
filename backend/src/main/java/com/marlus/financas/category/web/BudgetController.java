package com.marlus.financas.category.web;

import com.marlus.financas.category.domain.Budget;
import com.marlus.financas.category.mapper.BudgetMapper;
import com.marlus.financas.category.service.BudgetService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories/{categoryId}/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final BudgetMapper budgetMapper;

    public BudgetController(BudgetService budgetService, BudgetMapper budgetMapper) {
        this.budgetService = budgetService;
        this.budgetMapper = budgetMapper;
    }

    @GetMapping
    public List<BudgetResponse> list(@PathVariable UUID categoryId) {
        return budgetService.findAllByCategory(categoryId).stream().map(budgetMapper::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse create(@PathVariable UUID categoryId, @Valid @RequestBody BudgetRequest request) {
        Budget budget = budgetService.create(categoryId, request);
        return budgetMapper.toResponse(budget);
    }

    @PutMapping("/{id}")
    public BudgetResponse update(
            @PathVariable UUID categoryId, @PathVariable UUID id, @Valid @RequestBody BudgetRequest request) {
        Budget budget = budgetService.update(categoryId, id, request);
        return budgetMapper.toResponse(budget);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID categoryId, @PathVariable UUID id) {
        budgetService.delete(categoryId, id);
        return ResponseEntity.noContent().build();
    }
}
