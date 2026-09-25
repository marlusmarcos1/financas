package com.marlus.financas.investment.service;

import com.marlus.financas.investment.domain.InvestmentTransaction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Deriva posição e preço médio a partir do histórico de operações, pelo método do
 * <b>preço médio ponderado</b> (seção 7.6): cada compra recalcula o preço médio como o custo
 * total acumulado dividido pela quantidade total; vendas reduzem a quantidade mas não alteram
 * o preço médio (o resultado realizado é a diferença entre o preço de venda e o preço médio no
 * momento da venda). Proventos (dividendo/JCP/juros) não afetam posição nem preço médio.
 */
public final class PositionCalculator {

    private PositionCalculator() {
    }

    public static Position calculate(List<InvestmentTransaction> transactionsOrderedByDate) {
        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal averagePrice = BigDecimal.ZERO;
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal realizedGain = BigDecimal.ZERO;

        for (InvestmentTransaction tx : transactionsOrderedByDate) {
            switch (tx.getType()) {
                case BUY -> {
                    BigDecimal costBefore = quantity.multiply(averagePrice);
                    BigDecimal costOfPurchase = tx.getQuantity().multiply(tx.getUnitPrice()).add(tx.getFees());
                    BigDecimal newQuantity = quantity.add(tx.getQuantity());
                    averagePrice = newQuantity.signum() > 0
                            ? costBefore.add(costOfPurchase).divide(newQuantity, 6, RoundingMode.HALF_EVEN)
                            : BigDecimal.ZERO;
                    quantity = newQuantity;
                }
                case SELL -> {
                    BigDecimal proceeds = tx.getQuantity().multiply(tx.getUnitPrice()).subtract(tx.getFees());
                    BigDecimal costBasis = tx.getQuantity().multiply(averagePrice);
                    realizedGain = realizedGain.add(proceeds.subtract(costBasis));
                    quantity = quantity.subtract(tx.getQuantity());
                }
                case DIVIDEND, JCP, INTEREST -> totalIncome = totalIncome.add(tx.getAmount());
                case FEE -> realizedGain = realizedGain.subtract(tx.getAmount());
            }
        }

        BigDecimal totalCost = quantity.multiply(averagePrice).setScale(2, RoundingMode.HALF_EVEN);
        return new Position(quantity, averagePrice, totalCost, totalIncome, realizedGain);
    }
}
