package com.aurionpro.papms.repository;

import com.aurionpro.papms.entity.JobExecutionReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobExecutionReportRepository extends JpaRepository<JobExecutionReport, Long> {

    // Find a report by the Spring Batch job execution ID
    Optional<JobExecutionReport> findByJobExecutionId(Long jobExecutionId);

    // Find all reports for a specific organization, ordered by most recent first
    Page<JobExecutionReport> findByOrganizationIdOrderByCreatedAtDesc(Integer organizationId, Pageable pageable);
}