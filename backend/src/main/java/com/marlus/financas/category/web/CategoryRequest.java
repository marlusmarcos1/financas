package com.marlus.financas.category.web;

import com.marlus.financas.category.domain.CategoryKind;
import com.marlus.financas.category.domain.CategoryNature;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CategoryRequest(
        @NotBlank(message = "Informe o nome da categoria.") String name,
        @NotNull(message = "Informe se é receita ou despesa.") CategoryKind kind,
        @NotNull(message = "Informe se é fixa ou variável.") CategoryNature nature,
        UUID parentId,
        String icon,
        String color) {
}
