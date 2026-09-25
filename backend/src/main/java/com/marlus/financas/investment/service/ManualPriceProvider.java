package com.marlus.financas.investment.service;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Implementação padrão: não busca cotação automaticamente, o preço é sempre manual. */
@Component
public class ManualPriceProvider implements PriceProvider {

    @Override
    public Optional<BigDecimal> fetchCurrentPrice(String ticker) {
        return Optional.empty();
    }
}
