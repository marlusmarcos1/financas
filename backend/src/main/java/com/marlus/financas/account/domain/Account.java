package com.marlus.financas.account.domain;

import com.marlus.financas.common.UserOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "account")
public class Account extends UserOwnedEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    @Column(length = 120)
    private String institution;

    @Column(name = "initial_balance", nullable = false)
    private BigDecimal initialBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountPurpose purpose;

    @Column(nullable = false)
    private boolean archived;

    protected Account() {
    }

    public Account(
            UUID id,
            UUID userId,
            String name,
            AccountType type,
            String institution,
            BigDecimal initialBalance,
            AccountPurpose purpose) {
        super(userId);
        this.id = id;
        this.name = name;
        this.type = type;
        this.institution = institution;
        this.initialBalance = initialBalance;
        this.purpose = purpose;
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

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }

    public AccountPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(AccountPurpose purpose) {
        this.purpose = purpose;
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
