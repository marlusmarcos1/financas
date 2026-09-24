package com.marlus.financas.transaction.repository;

import com.marlus.financas.transaction.domain.Transaction;
import com.marlus.financas.transaction.domain.TransactionKind;
import com.marlus.financas.transaction.domain.TransactionStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> belongsToUser(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<Transaction> dateFrom(LocalDate from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("date"), from);
    }

    public static Specification<Transaction> dateTo(LocalDate to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("date"), to);
    }

    public static Specification<Transaction> categoryId(UUID categoryId) {
        return (root, query, cb) -> categoryId == null ? null : cb.equal(root.get("categoryId"), categoryId);
    }

    public static Specification<Transaction> accountId(UUID accountId) {
        return (root, query, cb) -> accountId == null ? null : cb.equal(root.get("accountId"), accountId);
    }

    public static Specification<Transaction> cardId(UUID cardId) {
        return (root, query, cb) -> cardId == null ? null : cb.equal(root.get("cardId"), cardId);
    }

    public static Specification<Transaction> status(TransactionStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Transaction> kind(TransactionKind kind) {
        return (root, query, cb) -> kind == null ? null : cb.equal(root.get("kind"), kind);
    }
}
