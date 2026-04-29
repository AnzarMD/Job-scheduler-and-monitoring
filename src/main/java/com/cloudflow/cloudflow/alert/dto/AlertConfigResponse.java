package com.cloudflow.cloudflow.alert.dto;

import com.cloudflow.cloudflow.alert.AlertConfig;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class AlertConfigResponse {
    private UUID id;
    private UUID jobId;
    private UUID tenantId;
    private Integer failureThreshold;
    private String webhookUrl;
    private Boolean isEnabled;
    private OffsetDateTime lastAlertedAt;
    private OffsetDateTime createdAt;

    public static AlertConfigResponse from(AlertConfig config) {
        return AlertConfigResponse.builder()
                .id(config.getId())
                .jobId(config.getJob().getId())
                .tenantId(config.getTenant().getId())
                .failureThreshold(config.getFailureThreshold())
                .webhookUrl(config.getWebhookUrl())
                .isEnabled(config.getIsEnabled())
                .lastAlertedAt(config.getLastAlertedAt())
                .createdAt(config.getCreatedAt())
                .build();
    }
}