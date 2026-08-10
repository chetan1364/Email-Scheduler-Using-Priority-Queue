package com.emailscheduler.util;

import com.emailscheduler.model.Email;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class RetryHandler {

    /**
     * Determines if an email is eligible for retry.
     */
    public static boolean canRetry(Email email) {
        if (email == null) return false;
        return email.getRetryCount() < email.getMaxRetries();
    }

    /**
     * Calculates the next execution time for the email based on the retry count.
     * Uses a simple backoff multiplier: 30 seconds * (retryCount + 1).
     * Always calculated in UTC to match the scheduler's comparison clock.
     */
    public static LocalDateTime calculateNextRetryTime(Email email) {
        if (email == null) return LocalDateTime.now(ZoneOffset.UTC);
        int attempts = email.getRetryCount(); // Current attempt count (before increment)
        int delaySeconds = 30 * (attempts + 1);
        return LocalDateTime.now(ZoneOffset.UTC).plusSeconds(delaySeconds);
    }
}
