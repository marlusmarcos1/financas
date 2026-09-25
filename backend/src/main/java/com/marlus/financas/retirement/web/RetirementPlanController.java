package com.marlus.financas.retirement.web;

import com.marlus.financas.retirement.domain.RetirementPlan;
import com.marlus.financas.retirement.service.RetirementPlanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/retirement-plan")
public class RetirementPlanController {

    private final RetirementPlanService retirementPlanService;

    public RetirementPlanController(RetirementPlanService retirementPlanService) {
        this.retirementPlanService = retirementPlanService;
    }

    @GetMapping
    public RetirementPlanResponse get() {
        return toResponse(retirementPlanService.find());
    }

    @PutMapping
    public RetirementPlanResponse upsert(@Valid @RequestBody RetirementPlanRequest request) {
        return toResponse(retirementPlanService.upsert(request));
    }

    @GetMapping("/projection")
    public RetirementProjectionResponse projection() {
        return retirementPlanService.project();
    }

    private RetirementPlanResponse toResponse(RetirementPlan plan) {
        return new RetirementPlanResponse(
                plan.getId(),
                plan.getMonthlyContribution(),
                plan.getContributionAnnualIncreasePercent(),
                plan.getStartDate(),
                plan.getHorizonYears(),
                plan.getExpectedReturnNominalAnnual(),
                plan.getExpectedInflationAnnual(),
                plan.getCurrentBalance());
    }
}
