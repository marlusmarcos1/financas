package com.marlus.financas.income.mapper;

import com.marlus.financas.income.domain.IncomeEntry;
import com.marlus.financas.income.web.IncomeEntryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IncomeEntryMapper {

    IncomeEntryResponse toResponse(IncomeEntry incomeEntry);
}
