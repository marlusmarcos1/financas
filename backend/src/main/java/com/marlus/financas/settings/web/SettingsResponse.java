package com.marlus.financas.settings.web;

import java.math.BigDecimal;

public record SettingsResponse(
        BigDecimal tithePercent, BigDecimal installmentLimitPercent, int emergencyMonthsTarget, String currency) {
}
