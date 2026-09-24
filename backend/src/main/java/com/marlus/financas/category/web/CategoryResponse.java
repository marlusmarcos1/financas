package com.marlus.financas.category.web;

import com.marlus.financas.category.domain.CategoryKind;
import com.marlus.financas.category.domain.CategoryNature;
import java.util.UUID;

public record CategoryResponse(
        UUID id, String name, CategoryKind kind, CategoryNature nature, UUID parentId, String icon, String color) {
}
