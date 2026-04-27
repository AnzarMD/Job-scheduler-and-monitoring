package com.cloudflow.cloudflow.execution;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class JobExecutionResponse {

    private UUID id;
    private UUID jobId;
    private UUID tenantId;
    private String status;
    private Integer attemptNumber;
    private String triggeredBy;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private Long durationMs;
    private Integer httpStatusCode;
    private String responseBody;
    private String errorMessage;
    private OffsetDateTime createdAt;

    public static JobExecutionResponse from(JobExecution execution) {
        return JobExecutionResponse.builder()
                .id(execution.getId())
                .jobId(execution.getJob().getId())
                .tenantId(execution.getTenant().getId())
                .status(execution.getStatus())
                .attemptNumber(execution.getAttemptNumber())
                .triggeredBy(execution.getTriggeredBy())
                .startedAt(execution.getStartedAt())
                .finishedAt(execution.getFinishedAt())
                .durationMs(execution.getDurationMs())
                .httpStatusCode(execution.getHttpStatusCode())
                .responseBody(execution.getResponseBody())
                .errorMessage(execution.getErrorMessage())
                .createdAt(execution.getCreatedAt())
                .build();
    }
}