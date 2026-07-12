package com.emailscheduler.util;

import com.emailscheduler.model.Email;
import com.emailscheduler.model.EmailPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PriorityComparatorTest {

    private PriorityComparator comparator;

    @BeforeEach
    public void setUp() {
        comparator = new PriorityComparator();
    }

    @Test
    public void testCompareDifferentPriorities() {
        Email high = new Email();
        high.setPriority(EmailPriority.HIGH);
        high.setScheduledTime(LocalDateTime.now());
        high.setCreatedAt(LocalDateTime.now());

        Email medium = new Email();
        medium.setPriority(EmailPriority.MEDIUM);
        medium.setScheduledTime(LocalDateTime.now());
        medium.setCreatedAt(LocalDateTime.now());

        // high should come before medium -> comparator should return negative
        int result = comparator.compare(high, medium);
        assertTrue(result < 0, "High priority should come before Medium priority");
    }

    @Test
    public void testCompareSamePriorityDifferentScheduledTimes() {
        LocalDateTime now = LocalDateTime.now();

        Email earlyScheduled = new Email();
        earlyScheduled.setPriority(EmailPriority.HIGH);
        earlyScheduled.setScheduledTime(now.minusMinutes(10));
        earlyScheduled.setCreatedAt(now);

        Email lateScheduled = new Email();
        lateScheduled.setPriority(EmailPriority.HIGH);
        lateScheduled.setScheduledTime(now.plusMinutes(10));
        lateScheduled.setCreatedAt(now);

        // earlyScheduled should come before lateScheduled -> comparator should return negative
        int result = comparator.compare(earlyScheduled, lateScheduled);
        assertTrue(result < 0, "Earlier scheduled time should come before later scheduled time");
    }

    @Test
    public void testCompareSamePrioritySameScheduledTimeDifferentCreatedAt() {
        LocalDateTime now = LocalDateTime.now();

        Email olderCreated = new Email();
        olderCreated.setPriority(EmailPriority.HIGH);
        olderCreated.setScheduledTime(now);
        olderCreated.setCreatedAt(now.minusHours(2));

        Email newerCreated = new Email();
        newerCreated.setPriority(EmailPriority.HIGH);
        newerCreated.setScheduledTime(now);
        newerCreated.setCreatedAt(now.minusHours(1));

        // olderCreated should come before newerCreated -> comparator should return negative
        int result = comparator.compare(olderCreated, newerCreated);
        assertTrue(result < 0, "Older created email should come before newer created email");
    }

    @Test
    public void testPrioritySortingInList() {
        LocalDateTime now = LocalDateTime.now();

        Email email1 = new Email();
        email1.setId(1L);
        email1.setPriority(EmailPriority.LOW);
        email1.setScheduledTime(now);
        email1.setCreatedAt(now);

        Email email2 = new Email();
        email2.setId(2L);
        email2.setPriority(EmailPriority.HIGH);
        email2.setScheduledTime(now);
        email2.setCreatedAt(now);

        Email email3 = new Email();
        email3.setId(3L);
        email3.setPriority(EmailPriority.MEDIUM);
        email3.setScheduledTime(now.minusMinutes(5));
        email3.setCreatedAt(now);

        Email email4 = new Email();
        email4.setId(4L);
        email4.setPriority(EmailPriority.MEDIUM);
        email4.setScheduledTime(now.plusMinutes(5));
        email4.setCreatedAt(now);

        List<Email> list = new ArrayList<>(List.of(email1, email2, email3, email4));
        list.sort(comparator);

        // Expected sorted order:
        // 1. HIGH priority (email2)
        // 2. MEDIUM priority with earlier scheduledTime (email3)
        // 3. MEDIUM priority with later scheduledTime (email4)
        // 4. LOW priority (email1)
        assertEquals(2L, list.get(0).getId());
        assertEquals(3L, list.get(1).getId());
        assertEquals(4L, list.get(2).getId());
        assertEquals(1L, list.get(3).getId());
    }
}
