package com.marlus.financas.dataio.handler;

import com.marlus.financas.dataio.csv.CsvFieldParser;
import com.marlus.financas.dataio.csv.RowValidationException;
import com.marlus.financas.dataio.service.EntityCsvHandler;
import com.marlus.financas.dataio.service.RowResult;
import com.marlus.financas.tithe.domain.TitheLedger;
import com.marlus.financas.tithe.repository.TitheLedgerRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * {@link TitheLedger} só expõe {@code recalculateBase} (absoluto) e {@code registerPayment}
 * (soma um delta ao pago) — para upsert por CSV aplicamos a diferença entre o valor pago do
 * arquivo e o já registrado. Mês de referência é imutável.
 */
@Component
public class TitheLedgerCsvHandler implements EntityCsvHandler {

    private final TitheLedgerRepository titheLedgerRepository;

    public TitheLedgerCsvHandler(TitheLedgerRepository titheLedgerRepository) {
        this.titheLedgerRepository = titheLedgerRepository;
    }

    @Override
    public String fileName() {
        return "tithe_ledger.csv";
    }

    @Override
    public List<String> header() {
        return List.of("id", "reference_month", "base_amount", "percent", "due_amount", "paid_amount", "paid_on", "status");
    }

    @Override
    public List<List<String>> exportRows(UUID userId) {
        return titheLedgerRepository.findAllByUserIdOrderByReferenceMonthDesc(userId).stream()
                .map(t -> List.of(
                        t.getId().toString(),
                        t.getReferenceMonth(),
                        CsvFieldParser.required(t.getBaseAmount()),
                        CsvFieldParser.required(t.getPercent()),
                        CsvFieldParser.required(t.getDueAmount()),
                        CsvFieldParser.required(t.getPaidAmount()),
                        CsvFieldParser.opt(t.getPaidOn()),
                        t.getStatus().name()))
                .toList();
    }

    @Override
    public RowResult importRow(UUID userId, Map<String, String> row, int line, boolean apply) {
        try {
            UUID id = CsvFieldParser.requiredUuid(row, "id");
            String referenceMonth = CsvFieldParser.requiredText(row, "reference_month");
            BigDecimal baseAmount = CsvFieldParser.requiredDecimal(row, "base_amount");
            BigDecimal percent = CsvFieldParser.requiredDecimal(row, "percent");
            CsvFieldParser.requiredDecimal(row, "due_amount");
            BigDecimal paidAmount = CsvFieldParser.requiredDecimal(row, "paid_amount");
            LocalDate paidOn = CsvFieldParser.optionalDate(row, "paid_on");
            CsvFieldParser.requiredEnum(row, "status", com.marlus.financas.tithe.domain.TitheStatus.class);

            var existing = titheLedgerRepository.findByIdAndUserId(id, userId);
            if (existing.isPresent()) {
                TitheLedger ledger = existing.get();
                if (!Objects.equals(ledger.getReferenceMonth(), referenceMonth)) {
                    return RowResult.error(
                            line, "o mês de referência de um dízimo existente não pode ser alterado por importação");
                }
                if (apply) {
                    ledger.recalculateBase(baseAmount, percent);
                    BigDecimal delta = paidAmount.subtract(ledger.getPaidAmount());
                    if (delta.signum() != 0) {
                        ledger.registerPayment(delta, paidOn);
                    }
                    titheLedgerRepository.save(ledger);
                }
                return RowResult.updated(line);
            }

            if (titheLedgerRepository.existsById(id)) {
                return RowResult.error(line, "id já pertence a outro registro");
            }
            if (apply) {
                TitheLedger ledger = new TitheLedger(id, userId, referenceMonth, percent);
                ledger.recalculateBase(baseAmount, percent);
                if (paidAmount.signum() != 0) {
                    ledger.registerPayment(paidAmount, paidOn);
                }
                titheLedgerRepository.save(ledger);
            }
            return RowResult.created(line);
        } catch (RowValidationException ex) {
            return RowResult.error(line, ex.getMessage());
        }
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        titheLedgerRepository.deleteAllByUserId(userId);
    }
}
