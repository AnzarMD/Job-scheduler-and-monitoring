package com.cloudflow.cloudflow.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResultEvent {
    private String executionId;
    private String jobId;
    private String tenantId;
    private String status;        // SUCCESS | FAILED | TIMEOUT
    private int attemptNumber;
    private long durationMs;
    private Integer httpStatusCode;
    private String errorMessage;
    private OffsetDateTime finishedAt;
}