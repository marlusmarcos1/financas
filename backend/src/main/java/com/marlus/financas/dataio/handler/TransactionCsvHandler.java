package com.marlus.financas.dataio.handler;

import com.marlus.financas.dataio.csv.CsvFieldParser;
import com.marlus.financas.dataio.csv.RowValidationException;
import com.marlus.financas.dataio.service.EntityCsvHandler;
import com.marlus.financas.dataio.service.RowResult;
import com.marlus.financas.transaction.domain.Transaction;
import com.marlus.financas.transaction.domain.TransactionKind;
import com.marlus.financas.transaction.domain.TransactionStatus;
import com.marlus.financas.transaction.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TransactionCsvHandler implements EntityCsvHandler {

    private final TransactionRepository transactionRepository;

    public TransactionCsvHandler(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public String fileName() {
        return "transactions.csv";
    }

    @Override
    public List<String> header() {
        return List.of(
                "id",
                "kind",
                "description",
                "amount",
                "date",
                "category_id",
                "account_id",
                "card_id",
                "invoice_id",
                "status",
                "installment_plan_id",
                "installment_number",
                "recurring_rule_id",
                "income_entry_id",
                "notes");
    }

    @Override
    public List<List<String>> exportRows(UUID userId) {
        return transactionRepository.findAllByUserId(userId).stream()
                .map(t -> List.of(
                        t.getId().toString(),
                        t.getKind().name(),
                        t.getDescription(),
                        CsvFieldParser.required(t.getAmount()),
                        t.getDate().toString(),
                        CsvFieldParser.opt(t.getCategoryId()),
                        CsvFieldParser.opt(t.getAccountId()),
                        CsvFieldParser.opt(t.getCardId()),
                        CsvFieldParser.opt(t.getInvoiceId()),
                        t.getStatus().name(),
                        CsvFieldParser.opt(t.getInstallmentPlanId()),
                        CsvFieldParser.opt(t.getInstallmentNumber()),
                        CsvFieldParser.opt(t.getRecurringRuleId()),
                        CsvFieldParser.opt(t.getIncomeEntryId()),
                        CsvFieldParser.opt(t.getNotes())))
                .toList();
    }

    @Override
    public RowResult importRow(UUID userId, Map<String, String> row, int line, boolean apply) {
        try {
            UUID id = CsvFieldParser.requiredUuid(row, "id");
            TransactionKind kind = CsvFieldParser.requiredEnum(row, "kind", TransactionKind.class);
            String description = CsvFieldParser.requiredText(row, "description");
            BigDecimal amount = CsvFieldParser.requiredDecimal(row, "amount");
            LocalDate date = CsvFieldParser.requiredDate(row, "date");
            UUID categoryId = CsvFieldParser.optionalUuid(row, "category_id");
            UUID accountId = CsvFieldParser.optionalUuid(row, "account_id");
            UUID cardId = CsvFieldParser.optionalUuid(row, "card_id");
            UUID invoiceId = CsvFieldParser.optionalUuid(row, "invoice_id");
            TransactionStatus status = CsvFieldParser.requiredEnum(row, "status", TransactionStatus.class);
            UUID installmentPlanId = CsvFieldParser.optionalUuid(row, "installment_plan_id");
            Short installmentNumber = CsvFieldParser.optionalShort(row, "installment_number");
            UUID recurringRuleId = CsvFieldParser.optionalUuid(row, "recurring_rule_id");
            UUID incomeEntryId = CsvFieldParser.optionalUuid(row, "income_entry_id");
            String notes = CsvFieldParser.optionalText(row, "notes");

            var existing = transactionRepository.findByIdAndUserId(id, userId);
            if (existing.isPresent()) {
                Transaction transaction = existing.get();
                if (apply) {
                    applyFields(
                            transaction,
                            kind,
                            description,
                            amount,
                            date,
                            categoryId,
                            accountId,
                            cardId,
                            invoiceId,
                            status,
                            installmentPlanId,
                            installmentNumber,
                            recurringRuleId,
                            incomeEntryId,
                            notes);
                    transactionRepository.save(transaction);
                }
                return RowResult.updated(line);
            }

            if (transactionRepository.existsById(id)) {
                return RowResult.error(line, "id já pertence a outro registro");
            }
            if (apply) {
                Transaction transaction =
                        new Transaction(id, userId, kind, description, amount, date, categoryId, accountId, cardId, status, notes);
                applyFields(
                        transaction,
                        kind,
                        description,
                        amount,
                        date,
                        categoryId,
                        accountId,
                        cardId,
                        invoiceId,
                        status,
                        installmentPlanId,
                        installmentNumber,
                        recurringRuleId,
                        incomeEntryId,
                        notes);
                transactionRepository.save(transaction);
            }
            return RowResult.created(line);
        } catch (RowValidationException ex) {
            return RowResult.error(line, ex.getMessage());
        }
    }

    private static void applyFields(
            Transaction transaction,
            TransactionKind kind,
            String description,
            BigDecimal amount,
            LocalDate date,
            UUID categoryId,
            UUID accountId,
            UUID cardId,
            UUID invoiceId,
            TransactionStatus status,
            UUID installmentPlanId,
            Short installmentNumber,
            UUID recurringRuleId,
            UUID incomeEntryId,
            String notes) {
        transaction.setKind(kind);
        transaction.setDescription(description);
        transaction.setAmount(amount);
        transaction.setDate(date);
        transaction.setCategoryId(categoryId);
        transaction.setPaymentMethod(accountId, cardId);
        transaction.setInvoiceId(invoiceId);
        transaction.setStatus(status);
        transaction.setInstallmentPlanId(installmentPlanId);
        transaction.setInstallmentNumber(installmentNumber);
        transaction.setRecurringRuleId(recurringRuleId);
        transaction.setIncomeEntryId(incomeEntryId);
        transaction.setNotes(notes);
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        transactionRepository.deleteAllByUserId(userId);
    }
}
