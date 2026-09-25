package com.marlus.financas.networth.service;

import com.marlus.financas.account.domain.Account;
import com.marlus.financas.account.repository.AccountRepository;
import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.investment.domain.InvestmentAsset;
import com.marlus.financas.investment.domain.InvestmentTransaction;
import com.marlus.financas.investment.repository.InvestmentAssetRepository;
import com.marlus.financas.investment.repository.InvestmentTransactionRepository;
import com.marlus.financas.investment.service.PositionCalculator;
import com.marlus.financas.networth.web.NetWorthPointResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Evolução patrimonial mês a mês (seção 11). O componente de contas é reconstruído com precisão
 * a partir do histórico de lançamentos; o de investimentos usa o <b>capital aportado até a
 * data</b> (custo, via {@link PositionCalculator}), não o valor de mercado histórico — o app não
 * tem um provedor de cotação automática (seção 13), então não há preço de fechamento de meses
 * passados para reconstruir o valor de mercado real. Ver DECISIONS.md (Fase 9).
 */
@Service
@Transactional(readOnly = true)
public class NetWorthHistoryService {

    private final AccountRepository accountRepository;
    private final AccountBalanceService accountBalanceService;
    private final InvestmentAssetRepository investmentAssetRepository;
    private final InvestmentTransactionRepository investmentTransactionRepository;
    private final CurrentUserProvider currentUserProvider;

    public NetWorthHistoryService(
            AccountRepository accountRepository,
            AccountBalanceService accountBalanceService,
            InvestmentAssetRepository investmentAssetRepository,
            InvestmentTransactionRepository investmentTransactionRepository,
            CurrentUserProvider currentUserProvider) {
        this.accountRepository = accountRepository;
        this.accountBalanceService = accountBalanceService;
        this.investmentAssetRepository = investmentAssetRepository;
        this.investmentTransactionRepository = investmentTransactionRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public List<NetWorthPointResponse> history(int months) {
        UUID userId = currentUserProvider.currentUserId();
        List<Account> accounts =
                accountRepository.findAllByUserIdOrderByArchivedAscNameAsc(userId).stream().toList();
        List<InvestmentAsset> assets =
                investmentAssetRepository.findAllByUserIdOrderByTickerAsc(userId).stream().toList();

        YearMonth end = YearMonth.now();
        YearMonth start = end.minusMonths(months - 1L);

        List<NetWorthPointResponse> points = new ArrayList<>();
        for (YearMonth cursor = start; !cursor.isAfter(end); cursor = cursor.plusMonths(1)) {
            LocalDate asOf = cursor.equals(end) ? LocalDate.now() : cursor.atEndOfMonth();

            BigDecimal accountsTotal = accounts.stream()
                    .map(account -> accountBalanceService.balanceAsOf(account, asOf))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal investedCapital = assets.stream()
                    .map(asset -> costBasisAsOf(asset, asOf))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            points.add(new NetWorthPointResponse(cursor, accountsTotal, investedCapital, accountsTotal.add(investedCapital)));
        }
        return points;
    }

    private BigDecimal costBasisAsOf(InvestmentAsset asset, LocalDate asOf) {
        List<InvestmentTransaction> transactionsUpToDate = investmentTransactionRepository
                .findAllByUserIdAndAssetIdOrderByDateAsc(asset.getUserId(), asset.getId())
                .stream()
                .filter(tx -> !tx.getDate().isAfter(asOf))
                .toList();
        return PositionCalculator.calculate(transactionsUpToDate).totalCost();
    }
}
