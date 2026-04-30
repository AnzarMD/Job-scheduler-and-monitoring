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
public class JobTriggerEvent {
    private String jobId;
    private String tenantId;
    private int attemptNumber;
    private String triggeredBy;   // SCHEDULER | USER
    private OffsetDateTime triggeredAt;
}