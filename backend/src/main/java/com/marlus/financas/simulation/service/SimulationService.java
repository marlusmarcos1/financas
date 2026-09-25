package com.marlus.financas.simulation.service;

import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.category.domain.Category;
import com.marlus.financas.category.domain.CategoryNature;
import com.marlus.financas.category.repository.CategoryRepository;
import com.marlus.financas.common.EntityNotFoundException;
import com.marlus.financas.creditcard.domain.CreditCard;
import com.marlus.financas.creditcard.repository.CreditCardRepository;
import com.marlus.financas.income.domain.IncomeRecurrence;
import com.marlus.financas.income.domain.IncomeSource;
import com.marlus.financas.income.repository.IncomeSourceRepository;
import com.marlus.financas.installment.domain.InstallmentPlan;
import com.marlus.financas.installment.service.InstallmentCalculator;
import com.marlus.financas.installment.repository.InstallmentPlanRepository;
import com.marlus.financas.invoice.service.InvoiceCycle;
import com.marlus.financas.invoice.service.InvoiceCycleCalculator;
import com.marlus.financas.invoice.service.InvoiceService;
import com.marlus.financas.settings.service.SettingsService;
import com.marlus.financas.settings.web.SettingsResponse;
import com.marlus.financas.simulation.web.CardLimitImpactResponse;
import com.marlus.financas.simulation.web.MonthSimulationResponse;
import com.marlus.financas.simulation.web.SimulationRequest;
import com.marlus.financas.simulation.web.SimulationResponse;
import com.marlus.financas.transaction.domain.Transaction;
import com.marlus.financas.transaction.domain.TransactionKind;
import com.marlus.financas.transaction.repository.TransactionRepository;
import com.marlus.financas.transaction.repository.TransactionSpecifications;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orquestra o simulador de compra segura: busca os dados reais e delega o cálculo ao {@link SafePurchaseCalculator}. */
@Service
@Transactional(readOnly = true)
public class SimulationService {

    private static final int EXTRA_HORIZON_MONTHS = 3;
    private static final int VARIABLE_SPEND_HISTORY_MONTHS = 6;

    private final CreditCardRepository creditCardRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final IncomeSourceRepository incomeSourceRepository;
    private final InstallmentPlanRepository installmentPlanRepository;
    private final InvoiceService invoiceService;
    private final InvoiceCycleCalculator invoiceCycleCalculator;
    private final SettingsService settingsService;
    private final CurrentUserProvider currentUserProvider;

    public SimulationService(
            CreditCardRepository creditCardRepository,
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            IncomeSourceRepository incomeSourceRepository,
            InstallmentPlanRepository installmentPlanRepository,
            InvoiceService invoiceService,
            InvoiceCycleCalculator invoiceCycleCalculator,
            SettingsService settingsService,
            CurrentUserProvider currentUserProvider) {
        this.creditCardRepository = creditCardRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.incomeSourceRepository = incomeSourceRepository;
        this.installmentPlanRepository = installmentPlanRepository;
        this.invoiceService = invoiceService;
        this.invoiceCycleCalculator = invoiceCycleCalculator;
        this.settingsService = settingsService;
        this.currentUserProvider = currentUserProvider;
    }

    public SimulationResponse simulate(SimulationRequest request) {
        UUID userId = currentUserProvider.currentUserId();
        CreditCard card = creditCardRepository
                .findByIdAndUserId(request.cardId(), userId)
                .orElseThrow(() -> new EntityNotFoundException("Cartão não encontrado."));

        int installmentCount = request.installmentCount();
        int horizonMonths = installmentCount + EXTRA_HORIZON_MONTHS;
        YearMonth today = YearMonth.now();
        List<YearMonth> horizon = new ArrayList<>();
        for (int i = 0; i < horizonMonths; i++) {
            horizon.add(today.plusMonths(i));
        }

        InvoiceCycle cycle = invoiceCycleCalculator.calculate(request.purchaseDate(), card.getClosingDay(), card.getDueDay());
        YearMonth firstInstallmentMonth = cycle.referenceMonth();
        YearMonth lastInstallmentMonth = firstInstallmentMonth.plusMonths(installmentCount - 1L);

        boolean hasInterest = request.interestRateMonthly() != null && request.interestRateMonthly().signum() > 0;
        BigDecimal installmentAmount = hasInterest
                ? InstallmentCalculator.pricePayment(request.totalAmount(), request.interestRateMonthly(), installmentCount)
                : InstallmentCalculator.withoutInterest(request.totalAmount(), installmentCount).get(0);

        List<BigDecimal> newInstallmentAmounts = horizon.stream()
                .map(month -> !month.isBefore(firstInstallmentMonth) && !month.isAfter(lastInstallmentMonth)
                        ? installmentAmount
                        : BigDecimal.ZERO)
                .toList();

        List<MonthCommitmentInput> existingCommitments = buildExistingCommitments(userId, horizon);

        SettingsResponse settings = settingsService.getSettings();
        BigDecimal baseIncomeMonthly = calculateBaseIncomeMonthly(userId);

        SimulationResult result = SafePurchaseCalculator.simulate(
                baseIncomeMonthly,
                settings.tithePercent(),
                settings.installmentLimitPercent(),
                existingCommitments,
                newInstallmentAmounts);

        BigDecimal availableLimitBefore = invoiceService.availableLimit(card.getId());
        BigDecimal totalNewPlanCost = installmentAmount.multiply(BigDecimal.valueOf(installmentCount));
        BigDecimal availableLimitAfter = availableLimitBefore.subtract(totalNewPlanCost);

        List<String> suggestions =
                buildSuggestions(userId, result.verdict(), installmentCount, horizon, request.totalAmount());

        return new SimulationResponse(
                result.verdict(),
                installmentAmount,
                result.maxSafeInstallmentToday(),
                new CardLimitImpactResponse(card.getCreditLimit(), availableLimitBefore, availableLimitAfter),
                result.months().stream().map(this::toResponse).toList(),
                suggestions);
    }

    private List<MonthCommitmentInput> buildExistingCommitments(UUID userId, List<YearMonth> horizon) {
        YearMonth from = horizon.get(0);
        YearMonth to = horizon.get(horizon.size() - 1);
        Specification<Transaction> spec = Specification.where(TransactionSpecifications.belongsToUser(userId))
                .and(TransactionSpecifications.committed())
                .and(TransactionSpecifications.dateFrom(from.atDay(1)))
                .and(TransactionSpecifications.dateTo(to.atEndOfMonth()));
        List<Transaction> committed = transactionRepository.findAll(spec);

        BigDecimal averageVariableSpend = calculateAverageVariableSpend(userId);

        return horizon.stream()
                .map(month -> {
                    BigDecimal existingInstallments = sumWhere(committed, month, t -> t.getInstallmentPlanId() != null);
                    BigDecimal fixedRecurring = sumWhere(committed, month, t -> t.getRecurringRuleId() != null);
                    return new MonthCommitmentInput(month, existingInstallments, fixedRecurring, averageVariableSpend);
                })
                .toList();
    }

    private BigDecimal sumWhere(List<Transaction> transactions, YearMonth month, java.util.function.Predicate<Transaction> filter) {
        return transactions.stream()
                .filter(t -> YearMonth.from(t.getDate()).equals(month))
                .filter(filter)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Média móvel dos gastos variáveis avulsos (sem parcelamento/recorrência) nos últimos meses fechados. */
    private BigDecimal calculateAverageVariableSpend(UUID userId) {
        Set<UUID> variableCategoryIds = categoryRepository.findAllByUserIdOrderByNameAsc(userId).stream()
                .filter(category -> category.getNature() == CategoryNature.VARIABLE)
                .map(Category::getId)
                .collect(java.util.stream.Collectors.toSet());
        if (variableCategoryIds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        YearMonth currentMonth = YearMonth.now();
        YearMonth from = currentMonth.minusMonths(VARIABLE_SPEND_HISTORY_MONTHS);
        YearMonth to = currentMonth.minusMonths(1);

        Specification<Transaction> spec = Specification.where(TransactionSpecifications.belongsToUser(userId))
                .and(TransactionSpecifications.kind(TransactionKind.EXPENSE))
                .and(TransactionSpecifications.adHoc())
                .and(TransactionSpecifications.dateFrom(from.atDay(1)))
                .and(TransactionSpecifications.dateTo(to.atEndOfMonth()));

        BigDecimal total = transactionRepository.findAll(spec).stream()
                .filter(t -> variableCategoryIds.contains(t.getCategoryId()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(BigDecimal.valueOf(VARIABLE_SPEND_HISTORY_MONTHS), 2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calculateBaseIncomeMonthly(UUID userId) {
        return incomeSourceRepository.findAllByUserIdOrderByNameAsc(userId).stream()
                .filter(source -> source.isCountsInBaseBudget() && source.getRecurrence() == IncomeRecurrence.MONTHLY)
                .map(IncomeSource::getExpectedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<String> buildSuggestions(
            UUID userId, SimulationVerdict verdict, int installmentCount, List<YearMonth> horizon, BigDecimal totalAmount) {
        if (verdict == SimulationVerdict.SAFE) {
            return List.of();
        }

        List<String> suggestions = new ArrayList<>();
        if (installmentCount > 1) {
            int fewerInstallments = Math.max(1, installmentCount / 2);
            suggestions.add(
                    "Considere parcelar em menos vezes (ex.: " + fewerInstallments + "x em vez de " + installmentCount + "x).");
        }

        installmentPlanRepository.findAllByUserIdOrderByPurchaseDateDesc(userId).stream()
                .map(InstallmentPlan::getLastInstallmentMonth)
                .filter(month -> horizon.contains(month))
                .min(java.util.Comparator.naturalOrder())
                .ifPresent(month -> suggestions.add(
                        "Espere até " + month + ", quando um parcelamento em andamento termina e libera espaço no orçamento."));

        suggestions.add("Se houver reserva de emergência suficiente, avalie pagar à vista em vez de parcelar "
                + "(evita juros e comprometimento futuro).");
        return suggestions;
    }

    private MonthSimulationResponse toResponse(MonthSimulation month) {
        return new MonthSimulationResponse(
                month.month(),
                month.existingInstallments(),
                month.newInstallment(),
                month.fixedRecurring(),
                month.averageVariableSpend(),
                month.titheDue(),
                month.totalCommittedInstallments(),
                month.commitmentPercent(),
                month.freeBalance(),
                month.freeBalanceWithoutPurchase());
    }
}
