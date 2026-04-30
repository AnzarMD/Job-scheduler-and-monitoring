package com.cloudflow.cloudflow.kafka;

import com.cloudflow.cloudflow.kafka.events.JobAlertEvent;
import com.cloudflow.cloudflow.kafka.events.JobResultEvent;
import com.cloudflow.cloudflow.kafka.events.JobTriggerEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cloudflow.kafka.topics.job-trigger}")
    private String jobTriggerTopic;

    @Value("${cloudflow.kafka.topics.job-result}")
    private String jobResultTopic;

    @Value("${cloudflow.kafka.topics.job-alert}")
    private String jobAlertTopic;

    public void publishJobTrigger(JobTriggerEvent event) {
        publish(jobTriggerTopic, event.getJobId(), event);
    }

    public void publishJobResult(JobResultEvent event) {
        publish(jobResultTopic, event.getJobId(), event);
    }

    public void publishJobAlert(JobAlertEvent event) {
        publish(jobAlertTopic, event.getJobId(), event);
    }

    private void publish(String topic, String key, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            // key = jobId — ensures all events for the same job go to the same partition
            // This preserves ordering: trigger → result → alert for each job
            kafkaTemplate.send(topic, key, json);
            log.debug("Published to [{}] key [{}]", topic, key);
        } catch (Exception e) {
            log.error("Failed to publish to topic [{}]: {}", topic, e.getMessage());
            throw new RuntimeException("Kafka publish failed", e);
        }
    }
}