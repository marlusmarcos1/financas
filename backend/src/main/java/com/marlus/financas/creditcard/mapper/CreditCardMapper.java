package com.marlus.financas.creditcard.mapper;

import com.marlus.financas.creditcard.domain.CreditCard;
import com.marlus.financas.creditcard.web.CreditCardResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CreditCardMapper {

    CreditCardResponse toResponse(CreditCard creditCard);
}
