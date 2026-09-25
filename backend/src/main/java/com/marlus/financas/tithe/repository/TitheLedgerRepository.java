package com.marlus.financas.tithe.repository;

import com.marlus.financas.tithe.domain.TitheLedger;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TitheLedgerRepository extends JpaRepository<TitheLedger, UUID> {

    Optional<TitheLedger> findByUserIdAndReferenceMonth(UUID userId, String referenceMonth);

    Optional<TitheLedger> findByIdAndUserId(UUID id, UUID userId);

    List<TitheLedger> findAllByUserIdOrderByReferenceMonthDesc(UUID userId);

    List<TitheLedger> findAllByUserIdAndReferenceMonthStartingWith(UUID userId, String yearPrefix);

    void deleteAllByUserId(UUID userId);
}
