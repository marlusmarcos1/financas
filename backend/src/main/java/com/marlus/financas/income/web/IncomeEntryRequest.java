package com.marlus.financas.income.web;

import com.marlus.financas.income.domain.IncomeEntryStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record IncomeEntryRequest(
        @NotNull(message = "Informe a fonte de receita.") UUID sourceId,
        UUID accountId,
        @NotNull(message = "Informe a competência.")
                @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "Competência deve estar no formato AAAA-MM.")
                String referenceMonth,
        LocalDate receivedOn,
        @NotNull(message = "Informe o valor.") @PositiveOrZero(message = "Não pode ser negativo.") BigDecimal amount,
        @NotNull(message = "Informe o status.") IncomeEntryStatus status) {
}
