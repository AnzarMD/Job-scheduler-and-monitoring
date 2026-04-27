package com.cloudflow.cloudflow.scheduler;

import com.cloudflow.cloudflow.execution.JobExecution;
import com.cloudflow.cloudflow.worker.JobWorkerService;
import com.cloudflow.cloudflow.worker.RetryService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.UUID;

@Slf4j
public class JobExecutionTask extends QuartzJobBean {

    // @Autowired works here because we use QuartzJobBean
    // which hooks into Spring's bean factory
    @Autowired
    private JobWorkerService jobWorkerService;

    @Autowired
    private RetryService retryService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        String jobIdStr = dataMap.getString("jobId");
        String jobName = dataMap.getString("jobName");

        log.info("Quartz trigger fired for job [{}] name [{}]", jobIdStr, jobName);

        try {
            UUID jobId = UUID.fromString(jobIdStr);

            JobExecution execution = jobWorkerService.executeJob(jobId, 1, "SCHEDULER");

            // Pass primitive values instead of the lazy-loaded entity
            retryService.handleExecutionResult(
                    jobId,
                    execution.getStatus(),
                    execution.getAttemptNumber()
            );

        } catch (Exception e) {
            log.error("Unexpected error in JobExecutionTask for job [{}]: {}",
                    jobIdStr, e.getMessage(), e);
        }
    }
}