package com.cloudflow.cloudflow.job;

import com.cloudflow.cloudflow.tenant.Tenant;
import com.cloudflow.cloudflow.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(name = "cron_expression", nullable = false)
    private String cronExpression;

    @Column(nullable = false)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "target_url", nullable = false)
    private String targetUrl;

    @Column(name = "http_method", nullable = false)
    @Builder.Default
    private String httpMethod = "POST";

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "request_headers", columnDefinition = "TEXT")
    private String requestHeaders;

    @Column(name = "timeout_seconds", nullable = false)
    @Builder.Default
    private Integer timeoutSeconds = 30;

    @Column(name = "retry_limit", nullable = false)
    @Builder.Default
    private Integer retryLimit = 3;

    @Column(name = "retry_delay_seconds", nullable = false)
    @Builder.Default
    private Integer retryDelaySeconds = 60;

    @Column(nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "consecutive_failures", nullable = false)
    @Builder.Default
    private Integer consecutiveFailures = 0;

    @Column(name = "last_executed_at")
    private OffsetDateTime lastExecutedAt;

    @Column(name = "next_execution_at")
    private OffsetDateTime nextExecutionAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}