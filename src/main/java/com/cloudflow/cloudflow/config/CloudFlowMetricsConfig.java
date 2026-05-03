package com.cloudflow.cloudflow.config;

import com.cloudflow.cloudflow.job.JobRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CloudFlowMetricsConfig {  // ← renamed from PrometheusConfig

    private final MeterRegistry meterRegistry;
    private final JobRepository jobRepository;

    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("cloudflow.jobs.active",
                        jobRepository,
                        repo -> repo.countByStatus("ACTIVE"))
                .description("Number of currently active (scheduled) jobs")
                .register(meterRegistry);
    }
}