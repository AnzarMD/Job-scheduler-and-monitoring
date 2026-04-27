package com.cloudflow.cloudflow.execution;

import com.cloudflow.cloudflow.multitenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobExecutionService {

    private final JobExecutionRepository executionRepository;

    // Always sort by startedAt DESC regardless of what Pageable says
    // This prevents Spring Data from appending an invalid sort to the JPQL query
    private Pageable sortedPageable(Pageable pageable) {
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "startedAt")
        );
    }

    @Transactional(readOnly = true)
    public Page<JobExecutionResponse> getExecutionsForJob(UUID jobId, Pageable pageable) {
        return executionRepository
                .findAllByJobIdWithDetails(jobId, sortedPageable(pageable))
                .map(JobExecutionResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<JobExecutionResponse> getAllExecutionsForTenant(Pageable pageable) {
        return executionRepository
                .findAllByTenantIdWithDetails(TenantContext.getTenantId(), sortedPageable(pageable))
                .map(JobExecutionResponse::from);
    }

    @Transactional(readOnly = true)
    public JobExecutionResponse getExecutionById(UUID executionId) {
        return executionRepository
                .findByIdAndTenantIdWithDetails(executionId, TenantContext.getTenantId())
                .map(JobExecutionResponse::from)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Execution not found: " + executionId));
    }
}