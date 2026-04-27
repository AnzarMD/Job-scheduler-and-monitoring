package com.cloudflow.cloudflow.execution;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Executions", description = "Job execution history")
@SecurityRequirement(name = "bearerAuth")
public class JobExecutionController {

    private final JobExecutionService executionService;

    // Get execution history for a specific job
    @GetMapping("/api/v1/jobs/{jobId}/executions")
    @Operation(summary = "Get execution history for a job")
    public ResponseEntity<Page<JobExecutionResponse>> getJobExecutions(
            @PathVariable UUID jobId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(executionService.getExecutionsForJob(jobId, pageable));
    }

    // Get all executions across all jobs for the tenant
    @GetMapping("/api/v1/executions")
    @Operation(summary = "Get all executions for the tenant")
    public ResponseEntity<Page<JobExecutionResponse>> getAllExecutions(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(executionService.getAllExecutionsForTenant(pageable));
    }

    // Get a single execution by ID
    @GetMapping("/api/v1/executions/{id}")
    @Operation(summary = "Get a single execution log")
    public ResponseEntity<JobExecutionResponse> getExecution(@PathVariable UUID id) {
        return ResponseEntity.ok(executionService.getExecutionById(id));
    }
}