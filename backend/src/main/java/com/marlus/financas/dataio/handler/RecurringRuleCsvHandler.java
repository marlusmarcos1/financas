package com.marlus.financas.dataio.handler;

import com.marlus.financas.dataio.csv.CsvFieldParser;
import com.marlus.financas.dataio.csv.RowValidationException;
import com.marlus.financas.dataio.service.EntityCsvHandler;
import com.marlus.financas.dataio.service.RowResult;
import com.marlus.financas.recurring.domain.RecurringFrequency;
import com.marlus.financas.recurring.domain.RecurringRule;
import com.marlus.financas.recurring.repository.RecurringRuleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RecurringRuleCsvHandler implements EntityCsvHandler {

    private final RecurringRuleRepository recurringRuleRepository;

    public RecurringRuleCsvHandler(RecurringRuleRepository recurringRuleRepository) {
        this.recurringRuleRepository = recurringRuleRepository;
    }

    @Override
    public String fileName() {
        return "recurring_rules.csv";
    }

    @Override
    public List<String> header() {
        return List.of(
                "id",
                "description",
                "amount",
                "amount_is_variable",
                "frequency",
                "day_of_month",
                "start_date",
                "end_date",
                "category_id",
                "card_id",
                "account_id",
                "active");
    }

    @Override
    public List<List<String>> exportRows(UUID userId) {
        return recurringRuleRepository.findAllByUserIdOrderByDescriptionAsc(userId).stream()
                .map(r -> List.of(
                        r.getId().toString(),
                        r.getDescription(),
                        CsvFieldParser.required(r.getAmount()),
                        CsvFieldParser.bool(r.isAmountIsVariable()),
                        r.getFrequency().name(),
                        String.valueOf(r.getDayOfMonth()),
                        r.getStartDate().toString(),
                        CsvFieldParser.opt(r.getEndDate()),
                        r.getCategoryId().toString(),
                        CsvFieldParser.opt(r.getCardId()),
                        CsvFieldParser.opt(r.getAccountId()),
                        CsvFieldParser.bool(r.isActive())))
                .toList();
    }

    @Override
    public RowResult importRow(UUID userId, Map<String, String> row, int line, boolean apply) {
        try {
            UUID id = CsvFieldParser.requiredUuid(row, "id");
            String description = CsvFieldParser.requiredText(row, "description");
            BigDecimal amount = CsvFieldParser.requiredDecimal(row, "amount");
            boolean amountIsVariable = CsvFieldParser.requiredBoolean(row, "amount_is_variable");
            RecurringFrequency frequency = CsvFieldParser.requiredEnum(row, "frequency", RecurringFrequency.class);
            short dayOfMonth = CsvFieldParser.requiredShort(row, "day_of_month");
            LocalDate startDate = CsvFieldParser.requiredDate(row, "start_date");
            LocalDate endDate = CsvFieldParser.optionalDate(row, "end_date");
            UUID categoryId = CsvFieldParser.requiredUuid(row, "category_id");
            UUID cardId = CsvFieldParser.optionalUuid(row, "card_id");
            UUID accountId = CsvFieldParser.optionalUuid(row, "account_id");
            boolean active = CsvFieldParser.requiredBoolean(row, "active");

            var existing = recurringRuleRepository.findByIdAndUserId(id, userId);
            if (existing.isPresent()) {
                RecurringRule rule = existing.get();
                if (apply) {
                    rule.setDescription(description);
                    rule.setAmount(amount);
                    rule.setAmountIsVariable(amountIsVariable);
                    rule.setFrequency(frequency);
                    rule.setDayOfMonth(dayOfMonth);
                    rule.setStartDate(startDate);
                    rule.setEndDate(endDate);
                    rule.setCategoryId(categoryId);
                    rule.setPaymentMethod(cardId, accountId);
                    rule.setActive(active);
                    recurringRuleRepository.save(rule);
                }
                return RowResult.updated(line);
            }

            if (recurringRuleRepository.existsById(id)) {
                return RowResult.error(line, "id já pertence a outro registro");
            }
            if (apply) {
                RecurringRule rule = new RecurringRule(
                        id,
                        userId,
                        description,
                        amount,
                        amountIsVariable,
                        frequency,
                        dayOfMonth,
                        startDate,
                        endDate,
                        categoryId,
                        cardId,
                        accountId);
                rule.setActive(active);
                recurringRuleRepository.save(rule);
            }
            return RowResult.created(line);
        } catch (RowValidationException ex) {
            return RowResult.error(line, ex.getMessage());
        }
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        recurringRuleRepository.deleteAllByUserId(userId);
    }
}
