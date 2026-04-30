package com.cloudflow.cloudflow.kafka;

import com.cloudflow.cloudflow.alert.AlertService;
import com.cloudflow.cloudflow.kafka.events.JobAlertEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobAlertConsumer {

    private final AlertService alertService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${cloudflow.kafka.topics.job-alert}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String message,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            JobAlertEvent event = objectMapper.readValue(message, JobAlertEvent.class);
            log.info("Consuming job.alert [offset={}] jobId={}", offset, event.getJobId());

            alertService.checkAndAlert(
                    UUID.fromString(event.getJobId()),
                    event.getLastErrorMessage()
            );

        } catch (Exception e) {
            log.error("Error consuming job.alert message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process job alert", e);
        }
    }
}