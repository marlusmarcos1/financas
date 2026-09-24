package com.marlus.financas.installment.web;

import com.marlus.financas.installment.domain.InstallmentPlan;
import com.marlus.financas.installment.service.InstallmentCalculator;
import com.marlus.financas.installment.service.InstallmentPlanService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
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
@RequestMapping("/api/v1/installment-plans")
public class InstallmentPlanController {

    private final InstallmentPlanService installmentPlanService;

    public InstallmentPlanController(InstallmentPlanService installmentPlanService) {
        this.installmentPlanService = installmentPlanService;
    }

    @GetMapping
    public List<InstallmentPlanResponse> list() {
        return installmentPlanService.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public InstallmentPlanResponse get(@PathVariable UUID id) {
        return toResponse(installmentPlanService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstallmentPlanResponse create(@Valid @RequestBody InstallmentPlanRequest request) {
        return toResponse(installmentPlanService.create(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        installmentPlanService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private InstallmentPlanResponse toResponse(InstallmentPlan plan) {
        boolean hasInterest = plan.getInterestRateMonthly() != null;
        BigDecimal totalToPay = hasInterest
                ? plan.getInstallmentAmount().multiply(BigDecimal.valueOf(plan.getInstallmentCount()))
                : plan.getTotalAmount();
        BigDecimal totalInterest = hasInterest ? totalToPay.subtract(plan.getTotalAmount()) : BigDecimal.ZERO;
        BigDecimal effectiveRate = hasInterest
                ? InstallmentCalculator.effectiveRate(plan.getInterestRateMonthly(), plan.getInstallmentCount())
                : null;

        return new InstallmentPlanResponse(
                plan.getId(),
                plan.getCardId(),
                plan.getDescription(),
                plan.getPurchaseDate(),
                plan.getTotalAmount(),
                plan.getInstallmentCount(),
                plan.getInstallmentAmount(),
                plan.getFirstInstallmentNumber(),
                plan.getFirstInvoiceMonth(),
                plan.getLastInstallmentMonth(),
                plan.getInterestRateMonthly(),
                totalToPay,
                totalInterest,
                effectiveRate,
                plan.getCategoryId());
    }
}
