package com.aurionpro.papms.service;

import com.aurionpro.papms.dto.JobReportDto;
import com.aurionpro.papms.entity.JobExecutionReport;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.JobExecutionReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobReportServiceImpl implements JobReportService {

    private final JobExecutionReportRepository reportRepository;
    private final AppUserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<JobReportDto> getReportsForOrganization(Integer organizationId, Pageable pageable) {
        validateOrgAccess(organizationId);
        Page<JobExecutionReport> reportPage = reportRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId, pageable);
        return reportPage.map(this::toSimpleDto); // Map to a DTO without the detailed failure list
    }

    @Override
    @Transactional(readOnly = true)
    public JobReportDto getReportById(Long reportId) {
        JobExecutionReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Job report not found with ID: " + reportId));
        validateOrgAccess(report.getOrganization().getId());
        return toDetailedDto(report); // Map to the full DTO with the failure list
    }

    // Helper to convert to a simple DTO for list view
    private JobReportDto toSimpleDto(JobExecutionReport report) {
        return JobReportDto.builder()
                .id(report.getId())
                .jobExecutionId(report.getJobExecutionId())
                .organizationName(report.getOrganization().getCompanyName())
                .jobName(report.getJobName())
                .status(report.getStatus())
                .totalRecordsRead(report.getTotalRecordsRead())
                .successfulImports(report.getSuccessfulImports())
                .failedImports(report.getFailedImports())
                .createdAt(report.getCreatedAt())
                .build();
    }

    // Helper to convert to a detailed DTO for single view
    private JobReportDto toDetailedDto(JobExecutionReport report) {
        JobReportDto dto = toSimpleDto(report);
        dto.setFailedRecords(JobReportDto.deserializeReportDetails(report.getReportDetails()));
        return dto;
    }

    private void validateOrgAccess(Integer organizationId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new SecurityException("Authenticated user not found"));

        if (!currentUser.getOrganizationId().equals(organizationId)) {
            throw new SecurityException("Access denied to this organization's resources.");
        }
    }
}