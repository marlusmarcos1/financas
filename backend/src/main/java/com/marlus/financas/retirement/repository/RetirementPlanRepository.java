package com.marlus.financas.retirement.repository;

import com.marlus.financas.retirement.domain.RetirementPlan;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetirementPlanRepository extends JpaRepository<RetirementPlan, UUID> {

    Optional<RetirementPlan> findByUserId(UUID userId);

    void deleteAllByUserId(UUID userId);
}
