package com.marlus.financas.auth.service;

import com.marlus.financas.common.UuidV7Generator;
import com.marlus.financas.user.AppUser;
import com.marlus.financas.user.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Cria o usuário administrador inicial a partir do .env, apenas se ainda não existir nenhum usuário. */
@Component
public class AdminUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public AdminUserInitializer(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username}") String adminUsername,
            @Value("${app.admin.password}") String adminPassword) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (appUserRepository.count() > 0) {
            return;
        }
        AppUser admin = new AppUser(
                UuidV7Generator.generate(),
                adminUsername,
                passwordEncoder.encode(adminPassword),
                adminUsername,
                null);
        appUserRepository.save(admin);
        log.info("Usuário administrador inicial '{}' criado.", adminUsername);
    }
}
