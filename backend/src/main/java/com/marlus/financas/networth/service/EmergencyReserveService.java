package com.marlus.financas.networth.service;

import com.marlus.financas.account.domain.Account;
import com.marlus.financas.account.domain.AccountPurpose;
import com.marlus.financas.account.repository.AccountRepository;
import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.networth.web.EmergencyReserveResponse;
import com.marlus.financas.settings.service.SettingsService;
import com.marlus.financas.transaction.domain.Transaction;
import com.marlus.financas.transaction.domain.TransactionKind;
import com.marlus.financas.transaction.repository.TransactionRepository;
import com.marlus.financas.transaction.repository.TransactionSpecifications;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reserva de emergência (seção 7.7): meta = meses-alvo × média dos gastos dos últimos 6 meses.
 * Como as categorias não têm uma marcação explícita de "essencial", usamos a média de TODAS as
 * despesas dos últimos 6 meses como aproximação — documentado em DECISIONS.md.
 */
@Service
@Transactional(readOnly = true)
public class EmergencyReserveService {

    private static final int EXPENSE_HISTORY_MONTHS = 6;

    private final AccountRepository accountRepository;
    private final AccountBalanceService accountBalanceService;
    private final TransactionRepository transactionRepository;
    private final SettingsService settingsService;
    private final CurrentUserProvider currentUserProvider;

    public EmergencyReserveService(
            AccountRepository accountRepository,
            AccountBalanceService accountBalanceService,
            TransactionRepository transactionRepository,
            SettingsService settingsService,
            CurrentUserProvider currentUserProvider) {
        this.accountRepository = accountRepository;
        this.accountBalanceService = accountBalanceService;
        this.transactionRepository = transactionRepository;
        this.settingsService = settingsService;
        this.currentUserProvider = currentUserProvider;
    }

    public EmergencyReserveResponse calculate() {
        UUID userId = currentUserProvider.currentUserId();

        BigDecimal currentReserve = accountRepository.findAllByUserIdOrderByArchivedAscNameAsc(userId).stream()
                .filter(account -> !account.isArchived())
                .filter(account -> account.getPurpose() == AccountPurpose.EMERGENCY_RESERVE)
                .map(accountBalanceService::currentBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        YearMonth currentMonth = YearMonth.now();
        YearMonth from = currentMonth.minusMonths(EXPENSE_HISTORY_MONTHS);
        YearMonth to = currentMonth.minusMonths(1);
        Specification<Transaction> spec = Specification.where(TransactionSpecifications.belongsToUser(userId))
                .and(TransactionSpecifications.kind(TransactionKind.EXPENSE))
                .and(TransactionSpecifications.dateFrom(from.atDay(1)))
                .and(TransactionSpecifications.dateTo(to.atEndOfMonth()));
        BigDecimal totalExpenses = transactionRepository.findAll(spec).stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageEssentialExpense =
                totalExpenses.divide(BigDecimal.valueOf(EXPENSE_HISTORY_MONTHS), 2, RoundingMode.HALF_EVEN);

        int emergencyMonthsTarget = settingsService.getSettings().emergencyMonthsTarget();
        BigDecimal targetAmount = averageEssentialExpense.multiply(BigDecimal.valueOf(emergencyMonthsTarget));
        BigDecimal monthsOfCoverage = averageEssentialExpense.signum() > 0
                ? currentReserve.divide(averageEssentialExpense, 1, RoundingMode.HALF_EVEN)
                : BigDecimal.ZERO;

        return new EmergencyReserveResponse(
                currentReserve, averageEssentialExpense, emergencyMonthsTarget, targetAmount, monthsOfCoverage);
    }
}
