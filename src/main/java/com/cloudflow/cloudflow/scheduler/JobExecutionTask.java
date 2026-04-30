package com.cloudflow.cloudflow.scheduler;

import com.cloudflow.cloudflow.kafka.KafkaProducerService;
import com.cloudflow.cloudflow.kafka.events.JobTriggerEvent;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.OffsetDateTime;

@Slf4j
@SuppressWarnings("SpringJavaAutowiredMembersInspection")
public class JobExecutionTask extends QuartzJobBean {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        String jobId = dataMap.getString("jobId");
        String tenantId = dataMap.getString("tenantId");
        String jobName = dataMap.getString("jobName");

        log.info("Quartz fired — publishing to job.trigger for job [{}] name [{}]",
                jobId, jobName);

        try {
            JobTriggerEvent event = JobTriggerEvent.builder()
                    .jobId(jobId)
                    .tenantId(tenantId)
                    .attemptNumber(1)
                    .triggeredBy("SCHEDULER")
                    .triggeredAt(OffsetDateTime.now())
                    .build();

            // Publish takes microseconds — Quartz thread is immediately free
            kafkaProducerService.publishJobTrigger(event);

        } catch (Exception e) {
            log.error("Failed to publish job.trigger for job [{}]: {}", jobId, e.getMessage());
        }
    }
}