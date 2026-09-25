package com.marlus.financas.allocation.domain;

import com.marlus.financas.common.UserOwnedEntity;
import com.marlus.financas.investment.domain.InvestmentAssetClass;
import com.marlus.financas.investment.domain.InvestmentPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "allocation_target")
public class AllocationTarget extends UserOwnedEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvestmentPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false, length = 20)
    private InvestmentAssetClass assetClass;

    @Column(name = "target_percent", nullable = false)
    private BigDecimal targetPercent;

    protected AllocationTarget() {
    }

    public AllocationTarget(UUID id, UUID userId, InvestmentPurpose purpose, InvestmentAssetClass assetClass, BigDecimal targetPercent) {
        super(userId);
        this.id = id;
        this.purpose = purpose;
        this.assetClass = assetClass;
        this.targetPercent = targetPercent;
    }

    public UUID getId() {
        return id;
    }

    public InvestmentPurpose getPurpose() {
        return purpose;
    }

    public InvestmentAssetClass getAssetClass() {
        return assetClass;
    }

    public BigDecimal getTargetPercent() {
        return targetPercent;
    }

    public void setTargetPercent(BigDecimal targetPercent) {
        this.targetPercent = targetPercent;
    }
}
