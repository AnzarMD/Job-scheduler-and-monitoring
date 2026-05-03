package com.cloudflow.cloudflow.worker;

import com.cloudflow.cloudflow.execution.JobExecution;
import com.cloudflow.cloudflow.execution.JobExecutionRepository;
import com.cloudflow.cloudflow.job.Job;
import com.cloudflow.cloudflow.job.JobRepository;
import com.cloudflow.cloudflow.tenant.Tenant;
import com.cloudflow.cloudflow.websocket.JobStatusBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobWorkerService {

    private final JobRepository jobRepository;
    private final JobExecutionRepository executionRepository;
    private final RestTemplate restTemplate;
    private final JobStatusBroadcaster jobStatusBroadcaster;
    private final MeterRegistry meterRegistry;

    @Transactional
    public JobExecution executeJob(UUID jobId, int attemptNumber, String triggeredBy) {
        // Load the job
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        Tenant tenant = job.getTenant();

        // Create execution record with RUNNING status
        JobExecution execution = JobExecution.builder()
                .job(job)
                .tenant(tenant)
                .status("RUNNING")
                .attemptNumber(attemptNumber)
                .triggeredBy(triggeredBy)
                .startedAt(OffsetDateTime.now())
                .build();
        execution = executionRepository.save(execution);

        log.info("Executing job [{}] attempt [{}] → {}", jobId, attemptNumber, job.getTargetUrl());

        long startTime = System.currentTimeMillis();

        try {
            // Build the HTTP request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Add any custom headers from the job config (stored as JSON string)
            // For simplicity we just set Content-Type for now
            // Day 7 can parse the requestHeaders JSON string

            HttpEntity<String> entity = new HttpEntity<>(job.getRequestBody(), headers);
            HttpMethod method = HttpMethod.valueOf(job.getHttpMethod());

            // Make the HTTP call
            ResponseEntity<String> response = restTemplate.exchange(
                    job.getTargetUrl(),
                    method,
                    entity,
                    String.class
            );

            long duration = System.currentTimeMillis() - startTime;

            // Truncate response body to 5000 chars max
            String responseBody = response.getBody();
            if (responseBody != null && responseBody.length() > 5000) {
                responseBody = responseBody.substring(0, 5000) + "...[truncated]";
            }

            // Mark execution as SUCCESS
            execution.setStatus("SUCCESS");
            execution.setFinishedAt(OffsetDateTime.now());
            execution.setDurationMs(duration);
            execution.setHttpStatusCode(response.getStatusCode().value());
            execution.setResponseBody(responseBody);

            // Reset consecutive failure counter on success
            job.setConsecutiveFailures(0);
            job.setLastExecutedAt(OffsetDateTime.now());
            jobRepository.save(job);

            log.info("Job [{}] SUCCESS in {}ms — HTTP {}", jobId, duration,
                    response.getStatusCode().value());
            meterRegistry.counter("cloudflow.job.executions",
                    "status", "SUCCESS",
                    "tenantId", tenant.getId().toString()
            ).increment();

            // Record duration as a timer (Prometheus gets min/max/avg/percentiles)
            meterRegistry.timer("cloudflow.job.duration",
                    "status", "SUCCESS"
            ).record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);

        } catch (HttpStatusCodeException e) {
            // Got an HTTP response but with error status (4xx, 5xx)
            long duration = System.currentTimeMillis() - startTime;

            execution.setStatus("FAILED");
            execution.setFinishedAt(OffsetDateTime.now());
            execution.setDurationMs(duration);
            execution.setHttpStatusCode(e.getStatusCode().value());
            execution.setErrorMessage(e.getMessage());

            job.setConsecutiveFailures(job.getConsecutiveFailures() + 1);
            job.setLastExecutedAt(OffsetDateTime.now());
            jobRepository.save(job);

            log.warn("Job [{}] FAILED — HTTP {} after {}ms", jobId,
                    e.getStatusCode().value(), duration);
            meterRegistry.counter("cloudflow.job.executions",
                    "status", "FAILED",
                    "tenantId", tenant.getId().toString()
            ).increment();

            meterRegistry.timer("cloudflow.job.duration",
                    "status", "FAILED"
            ).record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);

        } catch (ResourceAccessException e) {
            // Connection timeout or network error
            long duration = System.currentTimeMillis() - startTime;
            boolean isTimeout = e.getMessage() != null &&
                    e.getMessage().contains("Read timed out");

            String status = isTimeout ? "TIMEOUT" : "FAILED";
            execution.setStatus(status);
            execution.setFinishedAt(OffsetDateTime.now());
            execution.setDurationMs(duration);
            execution.setErrorMessage(e.getMessage());

            job.setConsecutiveFailures(job.getConsecutiveFailures() + 1);
            job.setLastExecutedAt(OffsetDateTime.now());
            jobRepository.save(job);

            log.warn("Job [{}] {} after {}ms — {}", jobId, status, duration, e.getMessage());
            meterRegistry.counter("cloudflow.job.executions",
                    "status", "TIMEOUT",
                    "tenantId", tenant.getId().toString()
            ).increment();

            meterRegistry.timer("cloudflow.job.duration",
                    "status", "TIMEOUT"
            ).record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            // Any other unexpected error
            long duration = System.currentTimeMillis() - startTime;

            execution.setStatus("FAILED");
            execution.setFinishedAt(OffsetDateTime.now());
            execution.setDurationMs(duration);
            execution.setErrorMessage(e.getMessage());

            job.setConsecutiveFailures(job.getConsecutiveFailures() + 1);
            job.setLastExecutedAt(OffsetDateTime.now());
            jobRepository.save(job);

            log.error("Job [{}] FAILED with unexpected error: {}", jobId, e.getMessage());
        }
        jobStatusBroadcaster.broadcastExecutionUpdate(execution);
        return executionRepository.save(execution);
    }
}