package com.example.aiprojectmanager.dashboard.controller;

import com.example.aiprojectmanager.dashboard.dto.DashboardOverviewDTO;
import com.example.aiprojectmanager.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public DashboardOverviewDTO getOverview() {
        return dashboardService.getDashboardOverview();
    }
}
