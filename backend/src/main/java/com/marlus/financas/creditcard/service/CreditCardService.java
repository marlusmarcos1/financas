package com.marlus.financas.creditcard.service;

import com.marlus.financas.account.repository.AccountRepository;
import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.common.EntityNotFoundException;
import com.marlus.financas.common.UuidV7Generator;
import com.marlus.financas.creditcard.domain.CreditCard;
import com.marlus.financas.creditcard.repository.CreditCardRepository;
import com.marlus.financas.creditcard.web.CreditCardRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreditCardService {

    private final CreditCardRepository creditCardRepository;
    private final AccountRepository accountRepository;
    private final CurrentUserProvider currentUserProvider;

    public CreditCardService(
            CreditCardRepository creditCardRepository,
            AccountRepository accountRepository,
            CurrentUserProvider currentUserProvider) {
        this.creditCardRepository = creditCardRepository;
        this.accountRepository = accountRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<CreditCard> findAll() {
        return creditCardRepository.findAllByUserIdOrderByArchivedAscNameAsc(currentUserProvider.currentUserId());
    }

    @Transactional(readOnly = true)
    public CreditCard findById(UUID id) {
        return creditCardRepository
                .findByIdAndUserId(id, currentUserProvider.currentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Cartão não encontrado."));
    }

    public CreditCard create(CreditCardRequest request) {
        UUID userId = currentUserProvider.currentUserId();
        validatePaymentAccount(request.defaultPaymentAccountId(), userId);
        CreditCard creditCard = new CreditCard(
                UuidV7Generator.generate(),
                userId,
                request.name(),
                request.issuer(),
                request.creditLimit(),
                request.closingDay(),
                request.dueDay(),
                request.defaultPaymentAccountId(),
                request.color());
        return creditCardRepository.save(creditCard);
    }

    public CreditCard update(UUID id, CreditCardRequest request) {
        CreditCard creditCard = findById(id);
        validatePaymentAccount(request.defaultPaymentAccountId(), creditCard.getUserId());
        creditCard.setName(request.name());
        creditCard.setIssuer(request.issuer());
        creditCard.setCreditLimit(request.creditLimit());
        creditCard.setClosingDay(request.closingDay());
        creditCard.setDueDay(request.dueDay());
        creditCard.setDefaultPaymentAccountId(request.defaultPaymentAccountId());
        creditCard.setColor(request.color());
        return creditCard;
    }

    public void archive(UUID id) {
        findById(id).archive();
    }

    public void unarchive(UUID id) {
        findById(id).unarchive();
    }

    private void validatePaymentAccount(UUID accountId, UUID userId) {
        if (accountId == null) {
            return;
        }
        accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Conta de pagamento padrão não encontrada."));
    }
}
