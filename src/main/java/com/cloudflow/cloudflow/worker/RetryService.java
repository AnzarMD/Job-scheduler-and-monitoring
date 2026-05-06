package com.cloudflow.cloudflow.worker;

import com.cloudflow.cloudflow.kafka.KafkaProducerService;
import com.cloudflow.cloudflow.kafka.events.JobAlertEvent;
import com.cloudflow.cloudflow.kafka.events.JobTriggerEvent;
import com.cloudflow.cloudflow.job.Job;
import com.cloudflow.cloudflow.job.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
//@RequiredArgsConstructor
public class RetryService {

    private final JobRepository jobRepository;
    private final KafkaProducerService kafkaProducerService;
    private final TaskScheduler taskScheduler;

    // Constructor injection with @Qualifier
    public RetryService(JobRepository jobRepository,
                        KafkaProducerService kafkaProducerService,
                        @Qualifier("retryTaskScheduler") TaskScheduler taskScheduler) {
        this.jobRepository = jobRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.taskScheduler = taskScheduler;
    }

    public void handleExecutionResult(UUID jobId, String status, int attemptNumber) {
        if ("SUCCESS".equals(status)) return;

        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        int retryLimit = job.getRetryLimit();

        if (attemptNumber < retryLimit) {
            int nextAttempt = attemptNumber + 1;
            long delayMs = job.getRetryDelaySeconds() * 1000L;

            log.info("Job [{}] failed on attempt {}/{}. Retrying in {}s via Kafka",
                    jobId, attemptNumber, retryLimit, job.getRetryDelaySeconds());

            // Capture values for lambda — lambda cannot capture non-final fields
            String tenantId = job.getTenant().getId().toString();
            Instant fireAt = Instant.now().plusMillis(delayMs);

            taskScheduler.schedule(() -> {
                JobTriggerEvent retryEvent = JobTriggerEvent.builder()
                        .jobId(jobId.toString())
                        .tenantId(tenantId)
                        .attemptNumber(nextAttempt)
                        .triggeredBy("SCHEDULER")
                        .triggeredAt(OffsetDateTime.now())
                        .build();
                kafkaProducerService.publishJobTrigger(retryEvent);
                log.info("Retry attempt {} published to Kafka for job [{}]", nextAttempt, jobId);
            }, fireAt);

        } else {
            log.warn("Job [{}] exhausted all {} retries. Publishing to job.alert topic.",
                    jobId, retryLimit);

            JobAlertEvent alertEvent = JobAlertEvent.builder()
                    .jobId(jobId.toString())
                    .tenantId(job.getTenant().getId().toString())
                    .failureCount(job.getConsecutiveFailures())
                    .lastErrorMessage("Max retries (" + retryLimit + ") exceeded")
                    .timestamp(OffsetDateTime.now())
                    .build();

            kafkaProducerService.publishJobAlert(alertEvent);
        }
    }
}