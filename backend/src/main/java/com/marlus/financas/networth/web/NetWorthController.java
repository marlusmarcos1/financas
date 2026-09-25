package com.marlus.financas.networth.web;

import com.marlus.financas.networth.service.NetWorthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/net-worth")
public class NetWorthController {

    private final NetWorthService netWorthService;

    public NetWorthController(NetWorthService netWorthService) {
        this.netWorthService = netWorthService;
    }

    @GetMapping
    public NetWorthResponse get() {
        return netWorthService.summarize();
    }
}
