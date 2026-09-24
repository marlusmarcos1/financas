package com.marlus.financas.transaction.domain;

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
@Table(name = "transaction")
public class Transaction extends UserOwnedEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionKind kind;

    @Column(nullable = false, length = 160)
    private String description;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "card_id")
    private UUID cardId;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionStatus status;

    @Column(name = "installment_plan_id")
    private UUID installmentPlanId;

    @Column(name = "installment_number")
    private Short installmentNumber;

    @Column(name = "recurring_rule_id")
    private UUID recurringRuleId;

    @Column(name = "income_entry_id")
    private UUID incomeEntryId;

    @Column(length = 500)
    private String notes;

    protected Transaction() {
    }

    public Transaction(
            UUID id,
            UUID userId,
            TransactionKind kind,
            String description,
            BigDecimal amount,
            LocalDate date,
            UUID categoryId,
            UUID accountId,
            UUID cardId,
            TransactionStatus status,
            String notes) {
        super(userId);
        this.id = id;
        this.kind = kind;
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.categoryId = categoryId;
        this.accountId = accountId;
        this.cardId = cardId;
        this.status = status;
        this.notes = notes;
    }

    public UUID getId() {
        return id;
    }

    public TransactionKind getKind() {
        return kind;
    }

    public void setKind(TransactionKind kind) {
        this.kind = kind;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getCardId() {
        return cardId;
    }

    public void setPaymentMethod(UUID accountId, UUID cardId) {
        this.accountId = accountId;
        this.cardId = cardId;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public UUID getInstallmentPlanId() {
        return installmentPlanId;
    }

    public void setInstallmentPlanId(UUID installmentPlanId) {
        this.installmentPlanId = installmentPlanId;
    }

    public Short getInstallmentNumber() {
        return installmentNumber;
    }

    public void setInstallmentNumber(Short installmentNumber) {
        this.installmentNumber = installmentNumber;
    }

    public UUID getRecurringRuleId() {
        return recurringRuleId;
    }

    public void setRecurringRuleId(UUID recurringRuleId) {
        this.recurringRuleId = recurringRuleId;
    }

    public UUID getIncomeEntryId() {
        return incomeEntryId;
    }

    public void setIncomeEntryId(UUID incomeEntryId) {
        this.incomeEntryId = incomeEntryId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
