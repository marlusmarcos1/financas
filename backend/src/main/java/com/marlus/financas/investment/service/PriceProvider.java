package com.marlus.financas.investment.service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Ponto de extensão para cotação automática de ativos (seção 7.6). Por ora só existe a
 * implementação manual ({@link ManualPriceProvider}), que nunca resolve um preço sozinha — o
 * preço é sempre informado pelo usuário via {@code InvestmentAssetService}. Uma implementação
 * futura pluga aqui uma API de cotações sem precisar mudar o resto do módulo.
 */
public interface PriceProvider {

    Optional<BigDecimal> fetchCurrentPrice(String ticker);
}
