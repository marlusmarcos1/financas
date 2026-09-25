package com.marlus.financas.investment.service;

import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.investment.domain.InvestmentAsset;
import com.marlus.financas.investment.repository.InvestmentAssetRepository;
import com.marlus.financas.investment.repository.InvestmentTransactionRepository;
import com.marlus.financas.investment.web.AssetPositionResponse;
import com.marlus.financas.investment.web.PortfolioResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PortfolioService {

    private final InvestmentAssetRepository investmentAssetRepository;
    private final InvestmentTransactionRepository investmentTransactionRepository;
    private final CurrentUserProvider currentUserProvider;

    public PortfolioService(
            InvestmentAssetRepository investmentAssetRepository,
            InvestmentTransactionRepository investmentTransactionRepository,
            CurrentUserProvider currentUserProvider) {
        this.investmentAssetRepository = investmentAssetRepository;
        this.investmentTransactionRepository = investmentTransactionRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public PortfolioResponse summarize() {
        UUID userId = currentUserProvider.currentUserId();
        List<InvestmentAsset> assets = investmentAssetRepository.findAllByUserIdOrderByTickerAsc(userId).stream()
                .filter(asset -> !asset.isArchived())
                .toList();

        List<AssetPositionResponse> positions = assets.stream().map(this::toPosition).toList();
        BigDecimal totalCurrentValue = positions.stream().map(AssetPositionResponse::currentValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = positions.stream().map(AssetPositionResponse::totalCost).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PortfolioResponse(positions, totalCurrentValue, totalCost);
    }

    private AssetPositionResponse toPosition(InvestmentAsset asset) {
        Position position = PositionCalculator.calculate(investmentTransactionRepository
                .findAllByUserIdAndAssetIdOrderByDateAsc(asset.getUserId(), asset.getId()));

        BigDecimal currentPrice = asset.getCurrentPrice() != null ? asset.getCurrentPrice() : position.averagePrice();
        BigDecimal currentValue = position.quantity().multiply(currentPrice).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal unrealizedGain = currentValue.subtract(position.totalCost());
        BigDecimal yieldOnCost = position.totalCost().signum() > 0
                ? position.totalIncome().multiply(BigDecimal.valueOf(100)).divide(position.totalCost(), 2, RoundingMode.HALF_EVEN)
                : BigDecimal.ZERO;

        return new AssetPositionResponse(
                asset.getId(),
                asset.getTicker(),
                asset.getName(),
                asset.getAssetClass(),
                asset.getPurpose(),
                position.quantity(),
                position.averagePrice(),
                currentPrice,
                position.totalCost(),
                currentValue,
                unrealizedGain,
                position.totalIncome(),
                yieldOnCost);
    }
}
