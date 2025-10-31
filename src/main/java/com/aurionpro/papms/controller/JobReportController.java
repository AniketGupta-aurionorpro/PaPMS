package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.JobReportDto;
import com.aurionpro.papms.service.JobReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Job Execution Reports", description = "APIs for retrieving bulk upload job reports")
public class JobReportController {

    private final JobReportService jobReportService;

    @GetMapping("/organizations/{organizationId}/job-reports")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Get a paginated list of all job reports for an organization")
    public ResponseEntity<Page<JobReportDto>> getJobReports(
            @PathVariable Integer organizationId,
            @ParameterObject Pageable pageable) {
        Page<JobReportDto> reports = jobReportService.getReportsForOrganization(organizationId, pageable);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/job-reports/{reportId}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Get the detailed report for a specific job execution")
    public ResponseEntity<JobReportDto> getJobReportDetails(@PathVariable Long reportId) {
        JobReportDto report = jobReportService.getReportById(reportId);
        return ResponseEntity.ok(report);
    }
}