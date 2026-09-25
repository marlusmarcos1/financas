package com.marlus.financas.dataio.handler;

import com.marlus.financas.dataio.csv.CsvFieldParser;
import com.marlus.financas.dataio.csv.RowValidationException;
import com.marlus.financas.dataio.service.EntityCsvHandler;
import com.marlus.financas.dataio.service.RowResult;
import com.marlus.financas.invoice.domain.Invoice;
import com.marlus.financas.invoice.domain.InvoiceStatus;
import com.marlus.financas.invoice.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * {@link Invoice} só permite alterar valor pago via {@code registerPayment}, que soma um delta ao
 * já pago — para upsert por CSV (valor absoluto), aplicamos a diferença entre o valor do arquivo e
 * o valor atualmente armazenado. Cartão, mês de referência, fechamento e vencimento são imutáveis.
 */
@Component
public class InvoiceCsvHandler implements EntityCsvHandler {

    private final InvoiceRepository invoiceRepository;

    public InvoiceCsvHandler(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    public String fileName() {
        return "invoices.csv";
    }

    @Override
    public List<String> header() {
        return List.of(
                "id", "card_id", "reference_month", "closing_date", "due_date", "status", "paid_amount", "paid_on",
                "paid_from_account_id");
    }

    @Override
    public List<List<String>> exportRows(UUID userId) {
        return invoiceRepository.findAllByUserId(userId).stream()
                .map(i -> List.of(
                        i.getId().toString(),
                        i.getCardId().toString(),
                        i.getReferenceMonth(),
                        i.getClosingDate().toString(),
                        i.getDueDate().toString(),
                        i.getStatus().name(),
                        CsvFieldParser.required(i.getPaidAmount()),
                        CsvFieldParser.opt(i.getPaidOn()),
                        CsvFieldParser.opt(i.getPaidFromAccountId())))
                .toList();
    }

    @Override
    public RowResult importRow(UUID userId, Map<String, String> row, int line, boolean apply) {
        try {
            UUID id = CsvFieldParser.requiredUuid(row, "id");
            UUID cardId = CsvFieldParser.requiredUuid(row, "card_id");
            String referenceMonth = CsvFieldParser.requiredText(row, "reference_month");
            YearMonth.parse(referenceMonth);
            LocalDate closingDate = CsvFieldParser.requiredDate(row, "closing_date");
            LocalDate dueDate = CsvFieldParser.requiredDate(row, "due_date");
            InvoiceStatus status = CsvFieldParser.requiredEnum(row, "status", InvoiceStatus.class);
            BigDecimal paidAmount = CsvFieldParser.requiredDecimal(row, "paid_amount");
            LocalDate paidOn = CsvFieldParser.optionalDate(row, "paid_on");
            UUID paidFromAccountId = CsvFieldParser.optionalUuid(row, "paid_from_account_id");

            var existing = invoiceRepository.findByIdAndUserId(id, userId);
            if (existing.isPresent()) {
                Invoice invoice = existing.get();
                boolean immutableFieldsMatch = Objects.equals(invoice.getCardId(), cardId)
                        && Objects.equals(invoice.getReferenceMonth(), referenceMonth)
                        && Objects.equals(invoice.getClosingDate(), closingDate)
                        && Objects.equals(invoice.getDueDate(), dueDate);
                if (!immutableFieldsMatch) {
                    return RowResult.error(
                            line,
                            "cartão, mês de referência, fechamento e vencimento de uma fatura existente não podem ser"
                                    + " alterados por importação");
                }
                if (apply) {
                    invoice.setStatus(status);
                    BigDecimal delta = paidAmount.subtract(invoice.getPaidAmount());
                    invoice.registerPayment(delta, paidOn, paidFromAccountId);
                    invoiceRepository.save(invoice);
                }
                return RowResult.updated(line);
            }

            if (invoiceRepository.existsById(id)) {
                return RowResult.error(line, "id já pertence a outro registro");
            }
            if (apply) {
                Invoice invoice =
                        new Invoice(id, userId, cardId, YearMonth.parse(referenceMonth), closingDate, dueDate);
                invoice.setStatus(status);
                if (paidAmount.signum() != 0 || paidOn != null || paidFromAccountId != null) {
                    invoice.registerPayment(paidAmount, paidOn, paidFromAccountId);
                }
                invoiceRepository.save(invoice);
            }
            return RowResult.created(line);
        } catch (RowValidationException | java.time.format.DateTimeParseException ex) {
            return RowResult.error(line, ex.getMessage());
        }
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        invoiceRepository.deleteAllByUserId(userId);
    }
}
