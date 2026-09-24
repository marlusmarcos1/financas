package com.marlus.financas.auth.service;

import com.marlus.financas.user.AppUser;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AppUserDetails implements UserDetails {

    private final UUID id;
    private final String username;
    private final String passwordHash;
    private final String displayName;

    public AppUserDetails(AppUser user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.displayName = user.getDisplayName();
    }

    public UUID getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
