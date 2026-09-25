package com.marlus.financas.income.repository;

import com.marlus.financas.income.domain.IncomeEntry;
import com.marlus.financas.income.domain.IncomeEntryStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomeEntryRepository extends JpaRepository<IncomeEntry, UUID> {

    List<IncomeEntry> findAllByUserIdAndReferenceMonthOrderByReceivedOnAsc(UUID userId, String referenceMonth);

    List<IncomeEntry> findAllByUserIdOrderByReferenceMonthAsc(UUID userId);

    List<IncomeEntry> findAllByUserIdAndSourceIdOrderByReferenceMonthDesc(UUID userId, UUID sourceId);

    Optional<IncomeEntry> findByIdAndUserId(UUID id, UUID userId);

    boolean existsBySourceIdAndUserId(UUID sourceId, UUID userId);

    List<IncomeEntry> findAllByUserIdAndReferenceMonthAndStatus(
            UUID userId, String referenceMonth, IncomeEntryStatus status);

    List<IncomeEntry> findAllByUserIdAndStatus(UUID userId, IncomeEntryStatus status);

    void deleteAllByUserId(UUID userId);
}
