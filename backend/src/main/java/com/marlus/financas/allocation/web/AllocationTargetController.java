package com.marlus.financas.allocation.web;

import com.marlus.financas.allocation.domain.AllocationTarget;
import com.marlus.financas.allocation.mapper.AllocationTargetMapper;
import com.marlus.financas.allocation.service.AllocationTargetService;
import com.marlus.financas.investment.domain.InvestmentPurpose;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/allocation-targets")
public class AllocationTargetController {

    private final AllocationTargetService allocationTargetService;
    private final AllocationTargetMapper allocationTargetMapper;

    public AllocationTargetController(AllocationTargetService allocationTargetService, AllocationTargetMapper allocationTargetMapper) {
        this.allocationTargetService = allocationTargetService;
        this.allocationTargetMapper = allocationTargetMapper;
    }

    @GetMapping
    public List<AllocationTargetResponse> list(@RequestParam InvestmentPurpose purpose) {
        return allocationTargetService.findAllByPurpose(purpose).stream()
                .map(allocationTargetMapper::toResponse)
                .toList();
    }

    @GetMapping("/comparison")
    public AllocationSummaryResponse comparison(@RequestParam InvestmentPurpose purpose) {
        return allocationTargetService.compare(purpose);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AllocationTargetResponse upsert(@Valid @RequestBody AllocationTargetRequest request) {
        AllocationTarget target = allocationTargetService.upsert(request);
        return allocationTargetMapper.toResponse(target);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        allocationTargetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
