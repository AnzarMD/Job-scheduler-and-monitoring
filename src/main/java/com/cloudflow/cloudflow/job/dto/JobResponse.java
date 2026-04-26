package com.cloudflow.cloudflow.job.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class JobResponse {
    private UUID id;
    private UUID tenantId;
    private String name;
    private String description;
    private String cronExpression;
    private String timezone;
    private String targetUrl;
    private String httpMethod;
    private String requestBody;
    private String requestHeaders;
    private Integer timeoutSeconds;
    private Integer retryLimit;
    private Integer retryDelaySeconds;
    private String status;
    private Integer consecutiveFailures;
    private OffsetDateTime lastExecutedAt;
    private OffsetDateTime nextExecutionAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Convenience method to build a JobResponse from a Job entity
    public static JobResponse from(com.cloudflow.cloudflow.job.Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .tenantId(job.getTenant().getId())
                .name(job.getName())
                .description(job.getDescription())
                .cronExpression(job.getCronExpression())
                .timezone(job.getTimezone())
                .targetUrl(job.getTargetUrl())
                .httpMethod(job.getHttpMethod())
                .requestBody(job.getRequestBody())
                .requestHeaders(job.getRequestHeaders())
                .timeoutSeconds(job.getTimeoutSeconds())
                .retryLimit(job.getRetryLimit())
                .retryDelaySeconds(job.getRetryDelaySeconds())
                .status(job.getStatus())
                .consecutiveFailures(job.getConsecutiveFailures())
                .lastExecutedAt(job.getLastExecutedAt())
                .nextExecutionAt(job.getNextExecutionAt())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}