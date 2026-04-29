package com.cloudflow.cloudflow.worker;

import com.cloudflow.cloudflow.alert.AlertService;
import com.cloudflow.cloudflow.execution.JobExecution;
import com.cloudflow.cloudflow.job.Job;
import com.cloudflow.cloudflow.job.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetryService {

    private final JobWorkerService jobWorkerService;
    private final JobRepository jobRepository;
    private final TaskScheduler taskScheduler;
    private final AlertService alertService; // NEW

    public void handleExecutionResult(UUID jobId, String status, int attemptNumber) {
        if ("SUCCESS".equals(status)) return;

        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        int retryLimit = job.getRetryLimit();

        if (attemptNumber < retryLimit) {
            int nextAttempt = attemptNumber + 1;
            long delayMs = job.getRetryDelaySeconds() * 1000L;

            log.info("Job [{}] failed on attempt {}/{}. Retrying in {}s",
                    jobId, attemptNumber, retryLimit, job.getRetryDelaySeconds());

            taskScheduler.schedule(
                    () -> {
                        JobExecution retryExecution = jobWorkerService.executeJob(
                                jobId, nextAttempt, "SCHEDULER");
                        handleExecutionResult(
                                jobId,
                                retryExecution.getStatus(),
                                retryExecution.getAttemptNumber()
                        );
                    },
                    Instant.now().plusMillis(delayMs)
            );

        } else {
            // Max retries exhausted — get last error and trigger alert
            log.warn("Job [{}] exhausted all {} retries. Triggering alert check.", jobId, retryLimit);

            // Get last error message from the job's last execution
            // Pass it to AlertService for inclusion in the webhook payload
            String lastError = "Max retries (" + retryLimit + ") exceeded";
            alertService.checkAndAlert(jobId, lastError);
        }
    }
}