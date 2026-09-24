package com.marlus.financas.income.mapper;

import com.marlus.financas.income.domain.IncomeSource;
import com.marlus.financas.income.web.IncomeSourceResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IncomeSourceMapper {

    IncomeSourceResponse toResponse(IncomeSource incomeSource);
}
