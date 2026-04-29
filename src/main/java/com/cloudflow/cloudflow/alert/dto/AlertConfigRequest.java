package com.cloudflow.cloudflow.alert.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class AlertConfigRequest {

    @Min(1) @Max(10)
    private Integer failureThreshold = 3;

    private String webhookUrl;

    private Boolean isEnabled = true;
}