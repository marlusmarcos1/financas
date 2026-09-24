package com.marlus.financas.account.web;

import com.marlus.financas.account.domain.Account;
import com.marlus.financas.account.mapper.AccountMapper;
import com.marlus.financas.account.service.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;

    public AccountController(AccountService accountService, AccountMapper accountMapper) {
        this.accountService = accountService;
        this.accountMapper = accountMapper;
    }

    @GetMapping
    public List<AccountResponse> list() {
        return accountService.findAll().stream().map(accountMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable UUID id) {
        return accountMapper.toResponse(accountService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody AccountRequest request) {
        Account account = accountService.create(request);
        return accountMapper.toResponse(account);
    }

    @PutMapping("/{id}")
    public AccountResponse update(@PathVariable UUID id, @Valid @RequestBody AccountRequest request) {
        Account account = accountService.update(id, request);
        return accountMapper.toResponse(account);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        accountService.archive(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable UUID id) {
        accountService.unarchive(id);
        return ResponseEntity.noContent().build();
    }
}
