package com.marlus.financas.category.repository;

import com.marlus.financas.category.domain.Budget;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findAllByUserIdAndCategoryIdOrderByMonthAsc(UUID userId, UUID categoryId);

    List<Budget> findAllByUserIdOrderByCategoryIdAsc(UUID userId);

    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByCategoryIdAndUserId(UUID categoryId, UUID userId);

    void deleteAllByUserId(UUID userId);
}
