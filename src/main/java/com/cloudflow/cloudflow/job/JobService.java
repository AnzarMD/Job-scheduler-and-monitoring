package com.cloudflow.cloudflow.job;

import com.cloudflow.cloudflow.job.dto.CreateJobRequest;
import com.cloudflow.cloudflow.job.dto.JobResponse;
import com.cloudflow.cloudflow.job.dto.UpdateJobRequest;
import com.cloudflow.cloudflow.multitenancy.TenantContext;
import com.cloudflow.cloudflow.tenant.Tenant;
import com.cloudflow.cloudflow.tenant.TenantRepository;
import com.cloudflow.cloudflow.user.User;
import com.cloudflow.cloudflow.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final CronValidator cronValidator;

    // Helper: gets current tenant from ThreadLocal (set by TenantFilter)
    private UUID currentTenantId() {
        return TenantContext.getTenantId();
    }

    // Helper: gets current user UUID from Spring Security context
    private UUID currentUserId() {
        return UUID.fromString(
                SecurityContextHolder.getContext().getAuthentication().getName()
        );
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> getAllJobs(Pageable pageable) {
        return jobRepository
                .findAllByTenantId(currentTenantId(), pageable)
                .map(JobResponse::from);
    }

    @Transactional(readOnly = true)
    public JobResponse getJobById(UUID jobId) {
        Job job = jobRepository.findByIdAndTenantId(jobId, currentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        return JobResponse.from(job);
    }

    @Transactional
    public JobResponse createJob(CreateJobRequest request) {
        UUID tenantId = currentTenantId();

        // Validate cron expression before saving
        cronValidator.validateOrThrow(request.getCronExpression());

        // Enforce unique job name within this tenant
        if (jobRepository.existsByNameAndTenantId(request.getName(), tenantId)) {
            throw new IllegalArgumentException("A job with this name already exists");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));

        User createdBy = userRepository.findById(currentUserId()).orElse(null);

        Job job = Job.builder()
                .tenant(tenant)
                .name(request.getName())
                .description(request.getDescription())
                .cronExpression(request.getCronExpression())
                .timezone(request.getTimezone())
                .targetUrl(request.getTargetUrl())
                .httpMethod(request.getHttpMethod())
                .requestBody(request.getRequestBody())
                .requestHeaders(request.getRequestHeaders())
                .timeoutSeconds(request.getTimeoutSeconds())
                .retryLimit(request.getRetryLimit())
                .retryDelaySeconds(request.getRetryDelaySeconds())
                .status("ACTIVE")
                .createdBy(createdBy)
                .build();

        job = jobRepository.save(job);
        return JobResponse.from(job);
    }

    @Transactional
    public JobResponse updateJob(UUID jobId, UpdateJobRequest request) {
        Job job = jobRepository.findByIdAndTenantId(jobId, currentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        // Only update fields that were actually provided (not null)
        if (request.getName() != null) {
            if (!request.getName().equals(job.getName()) &&
                    jobRepository.existsByNameAndTenantId(request.getName(), currentTenantId())) {
                throw new IllegalArgumentException("A job with this name already exists");
            }
            job.setName(request.getName());
        }
        if (request.getDescription() != null) job.setDescription(request.getDescription());
        if (request.getCronExpression() != null) {
            cronValidator.validateOrThrow(request.getCronExpression());
            job.setCronExpression(request.getCronExpression());
        }
        if (request.getTimezone() != null) job.setTimezone(request.getTimezone());
        if (request.getTargetUrl() != null) job.setTargetUrl(request.getTargetUrl());
        if (request.getHttpMethod() != null) job.setHttpMethod(request.getHttpMethod());
        if (request.getRequestBody() != null) job.setRequestBody(request.getRequestBody());
        if (request.getRequestHeaders() != null) job.setRequestHeaders(request.getRequestHeaders());
        if (request.getTimeoutSeconds() != null) job.setTimeoutSeconds(request.getTimeoutSeconds());
        if (request.getRetryLimit() != null) job.setRetryLimit(request.getRetryLimit());
        if (request.getRetryDelaySeconds() != null) job.setRetryDelaySeconds(request.getRetryDelaySeconds());

        job = jobRepository.save(job);
        return JobResponse.from(job);
    }

    @Transactional
    public void deleteJob(UUID jobId) {
        Job job = jobRepository.findByIdAndTenantId(jobId, currentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        jobRepository.delete(job);
    }

    @Transactional
    public JobResponse pauseJob(UUID jobId) {
        Job job = jobRepository.findByIdAndTenantId(jobId, currentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.setStatus("PAUSED");
        return JobResponse.from(jobRepository.save(job));
    }

    @Transactional
    public JobResponse resumeJob(UUID jobId) {
        Job job = jobRepository.findByIdAndTenantId(jobId, currentTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.setStatus("ACTIVE");
        return JobResponse.from(jobRepository.save(job));
    }
}