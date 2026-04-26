package com.cloudflow.cloudflow.config;

import org.springframework.context.annotation.Configuration;

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
}