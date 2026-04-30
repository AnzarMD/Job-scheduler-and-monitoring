package com.cloudflow.cloudflow.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${cloudflow.kafka.topics.job-trigger}")
    private String jobTriggerTopic;

    @Value("${cloudflow.kafka.topics.job-result}")
    private String jobResultTopic;

    @Value("${cloudflow.kafka.topics.job-alert}")
    private String jobAlertTopic;

    // Spring creates these topics automatically if they don't exist
    // This is cleaner than relying on KAFKA_AUTO_CREATE_TOPICS_ENABLE

    @Bean
    public NewTopic jobTriggerTopic() {
        return TopicBuilder.name(jobTriggerTopic)
                .partitions(3)    // 3 partitions = 3 consumers can process in parallel
                .replicas(1)      // 1 replica (dev) — use 3 in production
                .build();
    }

    @Bean
    public NewTopic jobResultTopic() {
        return TopicBuilder.name(jobResultTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic jobAlertTopic() {
        return TopicBuilder.name(jobAlertTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}