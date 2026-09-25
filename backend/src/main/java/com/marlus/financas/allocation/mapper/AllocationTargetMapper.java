package com.marlus.financas.allocation.mapper;

import com.marlus.financas.allocation.domain.AllocationTarget;
import com.marlus.financas.allocation.web.AllocationTargetResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AllocationTargetMapper {

    AllocationTargetResponse toResponse(AllocationTarget target);
}
