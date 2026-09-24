package com.marlus.financas.recurring.service;

import com.marlus.financas.account.repository.AccountRepository;
import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.category.repository.CategoryRepository;
import com.marlus.financas.common.EntityNotFoundException;
import com.marlus.financas.common.UuidV7Generator;
import com.marlus.financas.creditcard.repository.CreditCardRepository;
import com.marlus.financas.recurring.domain.RecurringFrequency;
import com.marlus.financas.recurring.domain.RecurringRule;
import com.marlus.financas.recurring.repository.RecurringRuleRepository;
import com.marlus.financas.recurring.web.RecurringRuleRequest;
import com.marlus.financas.transaction.domain.Transaction;
import com.marlus.financas.transaction.domain.TransactionKind;
import com.marlus.financas.transaction.domain.TransactionStatus;
import com.marlus.financas.transaction.repository.TransactionRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** CRUD de recorrências e materialização das ocorrências futuras em lançamentos PLANNED (seção 7 / item recorrências). */
@Service
@Transactional
public class RecurringRuleService {

    private static final int MATERIALIZATION_HORIZON_MONTHS = 12;

    private final RecurringRuleRepository recurringRuleRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final CreditCardRepository creditCardRepository;
    private final CurrentUserProvider currentUserProvider;

    public RecurringRuleService(
            RecurringRuleRepository recurringRuleRepository,
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            AccountRepository accountRepository,
            CreditCardRepository creditCardRepository,
            CurrentUserProvider currentUserProvider) {
        this.recurringRuleRepository = recurringRuleRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
        this.creditCardRepository = creditCardRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<RecurringRule> findAll() {
        return recurringRuleRepository.findAllByUserIdOrderByDescriptionAsc(currentUserProvider.currentUserId());
    }

    @Transactional(readOnly = true)
    public RecurringRule findById(UUID id) {
        return recurringRuleRepository
                .findByIdAndUserId(id, currentUserProvider.currentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Recorrência não encontrada."));
    }

    public RecurringRule create(RecurringRuleRequest request) {
        UUID userId = currentUserProvider.currentUserId();
        validate(request, userId);

        RecurringRule rule = new RecurringRule(
                UuidV7Generator.generate(),
                userId,
                request.description(),
                request.amount(),
                request.amountIsVariable(),
                request.frequency(),
                request.dayOfMonth(),
                request.startDate(),
                request.endDate(),
                request.categoryId(),
                request.cardId(),
                request.accountId());
        recurringRuleRepository.save(rule);
        materialize(rule);
        return rule;
    }

    public RecurringRule update(UUID id, RecurringRuleRequest request) {
        RecurringRule rule = findById(id);
        validate(request, rule.getUserId());

        rule.setDescription(request.description());
        rule.setAmount(request.amount());
        rule.setAmountIsVariable(request.amountIsVariable());
        rule.setFrequency(request.frequency());
        rule.setDayOfMonth(request.dayOfMonth());
        rule.setStartDate(request.startDate());
        rule.setEndDate(request.endDate());
        rule.setCategoryId(request.categoryId());
        rule.setPaymentMethod(request.cardId(), request.accountId());
        rule.setActive(request.active());

        if (rule.isActive()) {
            materialize(rule);
        }
        return rule;
    }

    public void delete(UUID id) {
        recurringRuleRepository.delete(findById(id));
    }

    /** Gera os lançamentos PLANNED ainda não criados para os próximos 12 meses desta recorrência. */
    public void materialize(RecurringRule rule) {
        if (!rule.isActive()) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusMonths(MATERIALIZATION_HORIZON_MONTHS);

        for (LocalDate occurrence : occurrenceDates(rule, today, horizon)) {
            if (transactionRepository.existsByRecurringRuleIdAndDate(rule.getId(), occurrence)) {
                continue;
            }
            Transaction transaction = new Transaction(
                    UuidV7Generator.generate(),
                    rule.getUserId(),
                    TransactionKind.EXPENSE,
                    rule.getDescription(),
                    rule.getAmount(),
                    occurrence,
                    rule.getCategoryId(),
                    rule.getAccountId(),
                    rule.getCardId(),
                    TransactionStatus.PLANNED,
                    null);
            transaction.setRecurringRuleId(rule.getId());
            transactionRepository.save(transaction);
        }
    }

    /** Materializa todas as recorrências ativas do usuário atual — usado pelo job agendado. */
    public void materializeAllActive() {
        for (RecurringRule rule : recurringRuleRepository.findAllByActiveTrue()) {
            materialize(rule);
        }
    }

    List<LocalDate> occurrenceDates(RecurringRule rule, LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate rangeStart = rule.getStartDate().isAfter(from) ? rule.getStartDate() : from;
        LocalDate rangeEnd = rule.getEndDate() != null && rule.getEndDate().isBefore(to) ? rule.getEndDate() : to;
        if (rangeStart.isAfter(rangeEnd)) {
            return dates;
        }

        YearMonth cursor = YearMonth.from(rangeStart);
        YearMonth lastMonth = YearMonth.from(rangeEnd);
        int anchorMonth = rule.getStartDate().getMonthValue();

        while (!cursor.isAfter(lastMonth)) {
            boolean matchesFrequency =
                    rule.getFrequency() == RecurringFrequency.MONTHLY || cursor.getMonthValue() == anchorMonth;
            if (matchesFrequency) {
                LocalDate occurrence = cursor.atDay(Math.min(rule.getDayOfMonth(), cursor.lengthOfMonth()));
                if (!occurrence.isBefore(rangeStart) && !occurrence.isAfter(rangeEnd)) {
                    dates.add(occurrence);
                }
            }
            cursor = cursor.plusMonths(1);
        }
        return dates;
    }

    private void validate(RecurringRuleRequest request, UUID userId) {
        if ((request.cardId() == null) == (request.accountId() == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Informe cartão ou conta para a recorrência, não os dois.");
        }
        categoryRepository
                .findByIdAndUserId(request.categoryId(), userId)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));
        if (request.accountId() != null) {
            accountRepository
                    .findByIdAndUserId(request.accountId(), userId)
                    .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada."));
        }
        if (request.cardId() != null) {
            creditCardRepository
                    .findByIdAndUserId(request.cardId(), userId)
                    .orElseThrow(() -> new EntityNotFoundException("Cartão não encontrado."));
        }
    }
}
