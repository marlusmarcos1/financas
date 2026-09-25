package com.marlus.financas.goal.web;

import com.marlus.financas.goal.service.GoalService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    public List<GoalProgressResponse> list() {
        return goalService.findAllWithProgress();
    }

    @GetMapping("/{id}")
    public GoalProgressResponse get(@PathVariable UUID id) {
        return goalService.findProgressById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalProgressResponse create(@Valid @RequestBody GoalRequest request) {
        return goalService.findProgressById(goalService.create(request).getId());
    }

    @PutMapping("/{id}")
    public GoalProgressResponse update(@PathVariable UUID id, @Valid @RequestBody GoalRequest request) {
        goalService.update(id, request);
        return goalService.findProgressById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        goalService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
