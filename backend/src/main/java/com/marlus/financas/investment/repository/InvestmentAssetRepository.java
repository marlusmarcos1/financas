package com.marlus.financas.investment.repository;

import com.marlus.financas.investment.domain.InvestmentAsset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestmentAssetRepository extends JpaRepository<InvestmentAsset, UUID> {

    List<InvestmentAsset> findAllByUserIdOrderByTickerAsc(UUID userId);

    Optional<InvestmentAsset> findByIdAndUserId(UUID id, UUID userId);

    void deleteAllByUserId(UUID userId);
}
