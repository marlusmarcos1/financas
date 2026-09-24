package com.marlus.financas.settings.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SettingsRequest(
        @NotNull(message = "Informe o percentual do dízimo.") BigDecimal tithePercent,
        @NotNull(message = "Informe o limite de comprometimento com parcelas.") BigDecimal installmentLimitPercent,
        @NotNull(message = "Informe a meta de meses de reserva.") @Min(value = 1, message = "A meta deve ser de pelo menos 1 mês.")
                Integer emergencyMonthsTarget,
        @NotBlank(message = "Informe a moeda.") String currency) {
}
