package com.marlus.financas.investment;

import static org.assertj.core.api.Assertions.assertThat;

import com.marlus.financas.common.UuidV7Generator;
import com.marlus.financas.investment.domain.InvestmentTransaction;
import com.marlus.financas.investment.domain.InvestmentTransactionType;
import com.marlus.financas.investment.service.Position;
import com.marlus.financas.investment.service.PositionCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PositionCalculatorTest {

    private static InvestmentTransaction tx(
            InvestmentTransactionType type, String date, String quantity, String unitPrice, String fees, String amount) {
        UUID user = UuidV7Generator.generate();
        return new InvestmentTransaction(
                UuidV7Generator.generate(),
                user,
                UuidV7Generator.generate(),
                type,
                LocalDate.parse(date),
                new BigDecimal(quantity),
                new BigDecimal(unitPrice),
                new BigDecimal(fees),
                new BigDecimal(amount),
                null);
    }

    @Test
    void singleBuySetsQuantityAndAveragePrice() {
        Position position = PositionCalculator.calculate(
                List.of(tx(InvestmentTransactionType.BUY, "2026-01-10", "100", "10.00", "0", "1000.00")));

        assertThat(position.quantity()).isEqualByComparingTo("100");
        assertThat(position.averagePrice()).isEqualByComparingTo("10.000000");
        assertThat(position.totalCost()).isEqualByComparingTo("1000.00");
    }

    @Test
    void secondBuyAtDifferentPriceRecalculatesWeightedAveragePrice() {
        // 100 a 10 (custo 1000) + 100 a 20 (custo 2000) = 200 cotas, custo 3000 -> médio 15.
        Position position = PositionCalculator.calculate(List.of(
                tx(InvestmentTransactionType.BUY, "2026-01-10", "100", "10.00", "0", "1000.00"),
                tx(InvestmentTransactionType.BUY, "2026-02-10", "100", "20.00", "0", "2000.00")));

        assertThat(position.quantity()).isEqualByComparingTo("200");
        assertThat(position.averagePrice()).isEqualByComparingTo("15.000000");
    }

    @Test
    void buyFeesAreIncludedInTheAveragePrice() {
        // 100 cotas a 10 + taxa de 50 -> custo total 1050 / 100 = 10.50 médio.
        Position position = PositionCalculator.calculate(
                List.of(tx(InvestmentTransactionType.BUY, "2026-01-10", "100", "10.00", "50", "1000.00")));

        assertThat(position.averagePrice()).isEqualByComparingTo("10.500000");
    }

    @Test
    void sellReducesQuantityWithoutChangingAveragePriceAndRealizesGain() {
        Position position = PositionCalculator.calculate(List.of(
                tx(InvestmentTransactionType.BUY, "2026-01-10", "100", "10.00", "0", "1000.00"),
                tx(InvestmentTransactionType.SELL, "2026-03-10", "40", "15.00", "0", "600.00")));

        assertThat(position.quantity()).isEqualByComparingTo("60");
        assertThat(position.averagePrice()).isEqualByComparingTo("10.000000"); // preço médio não muda na venda
        // Ganho realizado: 40 * (15 - 10) = 200.
        assertThat(position.realizedGain()).isEqualByComparingTo("200.00");
    }

    @Test
    void dividendsAccumulateIncomeWithoutAffectingPositionOrAveragePrice() {
        Position position = PositionCalculator.calculate(List.of(
                tx(InvestmentTransactionType.BUY, "2026-01-10", "100", "10.00", "0", "1000.00"),
                tx(InvestmentTransactionType.DIVIDEND, "2026-02-05", "0", "0", "0", "80.00"),
                tx(InvestmentTransactionType.JCP, "2026-03-05", "0", "0", "0", "20.00")));

        assertThat(position.quantity()).isEqualByComparingTo("100");
        assertThat(position.averagePrice()).isEqualByComparingTo("10.000000");
        assertThat(position.totalIncome()).isEqualByComparingTo("100.00");
    }
}
