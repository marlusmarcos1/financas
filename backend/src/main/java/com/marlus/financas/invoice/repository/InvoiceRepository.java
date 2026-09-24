package com.marlus.financas.invoice.repository;

import com.marlus.financas.invoice.domain.Invoice;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByCardIdAndReferenceMonth(UUID cardId, String referenceMonth);

    Optional<Invoice> findByIdAndUserId(UUID id, UUID userId);

    List<Invoice> findAllByUserIdAndCardIdOrderByReferenceMonthAsc(UUID userId, UUID cardId);
}
