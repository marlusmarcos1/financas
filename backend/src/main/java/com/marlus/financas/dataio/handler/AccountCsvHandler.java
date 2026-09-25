package com.marlus.financas.dataio.handler;

import com.marlus.financas.account.domain.Account;
import com.marlus.financas.account.domain.AccountPurpose;
import com.marlus.financas.account.domain.AccountType;
import com.marlus.financas.account.repository.AccountRepository;
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
public class AccountCsvHandler implements EntityCsvHandler {

    private final AccountRepository accountRepository;

    public AccountCsvHandler(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public String fileName() {
        return "accounts.csv";
    }

    @Override
    public List<String> header() {
        return List.of("id", "name", "type", "institution", "initial_balance", "purpose", "archived");
    }

    @Override
    public List<List<String>> exportRows(UUID userId) {
        return accountRepository.findAllByUserIdOrderByArchivedAscNameAsc(userId).stream()
                .map(a -> List.of(
                        a.getId().toString(),
                        a.getName(),
                        a.getType().name(),
                        CsvFieldParser.opt(a.getInstitution()),
                        CsvFieldParser.required(a.getInitialBalance()),
                        a.getPurpose().name(),
                        CsvFieldParser.bool(a.isArchived())))
                .toList();
    }

    @Override
    public RowResult importRow(UUID userId, Map<String, String> row, int line, boolean apply) {
        try {
            UUID id = CsvFieldParser.requiredUuid(row, "id");
            String name = CsvFieldParser.requiredText(row, "name");
            AccountType type = CsvFieldParser.requiredEnum(row, "type", AccountType.class);
            String institution = CsvFieldParser.optionalText(row, "institution");
            BigDecimal initialBalance = CsvFieldParser.requiredDecimal(row, "initial_balance");
            AccountPurpose purpose = CsvFieldParser.requiredEnum(row, "purpose", AccountPurpose.class);
            boolean archived = CsvFieldParser.requiredBoolean(row, "archived");

            var existing = accountRepository.findByIdAndUserId(id, userId);
            if (existing.isPresent()) {
                Account account = existing.get();
                if (apply) {
                    account.setName(name);
                    account.setType(type);
                    account.setInstitution(institution);
                    account.setInitialBalance(initialBalance);
                    account.setPurpose(purpose);
                    if (archived) {
                        account.archive();
                    } else {
                        account.unarchive();
                    }
                    accountRepository.save(account);
                }
                return RowResult.updated(line);
            }

            if (accountRepository.existsById(id)) {
                return RowResult.error(line, "id já pertence a outro registro");
            }
            if (apply) {
                Account account = new Account(id, userId, name, type, institution, initialBalance, purpose);
                if (archived) {
                    account.archive();
                }
                accountRepository.save(account);
            }
            return RowResult.created(line);
        } catch (RowValidationException ex) {
            return RowResult.error(line, ex.getMessage());
        }
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        accountRepository.deleteAllByUserId(userId);
    }
}
