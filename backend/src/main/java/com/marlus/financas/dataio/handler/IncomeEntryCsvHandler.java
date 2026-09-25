package com.marlus.financas.dataio.handler;

import com.marlus.financas.dataio.csv.CsvFieldParser;
import com.marlus.financas.dataio.csv.RowValidationException;
import com.marlus.financas.dataio.service.EntityCsvHandler;
import com.marlus.financas.dataio.service.RowResult;
import com.marlus.financas.income.domain.IncomeEntry;
import com.marlus.financas.income.domain.IncomeEntryStatus;
import com.marlus.financas.income.repository.IncomeEntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class IncomeEntryCsvHandler implements EntityCsvHandler {

    private final IncomeEntryRepository incomeEntryRepository;

    public IncomeEntryCsvHandler(IncomeEntryRepository incomeEntryRepository) {
        this.incomeEntryRepository = incomeEntryRepository;
    }

    @Override
    public String fileName() {
        return "income_entries.csv";
    }

    @Override
    public List<String> header() {
        return List.of("id", "source_id", "account_id", "reference_month", "received_on", "amount", "status");
    }

    @Override
    public List<List<String>> exportRows(UUID userId) {
        return incomeEntryRepository.findAllByUserIdOrderByReferenceMonthAsc(userId).stream()
                .map(e -> List.of(
                        e.getId().toString(),
                        e.getSourceId().toString(),
                        CsvFieldParser.opt(e.getAccountId()),
                        e.getReferenceMonth(),
                        CsvFieldParser.opt(e.getReceivedOn()),
                        CsvFieldParser.required(e.getAmount()),
                        e.getStatus().name()))
                .toList();
    }

    @Override
    public RowResult importRow(UUID userId, Map<String, String> row, int line, boolean apply) {
        try {
            UUID id = CsvFieldParser.requiredUuid(row, "id");
            UUID sourceId = CsvFieldParser.requiredUuid(row, "source_id");
            UUID accountId = CsvFieldParser.optionalUuid(row, "account_id");
            String referenceMonth = CsvFieldParser.requiredText(row, "reference_month");
            LocalDate receivedOn = CsvFieldParser.optionalDate(row, "received_on");
            BigDecimal amount = CsvFieldParser.requiredDecimal(row, "amount");
            IncomeEntryStatus status = CsvFieldParser.requiredEnum(row, "status", IncomeEntryStatus.class);

            var existing = incomeEntryRepository.findByIdAndUserId(id, userId);
            if (existing.isPresent()) {
                IncomeEntry entry = existing.get();
                if (!Objects.equals(entry.getSourceId(), sourceId)) {
                    return RowResult.error(
                            line, "a receita de origem de um lançamento existente não pode ser alterada por importação");
                }
                if (apply) {
                    entry.setAccountId(accountId);
                    entry.setReferenceMonth(referenceMonth);
                    entry.setReceivedOn(receivedOn);
                    entry.setAmount(amount);
                    entry.setStatus(status);
                    incomeEntryRepository.save(entry);
                }
                return RowResult.updated(line);
            }

            if (incomeEntryRepository.existsById(id)) {
                return RowResult.error(line, "id já pertence a outro registro");
            }
            if (apply) {
                incomeEntryRepository.save(
                        new IncomeEntry(id, userId, sourceId, accountId, referenceMonth, receivedOn, amount, status));
            }
            return RowResult.created(line);
        } catch (RowValidationException ex) {
            return RowResult.error(line, ex.getMessage());
        }
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        incomeEntryRepository.deleteAllByUserId(userId);
    }
}
