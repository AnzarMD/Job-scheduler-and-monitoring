package com.cloudflow.cloudflow.job.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateJobRequest {

    @NotBlank(message = "Job name is required")
    @Size(min = 1, max = 200)
    private String name;

    private String description;

    @NotBlank(message = "Cron expression is required")
    private String cronExpression;

    @NotBlank(message = "Timezone is required")
    private String timezone = "UTC";

    @NotBlank(message = "Target URL is required")
    @Pattern(regexp = "^https?://.*", message = "Target URL must start with http:// or https://")
    private String targetUrl;

    @Pattern(regexp = "^(GET|POST|PUT|PATCH|DELETE)$", message = "HTTP method must be GET, POST, PUT, PATCH, or DELETE")
    private String httpMethod = "POST";

    private String requestBody;

    private String requestHeaders;

    @Min(value = 1) @Max(value = 300)
    private Integer timeoutSeconds = 30;

    @Min(value = 0) @Max(value = 10)
    private Integer retryLimit = 3;

    @Min(value = 0) @Max(value = 3600)
    private Integer retryDelaySeconds = 60;
}