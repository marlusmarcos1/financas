package com.marlus.financas.auth.web;

import java.util.UUID;

public record UserResponse(UUID id, String username, String displayName) {
}
