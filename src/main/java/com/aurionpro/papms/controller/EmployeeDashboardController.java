package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.EmployeeDashboardDto;
import com.aurionpro.papms.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees/dashboard")
@RequiredArgsConstructor
@Tag(name = "Employee Dashboard", description = "APIs for an employee to view their personal dashboard")
public class EmployeeDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get the dashboard for the logged-in employee",
            description = "Retrieves a summary for the current employee, including their full profile and a summary of their latest payslip.")
    public ResponseEntity<EmployeeDashboardDto> getMyDashboard() {
        EmployeeDashboardDto employeeDashboard = dashboardService.getEmployeeDashboardData();
        return ResponseEntity.ok(employeeDashboard);
    }
}