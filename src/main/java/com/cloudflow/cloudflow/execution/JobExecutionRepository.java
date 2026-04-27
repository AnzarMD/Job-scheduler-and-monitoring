package com.cloudflow.cloudflow.execution;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JobExecutionRepository extends JpaRepository<JobExecution, UUID> {

    @Query("""
        SELECT e FROM JobExecution e
        JOIN FETCH e.job
        JOIN FETCH e.tenant
        WHERE e.job.id = :jobId
    """)
    Page<JobExecution> findAllByJobIdWithDetails(
            @Param("jobId") UUID jobId, Pageable pageable);

    @Query("""
        SELECT e FROM JobExecution e
        JOIN FETCH e.job
        JOIN FETCH e.tenant
        WHERE e.tenant.id = :tenantId
    """)
    Page<JobExecution> findAllByTenantIdWithDetails(
            @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("""
        SELECT e FROM JobExecution e
        JOIN FETCH e.job
        JOIN FETCH e.tenant
        WHERE e.id = :id AND e.tenant.id = :tenantId
    """)
    Optional<JobExecution> findByIdAndTenantIdWithDetails(
            @Param("id") UUID id,
            @Param("tenantId") UUID tenantId);
}