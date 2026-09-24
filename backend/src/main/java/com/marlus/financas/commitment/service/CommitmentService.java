package com.marlus.financas.commitment.service;

import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.commitment.web.EndingPlanResponse;
import com.marlus.financas.commitment.web.MonthCommitmentResponse;
import com.marlus.financas.installment.domain.InstallmentPlan;
import com.marlus.financas.installment.repository.InstallmentPlanRepository;
import com.marlus.financas.transaction.domain.Transaction;
import com.marlus.financas.transaction.repository.TransactionRepository;
import com.marlus.financas.transaction.repository.TransactionSpecifications;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tela "Compromissos futuros" (seção 7.2): quanto de parcelas/recorrências está comprometido mês a mês. */
@Service
@Transactional(readOnly = true)
public class CommitmentService {

    private final TransactionRepository transactionRepository;
    private final InstallmentPlanRepository installmentPlanRepository;
    private final CurrentUserProvider currentUserProvider;

    public CommitmentService(
            TransactionRepository transactionRepository,
            InstallmentPlanRepository installmentPlanRepository,
            CurrentUserProvider currentUserProvider) {
        this.transactionRepository = transactionRepository;
        this.installmentPlanRepository = installmentPlanRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public List<MonthCommitmentResponse> summarize(int months) {
        UUID userId = currentUserProvider.currentUserId();
        YearMonth start = YearMonth.now();
        YearMonth end = start.plusMonths(months - 1L);

        Specification<Transaction> spec = Specification.where(TransactionSpecifications.belongsToUser(userId))
                .and(TransactionSpecifications.dateFrom(start.atDay(1)))
                .and(TransactionSpecifications.dateTo(end.atEndOfMonth()))
                .and(TransactionSpecifications.committed());
        List<Transaction> committedTransactions = transactionRepository.findAll(spec);

        Map<YearMonth, List<Transaction>> byMonth =
                committedTransactions.stream().collect(Collectors.groupingBy(t -> YearMonth.from(t.getDate())));

        List<InstallmentPlan> plans = installmentPlanRepository.findAllByUserIdOrderByPurchaseDateDesc(userId);

        List<MonthCommitmentResponse> result = new ArrayList<>();
        for (YearMonth cursor = start; !cursor.isAfter(end); cursor = cursor.plusMonths(1)) {
            YearMonth month = cursor;
            List<Transaction> monthTransactions = byMonth.getOrDefault(month, List.of());
            BigDecimal installmentsTotal = sum(monthTransactions, t -> t.getInstallmentPlanId() != null);
            BigDecimal recurringTotal = sum(monthTransactions, t -> t.getRecurringRuleId() != null);

            List<EndingPlanResponse> endingPlans = plans.stream()
                    .filter(plan -> plan.getLastInstallmentMonth().equals(month))
                    .map(plan -> new EndingPlanResponse(plan.getId(), plan.getDescription(), plan.getInstallmentAmount()))
                    .toList();

            result.add(new MonthCommitmentResponse(
                    month, installmentsTotal, recurringTotal, installmentsTotal.add(recurringTotal), endingPlans));
        }
        return result;
    }

    private BigDecimal sum(List<Transaction> transactions, java.util.function.Predicate<Transaction> filter) {
        return transactions.stream()
                .filter(filter)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
