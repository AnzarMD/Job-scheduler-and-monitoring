package com.cloudflow.cloudflow.worker;

import com.cloudflow.cloudflow.kafka.KafkaProducerService;
import com.cloudflow.cloudflow.kafka.events.JobAlertEvent;
import com.cloudflow.cloudflow.kafka.events.JobTriggerEvent;
import com.cloudflow.cloudflow.job.Job;
import com.cloudflow.cloudflow.job.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetryService {

    private final JobRepository jobRepository;
    private final TaskScheduler taskScheduler;
    private final KafkaProducerService kafkaProducerService;

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

            // Schedule a delayed publish to job.trigger topic
            // TaskScheduler handles the delay, then Kafka handles the execution
            taskScheduler.schedule(
                    () -> {
                        JobTriggerEvent retryEvent = JobTriggerEvent.builder()
                                .jobId(jobId.toString())
                                .tenantId(job.getTenant().getId().toString())
                                .attemptNumber(nextAttempt)
                                .triggeredBy("SCHEDULER")
                                .triggeredAt(OffsetDateTime.now())
                                .build();
                        kafkaProducerService.publishJobTrigger(retryEvent);
                    },
                    Instant.now().plusMillis(delayMs)
            );

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