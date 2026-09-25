package com.marlus.financas.dataio.handler;

import com.marlus.financas.dataio.csv.CsvFieldParser;
import com.marlus.financas.dataio.csv.RowValidationException;
import com.marlus.financas.dataio.service.EntityCsvHandler;
import com.marlus.financas.dataio.service.RowResult;
import com.marlus.financas.income.domain.IncomeRecurrence;
import com.marlus.financas.income.domain.IncomeSource;
import com.marlus.financas.income.domain.IncomeSourceType;
import com.marlus.financas.income.repository.IncomeSourceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class IncomeSourceCsvHandler implements EntityCsvHandler {

    private final IncomeSourceRepository incomeSourceRepository;

    public IncomeSourceCsvHandler(IncomeSourceRepository incomeSourceRepository) {
        this.incomeSourceRepository = incomeSourceRepository;
    }

    @Override
    public String fileName() {
        return "income_sources.csv";
    }

    @Override
    public List<String> header() {
        return List.of(
                "id",
                "name",
                "type",
                "recurrence",
                "expected_amount",
                "pay_day",
                "start_date",
                "end_date",
                "expected_months",
                "tithe_applies",
                "counts_in_base_budget");
    }

    @Override
    public List<List<String>> exportRows(UUID userId) {
        return incomeSourceRepository.findAllByUserIdOrderByNameAsc(userId).stream()
                .map(s -> List.of(
                        s.getId().toString(),
                        s.getName(),
                        s.getType().name(),
                        s.getRecurrence().name(),
                        CsvFieldParser.required(s.getExpectedAmount()),
                        CsvFieldParser.opt(s.getPayDay()),
                        CsvFieldParser.opt(s.getStartDate()),
                        CsvFieldParser.opt(s.getEndDate()),
                        CsvFieldParser.opt(s.getExpectedMonths()),
                        CsvFieldParser.bool(s.isTitheApplies()),
                        CsvFieldParser.bool(s.isCountsInBaseBudget())))
                .toList();
    }

    @Override
    public RowResult importRow(UUID userId, Map<String, String> row, int line, boolean apply) {
        try {
            UUID id = CsvFieldParser.requiredUuid(row, "id");
            String name = CsvFieldParser.requiredText(row, "name");
            IncomeSourceType type = CsvFieldParser.requiredEnum(row, "type", IncomeSourceType.class);
            IncomeRecurrence recurrence = CsvFieldParser.requiredEnum(row, "recurrence", IncomeRecurrence.class);
            BigDecimal expectedAmount = CsvFieldParser.requiredDecimal(row, "expected_amount");
            Short payDay = CsvFieldParser.optionalShort(row, "pay_day");
            LocalDate startDate = CsvFieldParser.optionalDate(row, "start_date");
            LocalDate endDate = CsvFieldParser.optionalDate(row, "end_date");
            Short expectedMonths = CsvFieldParser.optionalShort(row, "expected_months");
            boolean titheApplies = CsvFieldParser.requiredBoolean(row, "tithe_applies");
            boolean countsInBaseBudget = CsvFieldParser.requiredBoolean(row, "counts_in_base_budget");

            var existing = incomeSourceRepository.findByIdAndUserId(id, userId);
            if (existing.isPresent()) {
                IncomeSource source = existing.get();
                if (apply) {
                    source.setName(name);
                    source.setType(type);
                    source.setRecurrence(recurrence);
                    source.setExpectedAmount(expectedAmount);
                    source.setPayDay(payDay);
                    source.setStartDate(startDate);
                    source.setEndDate(endDate);
                    source.setExpectedMonths(expectedMonths);
                    source.setTitheApplies(titheApplies);
                    source.setCountsInBaseBudget(countsInBaseBudget);
                    incomeSourceRepository.save(source);
                }
                return RowResult.updated(line);
            }

            if (incomeSourceRepository.existsById(id)) {
                return RowResult.error(line, "id já pertence a outro registro");
            }
            if (apply) {
                incomeSourceRepository.save(new IncomeSource(
                        id,
                        userId,
                        name,
                        type,
                        recurrence,
                        expectedAmount,
                        payDay,
                        startDate,
                        endDate,
                        expectedMonths,
                        titheApplies,
                        countsInBaseBudget));
            }
            return RowResult.created(line);
        } catch (RowValidationException ex) {
            return RowResult.error(line, ex.getMessage());
        }
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        incomeSourceRepository.deleteAllByUserId(userId);
    }
}
