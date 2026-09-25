package com.marlus.financas.investment.service;

import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.common.EntityNotFoundException;
import com.marlus.financas.common.UuidV7Generator;
import com.marlus.financas.investment.domain.InvestmentAsset;
import com.marlus.financas.investment.repository.InvestmentAssetRepository;
import com.marlus.financas.investment.repository.InvestmentTransactionRepository;
import com.marlus.financas.investment.web.InvestmentAssetRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InvestmentAssetService {

    private final InvestmentAssetRepository investmentAssetRepository;
    private final InvestmentTransactionRepository investmentTransactionRepository;
    private final CurrentUserProvider currentUserProvider;

    public InvestmentAssetService(
            InvestmentAssetRepository investmentAssetRepository,
            InvestmentTransactionRepository investmentTransactionRepository,
            CurrentUserProvider currentUserProvider) {
        this.investmentAssetRepository = investmentAssetRepository;
        this.investmentTransactionRepository = investmentTransactionRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<InvestmentAsset> findAll() {
        return investmentAssetRepository.findAllByUserIdOrderByTickerAsc(currentUserProvider.currentUserId());
    }

    @Transactional(readOnly = true)
    public InvestmentAsset findById(UUID id) {
        return investmentAssetRepository
                .findByIdAndUserId(id, currentUserProvider.currentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Ativo não encontrado."));
    }

    public InvestmentAsset create(InvestmentAssetRequest request) {
        InvestmentAsset asset = new InvestmentAsset(
                UuidV7Generator.generate(),
                currentUserProvider.currentUserId(),
                request.ticker(),
                request.name(),
                request.assetClass(),
                request.subclass(),
                request.indexer(),
                request.maturityDate(),
                request.purpose());
        return investmentAssetRepository.save(asset);
    }

    public InvestmentAsset update(UUID id, InvestmentAssetRequest request) {
        InvestmentAsset asset = findById(id);
        asset.setTicker(request.ticker());
        asset.setName(request.name());
        asset.setAssetClass(request.assetClass());
        asset.setSubclass(request.subclass());
        asset.setIndexer(request.indexer());
        asset.setMaturityDate(request.maturityDate());
        asset.setPurpose(request.purpose());
        return asset;
    }

    public InvestmentAsset updatePrice(UUID id, BigDecimal price) {
        InvestmentAsset asset = findById(id);
        asset.updatePrice(price, Instant.now());
        return asset;
    }

    public void archive(UUID id) {
        findById(id).archive();
    }

    public void unarchive(UUID id) {
        findById(id).unarchive();
    }

    @Transactional(readOnly = true)
    public Position calculatePosition(UUID assetId) {
        UUID userId = currentUserProvider.currentUserId();
        findById(assetId);
        return PositionCalculator.calculate(
                investmentTransactionRepository.findAllByUserIdAndAssetIdOrderByDateAsc(userId, assetId));
    }
}
