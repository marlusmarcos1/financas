package com.marlus.financas.category.mapper;

import com.marlus.financas.category.domain.Budget;
import com.marlus.financas.category.web.BudgetResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BudgetMapper {

    BudgetResponse toResponse(Budget budget);
}
