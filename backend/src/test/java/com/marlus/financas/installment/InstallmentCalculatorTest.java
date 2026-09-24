package com.marlus.financas.installment;

import static org.assertj.core.api.Assertions.assertThat;

import com.marlus.financas.installment.service.InstallmentCalculator;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstallmentCalculatorTest {

    @Test
    void evenDivisionProducesEqualInstallments() {
        List<BigDecimal> amounts = InstallmentCalculator.withoutInterest(new BigDecimal("4140.00"), 12);

        assertThat(amounts).hasSize(12);
        assertThat(amounts).allMatch(amount -> amount.compareTo(new BigDecimal("345.00")) == 0);
    }

    @Test
    void unevenDivisionAdjustsCentsOnLastInstallment() {
        // 100 / 3 = 33.33 (x2) + 33.34 na última, para fechar em 100.00 exatos.
        List<BigDecimal> amounts = InstallmentCalculator.withoutInterest(new BigDecimal("100.00"), 3);

        assertThat(amounts).hasSize(3);
        assertThat(amounts.get(0)).isEqualByComparingTo("33.33");
        assertThat(amounts.get(1)).isEqualByComparingTo("33.33");
        assertThat(amounts.get(2)).isEqualByComparingTo("33.34");

        BigDecimal total = amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("100.00");
    }

    @Test
    void pricePaymentMatchesKnownAmortizationExample() {
        // PV=1000, i=2% a.m., n=10 -> PMT conhecido (tabela Price clássica) ~= 111.33.
        BigDecimal pmt = InstallmentCalculator.pricePayment(new BigDecimal("1000"), new BigDecimal("0.02"), 10);

        assertThat(pmt).isEqualByComparingTo("111.33");
    }

    @Test
    void effectiveRateCompoundsMonthlyRateOverInstallments() {
        // (1.02)^10 - 1 ~= 0.218994
        BigDecimal rate = InstallmentCalculator.effectiveRate(new BigDecimal("0.02"), 10);

        assertThat(rate).isEqualByComparingTo("0.218994");
    }
}
