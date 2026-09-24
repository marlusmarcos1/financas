package com.marlus.financas.invoice.domain;

import com.marlus.financas.common.UserOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@Entity
@Table(name = "invoice")
public class Invoice extends UserOwnedEntity {

    @Id
    private UUID id;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "reference_month", nullable = false, length = 7)
    private String referenceMonth;

    @Column(name = "closing_date", nullable = false)
    private LocalDate closingDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private InvoiceStatus status;

    @Column(name = "paid_amount", nullable = false)
    private BigDecimal paidAmount;

    @Column(name = "paid_on")
    private LocalDate paidOn;

    @Column(name = "paid_from_account_id")
    private UUID paidFromAccountId;

    protected Invoice() {
    }

    public Invoice(
            UUID id, UUID userId, UUID cardId, YearMonth referenceMonth, LocalDate closingDate, LocalDate dueDate) {
        super(userId);
        this.id = id;
        this.cardId = cardId;
        this.referenceMonth = referenceMonth.toString();
        this.closingDate = closingDate;
        this.dueDate = dueDate;
        this.status = InvoiceStatus.OPEN;
        this.paidAmount = BigDecimal.ZERO;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCardId() {
        return cardId;
    }

    public String getReferenceMonth() {
        return referenceMonth;
    }

    public LocalDate getClosingDate() {
        return closingDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public LocalDate getPaidOn() {
        return paidOn;
    }

    public UUID getPaidFromAccountId() {
        return paidFromAccountId;
    }

    public void registerPayment(BigDecimal amount, LocalDate paidOn, UUID fromAccountId) {
        this.paidAmount = this.paidAmount.add(amount);
        this.paidOn = paidOn;
        this.paidFromAccountId = fromAccountId;
    }
}
