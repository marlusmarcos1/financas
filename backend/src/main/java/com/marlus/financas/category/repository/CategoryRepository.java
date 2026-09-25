package com.marlus.financas.category.repository;

import com.marlus.financas.category.domain.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByUserIdOrderByNameAsc(UUID userId);

    Optional<Category> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByParentIdAndUserId(UUID parentId, UUID userId);

    void deleteAllByUserId(UUID userId);
}
