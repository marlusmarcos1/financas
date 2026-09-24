package com.marlus.financas.settings.repository;

import com.marlus.financas.settings.domain.AppSetting;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, UUID> {

    List<AppSetting> findAllByUserId(UUID userId);

    Optional<AppSetting> findByUserIdAndKey(UUID userId, String key);
}
