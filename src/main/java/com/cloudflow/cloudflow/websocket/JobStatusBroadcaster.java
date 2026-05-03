package com.cloudflow.cloudflow.websocket;

import com.cloudflow.cloudflow.execution.JobExecution;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobStatusBroadcaster {

    // SimpMessagingTemplate is the main Spring class for sending WebSocket messages.
    // "Simp" stands for Simple Messaging Protocol — Spring's abstraction layer
    // over STOMP that lets us send messages without worrying about connections.
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Broadcasts a job execution result to all browser clients subscribed
     * to /topic/executions/{tenantId}.
     *
     * WHY per-tenant topics:
     * If we used one global topic, every tenant would receive every other
     * tenant's job updates — a data leak. By namespacing by tenantId,
     * each tenant's browser only subscribes to their own topic and sees
     * only their own data.
     */
    public void broadcastExecutionUpdate(JobExecution execution) {
        try {
            // Build the payload — only include what the frontend needs
            Map<String, Object> payload = new HashMap<>();
            payload.put("executionId", execution.getId().toString());
            payload.put("jobId", execution.getJob().getId().toString());
            payload.put("tenantId", execution.getTenant().getId().toString());
            payload.put("status", execution.getStatus());
            payload.put("attemptNumber", execution.getAttemptNumber());
            payload.put("durationMs", execution.getDurationMs());
            payload.put("httpStatusCode", execution.getHttpStatusCode());
            payload.put("errorMessage", execution.getErrorMessage());
            payload.put("finishedAt", execution.getFinishedAt() != null
                    ? execution.getFinishedAt().toString() : null);
            payload.put("broadcastedAt", OffsetDateTime.now().toString());

            String tenantId = execution.getTenant().getId().toString();
            String destination = "/topic/executions/" + tenantId;
            String json = objectMapper.writeValueAsString(payload);

            // convertAndSend routes the message through the in-memory broker
            // to all clients subscribed to this destination
            messagingTemplate.convertAndSend(destination, json);

            log.debug("Broadcasted execution update [{}] to {}", execution.getId(), destination);

        } catch (Exception e) {
            // WebSocket failures should NEVER crash the main execution flow.
            // A broadcast failure is non-critical — the job ran, the result
            // is saved to DB. The frontend just won't get the live update.
            log.warn("Failed to broadcast execution update: {}", e.getMessage());
        }
    }
}