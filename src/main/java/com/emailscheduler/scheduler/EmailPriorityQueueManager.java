package com.emailscheduler.scheduler;

import com.emailscheduler.model.Email;
import com.emailscheduler.util.PriorityComparator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

@Component
public class EmailPriorityQueueManager {

    private final PriorityBlockingQueue<Email> queue;

    public EmailPriorityQueueManager() {
        // Initialize with default capacity and the custom priority comparator
        this.queue = new PriorityBlockingQueue<>(50, new PriorityComparator());
    }

    /**
     * Adds an email to the priority queue.
     * Checks for duplicates based on the email ID.
     */
    public synchronized void addEmail(Email email) {
        if (email == null || email.getId() == null) return;
        if (!contains(email.getId())) {
            queue.add(email);
        }
    }

    /**
     * Polls the highest priority email from the queue.
     */
    public Email pollEmail() {
        return queue.poll();
    }

    /**
     * Checks if the queue is empty.
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Returns the size of the queue.
     */
    public int size() {
        return queue.size();
    }

    /**
     * Clears all items in the queue.
     */
    public void clear() {
        queue.clear();
    }

    /**
     * Checks if an email with a given ID is already in the queue.
     */
    public boolean contains(Long emailId) {
        if (emailId == null) return false;
        return queue.stream().anyMatch(e -> emailId.equals(e.getId()));
    }

    /**
     * Returns a snapshot copy of the emails currently in the queue.
     */
    public List<Email> getQueueSnapshot() {
        return new ArrayList<>(queue);
    }
}
