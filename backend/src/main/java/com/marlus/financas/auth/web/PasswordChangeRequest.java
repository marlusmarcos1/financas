package com.marlus.financas.auth.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @NotBlank(message = "Informe a senha atual.") String currentPassword,
        @NotBlank(message = "Informe a nova senha.") @Size(min = 6, message = "A nova senha deve ter pelo menos 6 caracteres.")
                String newPassword) {
}
