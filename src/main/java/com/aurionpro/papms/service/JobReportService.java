package com.aurionpro.papms.service;

import com.aurionpro.papms.dto.JobReportDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobReportService {
    Page<JobReportDto> getReportsForOrganization(Integer organizationId, Pageable pageable);
    JobReportDto getReportById(Long reportId);
}