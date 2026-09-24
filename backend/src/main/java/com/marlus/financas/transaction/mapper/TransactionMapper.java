package com.marlus.financas.transaction.mapper;

import com.marlus.financas.transaction.domain.Transaction;
import com.marlus.financas.transaction.web.TransactionResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    TransactionResponse toResponse(Transaction transaction);
}
