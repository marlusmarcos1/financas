package com.marlus.financas.user;

import com.marlus.financas.common.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser extends Auditable {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    protected AppUser() {
    }

    public AppUser(UUID id, String username, String passwordHash, String displayName, LocalDate birthDate) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.birthDate = birthDate;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }
}
