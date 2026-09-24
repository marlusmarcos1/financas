package com.marlus.financas.creditcard.repository;

import com.marlus.financas.creditcard.domain.CreditCard;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditCardRepository extends JpaRepository<CreditCard, UUID> {

    List<CreditCard> findAllByUserIdOrderByArchivedAscNameAsc(UUID userId);

    Optional<CreditCard> findByIdAndUserId(UUID id, UUID userId);
}
