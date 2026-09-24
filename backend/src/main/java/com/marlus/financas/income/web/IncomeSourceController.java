package com.marlus.financas.income.web;

import com.marlus.financas.income.domain.IncomeSource;
import com.marlus.financas.income.mapper.IncomeSourceMapper;
import com.marlus.financas.income.service.IncomeSourceService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/income-sources")
public class IncomeSourceController {

    private final IncomeSourceService incomeSourceService;
    private final IncomeSourceMapper incomeSourceMapper;

    public IncomeSourceController(IncomeSourceService incomeSourceService, IncomeSourceMapper incomeSourceMapper) {
        this.incomeSourceService = incomeSourceService;
        this.incomeSourceMapper = incomeSourceMapper;
    }

    @GetMapping
    public List<IncomeSourceResponse> list() {
        return incomeSourceService.findAll().stream().map(incomeSourceMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public IncomeSourceResponse get(@PathVariable UUID id) {
        return incomeSourceMapper.toResponse(incomeSourceService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncomeSourceResponse create(@Valid @RequestBody IncomeSourceRequest request) {
        IncomeSource source = incomeSourceService.create(request);
        return incomeSourceMapper.toResponse(source);
    }

    @PutMapping("/{id}")
    public IncomeSourceResponse update(@PathVariable UUID id, @Valid @RequestBody IncomeSourceRequest request) {
        IncomeSource source = incomeSourceService.update(id, request);
        return incomeSourceMapper.toResponse(source);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        incomeSourceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
