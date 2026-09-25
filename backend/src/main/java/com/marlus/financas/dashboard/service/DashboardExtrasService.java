package com.marlus.financas.dashboard.service;

import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.dashboard.web.ChecklistItemResponse;
import com.marlus.financas.dashboard.web.DashboardExtrasResponse;
import com.marlus.financas.dashboard.web.ScholarshipCountdownResponse;
import com.marlus.financas.income.domain.IncomeRecurrence;
import com.marlus.financas.income.domain.IncomeSource;
import com.marlus.financas.income.repository.IncomeSourceRepository;
import com.marlus.financas.networth.service.EmergencyReserveService;
import com.marlus.financas.networth.web.EmergencyReserveResponse;
import com.marlus.financas.retirement.domain.RetirementPlan;
import com.marlus.financas.retirement.repository.RetirementPlanRepository;
import com.marlus.financas.tithe.domain.TitheLedger;
import com.marlus.financas.tithe.repository.TitheLedgerRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checklist "pague-se primeiro" (dízimo, aposentadoria, reserva) e contagem regressiva de
 * receitas temporárias como a bolsa (seção 11). A simulação "e se a bolsa acabar agora?" não
 * precisa de cálculo próprio: {@link DashboardService#summarize} já separa renda base de extras,
 * então a sobra do mês já reflete o cenário sem a bolsa.
 */
@Service
@Transactional(readOnly = true)
public class DashboardExtrasService {

    private final TitheLedgerRepository titheLedgerRepository;
    private final RetirementPlanRepository retirementPlanRepository;
    private final EmergencyReserveService emergencyReserveService;
    private final IncomeSourceRepository incomeSourceRepository;
    private final CurrentUserProvider currentUserProvider;

    public DashboardExtrasService(
            TitheLedgerRepository titheLedgerRepository,
            RetirementPlanRepository retirementPlanRepository,
            EmergencyReserveService emergencyReserveService,
            IncomeSourceRepository incomeSourceRepository,
            CurrentUserProvider currentUserProvider) {
        this.titheLedgerRepository = titheLedgerRepository;
        this.retirementPlanRepository = retirementPlanRepository;
        this.emergencyReserveService = emergencyReserveService;
        this.incomeSourceRepository = incomeSourceRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public DashboardExtrasResponse get(YearMonth month) {
        UUID userId = currentUserProvider.currentUserId();

        return new DashboardExtrasResponse(payYourselfFirst(userId, month), scholarships(userId, month));
    }

    private List<ChecklistItemResponse> payYourselfFirst(UUID userId, YearMonth month) {
        TitheLedger tithe = titheLedgerRepository.findByUserIdAndReferenceMonth(userId, month.toString()).orElse(null);
        BigDecimal titheDue = tithe != null ? tithe.getDueAmount() : BigDecimal.ZERO;
        boolean titheDone = tithe != null && tithe.getPaidAmount().compareTo(tithe.getDueAmount()) >= 0;

        RetirementPlan plan = retirementPlanRepository.findByUserId(userId).orElse(null);
        BigDecimal retirementTarget = plan != null ? plan.getMonthlyContribution() : BigDecimal.ZERO;

        EmergencyReserveResponse reserve = emergencyReserveService.calculate();
        BigDecimal reserveGap = reserve.targetAmount().subtract(reserve.currentReserve());
        boolean reserveDone = reserveGap.signum() <= 0;

        return List.of(
                new ChecklistItemResponse("Dízimo do mês", titheDue, titheDone),
                new ChecklistItemResponse("Aporte de aposentadoria", retirementTarget, false),
                new ChecklistItemResponse(
                        "Reserva de emergência", reserveGap.max(BigDecimal.ZERO), reserveDone));
    }

    private List<ScholarshipCountdownResponse> scholarships(UUID userId, YearMonth month) {
        return incomeSourceRepository.findAllByUserIdOrderByNameAsc(userId).stream()
                .filter(source -> source.getRecurrence() == IncomeRecurrence.TEMPORARY)
                .filter(source -> source.getStartDate() != null && source.getExpectedMonths() != null)
                .map(source -> toCountdown(source, month))
                .filter(countdown -> countdown.remainingMonths() >= 0)
                .toList();
    }

    private ScholarshipCountdownResponse toCountdown(IncomeSource source, YearMonth month) {
        long elapsed = ChronoUnit.MONTHS.between(YearMonth.from(source.getStartDate()), month);
        long remaining = source.getExpectedMonths() - elapsed;
        YearMonth endsOn = YearMonth.from(source.getStartDate()).plusMonths(source.getExpectedMonths() - 1L);
        return new ScholarshipCountdownResponse(source.getId(), source.getName(), remaining, endsOn);
    }
}
