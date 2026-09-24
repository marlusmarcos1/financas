package com.marlus.financas.dashboard.service;

import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.category.service.BudgetSummaryService;
import com.marlus.financas.dashboard.web.DashboardResponse;
import com.marlus.financas.income.domain.IncomeEntry;
import com.marlus.financas.income.domain.IncomeEntryStatus;
import com.marlus.financas.income.domain.IncomeSource;
import com.marlus.financas.income.repository.IncomeEntryRepository;
import com.marlus.financas.income.repository.IncomeSourceRepository;
import com.marlus.financas.tithe.domain.TitheLedger;
import com.marlus.financas.tithe.repository.TitheLedgerRepository;
import com.marlus.financas.transaction.domain.Transaction;
import com.marlus.financas.transaction.domain.TransactionKind;
import com.marlus.financas.transaction.repository.TransactionRepository;
import com.marlus.financas.transaction.repository.TransactionSpecifications;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resumo do mês: renda base, extras, despesas, dízimo e sobra (seção 7.3 / tela Dashboard). */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final IncomeEntryRepository incomeEntryRepository;
    private final IncomeSourceRepository incomeSourceRepository;
    private final TitheLedgerRepository titheLedgerRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetSummaryService budgetSummaryService;
    private final CurrentUserProvider currentUserProvider;

    public DashboardService(
            IncomeEntryRepository incomeEntryRepository,
            IncomeSourceRepository incomeSourceRepository,
            TitheLedgerRepository titheLedgerRepository,
            TransactionRepository transactionRepository,
            BudgetSummaryService budgetSummaryService,
            CurrentUserProvider currentUserProvider) {
        this.incomeEntryRepository = incomeEntryRepository;
        this.incomeSourceRepository = incomeSourceRepository;
        this.titheLedgerRepository = titheLedgerRepository;
        this.transactionRepository = transactionRepository;
        this.budgetSummaryService = budgetSummaryService;
        this.currentUserProvider = currentUserProvider;
    }

    public DashboardResponse summarize(YearMonth month) {
        UUID userId = currentUserProvider.currentUserId();
        String referenceMonth = month.toString();

        Map<UUID, IncomeSource> sourcesById = incomeSourceRepository.findAllByUserIdOrderByNameAsc(userId).stream()
                .collect(Collectors.toMap(IncomeSource::getId, source -> source));
        List<IncomeEntry> entries =
                incomeEntryRepository.findAllByUserIdAndReferenceMonthOrderByReceivedOnAsc(userId, referenceMonth);

        BigDecimal baseIncomeExpected = sumWhere(entries, sourcesById, IncomeSource::isCountsInBaseBudget, null);
        BigDecimal baseIncomeReceived =
                sumWhere(entries, sourcesById, IncomeSource::isCountsInBaseBudget, IncomeEntryStatus.RECEIVED);
        BigDecimal extrasReceived = sumWhere(
                entries, sourcesById, source -> !source.isCountsInBaseBudget(), IncomeEntryStatus.RECEIVED);

        Specification<Transaction> expenseSpec = Specification.where(TransactionSpecifications.belongsToUser(userId))
                .and(TransactionSpecifications.kind(TransactionKind.EXPENSE))
                .and(TransactionSpecifications.dateFrom(month.atDay(1)))
                .and(TransactionSpecifications.dateTo(month.atEndOfMonth()));
        BigDecimal totalExpenses = transactionRepository.findAll(expenseSpec).stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        TitheLedger tithe = titheLedgerRepository.findByUserIdAndReferenceMonth(userId, referenceMonth).orElse(null);
        BigDecimal titheDue = tithe != null ? tithe.getDueAmount() : BigDecimal.ZERO;
        BigDecimal titheOutstanding = tithe != null ? titheDue.subtract(tithe.getPaidAmount()) : BigDecimal.ZERO;

        BigDecimal surplus = baseIncomeReceived.subtract(totalExpenses).subtract(titheOutstanding);

        return new DashboardResponse(
                month,
                baseIncomeExpected,
                baseIncomeReceived,
                extrasReceived,
                totalExpenses,
                titheDue,
                titheOutstanding,
                surplus,
                budgetSummaryService.summarize(month));
    }

    private BigDecimal sumWhere(
            List<IncomeEntry> entries,
            Map<UUID, IncomeSource> sourcesById,
            java.util.function.Predicate<IncomeSource> sourceFilter,
            IncomeEntryStatus status) {
        return entries.stream()
                .filter(entry -> status == null || entry.getStatus() == status)
                .filter(entry -> {
                    IncomeSource source = sourcesById.get(entry.getSourceId());
                    return source != null && sourceFilter.test(source);
                })
                .map(IncomeEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
