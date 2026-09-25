package com.marlus.financas.simulation.web;

import com.marlus.financas.simulation.service.SimulationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/simulations")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping("/installment-purchase")
    public SimulationResponse simulate(@Valid @RequestBody SimulationRequest request) {
        return simulationService.simulate(request);
    }
}
