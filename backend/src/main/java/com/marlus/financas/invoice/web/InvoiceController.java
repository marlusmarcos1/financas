package com.marlus.financas.invoice.web;

import com.marlus.financas.invoice.domain.Invoice;
import com.marlus.financas.invoice.service.InvoiceService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping("/api/v1/credit-cards/{cardId}/invoices")
    public List<InvoiceResponse> listByCard(@PathVariable UUID cardId) {
        return invoiceService.findAllByCard(cardId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/api/v1/credit-cards/{cardId}/available-limit")
    public Map<String, BigDecimal> availableLimit(@PathVariable UUID cardId) {
        return Map.of("availableLimit", invoiceService.availableLimit(cardId));
    }

    @GetMapping("/api/v1/invoices/{id}")
    public InvoiceResponse get(@PathVariable UUID id) {
        return toResponse(invoiceService.findById(id));
    }

    @PostMapping("/api/v1/invoices/{id}/pay")
    public InvoiceResponse pay(@PathVariable UUID id, @Valid @RequestBody PayInvoiceRequest request) {
        Invoice invoice = invoiceService.pay(id, request.accountId(), request.amount(), request.paidOn());
        return toResponse(invoice);
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getCardId(),
                invoice.getReferenceMonth(),
                invoice.getClosingDate(),
                invoice.getDueDate(),
                invoice.getStatus(),
                invoiceService.calculateTotal(invoice.getId()),
                invoice.getPaidAmount(),
                invoice.getPaidOn(),
                invoice.getPaidFromAccountId());
    }
}
