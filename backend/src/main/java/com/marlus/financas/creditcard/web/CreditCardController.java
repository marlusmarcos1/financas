package com.marlus.financas.creditcard.web;

import com.marlus.financas.creditcard.domain.CreditCard;
import com.marlus.financas.creditcard.mapper.CreditCardMapper;
import com.marlus.financas.creditcard.service.CreditCardService;
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
@RequestMapping("/api/v1/credit-cards")
public class CreditCardController {

    private final CreditCardService creditCardService;
    private final CreditCardMapper creditCardMapper;

    public CreditCardController(CreditCardService creditCardService, CreditCardMapper creditCardMapper) {
        this.creditCardService = creditCardService;
        this.creditCardMapper = creditCardMapper;
    }

    @GetMapping
    public List<CreditCardResponse> list() {
        return creditCardService.findAll().stream().map(creditCardMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public CreditCardResponse get(@PathVariable UUID id) {
        return creditCardMapper.toResponse(creditCardService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreditCardResponse create(@Valid @RequestBody CreditCardRequest request) {
        CreditCard creditCard = creditCardService.create(request);
        return creditCardMapper.toResponse(creditCard);
    }

    @PutMapping("/{id}")
    public CreditCardResponse update(@PathVariable UUID id, @Valid @RequestBody CreditCardRequest request) {
        CreditCard creditCard = creditCardService.update(id, request);
        return creditCardMapper.toResponse(creditCard);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        creditCardService.archive(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable UUID id) {
        creditCardService.unarchive(id);
        return ResponseEntity.noContent().build();
    }
}
