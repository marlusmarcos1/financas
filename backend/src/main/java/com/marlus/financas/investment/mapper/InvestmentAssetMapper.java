package com.marlus.financas.investment.mapper;

import com.marlus.financas.investment.domain.InvestmentAsset;
import com.marlus.financas.investment.web.InvestmentAssetResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InvestmentAssetMapper {

    InvestmentAssetResponse toResponse(InvestmentAsset asset);
}
