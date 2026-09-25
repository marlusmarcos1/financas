package com.marlus.financas.dashboard.web;

import com.marlus.financas.dashboard.service.AlertsService;
import com.marlus.financas.dashboard.service.DashboardExtrasService;
import com.marlus.financas.dashboard.service.DashboardService;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AlertsService alertsService;
    private final DashboardExtrasService dashboardExtrasService;

    public DashboardController(
            DashboardService dashboardService, AlertsService alertsService, DashboardExtrasService dashboardExtrasService) {
        this.dashboardService = dashboardService;
        this.alertsService = alertsService;
        this.dashboardExtrasService = dashboardExtrasService;
    }

    @GetMapping
    public DashboardResponse get(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return dashboardService.summarize(month != null ? month : YearMonth.now());
    }

    @GetMapping("/alerts")
    public List<AlertResponse> alerts() {
        return alertsService.list();
    }

    @GetMapping("/extras")
    public DashboardExtrasResponse extras(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return dashboardExtrasService.get(month != null ? month : YearMonth.now());
    }
}
