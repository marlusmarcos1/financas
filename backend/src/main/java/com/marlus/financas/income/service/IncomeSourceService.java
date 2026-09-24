package com.marlus.financas.income.service;

import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.common.EntityNotFoundException;
import com.marlus.financas.common.UuidV7Generator;
import com.marlus.financas.income.domain.IncomeSource;
import com.marlus.financas.income.repository.IncomeEntryRepository;
import com.marlus.financas.income.repository.IncomeSourceRepository;
import com.marlus.financas.income.web.IncomeSourceRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class IncomeSourceService {

    private final IncomeSourceRepository incomeSourceRepository;
    private final IncomeEntryRepository incomeEntryRepository;
    private final CurrentUserProvider currentUserProvider;

    public IncomeSourceService(
            IncomeSourceRepository incomeSourceRepository,
            IncomeEntryRepository incomeEntryRepository,
            CurrentUserProvider currentUserProvider) {
        this.incomeSourceRepository = incomeSourceRepository;
        this.incomeEntryRepository = incomeEntryRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<IncomeSource> findAll() {
        return incomeSourceRepository.findAllByUserIdOrderByNameAsc(currentUserProvider.currentUserId());
    }

    @Transactional(readOnly = true)
    public IncomeSource findById(UUID id) {
        return incomeSourceRepository
                .findByIdAndUserId(id, currentUserProvider.currentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Fonte de receita não encontrada."));
    }

    public IncomeSource create(IncomeSourceRequest request) {
        IncomeSource source = new IncomeSource(
                UuidV7Generator.generate(),
                currentUserProvider.currentUserId(),
                request.name(),
                request.type(),
                request.recurrence(),
                request.expectedAmount(),
                request.payDay(),
                request.startDate(),
                request.endDate(),
                request.expectedMonths(),
                request.titheApplies(),
                request.countsInBaseBudget());
        return incomeSourceRepository.save(source);
    }

    public IncomeSource update(UUID id, IncomeSourceRequest request) {
        IncomeSource source = findById(id);
        source.setName(request.name());
        source.setType(request.type());
        source.setRecurrence(request.recurrence());
        source.setExpectedAmount(request.expectedAmount());
        source.setPayDay(request.payDay());
        source.setStartDate(request.startDate());
        source.setEndDate(request.endDate());
        source.setExpectedMonths(request.expectedMonths());
        source.setTitheApplies(request.titheApplies());
        source.setCountsInBaseBudget(request.countsInBaseBudget());
        return source;
    }

    public void delete(UUID id) {
        UUID userId = currentUserProvider.currentUserId();
        IncomeSource source = findById(id);
        if (incomeEntryRepository.existsBySourceIdAndUserId(source.getId(), userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Não é possível excluir: existem receitas lançadas para esta fonte.");
        }
        incomeSourceRepository.delete(source);
    }
}
