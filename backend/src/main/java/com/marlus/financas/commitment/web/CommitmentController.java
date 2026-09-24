package com.marlus.financas.commitment.web;

import com.marlus.financas.commitment.service.CommitmentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/commitments")
public class CommitmentController {

    private final CommitmentService commitmentService;

    public CommitmentController(CommitmentService commitmentService) {
        this.commitmentService = commitmentService;
    }

    @GetMapping
    public List<MonthCommitmentResponse> list(@RequestParam(defaultValue = "12") int months) {
        int clamped = Math.max(1, Math.min(months, 24));
        return commitmentService.summarize(clamped);
    }
}
