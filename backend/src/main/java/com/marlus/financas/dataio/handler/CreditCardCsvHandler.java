package com.marlus.financas.dataio.handler;

import com.marlus.financas.creditcard.domain.CreditCard;
import com.marlus.financas.creditcard.repository.CreditCardRepository;
import com.marlus.financas.dataio.csv.CsvFieldParser;
import com.marlus.financas.dataio.csv.RowValidationException;
import com.marlus.financas.dataio.service.EntityCsvHandler;
import com.marlus.financas.dataio.service.RowResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CreditCardCsvHandler implements EntityCsvHandler {

    private final CreditCardRepository creditCardRepository;

    public CreditCardCsvHandler(CreditCardRepository creditCardRepository) {
        this.creditCardRepository = creditCardRepository;
    }

    @Override
    public String fileName() {
        return "credit_cards.csv";
    }

    @Override
    public List<String> header() {
        return List.of(
                "id",
                "name",
                "issuer",
                "credit_limit",
                "closing_day",
                "due_day",
                "default_payment_account_id",
                "color",
                "archived");
    }

    @Override
    public List<List<String>> exportRows(UUID userId) {
        return creditCardRepository.findAllByUserIdOrderByArchivedAscNameAsc(userId).stream()
                .map(c -> List.of(
                        c.getId().toString(),
                        c.getName(),
                        CsvFieldParser.opt(c.getIssuer()),
                        CsvFieldParser.required(c.getCreditLimit()),
                        String.valueOf(c.getClosingDay()),
                        String.valueOf(c.getDueDay()),
                        CsvFieldParser.opt(c.getDefaultPaymentAccountId()),
                        CsvFieldParser.opt(c.getColor()),
                        CsvFieldParser.bool(c.isArchived())))
                .toList();
    }

    @Override
    public RowResult importRow(UUID userId, Map<String, String> row, int line, boolean apply) {
        try {
            UUID id = CsvFieldParser.requiredUuid(row, "id");
            String name = CsvFieldParser.requiredText(row, "name");
            String issuer = CsvFieldParser.optionalText(row, "issuer");
            BigDecimal creditLimit = CsvFieldParser.requiredDecimal(row, "credit_limit");
            short closingDay = CsvFieldParser.requiredShort(row, "closing_day");
            short dueDay = CsvFieldParser.requiredShort(row, "due_day");
            UUID defaultPaymentAccountId = CsvFieldParser.optionalUuid(row, "default_payment_account_id");
            String color = CsvFieldParser.optionalText(row, "color");
            boolean archived = CsvFieldParser.requiredBoolean(row, "archived");

            var existing = creditCardRepository.findByIdAndUserId(id, userId);
            if (existing.isPresent()) {
                CreditCard card = existing.get();
                if (apply) {
                    card.setName(name);
                    card.setIssuer(issuer);
                    card.setCreditLimit(creditLimit);
                    card.setClosingDay(closingDay);
                    card.setDueDay(dueDay);
                    card.setDefaultPaymentAccountId(defaultPaymentAccountId);
                    card.setColor(color);
                    if (archived) {
                        card.archive();
                    } else {
                        card.unarchive();
                    }
                    creditCardRepository.save(card);
                }
                return RowResult.updated(line);
            }

            if (creditCardRepository.existsById(id)) {
                return RowResult.error(line, "id já pertence a outro registro");
            }
            if (apply) {
                CreditCard card = new CreditCard(
                        id, userId, name, issuer, creditLimit, closingDay, dueDay, defaultPaymentAccountId, color);
                if (archived) {
                    card.archive();
                }
                creditCardRepository.save(card);
            }
            return RowResult.created(line);
        } catch (RowValidationException ex) {
            return RowResult.error(line, ex.getMessage());
        }
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        creditCardRepository.deleteAllByUserId(userId);
    }
}
