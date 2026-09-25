package com.marlus.financas.retirement.service;

import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.common.EntityNotFoundException;
import com.marlus.financas.common.UuidV7Generator;
import com.marlus.financas.retirement.domain.RetirementPlan;
import com.marlus.financas.retirement.repository.RetirementPlanRepository;
import com.marlus.financas.retirement.web.RetirementPlanRequest;
import com.marlus.financas.retirement.web.RetirementProjectionResponse;
import com.marlus.financas.retirement.web.RetirementScenarioResponse;
import com.marlus.financas.retirement.web.YearSnapshotResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RetirementPlanService {

    private static final List<ScenarioDefinition> SCENARIOS = List.of(
            new ScenarioDefinition("Pessimista", new BigDecimal("0.06")),
            new ScenarioDefinition("Base", new BigDecimal("0.08")),
            new ScenarioDefinition("Otimista", new BigDecimal("0.10")));

    private final RetirementPlanRepository retirementPlanRepository;
    private final CurrentUserProvider currentUserProvider;

    public RetirementPlanService(RetirementPlanRepository retirementPlanRepository, CurrentUserProvider currentUserProvider) {
        this.retirementPlanRepository = retirementPlanRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public RetirementPlan find() {
        return retirementPlanRepository
                .findByUserId(currentUserProvider.currentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Nenhum plano de aposentadoria cadastrado ainda."));
    }

    public RetirementPlan upsert(RetirementPlanRequest request) {
        UUID userId = currentUserProvider.currentUserId();
        RetirementPlan plan = retirementPlanRepository
                .findByUserId(userId)
                .orElseGet(() -> new RetirementPlan(
                        UuidV7Generator.generate(),
                        userId,
                        request.monthlyContribution(),
                        request.contributionAnnualIncreasePercent(),
                        request.startDate(),
                        request.horizonYears(),
                        request.expectedReturnNominalAnnual(),
                        request.expectedInflationAnnual(),
                        request.currentBalance()));
        plan.update(
                request.monthlyContribution(),
                request.contributionAnnualIncreasePercent(),
                request.startDate(),
                request.horizonYears(),
                request.expectedReturnNominalAnnual(),
                request.expectedInflationAnnual(),
                request.currentBalance());
        return retirementPlanRepository.save(plan);
    }

    @Transactional(readOnly = true)
    public RetirementProjectionResponse project() {
        RetirementPlan plan = find();
        List<RetirementScenarioResponse> scenarios = SCENARIOS.stream().map(scenario -> {
            RetirementProjectionResult result = RetirementProjectionCalculator.project(
                    plan.getCurrentBalance(),
                    plan.getMonthlyContribution(),
                    plan.getContributionAnnualIncreasePercent(),
                    plan.getHorizonYears(),
                    scenario.annualRate());

            BigDecimal realRate = RetirementProjectionCalculator.realAnnualRate(
                    scenario.annualRate(), plan.getExpectedInflationAnnual());
            RetirementProjectionResult realResult = RetirementProjectionCalculator.project(
                    plan.getCurrentBalance(),
                    plan.getMonthlyContribution(),
                    plan.getContributionAnnualIncreasePercent(),
                    plan.getHorizonYears(),
                    realRate);

            List<YearSnapshotResponse> yearlyBalances = RetirementProjectionCalculator.projectYearlyBalances(
                            plan.getCurrentBalance(),
                            plan.getMonthlyContribution(),
                            plan.getContributionAnnualIncreasePercent(),
                            plan.getHorizonYears(),
                            scenario.annualRate())
                    .stream()
                    .map(snapshot -> new YearSnapshotResponse(snapshot.year(), snapshot.balance()))
                    .toList();

            return new RetirementScenarioResponse(
                    scenario.label(),
                    scenario.annualRate(),
                    result.finalBalance(),
                    result.totalContributed(),
                    result.interestEarned(),
                    realResult.finalBalance(),
                    yearlyBalances);
        }).toList();

        return new RetirementProjectionResponse(scenarios);
    }

    private record ScenarioDefinition(String label, BigDecimal annualRate) {
    }
}
