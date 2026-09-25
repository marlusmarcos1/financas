package com.marlus.financas.investment.mapper;

import com.marlus.financas.investment.domain.InvestmentTransaction;
import com.marlus.financas.investment.web.InvestmentTransactionResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InvestmentTransactionMapper {

    InvestmentTransactionResponse toResponse(InvestmentTransaction transaction);
}
