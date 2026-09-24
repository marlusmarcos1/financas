package com.marlus.financas.transaction.web;

import com.marlus.financas.transaction.domain.Transaction;
import com.marlus.financas.transaction.domain.TransactionKind;
import com.marlus.financas.transaction.domain.TransactionStatus;
import com.marlus.financas.transaction.mapper.TransactionMapper;
import com.marlus.financas.transaction.service.TransactionService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    public TransactionController(TransactionService transactionService, TransactionMapper transactionMapper) {
        this.transactionService = transactionService;
        this.transactionMapper = transactionMapper;
    }

    @GetMapping
    public Page<TransactionResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID cardId,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionKind kind,
            Pageable pageable) {
        return transactionService
                .search(from, to, categoryId, accountId, cardId, status, kind, pageable)
                .map(transactionMapper::toResponse);
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable UUID id) {
        return transactionMapper.toResponse(transactionService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody TransactionRequest request) {
        Transaction transaction = transactionService.create(request);
        return transactionMapper.toResponse(transaction);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(@PathVariable UUID id, @Valid @RequestBody TransactionRequest request) {
        Transaction transaction = transactionService.update(id, request);
        return transactionMapper.toResponse(transaction);
    }

    @PostMapping("/{id}/mark-as-paid")
    public ResponseEntity<Void> markAsPaid(@PathVariable UUID id) {
        transactionService.markAsPaid(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
