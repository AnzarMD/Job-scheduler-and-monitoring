package com.cloudflow.cloudflow.alert.dto;

import com.cloudflow.cloudflow.alert.AlertLog;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class AlertLogResponse {
    private UUID id;
    private UUID jobId;
    private UUID tenantId;
    private String webhookUrl;
    private String payload;
    private Integer httpStatus;
    private OffsetDateTime sentAt;
    private Boolean isDelivered;

    public static AlertLogResponse from(AlertLog log) {
        return AlertLogResponse.builder()
                .id(log.getId())
                .jobId(log.getJob().getId())
                .tenantId(log.getTenant().getId())
                .webhookUrl(log.getWebhookUrl())
                .payload(log.getPayload())
                .httpStatus(log.getHttpStatus())
                .sentAt(log.getSentAt())
                .isDelivered(log.getIsDelivered())
                .build();
    }
}