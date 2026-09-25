package com.marlus.financas.goal.web;

import com.marlus.financas.goal.domain.GoalContribution;
import com.marlus.financas.goal.mapper.GoalContributionMapper;
import com.marlus.financas.goal.service.GoalContributionService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/goals/{goalId}/contributions")
public class GoalContributionController {

    private final GoalContributionService goalContributionService;
    private final GoalContributionMapper goalContributionMapper;

    public GoalContributionController(GoalContributionService goalContributionService, GoalContributionMapper goalContributionMapper) {
        this.goalContributionService = goalContributionService;
        this.goalContributionMapper = goalContributionMapper;
    }

    @GetMapping
    public List<GoalContributionResponse> list(@PathVariable UUID goalId) {
        return goalContributionService.findAllByGoal(goalId).stream().map(goalContributionMapper::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalContributionResponse create(@PathVariable UUID goalId, @Valid @RequestBody GoalContributionRequest request) {
        GoalContribution contribution = goalContributionService.create(goalId, request);
        return goalContributionMapper.toResponse(contribution);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID goalId, @PathVariable UUID id) {
        goalContributionService.delete(goalId, id);
        return ResponseEntity.noContent().build();
    }
}
