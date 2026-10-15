package com.universalshield.api;

import com.universalshield.dto.ApiResponse;
import com.universalshield.dto.DashboardSummary;
import com.universalshield.security.AppUserPrincipal;
import com.universalshield.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/api/dashboard/summary")
    @Operation(summary = "Statistics computed from my own scans")
    public ApiResponse<DashboardSummary> summary(@AuthenticationPrincipal AppUserPrincipal p) {
        return ApiResponse.ok(dashboard.summary(p.getId()));
    }
}
