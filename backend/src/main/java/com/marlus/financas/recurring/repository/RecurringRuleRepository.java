package com.marlus.financas.recurring.repository;

import com.marlus.financas.recurring.domain.RecurringRule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringRuleRepository extends JpaRepository<RecurringRule, UUID> {

    List<RecurringRule> findAllByUserIdOrderByDescriptionAsc(UUID userId);

    Optional<RecurringRule> findByIdAndUserId(UUID id, UUID userId);

    List<RecurringRule> findAllByActiveTrue();

    void deleteAllByUserId(UUID userId);
}
