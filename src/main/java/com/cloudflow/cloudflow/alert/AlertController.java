package com.cloudflow.cloudflow.alert;

import com.cloudflow.cloudflow.alert.dto.AlertConfigRequest;
import com.cloudflow.cloudflow.alert.dto.AlertConfigResponse;
import com.cloudflow.cloudflow.alert.dto.AlertLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Alert configuration and logs")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertService alertService;

    @PutMapping("/api/v1/jobs/{jobId}/alert-config")
    @Operation(summary = "Configure alert settings for a job")
    public ResponseEntity<AlertConfigResponse> upsertAlertConfig(
            @PathVariable UUID jobId,
            @Valid @RequestBody AlertConfigRequest request) {
        return ResponseEntity.ok(alertService.upsertAlertConfig(jobId, request));
    }

    @GetMapping("/api/v1/jobs/{jobId}/alert-config")
    @Operation(summary = "Get alert config for a job")
    public ResponseEntity<AlertConfigResponse> getAlertConfig(@PathVariable UUID jobId) {
        return ResponseEntity.ok(alertService.getAlertConfig(jobId));
    }

    @GetMapping("/api/v1/alerts")
    @Operation(summary = "Get all alert logs for the tenant")
    public ResponseEntity<Page<AlertLogResponse>> getAlertLogs(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(alertService.getAlertLogs(pageable));
    }
}