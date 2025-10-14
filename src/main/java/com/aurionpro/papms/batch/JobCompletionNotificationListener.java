package com.aurionpro.papms.batch;

import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.OrganizationRepository;
import com.aurionpro.papms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class JobCompletionNotificationListener implements JobExecutionListener {

    private final NotificationService notificationService;
    private final AppUserRepository appUserRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    public void afterJob(JobExecution jobExecution) {
        Long organizationIdLong = jobExecution.getJobParameters().getLong("organizationId");
        if (organizationIdLong == null) {
            log.error("Organization ID not found in job parameters. Cannot send notification.");
            cleanupFile(jobExecution);
            return;
        }
        Integer organizationId = organizationIdLong.intValue();

        Optional<Organization> organizationOpt = organizationRepository.findById(organizationId);
        List<User> admins = appUserRepository.findByOrganizationIdAndRole(organizationId, Role.ORG_ADMIN);

        if (organizationOpt.isEmpty() || admins.isEmpty()) {
            log.error("Organization or ORG_ADMIN not found for ID {}. Cannot send notification.", organizationId);
            cleanupFile(jobExecution);
            return;
        }
        String organizationName = organizationOpt.get().getCompanyName();

        String message;
        String link = "/dashboard";

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            // Corrected line: Use mapToLong and sum into a long variable
            long writeCount = jobExecution.getStepExecutions().stream()
                    .mapToLong(StepExecution::getWriteCount)
                    .sum();
            log.info("!!! EMPLOYEE IMPORT JOB FINISHED for '{}'! {} employees imported.", organizationName, writeCount);
            message = String.format("Bulk employee import for '%s' is complete. %d employees were successfully imported.",
                    organizationName, writeCount);

        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            log.error("!!! EMPLOYEE IMPORT JOB FAILED for '{}'! Check logs for details.", organizationName);
            message = String.format("Bulk employee import for '%s' failed. Please check the system logs for more details.",
                    organizationName);
        } else {
            log.warn("Employee import job for '{}' finished with status: {}.", organizationName, jobExecution.getStatus());
            message = String.format("The bulk employee import for '%s' has finished with status: %s.",
                    organizationName, jobExecution.getStatus());
        }

        for (User admin : admins) {
            notificationService.createNotification(admin, message, link);
        }

        cleanupFile(jobExecution);
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