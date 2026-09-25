package com.marlus.financas.dataio.handler;

import com.marlus.financas.category.domain.Budget;
import com.marlus.financas.category.repository.BudgetRepository;
import com.marlus.financas.dataio.csv.CsvFieldParser;
import com.marlus.financas.dataio.csv.RowValidationException;
import com.marlus.financas.dataio.service.EntityCsvHandler;
import com.marlus.financas.dataio.service.RowResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BudgetCsvHandler implements EntityCsvHandler {

    private final BudgetRepository budgetRepository;

    public BudgetCsvHandler(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    @Override
    public String fileName() {
        return "budgets.csv";
    }

    @Override
    public List<String> header() {
        return List.of("id", "category_id", "month", "limit_amount");
    }

    @Override
    public List<List<String>> exportRows(UUID userId) {
        return budgetRepository.findAllByUserIdOrderByCategoryIdAsc(userId).stream()
                .map(b -> List.of(
                        b.getId().toString(),
                        b.getCategoryId().toString(),
                        CsvFieldParser.opt(b.getMonth()),
                        CsvFieldParser.required(b.getLimitAmount())))
                .toList();
    }

    @Override
    public RowResult importRow(UUID userId, Map<String, String> row, int line, boolean apply) {
        try {
            UUID id = CsvFieldParser.requiredUuid(row, "id");
            UUID categoryId = CsvFieldParser.requiredUuid(row, "category_id");
            String month = CsvFieldParser.optionalText(row, "month");
            BigDecimal limitAmount = CsvFieldParser.requiredDecimal(row, "limit_amount");

            var existing = budgetRepository.findByIdAndUserId(id, userId);
            if (existing.isPresent()) {
                Budget budget = existing.get();
                if (!Objects.equals(budget.getCategoryId(), categoryId) || !Objects.equals(budget.getMonth(), month)) {
                    return RowResult.error(
                            line, "categoria e mês de um orçamento existente não podem ser alterados por importação");
                }
                if (apply) {
                    budget.setLimitAmount(limitAmount);
                    budgetRepository.save(budget);
                }
                return RowResult.updated(line);
            }

            if (budgetRepository.existsById(id)) {
                return RowResult.error(line, "id já pertence a outro registro");
            }
            if (apply) {
                budgetRepository.save(new Budget(id, userId, categoryId, month, limitAmount));
            }
            return RowResult.created(line);
        } catch (RowValidationException ex) {
            return RowResult.error(line, ex.getMessage());
        }
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        budgetRepository.deleteAllByUserId(userId);
    }
}
