package com.marlus.financas.calendar.service;

import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.calendar.web.CalendarItemResponse;
import com.marlus.financas.creditcard.domain.CreditCard;
import com.marlus.financas.creditcard.repository.CreditCardRepository;
import com.marlus.financas.invoice.domain.Invoice;
import com.marlus.financas.invoice.domain.InvoiceStatus;
import com.marlus.financas.invoice.repository.InvoiceRepository;
import com.marlus.financas.invoice.service.InvoiceService;
import com.marlus.financas.transaction.domain.Transaction;
import com.marlus.financas.transaction.domain.TransactionStatus;
import com.marlus.financas.transaction.repository.TransactionRepository;
import com.marlus.financas.transaction.repository.TransactionSpecifications;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Calendário de vencimentos (seção 11): faturas, parcelas e recorrências futuras. */
@Service
@Transactional(readOnly = true)
public class CalendarService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final CreditCardRepository creditCardRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUserProvider currentUserProvider;

    public CalendarService(
            InvoiceRepository invoiceRepository,
            InvoiceService invoiceService,
            CreditCardRepository creditCardRepository,
            TransactionRepository transactionRepository,
            CurrentUserProvider currentUserProvider) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceService = invoiceService;
        this.creditCardRepository = creditCardRepository;
        this.transactionRepository = transactionRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public List<CalendarItemResponse> upcoming(LocalDate from, LocalDate to) {
        UUID userId = currentUserProvider.currentUserId();
        List<CalendarItemResponse> items = new ArrayList<>();

        Map<UUID, String> cardNames = creditCardRepository.findAllByUserIdOrderByArchivedAscNameAsc(userId).stream()
                .collect(Collectors.toMap(CreditCard::getId, CreditCard::getName));

        for (Invoice invoice : invoiceRepository.findAllByUserId(userId)) {
            if (invoice.getStatus() == InvoiceStatus.PAID) {
                continue;
            }
            if (!invoice.getDueDate().isBefore(from) && !invoice.getDueDate().isAfter(to)) {
                items.add(new CalendarItemResponse(
                        invoice.getDueDate(),
                        "INVOICE",
                        "Fatura " + cardNames.getOrDefault(invoice.getCardId(), "cartão"),
                        invoiceService.calculateTotal(invoice.getId()).subtract(invoice.getPaidAmount())));
            }
        }

        Specification<Transaction> spec = Specification.where(TransactionSpecifications.belongsToUser(userId))
                .and(TransactionSpecifications.status(TransactionStatus.PLANNED))
                .and(TransactionSpecifications.committed())
                .and(TransactionSpecifications.dateFrom(from))
                .and(TransactionSpecifications.dateTo(to));
        for (Transaction transaction : transactionRepository.findAll(spec)) {
            String type = transaction.getInstallmentPlanId() != null ? "INSTALLMENT" : "RECURRING";
            items.add(new CalendarItemResponse(
                    transaction.getDate(), type, transaction.getDescription(), transaction.getAmount()));
        }

        return items.stream().sorted(Comparator.comparing(CalendarItemResponse::date)).toList();
    }
}
