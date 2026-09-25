package com.marlus.financas.investment.domain;

import com.marlus.financas.common.UserOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "investment_transaction")
public class InvestmentTransaction extends UserOwnedEntity {

    @Id
    private UUID id;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private InvestmentTransactionType type;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private BigDecimal fees;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "account_id")
    private UUID accountId;

    protected InvestmentTransaction() {
    }

    public InvestmentTransaction(
            UUID id,
            UUID userId,
            UUID assetId,
            InvestmentTransactionType type,
            LocalDate date,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal fees,
            BigDecimal amount,
            UUID accountId) {
        super(userId);
        this.id = id;
        this.assetId = assetId;
        this.type = type;
        this.date = date;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.fees = fees;
        this.amount = amount;
        this.accountId = accountId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAssetId() {
        return assetId;
    }

    public InvestmentTransactionType getType() {
        return type;
    }

    public LocalDate getDate() {
        return date;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getFees() {
        return fees;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public UUID getAccountId() {
        return accountId;
    }
}
