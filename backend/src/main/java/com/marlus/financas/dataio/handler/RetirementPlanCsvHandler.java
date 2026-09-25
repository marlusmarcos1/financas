package com.marlus.financas.dataio.handler;

import com.marlus.financas.dataio.csv.CsvFieldParser;
import com.marlus.financas.dataio.csv.RowValidationException;
import com.marlus.financas.dataio.service.EntityCsvHandler;
import com.marlus.financas.dataio.service.RowResult;
import com.marlus.financas.retirement.domain.RetirementPlan;
import com.marlus.financas.retirement.repository.RetirementPlanRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Existe no máximo um {@link RetirementPlan} por usuário (ver {@code findByUserId}). */
@Component
public class RetirementPlanCsvHandler implements EntityCsvHandler {

    private final RetirementPlanRepository retirementPlanRepository;

    public RetirementPlanCsvHandler(RetirementPlanRepository retirementPlanRepository) {
        this.retirementPlanRepository = retirementPlanRepository;
    }

    @Override
    public String fileName() {
        return "retirement_plans.csv";
    }

    @Override
    public List<String> header() {
        return List.of(
                "id",
                "monthly_contribution",
                "contribution_annual_increase_percent",
                "start_date",
                "horizon_years",
                "expected_return_nominal_annual",
                "expected_inflation_annual",
                "current_balance");
    }

    @Override
    public List<List<String>> exportRows(UUID userId) {
        Optional<RetirementPlan> plan = retirementPlanRepository.findByUserId(userId);
        if (plan.isEmpty()) {
            return List.of();
        }
        RetirementPlan p = plan.get();
        return List.of(List.of(
                p.getId().toString(),
                CsvFieldParser.required(p.getMonthlyContribution()),
                CsvFieldParser.required(p.getContributionAnnualIncreasePercent()),
                p.getStartDate().toString(),
                String.valueOf(p.getHorizonYears()),
                CsvFieldParser.required(p.getExpectedReturnNominalAnnual()),
                CsvFieldParser.required(p.getExpectedInflationAnnual()),
                CsvFieldParser.required(p.getCurrentBalance())));
    }

    @Override
    public RowResult importRow(UUID userId, Map<String, String> row, int line, boolean apply) {
        try {
            UUID id = CsvFieldParser.requiredUuid(row, "id");
            BigDecimal monthlyContribution = CsvFieldParser.requiredDecimal(row, "monthly_contribution");
            BigDecimal contributionAnnualIncreasePercent =
                    CsvFieldParser.requiredDecimal(row, "contribution_annual_increase_percent");
            LocalDate startDate = CsvFieldParser.requiredDate(row, "start_date");
            short horizonYears = CsvFieldParser.requiredShort(row, "horizon_years");
            BigDecimal expectedReturnNominalAnnual = CsvFieldParser.requiredDecimal(row, "expected_return_nominal_annual");
            BigDecimal expectedInflationAnnual = CsvFieldParser.requiredDecimal(row, "expected_inflation_annual");
            BigDecimal currentBalance = CsvFieldParser.requiredDecimal(row, "current_balance");

            Optional<RetirementPlan> existingById = retirementPlanRepository.findById(id);
            if (existingById.isPresent() && !existingById.get().getUserId().equals(userId)) {
                return RowResult.error(line, "id já pertence a outro registro");
            }

            Optional<RetirementPlan> existingForUser = retirementPlanRepository.findByUserId(userId);
            if (existingForUser.isPresent() && !existingForUser.get().getId().equals(id)) {
                return RowResult.error(line, "já existe um plano de aposentadoria para este usuário com outro id");
            }

            if (existingForUser.isPresent()) {
                if (apply) {
                    existingForUser
                            .get()
                            .update(
                                    monthlyContribution,
                                    contributionAnnualIncreasePercent,
                                    startDate,
                                    horizonYears,
                                    expectedReturnNominalAnnual,
                                    expectedInflationAnnual,
                                    currentBalance);
                    retirementPlanRepository.save(existingForUser.get());
                }
                return RowResult.updated(line);
            }

            if (apply) {
                retirementPlanRepository.save(new RetirementPlan(
                        id,
                        userId,
                        monthlyContribution,
                        contributionAnnualIncreasePercent,
                        startDate,
                        horizonYears,
                        expectedReturnNominalAnnual,
                        expectedInflationAnnual,
                        currentBalance));
            }
            return RowResult.created(line);
        } catch (RowValidationException ex) {
            return RowResult.error(line, ex.getMessage());
        }
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        retirementPlanRepository.deleteAllByUserId(userId);
    }
}
