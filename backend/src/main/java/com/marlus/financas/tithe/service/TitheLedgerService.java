package com.marlus.financas.tithe.service;

import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.common.EntityNotFoundException;
import com.marlus.financas.common.UuidV7Generator;
import com.marlus.financas.income.domain.IncomeEntry;
import com.marlus.financas.income.domain.IncomeEntryStatus;
import com.marlus.financas.income.domain.IncomeSource;
import com.marlus.financas.income.repository.IncomeEntryRepository;
import com.marlus.financas.income.repository.IncomeSourceRepository;
import com.marlus.financas.settings.service.SettingsService;
import com.marlus.financas.tithe.domain.TitheLedger;
import com.marlus.financas.tithe.repository.TitheLedgerRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dízimo automático sobre toda receita recebida com tithe_applies=true, somado por competência (seção 7.5). */
@Service
@Transactional
public class TitheLedgerService {

    private final TitheLedgerRepository titheLedgerRepository;
    private final IncomeEntryRepository incomeEntryRepository;
    private final IncomeSourceRepository incomeSourceRepository;
    private final SettingsService settingsService;
    private final CurrentUserProvider currentUserProvider;

    public TitheLedgerService(
            TitheLedgerRepository titheLedgerRepository,
            IncomeEntryRepository incomeEntryRepository,
            IncomeSourceRepository incomeSourceRepository,
            SettingsService settingsService,
            CurrentUserProvider currentUserProvider) {
        this.titheLedgerRepository = titheLedgerRepository;
        this.incomeEntryRepository = incomeEntryRepository;
        this.incomeSourceRepository = incomeSourceRepository;
        this.settingsService = settingsService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<TitheLedger> findAll() {
        return titheLedgerRepository.findAllByUserIdOrderByReferenceMonthDesc(currentUserProvider.currentUserId());
    }

    @Transactional(readOnly = true)
    public TitheLedger findById(UUID id) {
        return titheLedgerRepository
                .findByIdAndUserId(id, currentUserProvider.currentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Registro de dízimo não encontrado."));
    }

    /** Recalcula (ou cria) o registro de dízimo do mês a partir das receitas recebidas com tithe_applies=true. */
    public TitheLedger recalculateForMonth(UUID userId, String referenceMonth) {
        List<IncomeEntry> received = incomeEntryRepository.findAllByUserIdAndReferenceMonthAndStatus(
                userId, referenceMonth, IncomeEntryStatus.RECEIVED);

        Map<UUID, IncomeSource> sourcesById = incomeSourceRepository.findAllByUserIdOrderByNameAsc(userId).stream()
                .collect(Collectors.toMap(IncomeSource::getId, source -> source));

        BigDecimal base = received.stream()
                .filter(entry -> {
                    IncomeSource source = sourcesById.get(entry.getSourceId());
                    return source != null && source.isTitheApplies();
                })
                .map(IncomeEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal percent = settingsService.getSettings().tithePercent();

        TitheLedger ledger = titheLedgerRepository
                .findByUserIdAndReferenceMonth(userId, referenceMonth)
                .orElseGet(() -> new TitheLedger(UuidV7Generator.generate(), userId, referenceMonth, percent));
        ledger.recalculateBase(base, percent);
        return titheLedgerRepository.save(ledger);
    }

    public TitheLedger registerPayment(UUID id, BigDecimal amount, LocalDate paidOn) {
        TitheLedger ledger = findById(id);
        ledger.registerPayment(amount, paidOn);
        return ledger;
    }
}
