package com.marlus.financas.settings.service;

import com.marlus.financas.auth.service.CurrentUserProvider;
import com.marlus.financas.common.UuidV7Generator;
import com.marlus.financas.settings.domain.AppSetting;
import com.marlus.financas.settings.domain.SettingKey;
import com.marlus.financas.settings.repository.AppSettingRepository;
import com.marlus.financas.settings.web.SettingsRequest;
import com.marlus.financas.settings.web.SettingsResponse;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SettingsService {

    private final AppSettingRepository appSettingRepository;
    private final CurrentUserProvider currentUserProvider;

    public SettingsService(AppSettingRepository appSettingRepository, CurrentUserProvider currentUserProvider) {
        this.appSettingRepository = appSettingRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public SettingsResponse getSettings() {
        Map<String, String> values = valuesByKey(currentUserProvider.currentUserId());
        return new SettingsResponse(
                new BigDecimal(values.get(SettingKey.TITHE_PERCENT.key())),
                new BigDecimal(values.get(SettingKey.INSTALLMENT_LIMIT_PERCENT.key())),
                Integer.parseInt(values.get(SettingKey.EMERGENCY_MONTHS_TARGET.key())),
                values.get(SettingKey.CURRENCY.key()));
    }

    public SettingsResponse updateSettings(SettingsRequest request) {
        UUID userId = currentUserProvider.currentUserId();
        upsert(userId, SettingKey.TITHE_PERCENT.key(), request.tithePercent().toPlainString());
        upsert(userId, SettingKey.INSTALLMENT_LIMIT_PERCENT.key(), request.installmentLimitPercent().toPlainString());
        upsert(userId, SettingKey.EMERGENCY_MONTHS_TARGET.key(), String.valueOf(request.emergencyMonthsTarget()));
        upsert(userId, SettingKey.CURRENCY.key(), request.currency());
        return getSettings();
    }

    private Map<String, String> valuesByKey(UUID userId) {
        Map<String, String> stored = appSettingRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(AppSetting::getKey, AppSetting::getValue));
        return java.util.Arrays.stream(SettingKey.values())
                .collect(Collectors.toMap(SettingKey::key, key -> stored.getOrDefault(key.key(), key.defaultValue())));
    }

    private void upsert(UUID userId, String key, String value) {
        AppSetting setting = appSettingRepository
                .findByUserIdAndKey(userId, key)
                .orElseGet(() -> appSettingRepository.save(new AppSetting(UuidV7Generator.generate(), userId, key, value)));
        setting.setValue(value);
    }
}
