package com.marlus.financas.investment.repository;

import com.marlus.financas.investment.domain.InvestmentTransaction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestmentTransactionRepository extends JpaRepository<InvestmentTransaction, UUID> {

    List<InvestmentTransaction> findAllByUserIdAndAssetIdOrderByDateAsc(UUID userId, UUID assetId);

    List<InvestmentTransaction> findAllByUserIdOrderByDateDesc(UUID userId);

    Optional<InvestmentTransaction> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByAssetIdAndUserId(UUID assetId, UUID userId);
}
