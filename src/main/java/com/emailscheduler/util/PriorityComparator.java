package com.emailscheduler.util;

import com.emailscheduler.model.Email;
import com.emailscheduler.model.EmailPriority;

import java.util.Comparator;

public class PriorityComparator implements Comparator<Email> {

    @Override
    public int compare(Email e1, Email e2) {
        if (e1 == null && e2 == null) return 0;
        if (e1 == null) return 1;
        if (e2 == null) return -1;

        // 1. Compare Priority level (HIGH > MEDIUM > LOW)
        int p1 = getPriorityWeight(e1.getPriority());
        int p2 = getPriorityWeight(e2.getPriority());
        if (p1 != p2) {
            return Integer.compare(p2, p1); // Descending order (higher weight first)
        }

        // 2. Compare Scheduled Time (earlier scheduled time first)
        if (e1.getScheduledTime() != null && e2.getScheduledTime() != null) {
            int timeCompare = e1.getScheduledTime().compareTo(e2.getScheduledTime());
            if (timeCompare != 0) {
                return timeCompare; // Ascending order (earlier time first)
            }
        }

        // 3. Compare Creation Timestamp (older first, i.e., earlier createdAt first)
        if (e1.getCreatedAt() != null && e2.getCreatedAt() != null) {
            return e1.getCreatedAt().compareTo(e2.getCreatedAt()); // Ascending order (older first)
        }

        // Final tie-breaker: compare database ID if present
        if (e1.getId() != null && e2.getId() != null) {
            return e1.getId().compareTo(e2.getId());
        }

        return 0;
    }

    private int getPriorityWeight(EmailPriority priority) {
        if (priority == null) return 0;
        switch (priority) {
            case HIGH:
                return 3;
            case MEDIUM:
                return 2;
            case LOW:
                return 1;
            default:
                return 0;
        }
    }
}
