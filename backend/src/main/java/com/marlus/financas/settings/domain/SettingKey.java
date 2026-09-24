package com.marlus.financas.settings.domain;

/** Chaves conhecidas de {@code app_setting}, com seus valores padrão. */
public enum SettingKey {
    TITHE_PERCENT("tithe_percent", "10"),
    INSTALLMENT_LIMIT_PERCENT("installment_limit_percent", "30"),
    EMERGENCY_MONTHS_TARGET("emergency_months_target", "6"),
    CURRENCY("currency", "BRL");

    private final String key;
    private final String defaultValue;

    SettingKey(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String key() {
        return key;
    }

    public String defaultValue() {
        return defaultValue;
    }
}
