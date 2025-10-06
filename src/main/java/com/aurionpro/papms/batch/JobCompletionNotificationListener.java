package com.aurionpro.papms.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Slf4j
public class JobCompletionNotificationListener implements JobExecutionListener {

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("!!! EMPLOYEE IMPORT JOB FINISHED! Time to verify the results.");
        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            log.error("!!! EMPLOYEE IMPORT JOB FAILED! Check logs for details.");
        }

        // Cleanup the temporary file
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