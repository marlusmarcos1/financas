package com.marlus.financas.networth.web;

import com.marlus.financas.networth.service.NetWorthHistoryService;
import com.marlus.financas.networth.service.NetWorthService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/net-worth")
public class NetWorthController {

    private final NetWorthService netWorthService;
    private final NetWorthHistoryService netWorthHistoryService;

    public NetWorthController(NetWorthService netWorthService, NetWorthHistoryService netWorthHistoryService) {
        this.netWorthService = netWorthService;
        this.netWorthHistoryService = netWorthHistoryService;
    }

    @GetMapping
    public NetWorthResponse get() {
        return netWorthService.summarize();
    }

    @GetMapping("/history")
    public List<NetWorthPointResponse> history(@RequestParam(defaultValue = "6") int months) {
        int clamped = Math.max(1, Math.min(months, 24));
        return netWorthHistoryService.history(clamped);
    }
}
