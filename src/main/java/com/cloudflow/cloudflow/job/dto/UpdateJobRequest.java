package com.cloudflow.cloudflow.job.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateJobRequest {

    @Size(min = 1, max = 200)
    private String name;

    private String description;

    private String cronExpression;

    private String timezone;

    @Pattern(regexp = "^https?://.*", message = "Target URL must start with http:// or https://")
    private String targetUrl;

    @Pattern(regexp = "^(GET|POST|PUT|PATCH|DELETE)$")
    private String httpMethod;

    private String requestBody;
    private String requestHeaders;

    @Min(1) @Max(300)
    private Integer timeoutSeconds;

    @Min(0) @Max(10)
    private Integer retryLimit;

    @Min(0) @Max(3600)
    private Integer retryDelaySeconds;
}