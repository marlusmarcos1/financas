package com.marlus.financas.auth.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Bloqueia login por username após N tentativas falhas, guardado em memória (Caffeine).
 * Suficiente para um app local de um único usuário — não precisa sobreviver a reinícios.
 */
@Service
public class LoginAttemptService {

    private final int maxAttempts;
    private final Cache<String, Integer> attemptsByUsername;

    public LoginAttemptService(
            @Value("${app.security.login-max-attempts}") int maxAttempts,
            @Value("${app.security.login-lockout-minutes}") long lockoutMinutes) {
        this.maxAttempts = maxAttempts;
        this.attemptsByUsername =
                Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(lockoutMinutes)).build();
    }

    public void loginFailed(String username) {
        int attempts = attemptsByUsername.get(username, key -> 0);
        attemptsByUsername.put(username, attempts + 1);
    }

    public void loginSucceeded(String username) {
        attemptsByUsername.invalidate(username);
    }

    public boolean isBlocked(String username) {
        Integer attempts = attemptsByUsername.getIfPresent(username);
        return attempts != null && attempts >= maxAttempts;
    }
}
