package com.cloudflow.cloudflow.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
public class JobExecutionTask extends QuartzJobBean {

    // Quartz calls this method when a trigger fires.
    // QuartzJobBean is a Spring helper that injects Spring beans into this class.
    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        String jobId = dataMap.getString("jobId");
        String tenantId = dataMap.getString("tenantId");
        String jobName = dataMap.getString("jobName");

        log.info("Quartz trigger fired for job [{}] tenant [{}] name [{}]",
                jobId, tenantId, jobName);

        // Day 7: this will publish to Kafka job.trigger topic instead of logging.
        // For now we just log to confirm Quartz is firing correctly.
    }
}