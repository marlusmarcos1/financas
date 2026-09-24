package com.marlus.financas.account.service;

import com.marlus.financas.account.domain.Account;
import com.marlus.financas.account.repository.AccountRepository;
import com.marlus.financas.account.web.AccountRequest;
import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.common.EntityNotFoundException;
import com.marlus.financas.common.UuidV7Generator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final CurrentUserProvider currentUserProvider;

    public AccountService(AccountRepository accountRepository, CurrentUserProvider currentUserProvider) {
        this.accountRepository = accountRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<Account> findAll() {
        return accountRepository.findAllByUserIdOrderByArchivedAscNameAsc(currentUserProvider.currentUserId());
    }

    @Transactional(readOnly = true)
    public Account findById(UUID id) {
        return accountRepository
                .findByIdAndUserId(id, currentUserProvider.currentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada."));
    }

    public Account create(AccountRequest request) {
        Account account = new Account(
                UuidV7Generator.generate(),
                currentUserProvider.currentUserId(),
                request.name(),
                request.type(),
                request.institution(),
                request.initialBalance(),
                request.purpose());
        return accountRepository.save(account);
    }

    public Account update(UUID id, AccountRequest request) {
        Account account = findById(id);
        account.setName(request.name());
        account.setType(request.type());
        account.setInstitution(request.institution());
        account.setInitialBalance(request.initialBalance());
        account.setPurpose(request.purpose());
        return account;
    }

    public void archive(UUID id) {
        findById(id).archive();
    }

    public void unarchive(UUID id) {
        findById(id).unarchive();
    }
}
