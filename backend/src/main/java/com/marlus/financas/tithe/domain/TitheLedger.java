package com.marlus.financas.tithe.domain;

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
@Table(name = "tithe_ledger")
public class TitheLedger extends UserOwnedEntity {

    @Id
    private UUID id;

    @Column(name = "reference_month", nullable = false, length = 7)
    private String referenceMonth;

    @Column(name = "base_amount", nullable = false)
    private BigDecimal baseAmount;

    @Column(nullable = false)
    private BigDecimal percent;

    @Column(name = "due_amount", nullable = false)
    private BigDecimal dueAmount;

    @Column(name = "paid_amount", nullable = false)
    private BigDecimal paidAmount;

    @Column(name = "paid_on")
    private LocalDate paidOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TitheStatus status;

    protected TitheLedger() {
    }

    public TitheLedger(UUID id, UUID userId, String referenceMonth, BigDecimal percent) {
        super(userId);
        this.id = id;
        this.referenceMonth = referenceMonth;
        this.percent = percent;
        this.baseAmount = BigDecimal.ZERO;
        this.dueAmount = BigDecimal.ZERO;
        this.paidAmount = BigDecimal.ZERO;
        this.status = TitheStatus.PENDING;
    }

    public UUID getId() {
        return id;
    }

    public String getReferenceMonth() {
        return referenceMonth;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public BigDecimal getPercent() {
        return percent;
    }

    public BigDecimal getDueAmount() {
        return dueAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public LocalDate getPaidOn() {
        return paidOn;
    }

    public TitheStatus getStatus() {
        return status;
    }

    /** Recalcula base/devido a partir da soma das receitas do mês que geram dízimo, preservando o já pago. */
    public void recalculateBase(BigDecimal newBaseAmount, BigDecimal percent) {
        this.baseAmount = newBaseAmount;
        this.percent = percent;
        this.dueAmount = newBaseAmount.multiply(percent).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_EVEN);
        refreshStatus();
    }

    public void registerPayment(BigDecimal amount, LocalDate paidOn) {
        this.paidAmount = this.paidAmount.add(amount);
        this.paidOn = paidOn;
        refreshStatus();
    }

    private void refreshStatus() {
        this.status = paidAmount.compareTo(dueAmount) >= 0 && dueAmount.signum() > 0 ? TitheStatus.PAID : TitheStatus.PENDING;
    }
}
