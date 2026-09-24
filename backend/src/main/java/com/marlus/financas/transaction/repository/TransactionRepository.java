package com.marlus.financas.transaction.repository;

import com.marlus.financas.transaction.domain.Transaction;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    List<Transaction> findAllByUserIdAndInvoiceId(UUID userId, UUID invoiceId);

    List<Transaction> findAllByUserIdAndInstallmentPlanId(UUID userId, UUID installmentPlanId);

    boolean existsByRecurringRuleIdAndDate(UUID recurringRuleId, LocalDate date);
}
