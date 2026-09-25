package com.marlus.financas.investment.service;

import com.marlus.financas.account.repository.AccountRepository;
import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.common.EntityNotFoundException;
import com.marlus.financas.common.UuidV7Generator;
import com.marlus.financas.investment.domain.InvestmentAsset;
import com.marlus.financas.investment.domain.InvestmentTransaction;
import com.marlus.financas.investment.repository.InvestmentAssetRepository;
import com.marlus.financas.investment.repository.InvestmentTransactionRepository;
import com.marlus.financas.investment.web.InvestmentTransactionRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InvestmentTransactionService {

    private final InvestmentTransactionRepository investmentTransactionRepository;
    private final InvestmentAssetRepository investmentAssetRepository;
    private final AccountRepository accountRepository;
    private final CurrentUserProvider currentUserProvider;

    public InvestmentTransactionService(
            InvestmentTransactionRepository investmentTransactionRepository,
            InvestmentAssetRepository investmentAssetRepository,
            AccountRepository accountRepository,
            CurrentUserProvider currentUserProvider) {
        this.investmentTransactionRepository = investmentTransactionRepository;
        this.investmentAssetRepository = investmentAssetRepository;
        this.accountRepository = accountRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<InvestmentTransaction> findAllByAsset(UUID assetId) {
        UUID userId = currentUserProvider.currentUserId();
        investmentAssetRepository
                .findByIdAndUserId(assetId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Ativo não encontrado."));
        return investmentTransactionRepository.findAllByUserIdAndAssetIdOrderByDateAsc(userId, assetId);
    }

    public InvestmentTransaction create(UUID assetId, InvestmentTransactionRequest request) {
        UUID userId = currentUserProvider.currentUserId();
        InvestmentAsset asset = investmentAssetRepository
                .findByIdAndUserId(assetId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Ativo não encontrado."));
        if (request.accountId() != null) {
            accountRepository
                    .findByIdAndUserId(request.accountId(), userId)
                    .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada."));
        }

        InvestmentTransaction transaction = new InvestmentTransaction(
                UuidV7Generator.generate(),
                userId,
                asset.getId(),
                request.type(),
                request.date(),
                orZero(request.quantity()),
                orZero(request.unitPrice()),
                orZero(request.fees()),
                request.amount(),
                request.accountId());
        return investmentTransactionRepository.save(transaction);
    }

    public void delete(UUID id) {
        InvestmentTransaction transaction = investmentTransactionRepository
                .findByIdAndUserId(id, currentUserProvider.currentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Operação não encontrada."));
        investmentTransactionRepository.delete(transaction);
    }

    private BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
