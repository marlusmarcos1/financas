package com.marlus.financas.installment.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Cálculo do valor das parcelas: divisão simples com ajuste de centavos na última parcela
 * (sem juros) ou Tabela Price / sistema de amortização francês (com juros). Ver seção 7.2.
 */
public final class InstallmentCalculator {

    private static final MathContext MC = new MathContext(20);

    private InstallmentCalculator() {
    }

    /** Sem juros: total / n, arredondado HALF_EVEN, com a última parcela absorvendo a diferença de centavos. */
    public static List<BigDecimal> withoutInterest(BigDecimal totalAmount, int installmentCount) {
        BigDecimal base = totalAmount
                .divide(BigDecimal.valueOf(installmentCount), 2, RoundingMode.HALF_EVEN);
        BigDecimal last = totalAmount.subtract(base.multiply(BigDecimal.valueOf(installmentCount - 1)));

        List<BigDecimal> amounts = new ArrayList<>();
        for (int i = 1; i < installmentCount; i++) {
            amounts.add(base);
        }
        amounts.add(last);
        return amounts;
    }

    /** Com juros (Tabela Price): parcelas iguais, calculadas pelo sistema de amortização francês. */
    public static BigDecimal pricePayment(BigDecimal presentValue, BigDecimal monthlyRate, int installmentCount) {
        BigDecimal onePlusI = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusIPowN = onePlusI.pow(installmentCount, MC);
        BigDecimal numerator = presentValue.multiply(monthlyRate, MC).multiply(onePlusIPowN, MC);
        BigDecimal denominator = onePlusIPowN.subtract(BigDecimal.ONE);
        return numerator.divide(denominator, MC).setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Taxa efetiva total do período: (1+i)^n - 1. */
    public static BigDecimal effectiveRate(BigDecimal monthlyRate, int installmentCount) {
        BigDecimal onePlusI = BigDecimal.ONE.add(monthlyRate);
        return onePlusI.pow(installmentCount, MC).subtract(BigDecimal.ONE).setScale(6, RoundingMode.HALF_EVEN);
    }
}
