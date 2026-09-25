package com.marlus.financas.investment.domain;

import com.marlus.financas.common.UserOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "investment_asset")
public class InvestmentAsset extends UserOwnedEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false, length = 20)
    private InvestmentAssetClass assetClass;

    @Column(length = 20)
    private String subclass;

    @Column(length = 40)
    private String indexer;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "current_price")
    private BigDecimal currentPrice;

    @Column(name = "price_updated_at")
    private Instant priceUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvestmentPurpose purpose;

    @Column(nullable = false)
    private boolean archived;

    protected InvestmentAsset() {
    }

    public InvestmentAsset(
            UUID id,
            UUID userId,
            String ticker,
            String name,
            InvestmentAssetClass assetClass,
            String subclass,
            String indexer,
            LocalDate maturityDate,
            InvestmentPurpose purpose) {
        super(userId);
        this.id = id;
        this.ticker = ticker;
        this.name = name;
        this.assetClass = assetClass;
        this.subclass = subclass;
        this.indexer = indexer;
        this.maturityDate = maturityDate;
        this.purpose = purpose;
        this.archived = false;
    }

    public UUID getId() {
        return id;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public InvestmentAssetClass getAssetClass() {
        return assetClass;
    }

    public void setAssetClass(InvestmentAssetClass assetClass) {
        this.assetClass = assetClass;
    }

    public String getSubclass() {
        return subclass;
    }

    public void setSubclass(String subclass) {
        this.subclass = subclass;
    }

    public String getIndexer() {
        return indexer;
    }

    public void setIndexer(String indexer) {
        this.indexer = indexer;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public void setMaturityDate(LocalDate maturityDate) {
        this.maturityDate = maturityDate;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public Instant getPriceUpdatedAt() {
        return priceUpdatedAt;
    }

    public void updatePrice(BigDecimal price, Instant updatedAt) {
        this.currentPrice = price;
        this.priceUpdatedAt = updatedAt;
    }

    public InvestmentPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(InvestmentPurpose purpose) {
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
