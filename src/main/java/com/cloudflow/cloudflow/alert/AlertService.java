package com.cloudflow.cloudflow.alert;

import com.cloudflow.cloudflow.alert.dto.AlertConfigRequest;
import com.cloudflow.cloudflow.alert.dto.AlertConfigResponse;
import com.cloudflow.cloudflow.alert.dto.AlertLogResponse;
import com.cloudflow.cloudflow.job.Job;
import com.cloudflow.cloudflow.job.JobRepository;
import com.cloudflow.cloudflow.multitenancy.TenantContext;
import com.cloudflow.cloudflow.tenant.Tenant;
import com.cloudflow.cloudflow.tenant.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertConfigRepository alertConfigRepository;
    private final AlertLogRepository alertLogRepository;
    private final JobRepository jobRepository;
    private final TenantRepository tenantRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // ─── Called by RetryService after max retries exceeded ───────
    @Transactional
    public void checkAndAlert(UUID jobId, String lastErrorMessage) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) return;

        AlertConfig config = alertConfigRepository.findByJobIdWithDetails(jobId).orElse(null);

        // No alert config = alerting not configured for this job
        if (config == null || !config.getIsEnabled() || config.getWebhookUrl() == null) {
            log.info("No alert config for job [{}] — skipping alert", jobId);
            return;
        }

        // Only alert if consecutive failures >= threshold
        if (job.getConsecutiveFailures() < config.getFailureThreshold()) {
            log.info("Job [{}] has {} failures, threshold is {} — not alerting yet",
                    jobId, job.getConsecutiveFailures(), config.getFailureThreshold());
            return;
        }

        // Build the alert payload
        String payload = buildAlertPayload(job, lastErrorMessage);

        // Send the webhook
        boolean delivered = sendWebhook(config.getWebhookUrl(), payload, job);

        // Update last alerted time
        config.setLastAlertedAt(OffsetDateTime.now());
        alertConfigRepository.save(config);

        log.info("Alert {} for job [{}] to webhook [{}]",
                delivered ? "delivered" : "FAILED", jobId, config.getWebhookUrl());
    }

    private String buildAlertPayload(Job job, String lastErrorMessage) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "job_failure_alert");
            payload.put("job_id", job.getId().toString());
            payload.put("job_name", job.getName());
            payload.put("tenant_id", job.getTenant().getId().toString());
            payload.put("consecutive_failures", job.getConsecutiveFailures());
            payload.put("last_error_message", lastErrorMessage);
            payload.put("target_url", job.getTargetUrl());
            payload.put("timestamp", OffsetDateTime.now().toString());
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Failed to serialize alert payload: {}", e.getMessage());
            return "{\"event\":\"job_failure_alert\",\"job_id\":\"" + job.getId() + "\"}";
        }
    }

    private boolean sendWebhook(String webhookUrl, String payload, Job job) {
        AlertLog alertLog = AlertLog.builder()
                .job(job)
                .tenant(job.getTenant())
                .webhookUrl(webhookUrl)
                .payload(payload)
                .sentAt(OffsetDateTime.now())
                .isDelivered(false)
                .build();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    webhookUrl, request, String.class);

            boolean success = response.getStatusCode().is2xxSuccessful();
            alertLog.setHttpStatus(response.getStatusCode().value());
            alertLog.setIsDelivered(success);
            alertLogRepository.save(alertLog);
            return success;

        } catch (Exception e) {
            log.error("Failed to deliver alert to [{}]: {}", webhookUrl, e.getMessage());
            alertLog.setIsDelivered(false);
            alertLogRepository.save(alertLog);
            return false;
        }
    }

    // ─── Configure alert settings for a job ──────────────────────
    @Transactional
    public AlertConfigResponse upsertAlertConfig(UUID jobId, AlertConfigRequest request) {
        UUID tenantId = TenantContext.getTenantId();

        Job job = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));

        // Upsert — create if not exists, update if exists
        AlertConfig config = alertConfigRepository.findByJobIdWithDetails(jobId)
                .orElse(AlertConfig.builder().job(job).tenant(tenant).build());

        if (request.getFailureThreshold() != null)
            config.setFailureThreshold(request.getFailureThreshold());
        if (request.getWebhookUrl() != null)
            config.setWebhookUrl(request.getWebhookUrl());
        if (request.getIsEnabled() != null)
            config.setIsEnabled(request.getIsEnabled());

        return AlertConfigResponse.from(alertConfigRepository.save(config));
    }

    @Transactional(readOnly = true)
    public AlertConfigResponse getAlertConfig(UUID jobId) {
        UUID tenantId = TenantContext.getTenantId();
        return alertConfigRepository.findByJobIdWithDetails(jobId)
                .filter(c -> c.getTenant().getId().equals(tenantId))
                .map(AlertConfigResponse::from)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No alert config found for job: " + jobId));
    }

    @Transactional(readOnly = true)
    public Page<AlertLogResponse> getAlertLogs(Pageable pageable) {
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "sentAt")
        );
        return alertLogRepository
                .findAllByTenantIdWithDetails(TenantContext.getTenantId(), sorted)
                .map(AlertLogResponse::from);
    }
}