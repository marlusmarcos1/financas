package com.marlus.financas.goal.repository;

import com.marlus.financas.goal.domain.GoalContribution;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, UUID> {

    List<GoalContribution> findAllByUserIdAndGoalIdOrderByDateDesc(UUID userId, UUID goalId);

    List<GoalContribution> findAllByUserId(UUID userId);

    Optional<GoalContribution> findByIdAndUserId(UUID id, UUID userId);

    void deleteAllByUserId(UUID userId);
}
