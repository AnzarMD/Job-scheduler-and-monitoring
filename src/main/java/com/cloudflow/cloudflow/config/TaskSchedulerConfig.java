package com.cloudflow.cloudflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class TaskSchedulerConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // Pool of 5 threads for retry scheduling
        // Each retry is a lightweight task (just schedules a future call)
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("retry-scheduler-");
        scheduler.initialize();
        return scheduler;
    }
}