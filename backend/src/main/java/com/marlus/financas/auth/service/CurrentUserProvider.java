package com.marlus.financas.auth.service;

import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resolve o ID do usuário autenticado a partir do contexto de segurança da requisição atual. */
@Component
public class CurrentUserProvider {

    public UUID currentUserId() {
        AppUserDetails principal =
                (AppUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal.getId();
    }
}
