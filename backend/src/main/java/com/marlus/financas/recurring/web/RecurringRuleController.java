package com.marlus.financas.recurring.web;

import com.marlus.financas.recurring.domain.RecurringRule;
import com.marlus.financas.recurring.mapper.RecurringRuleMapper;
import com.marlus.financas.recurring.service.RecurringRuleService;
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
@RequestMapping("/api/v1/recurring-rules")
public class RecurringRuleController {

    private final RecurringRuleService recurringRuleService;
    private final RecurringRuleMapper recurringRuleMapper;

    public RecurringRuleController(RecurringRuleService recurringRuleService, RecurringRuleMapper recurringRuleMapper) {
        this.recurringRuleService = recurringRuleService;
        this.recurringRuleMapper = recurringRuleMapper;
    }

    @GetMapping
    public List<RecurringRuleResponse> list() {
        return recurringRuleService.findAll().stream().map(recurringRuleMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public RecurringRuleResponse get(@PathVariable UUID id) {
        return recurringRuleMapper.toResponse(recurringRuleService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringRuleResponse create(@Valid @RequestBody RecurringRuleRequest request) {
        RecurringRule rule = recurringRuleService.create(request);
        return recurringRuleMapper.toResponse(rule);
    }

    @PutMapping("/{id}")
    public RecurringRuleResponse update(@PathVariable UUID id, @Valid @RequestBody RecurringRuleRequest request) {
        RecurringRule rule = recurringRuleService.update(id, request);
        return recurringRuleMapper.toResponse(rule);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        recurringRuleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
