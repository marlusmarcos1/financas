package com.marlus.financas.tithe.web;

import com.marlus.financas.tithe.domain.TitheLedger;
import com.marlus.financas.tithe.mapper.TitheLedgerMapper;
import com.marlus.financas.tithe.service.TitheLedgerService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tithe-ledger")
public class TitheLedgerController {

    private final TitheLedgerService titheLedgerService;
    private final TitheLedgerMapper titheLedgerMapper;

    public TitheLedgerController(TitheLedgerService titheLedgerService, TitheLedgerMapper titheLedgerMapper) {
        this.titheLedgerService = titheLedgerService;
        this.titheLedgerMapper = titheLedgerMapper;
    }

    @GetMapping
    public List<TitheLedgerResponse> list() {
        return titheLedgerService.findAll().stream().map(titheLedgerMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public TitheLedgerResponse get(@PathVariable UUID id) {
        return titheLedgerMapper.toResponse(titheLedgerService.findById(id));
    }

    @PostMapping("/{id}/pay")
    public TitheLedgerResponse pay(@PathVariable UUID id, @Valid @RequestBody TithePaymentRequest request) {
        TitheLedger ledger = titheLedgerService.registerPayment(id, request.amount(), request.paidOn());
        return titheLedgerMapper.toResponse(ledger);
    }
}
