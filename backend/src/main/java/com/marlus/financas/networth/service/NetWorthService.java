package com.marlus.financas.networth.service;

import com.marlus.financas.account.domain.Account;
import com.marlus.financas.account.repository.AccountRepository;
import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.investment.service.PortfolioService;
import com.marlus.financas.networth.web.AccountBalanceResponse;
import com.marlus.financas.networth.web.NetWorthResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Patrimônio total = contas (inclui reserva) + investimentos (seção 7.6). */
@Service
@Transactional(readOnly = true)
public class NetWorthService {

    private final AccountRepository accountRepository;
    private final AccountBalanceService accountBalanceService;
    private final PortfolioService portfolioService;
    private final CurrentUserProvider currentUserProvider;

    public NetWorthService(
            AccountRepository accountRepository,
            AccountBalanceService accountBalanceService,
            PortfolioService portfolioService,
            CurrentUserProvider currentUserProvider) {
        this.accountRepository = accountRepository;
        this.accountBalanceService = accountBalanceService;
        this.portfolioService = portfolioService;
        this.currentUserProvider = currentUserProvider;
    }

    public NetWorthResponse summarize() {
        UUID userId = currentUserProvider.currentUserId();
        List<Account> accounts = accountRepository.findAllByUserIdOrderByArchivedAscNameAsc(userId).stream()
                .filter(account -> !account.isArchived())
                .toList();

        List<AccountBalanceResponse> balances = accounts.stream()
                .map(account -> new AccountBalanceResponse(
                        account.getId(),
                        account.getName(),
                        account.getPurpose().name(),
                        accountBalanceService.currentBalance(account)))
                .toList();

        BigDecimal accountsTotal = balances.stream().map(AccountBalanceResponse::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal investmentsTotal = portfolioService.summarize().totalCurrentValue();

        return new NetWorthResponse(accountsTotal, investmentsTotal, accountsTotal.add(investmentsTotal), balances);
    }
}
