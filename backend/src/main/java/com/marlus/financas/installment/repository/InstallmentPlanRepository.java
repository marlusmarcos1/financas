package com.marlus.financas.installment.repository;

import com.marlus.financas.installment.domain.InstallmentPlan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallmentPlanRepository extends JpaRepository<InstallmentPlan, UUID> {

    List<InstallmentPlan> findAllByUserIdOrderByPurchaseDateDesc(UUID userId);

    Optional<InstallmentPlan> findByIdAndUserId(UUID id, UUID userId);

    void deleteAllByUserId(UUID userId);
}
