package com.cloudflow.cloudflow.alert;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AlertLogRepository extends JpaRepository<AlertLog, UUID> {

    @Query("SELECT a FROM AlertLog a JOIN FETCH a.job JOIN FETCH a.tenant WHERE a.tenant.id = :tenantId ORDER BY a.sentAt DESC")
    Page<AlertLog> findAllByTenantIdWithDetails(@Param("tenantId") UUID tenantId, Pageable pageable);
}