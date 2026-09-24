package com.marlus.financas.invoice.service;

import com.marlus.financas.account.repository.AccountRepository;
import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.common.EntityNotFoundException;
import com.marlus.financas.common.UuidV7Generator;
import com.marlus.financas.creditcard.domain.CreditCard;
import com.marlus.financas.creditcard.repository.CreditCardRepository;
import com.marlus.financas.invoice.domain.Invoice;
import com.marlus.financas.invoice.domain.InvoiceStatus;
import com.marlus.financas.invoice.repository.InvoiceRepository;
import com.marlus.financas.transaction.domain.Transaction;
import com.marlus.financas.transaction.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CreditCardRepository creditCardRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final InvoiceCycleCalculator invoiceCycleCalculator;
    private final CurrentUserProvider currentUserProvider;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            CreditCardRepository creditCardRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            InvoiceCycleCalculator invoiceCycleCalculator,
            CurrentUserProvider currentUserProvider) {
        this.invoiceRepository = invoiceRepository;
        this.creditCardRepository = creditCardRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.invoiceCycleCalculator = invoiceCycleCalculator;
        this.currentUserProvider = currentUserProvider;
    }

    /** Encontra a fatura do cartão para a competência da data informada, criando-a se necessário. */
    public Invoice findOrCreateInvoiceFor(CreditCard card, LocalDate purchaseDate) {
        InvoiceCycle cycle = invoiceCycleCalculator.calculate(purchaseDate, card.getClosingDay(), card.getDueDay());
        return invoiceRepository
                .findByCardIdAndReferenceMonth(card.getId(), cycle.referenceMonth().toString())
                .orElseGet(() -> invoiceRepository.save(new Invoice(
                        UuidV7Generator.generate(),
                        card.getUserId(),
                        card.getId(),
                        cycle.referenceMonth(),
                        cycle.closingDate(),
                        cycle.dueDate())));
    }

    /**
     * Encontra/cria a fatura de um cartão para uma competência específica (usado ao gerar as
     * parcelas restantes de um parcelamento, onde já sabemos o mês-alvo de cada uma).
     */
    public Invoice findOrCreateInvoiceForMonth(CreditCard card, YearMonth referenceMonth) {
        LocalDate closingDateOfThatMonth =
                referenceMonth.atDay(Math.min(card.getClosingDay(), referenceMonth.lengthOfMonth()));
        return findOrCreateInvoiceFor(card, closingDateOfThatMonth);
    }

    @Transactional(readOnly = true)
    public List<Invoice> findAllByCard(UUID cardId) {
        return invoiceRepository.findAllByUserIdAndCardIdOrderByReferenceMonthAsc(
                currentUserProvider.currentUserId(), cardId);
    }

    @Transactional(readOnly = true)
    public Invoice findById(UUID id) {
        return invoiceRepository
                .findByIdAndUserId(id, currentUserProvider.currentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Fatura não encontrada."));
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotal(UUID invoiceId) {
        UUID userId = currentUserProvider.currentUserId();
        return transactionRepository.findAllByUserIdAndInvoiceId(userId, invoiceId).stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Invoice pay(UUID invoiceId, UUID accountId, BigDecimal amount, LocalDate paidOn) {
        Invoice invoice = findById(invoiceId);
        accountRepository
                .findByIdAndUserId(accountId, invoice.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Conta de pagamento não encontrada."));

        BigDecimal total = calculateTotal(invoiceId);
        invoice.registerPayment(amount, paidOn, accountId);
        if (invoice.getPaidAmount().compareTo(total) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        }
        return invoice;
    }

    /** Limite disponível = limite do cartão - total das faturas ainda não totalmente pagas. */
    @Transactional(readOnly = true)
    public BigDecimal availableLimit(UUID cardId) {
        UUID userId = currentUserProvider.currentUserId();
        CreditCard card = creditCardRepository
                .findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Cartão não encontrado."));

        BigDecimal committed = invoiceRepository.findAllByUserIdAndCardIdOrderByReferenceMonthAsc(userId, cardId)
                .stream()
                .filter(invoice -> invoice.getStatus() != InvoiceStatus.PAID)
                .map(invoice -> calculateTotal(invoice.getId()).subtract(invoice.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return card.getCreditLimit().subtract(committed);
    }
}
