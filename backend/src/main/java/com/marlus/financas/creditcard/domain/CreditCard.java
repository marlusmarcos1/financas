package com.marlus.financas.creditcard.domain;

import com.marlus.financas.common.UserOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "credit_card")
public class CreditCard extends UserOwnedEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 120)
    private String issuer;

    @Column(name = "credit_limit", nullable = false)
    private BigDecimal creditLimit;

    @Column(name = "closing_day", nullable = false)
    private short closingDay;

    @Column(name = "due_day", nullable = false)
    private short dueDay;

    @Column(name = "default_payment_account_id")
    private UUID defaultPaymentAccountId;

    @Column(length = 7)
    private String color;

    @Column(nullable = false)
    private boolean archived;

    protected CreditCard() {
    }

    public CreditCard(
            UUID id,
            UUID userId,
            String name,
            String issuer,
            BigDecimal creditLimit,
            short closingDay,
            short dueDay,
            UUID defaultPaymentAccountId,
            String color) {
        super(userId);
        this.id = id;
        this.name = name;
        this.issuer = issuer;
        this.creditLimit = creditLimit;
        this.closingDay = closingDay;
        this.dueDay = dueDay;
        this.defaultPaymentAccountId = defaultPaymentAccountId;
        this.color = color;
        this.archived = false;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public short getClosingDay() {
        return closingDay;
    }

    public void setClosingDay(short closingDay) {
        this.closingDay = closingDay;
    }

    public short getDueDay() {
        return dueDay;
    }

    public void setDueDay(short dueDay) {
        this.dueDay = dueDay;
    }

    public UUID getDefaultPaymentAccountId() {
        return defaultPaymentAccountId;
    }

    public void setDefaultPaymentAccountId(UUID defaultPaymentAccountId) {
        this.defaultPaymentAccountId = defaultPaymentAccountId;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isArchived() {
        return archived;
    }

    public void archive() {
        this.archived = true;
    }

    public void unarchive() {
        this.archived = false;
    }
}
