package com.marlus.financas.networth.web;

import com.marlus.financas.networth.service.EmergencyReserveService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/emergency-reserve")
public class EmergencyReserveController {

    private final EmergencyReserveService emergencyReserveService;

    public EmergencyReserveController(EmergencyReserveService emergencyReserveService) {
        this.emergencyReserveService = emergencyReserveService;
    }

    @GetMapping
    public EmergencyReserveResponse get() {
        return emergencyReserveService.calculate();
    }
}
