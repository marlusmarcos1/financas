package com.marlus.financas.recurring.mapper;

import com.marlus.financas.recurring.domain.RecurringRule;
import com.marlus.financas.recurring.web.RecurringRuleResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RecurringRuleMapper {

    RecurringRuleResponse toResponse(RecurringRule recurringRule);
}
