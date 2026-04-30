package com.cloudflow.cloudflow.kafka;

import com.cloudflow.cloudflow.kafka.events.JobTriggerEvent;
import com.cloudflow.cloudflow.execution.JobExecution;
import com.cloudflow.cloudflow.worker.JobWorkerService;
import com.cloudflow.cloudflow.worker.RetryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobTriggerConsumer {

    private final JobWorkerService jobWorkerService;
    private final RetryService retryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${cloudflow.kafka.topics.job-trigger}",
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "3"   // 3 consumer threads = matches 3 partitions
    )
    public void consume(String message,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            JobTriggerEvent event = objectMapper.readValue(message, JobTriggerEvent.class);
            log.info("Consuming job.trigger [topic={} partition={} offset={}] jobId={}",
                    topic, partition, offset, event.getJobId());

            UUID jobId = UUID.fromString(event.getJobId());
            JobExecution execution = jobWorkerService.executeJob(
                    jobId,
                    event.getAttemptNumber(),
                    event.getTriggeredBy()
            );

            retryService.handleExecutionResult(
                    jobId,
                    execution.getStatus(),
                    execution.getAttemptNumber()
            );

        } catch (Exception e) {
            log.error("Error consuming job.trigger message: {}", e.getMessage(), e);
            // Rethrowing causes Kafka to route to DLT (Dead Letter Topic)
            throw new RuntimeException("Failed to process job trigger", e);
        }
    }
}