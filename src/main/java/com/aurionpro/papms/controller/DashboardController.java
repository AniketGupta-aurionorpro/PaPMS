package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.DashboardStatsDto;
import com.aurionpro.papms.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations/{organizationId}/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "APIs for fetching aggregated dashboard statistics")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Get dashboard statistics for an organization",
            description = "Retrieves a summary of key metrics for the organization's dashboard, such as balance, employee count, and recent financial activity.")
    public ResponseEntity<DashboardStatsDto> getDashboardStats(@PathVariable Integer organizationId) {
        DashboardStatsDto stats = dashboardService.getDashboardStatsForOrganization(organizationId);
        return ResponseEntity.ok(stats);
    }
}