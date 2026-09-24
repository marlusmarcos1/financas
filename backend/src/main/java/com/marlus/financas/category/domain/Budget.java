package com.marlus.financas.category.domain;

import com.marlus.financas.common.UserOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "budget")
public class Budget extends UserOwnedEntity {

    @Id
    private UUID id;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    /** Competência no formato "YYYY-MM"; {@code null} representa o teto padrão da categoria. */
    @Column(length = 7)
    private String month;

    @Column(name = "limit_amount", nullable = false)
    private BigDecimal limitAmount;

    protected Budget() {
    }

    public Budget(UUID id, UUID userId, UUID categoryId, String month, BigDecimal limitAmount) {
        super(userId);
        this.id = id;
        this.categoryId = categoryId;
        this.month = month;
        this.limitAmount = limitAmount;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getMonth() {
        return month;
    }

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(BigDecimal limitAmount) {
        this.limitAmount = limitAmount;
    }
}
