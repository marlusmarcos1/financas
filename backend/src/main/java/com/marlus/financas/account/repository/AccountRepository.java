package com.marlus.financas.account.repository;

import com.marlus.financas.account.domain.Account;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findAllByUserIdOrderByArchivedAscNameAsc(UUID userId);

    Optional<Account> findByIdAndUserId(UUID id, UUID userId);

    void deleteAllByUserId(UUID userId);
}
