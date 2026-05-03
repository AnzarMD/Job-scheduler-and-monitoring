package com.cloudflow.cloudflow.job;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    // Find all jobs for a specific tenant (paginated)
    Page<Job> findAllByTenantId(UUID tenantId, Pageable pageable);

    // Find a single job — MUST match both id and tenantId for security
    // This prevents Tenant A from accessing Tenant B's jobs by guessing a UUID
    Optional<Job> findByIdAndTenantId(UUID id, UUID tenantId);

    // Check name uniqueness within a tenant
    boolean existsByNameAndTenantId(String name, UUID tenantId);

    // Count active jobs for a tenant (used for plan limit enforcement)
    long countByTenantIdAndStatus(UUID tenantId, String status);


    long countByStatus(String status);
}