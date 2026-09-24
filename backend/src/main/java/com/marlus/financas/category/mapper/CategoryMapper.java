package com.marlus.financas.category.mapper;

import com.marlus.financas.category.domain.Category;
import com.marlus.financas.category.web.CategoryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);
}
