package com.marlus.financas.invoice.service;

import java.time.LocalDate;

/**
 * Estratégia de cálculo do ciclo de fatura (competência, fechamento, vencimento) a partir da
 * data de uma compra e dos dias de fechamento/vencimento configurados no cartão. Ver seção 7.1
 * da especificação.
 */
public interface InvoiceCycleCalculator {

    InvoiceCycle calculate(LocalDate purchaseDate, short closingDay, short dueDay);
}
