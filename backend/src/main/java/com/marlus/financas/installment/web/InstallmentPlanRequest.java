package com.marlus.financas.installment.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

public record InstallmentPlanRequest(
        @NotNull(message = "Informe o cartão.") UUID cardId,
        @NotBlank(message = "Informe a descrição.") String description,
        @NotNull(message = "Informe a data da compra.") LocalDate purchaseDate,
        @NotNull(message = "Informe o valor total.") @Positive(message = "O valor total deve ser maior que zero.")
                BigDecimal totalAmount,
        @NotNull(message = "Informe o número de parcelas.") @Min(value = 1, message = "Pelo menos 1 parcela.")
                Short installmentCount,
        @Min(value = 1, message = "A primeira parcela informada deve ser pelo menos 1.") Short firstInstallmentNumber,
        YearMonth firstInvoiceMonth,
        BigDecimal interestRateMonthly,
        UUID categoryId) {
}
