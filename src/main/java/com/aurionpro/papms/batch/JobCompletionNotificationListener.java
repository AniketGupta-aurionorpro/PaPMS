package com.aurionpro.papms.batch;

import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.dto.FailedEmployeeRecord; // NEW IMPORT
import com.aurionpro.papms.entity.JobExecutionReport; // NEW IMPORT
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.JobExecutionReportRepository; // NEW IMPORT
import com.aurionpro.papms.repository.OrganizationRepository;
import com.aurionpro.papms.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper; // NEW IMPORT
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList; // NEW IMPORT
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class JobCompletionNotificationListener implements JobExecutionListener {

    private final NotificationService notificationService;
    private final AppUserRepository appUserRepository;
    private final OrganizationRepository organizationRepository;
    private final JobExecutionReportRepository jobExecutionReportRepository; // NEW REPO
    private final ObjectMapper objectMapper; // NEW OBJECT MAPPER for JSON conversion

    @Override
    public void afterJob(JobExecution jobExecution) {
        Long organizationIdLong = jobExecution.getJobParameters().getLong("organizationId");
        if (organizationIdLong == null) {
            log.error("Organization ID not found in job parameters. Cannot process job completion.");
            cleanupFile(jobExecution);
            return;
        }
        Integer organizationId = organizationIdLong.intValue();

        Optional<Organization> organizationOpt = organizationRepository.findById(organizationId);
        if (organizationOpt.isEmpty()) {
            log.error("Organization not found for ID {}. Cannot send notification or save report.", organizationId);
            cleanupFile(jobExecution);
            return;
        }
        Organization organization = organizationOpt.get();

        // --- GATHER STATS FROM ALL STEPS ---
        long writeCount = 0;
        long readCount = 0;
        long processSkipCount = 0;
        List<FailedEmployeeRecord> allFailedRecords = new ArrayList<>();

        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            writeCount += stepExecution.getWriteCount();
            readCount += stepExecution.getReadCount();
            processSkipCount += stepExecution.getProcessSkipCount();

            // Retrieve the list of failed records from the step's execution context
            List<FailedEmployeeRecord> stepFailedRecords = (List<FailedEmployeeRecord>) stepExecution.getExecutionContext().get("failedRecords");
            if (stepFailedRecords != null) {
                allFailedRecords.addAll(stepFailedRecords);
            }
        }

        // --- SERIALIZE FAILED RECORDS TO JSON ---
        String reportDetailsJson = "[]";
        try {
            reportDetailsJson = objectMapper.writeValueAsString(allFailedRecords);
        } catch (Exception e) {
            log.error("Failed to serialize failed records to JSON for jobExecutionId {}", jobExecution.getId(), e);
            reportDetailsJson = "[{\"error\":\"Failed to serialize report details.\"}]";
        }

        // --- SAVE THE JOB REPORT TO THE DATABASE ---
        JobExecutionReport report = JobExecutionReport.builder()
                .jobExecutionId(jobExecution.getId())
                .organization(organization)
                .jobName(jobExecution.getJobInstance().getJobName())
                .status(jobExecution.getStatus().toString())
                .totalRecordsRead((int) readCount)
                .successfulImports((int) writeCount)
                .failedImports((int) processSkipCount)
                .reportDetails(reportDetailsJson)
                .build();
        jobExecutionReportRepository.save(report);
        log.info("Saved job execution report with ID {} for jobExecutionId {}", report.getId(), jobExecution.getId());

        // --- CREATE AND SEND NOTIFICATION ---
        sendCompletionNotification(jobExecution, organization, report);

        cleanupFile(jobExecution);
    }

    private void sendCompletionNotification(JobExecution jobExecution, Organization organization, JobExecutionReport report) {
        List<User> admins = appUserRepository.findByOrganizationIdAndRole(organization.getId(), Role.ORG_ADMIN);
        if (admins.isEmpty()) {
            log.warn("No ORG_ADMIN found for organization ID {}. Skipping notification.", organization.getId());
            return;
        }

        String message;
        // The link now points to a future frontend page to view this specific report
        String link = "/org-admin/employees/bulk-upload-report/" + report.getId();

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            message = String.format("Bulk import finished. Success: %d, Failed: %d. Click to view the detailed report.",
                    report.getSuccessfulImports(), report.getFailedImports());
        } else {
            message = String.format("Bulk import failed. Total records read: %d. Click to view the error report.",
                    report.getTotalRecordsRead());
        }

        for (User admin : admins) {
            notificationService.createNotification(admin, message, link);
        }
        log.info("Sent job completion notification to {} admins.", admins.size());
    }

    private void cleanupFile(JobExecution jobExecution) {
        String filePath = jobExecution.getJobParameters().getString("filePath");
        if (filePath != null) {
            try {
                File file = new File(filePath);
                if (file.delete()) {
                    log.info("Successfully deleted temporary file: {}", filePath);
                } else {
                    log.warn("Could not delete temporary file: {}", filePath);
                }
            } catch (Exception e) {
                log.error("Error deleting temporary file: {}", filePath, e);
            }
        }
    }
}