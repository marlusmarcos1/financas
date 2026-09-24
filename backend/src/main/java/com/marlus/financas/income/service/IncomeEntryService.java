package com.marlus.financas.income.service;

import com.marlus.financas.account.repository.AccountRepository;
import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.common.EntityNotFoundException;
import com.marlus.financas.common.UuidV7Generator;
import com.marlus.financas.income.domain.IncomeEntry;
import com.marlus.financas.income.domain.IncomeSource;
import com.marlus.financas.income.repository.IncomeEntryRepository;
import com.marlus.financas.income.repository.IncomeSourceRepository;
import com.marlus.financas.income.web.IncomeEntryRequest;
import com.marlus.financas.tithe.service.TitheLedgerService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IncomeEntryService {

    private final IncomeEntryRepository incomeEntryRepository;
    private final IncomeSourceRepository incomeSourceRepository;
    private final AccountRepository accountRepository;
    private final TitheLedgerService titheLedgerService;
    private final CurrentUserProvider currentUserProvider;

    public IncomeEntryService(
            IncomeEntryRepository incomeEntryRepository,
            IncomeSourceRepository incomeSourceRepository,
            AccountRepository accountRepository,
            TitheLedgerService titheLedgerService,
            CurrentUserProvider currentUserProvider) {
        this.incomeEntryRepository = incomeEntryRepository;
        this.incomeSourceRepository = incomeSourceRepository;
        this.accountRepository = accountRepository;
        this.titheLedgerService = titheLedgerService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<IncomeEntry> findAllByMonth(String referenceMonth) {
        return incomeEntryRepository.findAllByUserIdAndReferenceMonthOrderByReceivedOnAsc(
                currentUserProvider.currentUserId(), referenceMonth);
    }

    @Transactional(readOnly = true)
    public List<IncomeEntry> findAllBySource(UUID sourceId) {
        return incomeEntryRepository.findAllByUserIdAndSourceIdOrderByReferenceMonthDesc(
                currentUserProvider.currentUserId(), sourceId);
    }

    @Transactional(readOnly = true)
    public IncomeEntry findById(UUID id) {
        return incomeEntryRepository
                .findByIdAndUserId(id, currentUserProvider.currentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Lançamento de receita não encontrado."));
    }

    public IncomeEntry create(IncomeEntryRequest request) {
        UUID userId = currentUserProvider.currentUserId();
        IncomeSource source = validateSource(request.sourceId(), userId);
        validateAccount(request.accountId(), userId);

        IncomeEntry entry = new IncomeEntry(
                UuidV7Generator.generate(),
                userId,
                source.getId(),
                request.accountId(),
                request.referenceMonth(),
                request.receivedOn(),
                request.amount(),
                request.status());
        incomeEntryRepository.save(entry);
        titheLedgerService.recalculateForMonth(userId, request.referenceMonth());
        return entry;
    }

    public IncomeEntry update(UUID id, IncomeEntryRequest request) {
        IncomeEntry entry = findById(id);
        UUID userId = entry.getUserId();
        validateSource(request.sourceId(), userId);
        validateAccount(request.accountId(), userId);

        String previousMonth = entry.getReferenceMonth();
        entry.setAccountId(request.accountId());
        entry.setReferenceMonth(request.referenceMonth());
        entry.setReceivedOn(request.receivedOn());
        entry.setAmount(request.amount());
        entry.setStatus(request.status());

        titheLedgerService.recalculateForMonth(userId, request.referenceMonth());
        if (!previousMonth.equals(request.referenceMonth())) {
            titheLedgerService.recalculateForMonth(userId, previousMonth);
        }
        return entry;
    }

    public void delete(UUID id) {
        IncomeEntry entry = findById(id);
        UUID userId = entry.getUserId();
        String month = entry.getReferenceMonth();
        incomeEntryRepository.delete(entry);
        titheLedgerService.recalculateForMonth(userId, month);
    }

    private IncomeSource validateSource(UUID sourceId, UUID userId) {
        return incomeSourceRepository
                .findByIdAndUserId(sourceId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Fonte de receita não encontrada."));
    }

    private void validateAccount(UUID accountId, UUID userId) {
        if (accountId == null) {
            return;
        }
        accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada."));
    }
}
