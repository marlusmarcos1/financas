package com.marlus.financas.tithe.mapper;

import com.marlus.financas.tithe.domain.TitheLedger;
import com.marlus.financas.tithe.web.TitheLedgerResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TitheLedgerMapper {

    TitheLedgerResponse toResponse(TitheLedger titheLedger);
}
