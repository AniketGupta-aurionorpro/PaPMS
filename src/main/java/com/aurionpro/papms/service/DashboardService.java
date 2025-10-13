package com.aurionpro.papms.service;

import com.aurionpro.papms.dto.DashboardStatsDto;
import com.aurionpro.papms.dto.EmployeeDashboardDto;

public interface DashboardService {
    DashboardStatsDto getDashboardStatsForOrganization(Integer organizationId);
    EmployeeDashboardDto getEmployeeDashboardData();
}