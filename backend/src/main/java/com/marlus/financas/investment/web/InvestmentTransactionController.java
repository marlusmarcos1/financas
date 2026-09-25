package com.marlus.financas.investment.web;

import com.marlus.financas.investment.domain.InvestmentTransaction;
import com.marlus.financas.investment.mapper.InvestmentTransactionMapper;
import com.marlus.financas.investment.service.InvestmentTransactionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/investment-assets/{assetId}/transactions")
public class InvestmentTransactionController {

    private final InvestmentTransactionService investmentTransactionService;
    private final InvestmentTransactionMapper investmentTransactionMapper;

    public InvestmentTransactionController(
            InvestmentTransactionService investmentTransactionService, InvestmentTransactionMapper investmentTransactionMapper) {
        this.investmentTransactionService = investmentTransactionService;
        this.investmentTransactionMapper = investmentTransactionMapper;
    }

    @GetMapping
    public List<InvestmentTransactionResponse> list(@PathVariable UUID assetId) {
        return investmentTransactionService.findAllByAsset(assetId).stream()
                .map(investmentTransactionMapper::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvestmentTransactionResponse create(
            @PathVariable UUID assetId, @Valid @RequestBody InvestmentTransactionRequest request) {
        InvestmentTransaction transaction = investmentTransactionService.create(assetId, request);
        return investmentTransactionMapper.toResponse(transaction);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID assetId, @PathVariable UUID id) {
        investmentTransactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
