package com.marlus.financas.goal.repository;

import com.marlus.financas.goal.domain.Goal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findAllByUserIdOrderByPriorityAscNameAsc(UUID userId);

    Optional<Goal> findByIdAndUserId(UUID id, UUID userId);
}
