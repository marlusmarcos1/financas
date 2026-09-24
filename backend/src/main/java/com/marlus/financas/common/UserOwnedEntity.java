package com.marlus.financas.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;

@MappedSuperclass
public abstract class UserOwnedEntity extends Auditable {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    protected UserOwnedEntity() {
    }

    protected UserOwnedEntity(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
