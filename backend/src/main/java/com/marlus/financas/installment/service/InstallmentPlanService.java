package com.marlus.financas.installment.service;

import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.category.repository.CategoryRepository;
import com.marlus.financas.common.EntityNotFoundException;
import com.marlus.financas.common.UuidV7Generator;
import com.marlus.financas.creditcard.domain.CreditCard;
import com.marlus.financas.creditcard.repository.CreditCardRepository;
import com.marlus.financas.installment.domain.InstallmentPlan;
import com.marlus.financas.installment.repository.InstallmentPlanRepository;
import com.marlus.financas.installment.web.InstallmentPlanRequest;
import com.marlus.financas.invoice.domain.Invoice;
import com.marlus.financas.invoice.service.InvoiceService;
import com.marlus.financas.transaction.domain.Transaction;
import com.marlus.financas.transaction.domain.TransactionKind;
import com.marlus.financas.transaction.domain.TransactionStatus;
import com.marlus.financas.transaction.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Criação de planos de parcela (novos ou já em andamento) e geração das parcelas restantes. Ver seção 7.2. */
@Service
@Transactional
public class InstallmentPlanService {

    private final InstallmentPlanRepository installmentPlanRepository;
    private final CreditCardRepository creditCardRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final InvoiceService invoiceService;
    private final CurrentUserProvider currentUserProvider;

    public InstallmentPlanService(
            InstallmentPlanRepository installmentPlanRepository,
            CreditCardRepository creditCardRepository,
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            InvoiceService invoiceService,
            CurrentUserProvider currentUserProvider) {
        this.installmentPlanRepository = installmentPlanRepository;
        this.creditCardRepository = creditCardRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.invoiceService = invoiceService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<InstallmentPlan> findAll() {
        return installmentPlanRepository.findAllByUserIdOrderByPurchaseDateDesc(currentUserProvider.currentUserId());
    }

    @Transactional(readOnly = true)
    public InstallmentPlan findById(UUID id) {
        return installmentPlanRepository
                .findByIdAndUserId(id, currentUserProvider.currentUserId())
                .orElseThrow(() -> new EntityNotFoundException("Parcelamento não encontrado."));
    }

    public InstallmentPlan create(InstallmentPlanRequest request) {
        UUID userId = currentUserProvider.currentUserId();
        CreditCard card = creditCardRepository
                .findByIdAndUserId(request.cardId(), userId)
                .orElseThrow(() -> new EntityNotFoundException("Cartão não encontrado."));
        if (request.categoryId() != null) {
            categoryRepository
                    .findByIdAndUserId(request.categoryId(), userId)
                    .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada."));
        }

        short firstInstallmentNumber = request.firstInstallmentNumber() != null ? request.firstInstallmentNumber() : 1;
        if (firstInstallmentNumber > request.installmentCount()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A parcela inicial não pode ser maior que o total de parcelas.");
        }

        YearMonth firstInvoiceMonth = request.firstInvoiceMonth() != null
                ? request.firstInvoiceMonth()
                : YearMonth.parse(invoiceService
                        .findOrCreateInvoiceFor(card, request.purchaseDate())
                        .getReferenceMonth());

        boolean hasInterest = request.interestRateMonthly() != null && request.interestRateMonthly().signum() > 0;
        List<BigDecimal> amounts = hasInterest
                ? java.util.Collections.nCopies(
                        request.installmentCount(),
                        InstallmentCalculator.pricePayment(
                                request.totalAmount(), request.interestRateMonthly(), request.installmentCount()))
                : InstallmentCalculator.withoutInterest(request.totalAmount(), request.installmentCount());

        InstallmentPlan plan = new InstallmentPlan(
                UuidV7Generator.generate(),
                userId,
                card.getId(),
                request.description(),
                request.purchaseDate(),
                request.totalAmount(),
                request.installmentCount(),
                amounts.get(0),
                firstInstallmentNumber,
                firstInvoiceMonth,
                hasInterest ? request.interestRateMonthly() : null,
                request.categoryId());
        installmentPlanRepository.save(plan);

        generateInstallmentTransactions(plan, card, amounts);
        return plan;
    }

    public void delete(UUID id) {
        InstallmentPlan plan = findById(id);
        List<Transaction> transactions =
                transactionRepository.findAllByUserIdAndInstallmentPlanId(plan.getUserId(), plan.getId());
        transactionRepository.deleteAll(transactions);
        installmentPlanRepository.delete(plan);
    }

    private void generateInstallmentTransactions(InstallmentPlan plan, CreditCard card, List<BigDecimal> amounts) {
        for (int installmentNumber = plan.getFirstInstallmentNumber();
                installmentNumber <= plan.getInstallmentCount();
                installmentNumber++) {
            int offset = installmentNumber - plan.getFirstInstallmentNumber();
            YearMonth referenceMonth = plan.getFirstInvoiceMonth().plusMonths(offset);
            Invoice invoice = invoiceService.findOrCreateInvoiceForMonth(card, referenceMonth);

            Transaction transaction = new Transaction(
                    UuidV7Generator.generate(),
                    plan.getUserId(),
                    TransactionKind.EXPENSE,
                    plan.getDescription() + " (parcela " + installmentNumber + "/" + plan.getInstallmentCount() + ")",
                    amounts.get(installmentNumber - 1),
                    invoice.getClosingDate(),
                    plan.getCategoryId(),
                    null,
                    card.getId(),
                    TransactionStatus.PLANNED,
                    null);
            transaction.setInvoiceId(invoice.getId());
            transaction.setInstallmentPlanId(plan.getId());
            transaction.setInstallmentNumber((short) installmentNumber);
            transactionRepository.save(transaction);
        }
    }
}
