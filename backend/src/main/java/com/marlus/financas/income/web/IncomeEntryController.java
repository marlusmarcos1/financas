package com.marlus.financas.income.web;

import com.marlus.financas.income.domain.IncomeEntry;
import com.marlus.financas.income.mapper.IncomeEntryMapper;
import com.marlus.financas.income.service.IncomeEntryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/income-entries")
public class IncomeEntryController {

    private final IncomeEntryService incomeEntryService;
    private final IncomeEntryMapper incomeEntryMapper;

    public IncomeEntryController(IncomeEntryService incomeEntryService, IncomeEntryMapper incomeEntryMapper) {
        this.incomeEntryService = incomeEntryService;
        this.incomeEntryMapper = incomeEntryMapper;
    }

    @GetMapping
    public List<IncomeEntryResponse> list(
            @RequestParam(required = false) String referenceMonth, @RequestParam(required = false) UUID sourceId) {
        List<IncomeEntry> entries;
        if (referenceMonth != null) {
            entries = incomeEntryService.findAllByMonth(referenceMonth);
        } else if (sourceId != null) {
            entries = incomeEntryService.findAllBySource(sourceId);
        } else {
            entries = incomeEntryService.findAllByMonth(java.time.YearMonth.now().toString());
        }
        return entries.stream().map(incomeEntryMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public IncomeEntryResponse get(@PathVariable UUID id) {
        return incomeEntryMapper.toResponse(incomeEntryService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncomeEntryResponse create(@Valid @RequestBody IncomeEntryRequest request) {
        IncomeEntry entry = incomeEntryService.create(request);
        return incomeEntryMapper.toResponse(entry);
    }

    @PutMapping("/{id}")
    public IncomeEntryResponse update(@PathVariable UUID id, @Valid @RequestBody IncomeEntryRequest request) {
        IncomeEntry entry = incomeEntryService.update(id, request);
        return incomeEntryMapper.toResponse(entry);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        incomeEntryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
