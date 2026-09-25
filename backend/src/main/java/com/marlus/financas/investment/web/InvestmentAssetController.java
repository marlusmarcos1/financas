package com.marlus.financas.investment.web;

import com.marlus.financas.investment.domain.InvestmentAsset;
import com.marlus.financas.investment.mapper.InvestmentAssetMapper;
import com.marlus.financas.investment.service.InvestmentAssetService;
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
@RequestMapping("/api/v1/investment-assets")
public class InvestmentAssetController {

    private final InvestmentAssetService investmentAssetService;
    private final InvestmentAssetMapper investmentAssetMapper;

    public InvestmentAssetController(InvestmentAssetService investmentAssetService, InvestmentAssetMapper investmentAssetMapper) {
        this.investmentAssetService = investmentAssetService;
        this.investmentAssetMapper = investmentAssetMapper;
    }

    @GetMapping
    public List<InvestmentAssetResponse> list() {
        return investmentAssetService.findAll().stream().map(investmentAssetMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public InvestmentAssetResponse get(@PathVariable UUID id) {
        return investmentAssetMapper.toResponse(investmentAssetService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvestmentAssetResponse create(@Valid @RequestBody InvestmentAssetRequest request) {
        InvestmentAsset asset = investmentAssetService.create(request);
        return investmentAssetMapper.toResponse(asset);
    }

    @PutMapping("/{id}")
    public InvestmentAssetResponse update(@PathVariable UUID id, @Valid @RequestBody InvestmentAssetRequest request) {
        InvestmentAsset asset = investmentAssetService.update(id, request);
        return investmentAssetMapper.toResponse(asset);
    }

    @PutMapping("/{id}/price")
    public InvestmentAssetResponse updatePrice(@PathVariable UUID id, @Valid @RequestBody PriceUpdateRequest request) {
        InvestmentAsset asset = investmentAssetService.updatePrice(id, request.price());
        return investmentAssetMapper.toResponse(asset);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        investmentAssetService.archive(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable UUID id) {
        investmentAssetService.unarchive(id);
        return ResponseEntity.noContent().build();
    }
}
