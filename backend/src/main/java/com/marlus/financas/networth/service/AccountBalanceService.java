package com.marlus.financas.networth.service;

import com.marlus.financas.account.domain.Account;
import com.marlus.financas.income.domain.IncomeEntryStatus;
import com.marlus.financas.income.repository.IncomeEntryRepository;
import com.marlus.financas.invoice.repository.InvoiceRepository;
import com.marlus.financas.transaction.domain.Transaction;
import com.marlus.financas.transaction.domain.TransactionKind;
import com.marlus.financas.transaction.repository.TransactionRepository;
import com.marlus.financas.transaction.repository.TransactionSpecifications;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saldo aproximado de uma conta: saldo inicial + receitas recebidas nela (lançamentos e
 * entradas de receita) − despesas/transferências saídas dela − faturas pagas a partir dela. O
 * app não mantém um ledger de saldo corrente por lançamento; este cálculo é derivado sob
 * demanda a partir do histórico. Ver DECISIONS.md (Fase 7) para as limitações conhecidas.
 */
@Service
@Transactional(readOnly = true)
public class AccountBalanceService {

    private final TransactionRepository transactionRepository;
    private final IncomeEntryRepository incomeEntryRepository;
    private final InvoiceRepository invoiceRepository;

    public AccountBalanceService(
            TransactionRepository transactionRepository,
            IncomeEntryRepository incomeEntryRepository,
            InvoiceRepository invoiceRepository) {
        this.transactionRepository = transactionRepository;
        this.incomeEntryRepository = incomeEntryRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public BigDecimal currentBalance(Account account) {
        Specification<Transaction> spec = Specification.where(TransactionSpecifications.belongsToUser(account.getUserId()))
                .and(TransactionSpecifications.accountId(account.getId()));
        java.util.List<Transaction> transactions = transactionRepository.findAll(spec);

        BigDecimal income = transactions.stream()
                .filter(t -> t.getKind() == TransactionKind.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outflow = transactions.stream()
                .filter(t -> t.getKind() == TransactionKind.EXPENSE || t.getKind() == TransactionKind.TRANSFER)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal incomeEntries = incomeEntryRepository
                .findAllByUserIdAndStatus(account.getUserId(), IncomeEntryStatus.RECEIVED)
                .stream()
                .filter(entry -> account.getId().equals(entry.getAccountId()))
                .map(entry -> entry.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal invoicePayments = invoiceRepository
                .findAllByUserIdAndPaidFromAccountId(account.getUserId(), account.getId())
                .stream()
                .map(invoice -> invoice.getPaidAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return account.getInitialBalance().add(income).add(incomeEntries).subtract(outflow).subtract(invoicePayments);
    }
}
