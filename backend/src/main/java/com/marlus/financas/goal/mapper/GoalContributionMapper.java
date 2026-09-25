package com.marlus.financas.goal.mapper;

import com.marlus.financas.goal.domain.GoalContribution;
import com.marlus.financas.goal.web.GoalContributionResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GoalContributionMapper {

    GoalContributionResponse toResponse(GoalContribution contribution);
}
