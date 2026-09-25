package com.marlus.financas.seed;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seed-demo")
public class SeedDemoController {

    private final SeedDemoService seedDemoService;

    public SeedDemoController(SeedDemoService seedDemoService) {
        this.seedDemoService = seedDemoService;
    }

    @PostMapping
    public SeedDemoResponse seed() {
        return seedDemoService.seed();
    }
}
