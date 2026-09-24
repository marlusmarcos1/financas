package com.marlus.financas.auth.service;

import com.marlus.financas.user.AppUser;
import com.marlus.financas.user.AppUserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PasswordChangeService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;

    public PasswordChangeService(
            AppUserRepository appUserRepository, PasswordEncoder passwordEncoder, CurrentUserProvider currentUserProvider) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserProvider = currentUserProvider;
    }

    public void changePassword(String currentPassword, String newPassword) {
        AppUser user = appUserRepository.findById(currentUserProvider.currentUserId()).orElseThrow();
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Senha atual incorreta.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }
}
