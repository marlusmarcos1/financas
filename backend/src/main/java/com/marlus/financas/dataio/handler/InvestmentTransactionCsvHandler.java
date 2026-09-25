package com.marlus.financas.dataio.handler;

import com.marlus.financas.dataio.csv.CsvFieldParser;
import com.marlus.financas.dataio.csv.RowValidationException;
import com.marlus.financas.dataio.service.EntityCsvHandler;
import com.marlus.financas.dataio.service.RowResult;
import com.marlus.financas.investment.domain.InvestmentTransaction;
import com.marlus.financas.investment.domain.InvestmentTransactionType;
import com.marlus.financas.investment.repository.InvestmentTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** {@link InvestmentTransaction} é imutável (sem setters) — mesma regra do plano de parcelamento. */
@Component
public class InvestmentTransactionCsvHandler implements EntityCsvHandler {

    private final InvestmentTransactionRepository investmentTransactionRepository;

    public InvestmentTransactionCsvHandler(InvestmentTransactionRepository investmentTransactionRepository) {
        this.investmentTransactionRepository = investmentTransactionRepository;
    }

    @Override
    public String fileName() {
        return "investment_transactions.csv";
    }

    @Override
    public List<String> header() {
        return List.of("id", "asset_id", "type", "date", "quantity", "unit_price", "fees", "amount", "account_id");
    }

    @Override
    public List<List<String>> exportRows(UUID userId) {
        return investmentTransactionRepository.findAllByUserIdOrderByDateDesc(userId).stream()
                .map(t -> List.of(
                        t.getId().toString(),
                        t.getAssetId().toString(),
                        t.getType().name(),
                        t.getDate().toString(),
                        CsvFieldParser.required(t.getQuantity()),
                        CsvFieldParser.required(t.getUnitPrice()),
                        CsvFieldParser.required(t.getFees()),
                        CsvFieldParser.required(t.getAmount()),
                        CsvFieldParser.opt(t.getAccountId())))
                .toList();
    }

    @Override
    public RowResult importRow(UUID userId, Map<String, String> row, int line, boolean apply) {
        try {
            UUID id = CsvFieldParser.requiredUuid(row, "id");
            UUID assetId = CsvFieldParser.requiredUuid(row, "asset_id");
            InvestmentTransactionType type =
                    CsvFieldParser.requiredEnum(row, "type", InvestmentTransactionType.class);
            LocalDate date = CsvFieldParser.requiredDate(row, "date");
            BigDecimal quantity = CsvFieldParser.requiredDecimal(row, "quantity");
            BigDecimal unitPrice = CsvFieldParser.requiredDecimal(row, "unit_price");
            BigDecimal fees = CsvFieldParser.requiredDecimal(row, "fees");
            BigDecimal amount = CsvFieldParser.requiredDecimal(row, "amount");
            UUID accountId = CsvFieldParser.optionalUuid(row, "account_id");

            var existing = investmentTransactionRepository.findByIdAndUserId(id, userId);
            if (existing.isPresent()) {
                InvestmentTransaction transaction = existing.get();
                boolean identical = Objects.equals(transaction.getAssetId(), assetId)
                        && transaction.getType() == type
                        && Objects.equals(transaction.getDate(), date)
                        && transaction.getQuantity().compareTo(quantity) == 0
                        && transaction.getUnitPrice().compareTo(unitPrice) == 0
                        && transaction.getFees().compareTo(fees) == 0
                        && transaction.getAmount().compareTo(amount) == 0
                        && Objects.equals(transaction.getAccountId(), accountId);
                if (!identical) {
                    return RowResult.error(
                            line,
                            "movimentação de investimento existente não pode ser alterada por importação; exclua e"
                                    + " recrie");
                }
                return RowResult.updated(line);
            }

            if (investmentTransactionRepository.existsById(id)) {
                return RowResult.error(line, "id já pertence a outro registro");
            }
            if (apply) {
                investmentTransactionRepository.save(
                        new InvestmentTransaction(id, userId, assetId, type, date, quantity, unitPrice, fees, amount, accountId));
            }
            return RowResult.created(line);
        } catch (RowValidationException ex) {
            return RowResult.error(line, ex.getMessage());
        }
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        investmentTransactionRepository.deleteAllByUserId(userId);
    }
}
