package com.cloudflow.cloudflow.job;

import org.quartz.CronExpression;
import org.springframework.stereotype.Component;

@Component
public class CronValidator {

    public boolean isValid(String cronExpression) {
        return CronExpression.isValidExpression(cronExpression);
    }

    public void validateOrThrow(String cronExpression) {
        if (!isValid(cronExpression)) {
            throw new IllegalArgumentException(
                    "Invalid cron expression: '" + cronExpression + "'. " +
                            "Expected format: 'seconds minutes hours day month weekday' (6 fields) " +
                            "or 'minutes hours day month weekday' (5 fields). " +
                            "Example: '0 0 23 * * ?' fires every night at 11 PM."
            );
        }
    }
}