package com.marlus.financas.dataio.handler;

import com.marlus.financas.dataio.csv.CsvFieldParser;
import com.marlus.financas.dataio.csv.RowValidationException;
import com.marlus.financas.dataio.service.EntityCsvHandler;
import com.marlus.financas.dataio.service.RowResult;
import com.marlus.financas.settings.domain.AppSetting;
import com.marlus.financas.settings.repository.AppSettingRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AppSettingCsvHandler implements EntityCsvHandler {

    private final AppSettingRepository appSettingRepository;

    public AppSettingCsvHandler(AppSettingRepository appSettingRepository) {
        this.appSettingRepository = appSettingRepository;
    }

    @Override
    public String fileName() {
        return "settings.csv";
    }

    @Override
    public List<String> header() {
        return List.of("id", "key", "value");
    }

    @Override
    public List<List<String>> exportRows(UUID userId) {
        return appSettingRepository.findAllByUserId(userId).stream()
                .map(s -> List.of(s.getId().toString(), s.getKey(), s.getValue()))
                .toList();
    }

    @Override
    public RowResult importRow(UUID userId, Map<String, String> row, int line, boolean apply) {
        try {
            UUID id = CsvFieldParser.requiredUuid(row, "id");
            String key = CsvFieldParser.requiredText(row, "key");
            String value = CsvFieldParser.requiredText(row, "value");

            var existing = appSettingRepository.findByIdAndUserId(id, userId);
            if (existing.isPresent()) {
                AppSetting setting = existing.get();
                if (!Objects.equals(setting.getKey(), key)) {
                    return RowResult.error(line, "a chave de uma configuração existente não pode ser alterada por importação");
                }
                if (apply) {
                    setting.setValue(value);
                    appSettingRepository.save(setting);
                }
                return RowResult.updated(line);
            }

            if (appSettingRepository.existsById(id)) {
                return RowResult.error(line, "id já pertence a outro registro");
            }
            if (apply) {
                appSettingRepository.save(new AppSetting(id, userId, key, value));
            }
            return RowResult.created(line);
        } catch (RowValidationException ex) {
            return RowResult.error(line, ex.getMessage());
        }
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        appSettingRepository.deleteAllByUserId(userId);
    }
}
