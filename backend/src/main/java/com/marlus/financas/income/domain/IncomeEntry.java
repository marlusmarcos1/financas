package com.marlus.financas.income.domain;

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
@Table(name = "income_entry")
public class IncomeEntry extends UserOwnedEntity {

    @Id
    private UUID id;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "reference_month", nullable = false, length = 7)
    private String referenceMonth;

    @Column(name = "received_on")
    private LocalDate receivedOn;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private IncomeEntryStatus status;

    protected IncomeEntry() {
    }

    public IncomeEntry(
            UUID id,
            UUID userId,
            UUID sourceId,
            UUID accountId,
            String referenceMonth,
            LocalDate receivedOn,
            BigDecimal amount,
            IncomeEntryStatus status) {
        super(userId);
        this.id = id;
        this.sourceId = sourceId;
        this.accountId = accountId;
        this.referenceMonth = referenceMonth;
        this.receivedOn = receivedOn;
        this.amount = amount;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getReferenceMonth() {
        return referenceMonth;
    }

    public void setReferenceMonth(String referenceMonth) {
        this.referenceMonth = referenceMonth;
    }

    public LocalDate getReceivedOn() {
        return receivedOn;
    }

    public void setReceivedOn(LocalDate receivedOn) {
        this.receivedOn = receivedOn;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public IncomeEntryStatus getStatus() {
        return status;
    }

    public void setStatus(IncomeEntryStatus status) {
        this.status = status;
    }
}
