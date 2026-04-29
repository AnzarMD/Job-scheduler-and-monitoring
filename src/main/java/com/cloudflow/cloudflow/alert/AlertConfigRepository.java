package com.cloudflow.cloudflow.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AlertConfigRepository extends JpaRepository<AlertConfig, UUID> {

    @Query("SELECT a FROM AlertConfig a JOIN FETCH a.job JOIN FETCH a.tenant WHERE a.job.id = :jobId")
    Optional<AlertConfig> findByJobIdWithDetails(@Param("jobId") UUID jobId);
}