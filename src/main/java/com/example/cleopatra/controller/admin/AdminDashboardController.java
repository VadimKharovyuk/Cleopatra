package com.example.cleopatra.controller.admin;

import com.example.cleopatra.dto.user.AdminAnalyticsData;
import com.example.cleopatra.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin")
public class AdminDashboardController {

   private final AdminAnalyticsService analyticsService ;


    @GetMapping
    public String adminDashboard(Model model) {
        AdminAnalyticsData analytics = analyticsService.getAnalyticsData();
        model.addAttribute("analytics", analytics);
        return "admin/dashboard";
    }

    @GetMapping("/analytics")
    public String analytics(Model model) {
        AdminAnalyticsData analytics = analyticsService.getAnalyticsData();
        model.addAttribute("analytics", analytics);
        return "admin/analytics";
    }

    @GetMapping("/analytics/api")
    @ResponseBody
    public ResponseEntity<AdminAnalyticsData> getAnalyticsApi() {
        AdminAnalyticsData analytics = analyticsService.getAnalyticsData();
        return ResponseEntity.ok(analytics);
    }
}
