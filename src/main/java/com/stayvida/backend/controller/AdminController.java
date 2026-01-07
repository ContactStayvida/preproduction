package com.stayvida.backend.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stayvida.backend.service.AdminDashboardService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminDashboardService adminDashboardService;

    @GetMapping("/test")
    public String testAdminAccess() {
        return "✅ Supabase JWT authentication successful!";
    }

    public AdminController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/monthly-revenue")
    public Map<String, BigDecimal> getMonthlyRevenue() {
        return adminDashboardService.getCurrentMonthRevenue();
    }
}
