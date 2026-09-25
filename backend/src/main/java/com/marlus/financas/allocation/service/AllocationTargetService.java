package com.marlus.financas.allocation.service;

import com.marlus.financas.allocation.domain.AllocationTarget;
import com.marlus.financas.allocation.repository.AllocationTargetRepository;
import com.marlus.financas.allocation.web.AllocationComparisonResponse;
import com.marlus.financas.allocation.web.AllocationSummaryResponse;
import com.marlus.financas.allocation.web.AllocationTargetRequest;
import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.common.EntityNotFoundException;
import com.marlus.financas.common.UuidV7Generator;
import com.marlus.financas.investment.domain.InvestmentAssetClass;
import com.marlus.financas.investment.domain.InvestmentPurpose;
import com.marlus.financas.investment.web.AssetPositionResponse;
import com.marlus.financas.investment.web.PortfolioResponse;
import com.marlus.financas.investment.service.PortfolioService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Alvos de alocação por finalidade/classe e comparação com a carteira real (seção 7.6). */
@Service
@Transactional
public class AllocationTargetService {

    private final AllocationTargetRepository allocationTargetRepository;
    private final PortfolioService portfolioService;
    private final CurrentUserProvider currentUserProvider;

    public AllocationTargetService(
            AllocationTargetRepository allocationTargetRepository,
            PortfolioService portfolioService,
            CurrentUserProvider currentUserProvider) {
        this.allocationTargetRepository = allocationTargetRepository;
        this.portfolioService = portfolioService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<AllocationTarget> findAllByPurpose(InvestmentPurpose purpose) {
        return allocationTargetRepository.findAllByUserIdAndPurpose(currentUserProvider.currentUserId(), purpose);
    }

    public AllocationTarget upsert(AllocationTargetRequest request) {
        UUID userId = currentUserProvider.currentUserId();
        AllocationTarget target = allocationTargetRepository
                .findByUserIdAndPurposeAndAssetClass(userId, request.purpose(), request.assetClass())
                .orElseGet(() -> new AllocationTarget(
                        UuidV7Generator.generate(), userId, request.purpose(), request.assetClass(), request.targetPercent()));
        target.setTargetPercent(request.targetPercent());
        return allocationTargetRepository.save(target);
    }

    public void delete(UUID id) {
        AllocationTarget target = allocationTargetRepository
                .findByIdAndUserId(id, currentUserProvider.currentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Alvo de alocação não encontrado."));
        allocationTargetRepository.delete(target);
    }

    @Transactional(readOnly = true)
    public AllocationSummaryResponse compare(InvestmentPurpose purpose) {
        List<AllocationTarget> targets = findAllByPurpose(purpose);
        PortfolioResponse portfolio = portfolioService.summarize();

        Map<InvestmentAssetClass, BigDecimal> currentValueByClass = portfolio.positions().stream()
                .filter(position -> position.purpose() == purpose)
                .collect(Collectors.groupingBy(
                        AssetPositionResponse::assetClass,
                        Collectors.reducing(BigDecimal.ZERO, AssetPositionResponse::currentValue, BigDecimal::add)));

        BigDecimal totalForPurpose = currentValueByClass.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AllocationComparisonResponse> comparisons = targets.stream()
                .map(target -> {
                    BigDecimal currentValue = currentValueByClass.getOrDefault(target.getAssetClass(), BigDecimal.ZERO);
                    BigDecimal currentPercent = totalForPurpose.signum() > 0
                            ? currentValue.multiply(BigDecimal.valueOf(100)).divide(totalForPurpose, 2, RoundingMode.HALF_EVEN)
                            : BigDecimal.ZERO;
                    BigDecimal difference = target.getTargetPercent().subtract(currentPercent);
                    return new AllocationComparisonResponse(
                            target.getAssetClass(), target.getTargetPercent(), currentPercent, currentValue, difference);
                })
                .toList();

        InvestmentAssetClass suggestion = comparisons.stream()
                .max(Comparator.comparing(AllocationComparisonResponse::differencePercent))
                .filter(comparison -> comparison.differencePercent().signum() > 0)
                .map(AllocationComparisonResponse::assetClass)
                .orElse(null);

        return new AllocationSummaryResponse(comparisons, suggestion);
    }
}
