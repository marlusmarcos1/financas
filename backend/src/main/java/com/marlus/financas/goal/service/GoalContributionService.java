package com.marlus.financas.goal.service;

import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.common.EntityNotFoundException;
import com.marlus.financas.common.UuidV7Generator;
import com.marlus.financas.goal.domain.Goal;
import com.marlus.financas.goal.domain.GoalContribution;
import com.marlus.financas.goal.repository.GoalContributionRepository;
import com.marlus.financas.goal.web.GoalContributionRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GoalContributionService {

    private final GoalContributionRepository goalContributionRepository;
    private final GoalService goalService;
    private final CurrentUserProvider currentUserProvider;

    public GoalContributionService(
            GoalContributionRepository goalContributionRepository, GoalService goalService, CurrentUserProvider currentUserProvider) {
        this.goalContributionRepository = goalContributionRepository;
        this.goalService = goalService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<GoalContribution> findAllByGoal(UUID goalId) {
        UUID userId = currentUserProvider.currentUserId();
        goalService.findById(goalId);
        return goalContributionRepository.findAllByUserIdAndGoalIdOrderByDateDesc(userId, goalId);
    }

    public GoalContribution create(UUID goalId, GoalContributionRequest request) {
        Goal goal = goalService.findById(goalId);
        GoalContribution contribution = new GoalContribution(
                UuidV7Generator.generate(), goal.getUserId(), goal.getId(), request.date(), request.amount(), request.source());
        return goalContributionRepository.save(contribution);
    }

    public void delete(UUID goalId, UUID id) {
        GoalContribution contribution = goalContributionRepository
                .findByIdAndUserId(id, currentUserProvider.currentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Aporte não encontrado."));
        if (!contribution.getGoalId().equals(goalId)) {
            throw new EntityNotFoundException("Aporte não encontrado.");
        }
        goalContributionRepository.delete(contribution);
    }
}
