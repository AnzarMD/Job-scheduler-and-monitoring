package com.cloudflow.cloudflow.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

@Configuration
public class QuartzConfig {
    // Spring Boot auto-configures Quartz from application.yml.
    // The quartz section in application.yml already sets:
    //   job-store-type: jdbc        → store triggers in PostgreSQL
    //   initialize-schema: always   → create qrtz_* tables on startup
    //   isClustered: true           → DB locking prevents duplicate fires
    //   instanceId: AUTO            → unique ID per app instance
    //
    // No additional Java config needed — Spring Boot handles it all.
    // This class exists as a placeholder for any future Quartz beans.
    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public SpringBeanJobFactory springBeanJobFactory() {
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }

    // Inner class that makes the autowiring explicit
    static class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory {
        // SpringBeanJobFactory already does everything we need.
        // This subclass exists just to make the intent clear.
    }

}