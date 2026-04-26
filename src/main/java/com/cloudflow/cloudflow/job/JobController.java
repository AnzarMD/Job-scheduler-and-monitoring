package com.cloudflow.cloudflow.job;

import com.cloudflow.cloudflow.job.dto.CreateJobRequest;
import com.cloudflow.cloudflow.job.dto.JobResponse;
import com.cloudflow.cloudflow.job.dto.UpdateJobRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Job scheduling CRUD operations")
@SecurityRequirement(name = "bearerAuth")
public class JobController {

    private final JobService jobService;

    @GetMapping
    @Operation(summary = "List all jobs for the authenticated tenant")
    public ResponseEntity<Page<JobResponse>> getAllJobs(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(jobService.getAllJobs(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single job by ID")
    public ResponseEntity<JobResponse> getJob(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new scheduled job")
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.createJob(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing job")
    public ResponseEntity<JobResponse> updateJob(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateJobRequest request) {
        return ResponseEntity.ok(jobService.updateJob(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a job")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID id) {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause a running job")
    public ResponseEntity<JobResponse> pauseJob(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.pauseJob(id));
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Resume a paused job")
    public ResponseEntity<JobResponse> resumeJob(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.resumeJob(id));
    }
}