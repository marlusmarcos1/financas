package com.marlus.financas.allocation.repository;

import com.marlus.financas.allocation.domain.AllocationTarget;
import com.marlus.financas.investment.domain.InvestmentAssetClass;
import com.marlus.financas.investment.domain.InvestmentPurpose;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllocationTargetRepository extends JpaRepository<AllocationTarget, UUID> {

    List<AllocationTarget> findAllByUserIdAndPurpose(UUID userId, InvestmentPurpose purpose);

    Optional<AllocationTarget> findByUserIdAndPurposeAndAssetClass(
            UUID userId, InvestmentPurpose purpose, InvestmentAssetClass assetClass);

    Optional<AllocationTarget> findByIdAndUserId(UUID id, UUID userId);
}
