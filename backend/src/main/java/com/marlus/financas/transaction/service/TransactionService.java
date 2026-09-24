package com.marlus.financas.transaction.service;

import com.marlus.financas.account.repository.AccountRepository;
import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.category.repository.CategoryRepository;
import com.marlus.financas.common.EntityNotFoundException;
import com.marlus.financas.common.UuidV7Generator;
import com.marlus.financas.creditcard.domain.CreditCard;
import com.marlus.financas.creditcard.repository.CreditCardRepository;
import com.marlus.financas.invoice.domain.Invoice;
import com.marlus.financas.invoice.service.InvoiceService;
import com.marlus.financas.transaction.domain.Transaction;
import com.marlus.financas.transaction.domain.TransactionKind;
import com.marlus.financas.transaction.domain.TransactionStatus;
import com.marlus.financas.transaction.repository.TransactionRepository;
import com.marlus.financas.transaction.repository.TransactionSpecifications;
import com.marlus.financas.transaction.web.TransactionRequest;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CreditCardRepository creditCardRepository;
    private final CategoryRepository categoryRepository;
    private final InvoiceService invoiceService;
    private final CurrentUserProvider currentUserProvider;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            CreditCardRepository creditCardRepository,
            CategoryRepository categoryRepository,
            InvoiceService invoiceService,
            CurrentUserProvider currentUserProvider) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.creditCardRepository = creditCardRepository;
        this.categoryRepository = categoryRepository;
        this.invoiceService = invoiceService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public Page<Transaction> search(
            LocalDate from,
            LocalDate to,
            UUID categoryId,
            UUID accountId,
            UUID cardId,
            TransactionStatus status,
            TransactionKind kind,
            Pageable pageable) {
        UUID userId = currentUserProvider.currentUserId();
        Specification<Transaction> spec = Specification.where(TransactionSpecifications.belongsToUser(userId))
                .and(TransactionSpecifications.dateFrom(from))
                .and(TransactionSpecifications.dateTo(to))
                .and(TransactionSpecifications.categoryId(categoryId))
                .and(TransactionSpecifications.accountId(accountId))
                .and(TransactionSpecifications.cardId(cardId))
                .and(TransactionSpecifications.status(status))
                .and(TransactionSpecifications.kind(kind));
        return transactionRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Transaction findById(UUID id) {
        return transactionRepository
                .findByIdAndUserId(id, currentUserProvider.currentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Lançamento não encontrado."));
    }

    public Transaction create(TransactionRequest request) {
        UUID userId = currentUserProvider.currentUserId();
        validatePaymentMethod(request.kind(), request.accountId(), request.cardId());
        validateReferences(userId, request.categoryId(), request.accountId(), request.cardId());

        Transaction transaction = new Transaction(
                UuidV7Generator.generate(),
                userId,
                request.kind(),
                request.description(),
                request.amount(),
                request.date(),
                request.categoryId(),
                request.accountId(),
                request.cardId(),
                request.status() != null ? request.status() : TransactionStatus.PLANNED,
                request.notes());

        attachInvoiceIfCardExpense(transaction);
        return transactionRepository.save(transaction);
    }

    public Transaction update(UUID id, TransactionRequest request) {
        Transaction transaction = findById(id);
        validatePaymentMethod(request.kind(), request.accountId(), request.cardId());
        validateReferences(transaction.getUserId(), request.categoryId(), request.accountId(), request.cardId());

        transaction.setKind(request.kind());
        transaction.setDescription(request.description());
        transaction.setAmount(request.amount());
        transaction.setDate(request.date());
        transaction.setCategoryId(request.categoryId());
        transaction.setPaymentMethod(request.accountId(), request.cardId());
        transaction.setStatus(request.status() != null ? request.status() : transaction.getStatus());
        transaction.setNotes(request.notes());
        transaction.setInvoiceId(null);

        attachInvoiceIfCardExpense(transaction);
        return transaction;
    }

    public void markAsPaid(UUID id) {
        findById(id).setStatus(TransactionStatus.PAID);
    }

    public void delete(UUID id) {
        transactionRepository.delete(findById(id));
    }

    private void attachInvoiceIfCardExpense(Transaction transaction) {
        if (transaction.getCardId() == null) {
            return;
        }
        CreditCard card = creditCardRepository
                .findByIdAndUserId(transaction.getCardId(), transaction.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Cartão não encontrado."));
        Invoice invoice = invoiceService.findOrCreateInvoiceFor(card, transaction.getDate());
        transaction.setInvoiceId(invoice.getId());
    }

    private void validatePaymentMethod(TransactionKind kind, UUID accountId, UUID cardId) {
        if (kind == TransactionKind.EXPENSE) {
            if ((accountId == null) == (cardId == null)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Uma despesa deve ser de conta ou de cartão, não os dois.");
            }
            return;
        }
        if (accountId == null || cardId != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Receitas e transferências precisam de uma conta, sem cartão.");
        }
    }

    private void validateReferences(UUID userId, UUID categoryId, UUID accountId, UUID cardId) {
        if (categoryId != null) {
            categoryRepository
                    .findByIdAndUserId(categoryId, userId)
                    .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));
        }
        if (accountId != null) {
            accountRepository
                    .findByIdAndUserId(accountId, userId)
                    .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada."));
        }
        if (cardId != null) {
            creditCardRepository
                    .findByIdAndUserId(cardId, userId)
                    .orElseThrow(() -> new EntityNotFoundException("Cartão não encontrado."));
        }
    }
}
