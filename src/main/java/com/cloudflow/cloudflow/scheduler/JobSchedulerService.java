package com.cloudflow.cloudflow.scheduler;

import com.cloudflow.cloudflow.job.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobSchedulerService {

    private final Scheduler scheduler;

    // ─── Schedule a new job in Quartz ────────────────────────────
    public void scheduleJob(Job job) {
        try {
            JobDetail jobDetail = buildJobDetail(job);
            Trigger trigger = buildTrigger(job, jobDetail);

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled job [{}] with cron [{}]", job.getId(), job.getCronExpression());

        } catch (SchedulerException e) {
            log.error("Failed to schedule job [{}]: {}", job.getId(), e.getMessage());
            throw new RuntimeException("Failed to schedule job: " + e.getMessage(), e);
        }
    }

    // ─── Update an existing job's schedule ───────────────────────
    public void rescheduleJob(Job job) {
        try {
            TriggerKey triggerKey = triggerKey(job.getId());

            // Only reschedule if the trigger exists in Quartz
            if (scheduler.checkExists(triggerKey)) {
                Trigger newTrigger = buildTrigger(job, buildJobDetail(job));
                scheduler.rescheduleJob(triggerKey, newTrigger);
                log.info("Rescheduled job [{}] with new cron [{}]",
                        job.getId(), job.getCronExpression());
            } else {
                // Trigger doesn't exist yet — create it fresh
                scheduleJob(job);
            }
        } catch (SchedulerException e) {
            log.error("Failed to reschedule job [{}]: {}", job.getId(), e.getMessage());
            throw new RuntimeException("Failed to reschedule job: " + e.getMessage(), e);
        }
    }

    // ─── Pause a job (stops it from firing) ──────────────────────
    public void pauseJob(UUID jobId) {
        try {
            scheduler.pauseJob(jobKey(jobId));
            log.info("Paused job [{}] in Quartz", jobId);
        } catch (SchedulerException e) {
            log.error("Failed to pause job [{}]: {}", jobId, e.getMessage());
        }
    }

    // ─── Resume a paused job ──────────────────────────────────────
    public void resumeJob(UUID jobId) {
        try {
            scheduler.resumeJob(jobKey(jobId));
            log.info("Resumed job [{}] in Quartz", jobId);
        } catch (SchedulerException e) {
            log.error("Failed to resume job [{}]: {}", jobId, e.getMessage());
        }
    }

    // ─── Delete a job from Quartz entirely ───────────────────────
    public void deleteJob(UUID jobId) {
        try {
            boolean deleted = scheduler.deleteJob(jobKey(jobId));
            if (deleted) {
                log.info("Deleted job [{}] from Quartz", jobId);
            } else {
                log.warn("Job [{}] not found in Quartz for deletion", jobId);
            }
        } catch (SchedulerException e) {
            log.error("Failed to delete job [{}]: {}", jobId, e.getMessage());
        }
    }

    // ─── Manually trigger a job immediately ──────────────────────
    public void triggerJobNow(UUID jobId) {
        try {
            scheduler.triggerJob(jobKey(jobId));
            log.info("Manually triggered job [{}]", jobId);
        } catch (SchedulerException e) {
            log.error("Failed to manually trigger job [{}]: {}", jobId, e.getMessage());
            throw new RuntimeException("Failed to trigger job: " + e.getMessage(), e);
        }
    }

    // ─── Private helpers ─────────────────────────────────────────

    private JobDetail buildJobDetail(Job job) {
        // JobDataMap stores the data Quartz passes to executeInternal()
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("jobId", job.getId().toString());
        dataMap.put("tenantId", job.getTenant().getId().toString());
        dataMap.put("jobName", job.getName());

        return JobBuilder.newJob(JobExecutionTask.class)
                .withIdentity(jobKey(job.getId()))       // unique key in Quartz
                .withDescription(job.getName())
                .usingJobData(dataMap)
                .storeDurably()                           // keep job even if no trigger attached
                .build();
    }

    private Trigger buildTrigger(Job job, JobDetail jobDetail) {
        // Build a cron schedule from the job's cron expression and timezone
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder
                .cronSchedule(job.getCronExpression())
                .inTimeZone(java.util.TimeZone.getTimeZone(job.getTimezone()))
                .withMisfireHandlingInstructionDoNothing(); // if server was down, skip missed fires

        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(triggerKey(job.getId()))
                .withDescription("Trigger for: " + job.getName())
                .withSchedule(scheduleBuilder)
                .build();
    }

    // Use the CloudFlow job UUID as the Quartz job key (in group "cloudflow")
    private JobKey jobKey(UUID jobId) {
        return JobKey.jobKey(jobId.toString(), "cloudflow");
    }

    private TriggerKey triggerKey(UUID jobId) {
        return TriggerKey.triggerKey(jobId.toString(), "cloudflow");
    }
}