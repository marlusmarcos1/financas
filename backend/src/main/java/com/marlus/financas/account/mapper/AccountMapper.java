package com.marlus.financas.account.mapper;

import com.marlus.financas.account.domain.Account;
import com.marlus.financas.account.web.AccountResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    AccountResponse toResponse(Account account);
}
