package com.marlus.financas.income.repository;

import com.marlus.financas.income.domain.IncomeSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncomeSourceRepository extends JpaRepository<IncomeSource, UUID> {

    List<IncomeSource> findAllByUserIdOrderByNameAsc(UUID userId);

    Optional<IncomeSource> findByIdAndUserId(UUID id, UUID userId);

    void deleteAllByUserId(UUID userId);
}
