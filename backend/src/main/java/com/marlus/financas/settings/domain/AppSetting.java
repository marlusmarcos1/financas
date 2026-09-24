package com.marlus.financas.settings.domain;

import com.marlus.financas.common.UserOwnedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "app_setting")
public class AppSetting extends UserOwnedEntity {

    @Id
    private UUID id;

    @Column(name = "key", nullable = false, length = 60)
    private String key;

    @Column(name = "value", nullable = false, length = 255)
    private String value;

    protected AppSetting() {
    }

    public AppSetting(UUID id, UUID userId, String key, String value) {
        super(userId);
        this.id = id;
        this.key = key;
        this.value = value;
    }

    public UUID getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
