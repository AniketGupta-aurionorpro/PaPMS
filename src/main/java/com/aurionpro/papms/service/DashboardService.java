package com.aurionpro.papms.service;

import com.aurionpro.papms.dto.DashboardStatsDto;
import com.aurionpro.papms.dto.EmployeeDashboardDto;
import com.aurionpro.papms.dto.FinancialSummaryDto;

public interface DashboardService {
    DashboardStatsDto getDashboardStatsForOrganization(Integer organizationId);
    EmployeeDashboardDto getEmployeeDashboardData();
    FinancialSummaryDto getFinancialSummaryForBankAdmin(Integer organizationId);
}