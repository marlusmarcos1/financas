package com.marlus.financas.dataio.repository;

import com.marlus.financas.dataio.domain.ImportJob;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {

    List<ImportJob> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}
