package com.marlus.financas.installment.domain;

import com.marlus.financas.common.UserOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@Entity
@Table(name = "installment_plan")
public class InstallmentPlan extends UserOwnedEntity {

    @Id
    private UUID id;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(nullable = false, length = 160)
    private String description;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "installment_count", nullable = false)
    private short installmentCount;

    @Column(name = "installment_amount", nullable = false)
    private BigDecimal installmentAmount;

    @Column(name = "first_installment_number", nullable = false)
    private short firstInstallmentNumber;

    @Column(name = "first_invoice_month", nullable = false, length = 7)
    private String firstInvoiceMonth;

    @Column(name = "interest_rate_monthly")
    private BigDecimal interestRateMonthly;

    @Column(name = "category_id")
    private UUID categoryId;

    protected InstallmentPlan() {
    }

    public InstallmentPlan(
            UUID id,
            UUID userId,
            UUID cardId,
            String description,
            LocalDate purchaseDate,
            BigDecimal totalAmount,
            short installmentCount,
            BigDecimal installmentAmount,
            short firstInstallmentNumber,
            YearMonth firstInvoiceMonth,
            BigDecimal interestRateMonthly,
            UUID categoryId) {
        super(userId);
        this.id = id;
        this.cardId = cardId;
        this.description = description;
        this.purchaseDate = purchaseDate;
        this.totalAmount = totalAmount;
        this.installmentCount = installmentCount;
        this.installmentAmount = installmentAmount;
        this.firstInstallmentNumber = firstInstallmentNumber;
        this.firstInvoiceMonth = firstInvoiceMonth.toString();
        this.interestRateMonthly = interestRateMonthly;
        this.categoryId = categoryId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCardId() {
        return cardId;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public short getInstallmentCount() {
        return installmentCount;
    }

    public BigDecimal getInstallmentAmount() {
        return installmentAmount;
    }

    public short getFirstInstallmentNumber() {
        return firstInstallmentNumber;
    }

    public YearMonth getFirstInvoiceMonth() {
        return YearMonth.parse(firstInvoiceMonth);
    }

    public BigDecimal getInterestRateMonthly() {
        return interestRateMonthly;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    /** Competência do mês em que a última parcela é lançada — quando o parcelamento "libera" o comprometimento. */
    public YearMonth getLastInstallmentMonth() {
        int remainingAfterFirst = installmentCount - firstInstallmentNumber;
        return getFirstInvoiceMonth().plusMonths(remainingAfterFirst);
    }
}
