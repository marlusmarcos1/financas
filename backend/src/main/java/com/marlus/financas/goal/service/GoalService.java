package com.marlus.financas.goal.service;

import com.marlus.financas.account.repository.AccountRepository;
import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.common.EntityNotFoundException;
import com.marlus.financas.common.UuidV7Generator;
import com.marlus.financas.goal.domain.Goal;
import com.marlus.financas.goal.domain.GoalType;
import com.marlus.financas.goal.repository.GoalContributionRepository;
import com.marlus.financas.goal.repository.GoalRepository;
import com.marlus.financas.goal.web.GoalProgressResponse;
import com.marlus.financas.goal.web.GoalRequest;
import com.marlus.financas.investment.domain.InvestmentAssetClass;
import com.marlus.financas.investment.domain.InvestmentPurpose;
import com.marlus.financas.investment.repository.InvestmentAssetRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD de metas e cálculo de progresso (aporte necessário, meses restantes, alerta de risco). Ver seção 7.7. */
@Service
@Transactional
public class GoalService {

    private static final Set<InvestmentAssetClass> VARIABLE_INCOME_CLASSES =
            EnumSet.of(InvestmentAssetClass.FII, InvestmentAssetClass.STOCK, InvestmentAssetClass.ETF, InvestmentAssetClass.CRYPTO);
    private static final int RISK_WARNING_HORIZON_YEARS = 3;

    private final GoalRepository goalRepository;
    private final GoalContributionRepository goalContributionRepository;
    private final AccountRepository accountRepository;
    private final InvestmentAssetRepository investmentAssetRepository;
    private final CurrentUserProvider currentUserProvider;

    public GoalService(
            GoalRepository goalRepository,
            GoalContributionRepository goalContributionRepository,
            AccountRepository accountRepository,
            InvestmentAssetRepository investmentAssetRepository,
            CurrentUserProvider currentUserProvider) {
        this.goalRepository = goalRepository;
        this.goalContributionRepository = goalContributionRepository;
        this.accountRepository = accountRepository;
        this.investmentAssetRepository = investmentAssetRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<GoalProgressResponse> findAllWithProgress() {
        UUID userId = currentUserProvider.currentUserId();
        return goalRepository.findAllByUserIdOrderByPriorityAscNameAsc(userId).stream()
                .map(this::toProgress)
                .toList();
    }

    @Transactional(readOnly = true)
    public Goal findById(UUID id) {
        return goalRepository
                .findByIdAndUserId(id, currentUserProvider.currentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Meta não encontrada."));
    }

    @Transactional(readOnly = true)
    public GoalProgressResponse findProgressById(UUID id) {
        return toProgress(findById(id));
    }

    public Goal create(GoalRequest request) {
        UUID userId = currentUserProvider.currentUserId();
        validateLinkedAccount(request.linkedAccountId(), userId);
        Goal goal = new Goal(
                UuidV7Generator.generate(),
                userId,
                request.name(),
                request.type(),
                request.targetAmount(),
                request.targetDate(),
                request.linkedAccountId(),
                request.monthlyContributionPlanned() != null ? request.monthlyContributionPlanned() : BigDecimal.ZERO,
                request.priority(),
                request.notes());
        return goalRepository.save(goal);
    }

    public Goal update(UUID id, GoalRequest request) {
        Goal goal = findById(id);
        validateLinkedAccount(request.linkedAccountId(), goal.getUserId());
        goal.setName(request.name());
        goal.setType(request.type());
        goal.setTargetAmount(request.targetAmount());
        goal.setTargetDate(request.targetDate());
        goal.setLinkedAccountId(request.linkedAccountId());
        goal.setMonthlyContributionPlanned(
                request.monthlyContributionPlanned() != null ? request.monthlyContributionPlanned() : BigDecimal.ZERO);
        goal.setPriority(request.priority());
        goal.setNotes(request.notes());
        return goal;
    }

    public void delete(UUID id) {
        Goal goal = findById(id);
        goalContributionRepository.findAllByUserIdAndGoalIdOrderByDateDesc(goal.getUserId(), goal.getId())
                .forEach(goalContributionRepository::delete);
        goalRepository.delete(goal);
    }

    private void validateLinkedAccount(UUID accountId, UUID userId) {
        if (accountId == null) {
            return;
        }
        accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Conta vinculada não encontrada."));
    }

    private GoalProgressResponse toProgress(Goal goal) {
        BigDecimal currentAmount = goalContributionRepository
                .findAllByUserIdAndGoalIdOrderByDateDesc(goal.getUserId(), goal.getId())
                .stream()
                .map(contribution -> contribution.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal amountRemaining = goal.getTargetAmount().subtract(currentAmount).max(BigDecimal.ZERO);

        Integer monthsRemaining = null;
        BigDecimal requiredMonthlyContribution = null;
        if (goal.getTargetDate() != null) {
            int months = (int) Period.between(LocalDate.now().withDayOfMonth(1), goal.getTargetDate().withDayOfMonth(1)).toTotalMonths();
            monthsRemaining = Math.max(months, 0);
            requiredMonthlyContribution = monthsRemaining > 0
                    ? amountRemaining.divide(BigDecimal.valueOf(monthsRemaining), 2, RoundingMode.HALF_EVEN)
                    : amountRemaining;
        }

        String warning = buildRiskWarning(goal);

        return new GoalProgressResponse(
                goal.getId(),
                goal.getName(),
                goal.getType(),
                goal.getTargetAmount(),
                goal.getTargetDate(),
                goal.getLinkedAccountId(),
                goal.getMonthlyContributionPlanned(),
                goal.getPriority(),
                goal.getNotes(),
                currentAmount,
                amountRemaining,
                monthsRemaining,
                requiredMonthlyContribution,
                warning);
    }

    /** Alerta se a meta tiver prazo curto (&lt;3 anos) e existirem ativos de renda variável com essa finalidade. */
    private String buildRiskWarning(Goal goal) {
        if (goal.getTargetDate() == null || goal.getType() != GoalType.HOUSE) {
            return null;
        }
        boolean shortHorizon = goal.getTargetDate().isBefore(LocalDate.now().plusYears(RISK_WARNING_HORIZON_YEARS));
        if (!shortHorizon) {
            return null;
        }
        boolean hasRiskyAssets = investmentAssetRepository.findAllByUserIdOrderByTickerAsc(goal.getUserId()).stream()
                .filter(asset -> !asset.isArchived())
                .anyMatch(asset -> asset.getPurpose() == InvestmentPurpose.HOUSE
                        && VARIABLE_INCOME_CLASSES.contains(asset.getAssetClass()));
        return hasRiskyAssets
                ? "Esta meta está vinculada a ativos de renda variável (FII/ação) e a data-alvo é menor que 3 anos — "
                        + "considere migrar para renda fixa para não correr risco de perda perto do prazo."
                : null;
    }
}
