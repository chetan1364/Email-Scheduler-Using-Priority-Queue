package com.emailscheduler.scheduler;

import com.emailscheduler.model.Email;
import com.emailscheduler.model.EmailStatus;
import com.emailscheduler.repository.EmailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EmailDispatchJob {

    private static final Logger logger = LoggerFactory.getLogger(EmailDispatchJob.class);

    private final EmailRepository emailRepository;
    private final EmailPriorityQueueManager queueManager;
    private final EmailDispatchTransactionService dispatchTransactionService;

    public EmailDispatchJob(EmailRepository emailRepository,
                            EmailPriorityQueueManager queueManager,
                            EmailDispatchTransactionService dispatchTransactionService) {
        this.emailRepository = emailRepository;
        this.queueManager = queueManager;
        this.dispatchTransactionService = dispatchTransactionService;
    }

    /**
     * Scheduled task to fetch ready emails, load into Priority Queue, and dispatch.
     * Runs every 10 seconds.
     *
     * Fetches PENDING, RETRIED, and QUEUED so that emails stuck in QUEUED
     * after an app restart are automatically recovered and dispatched.
     */
    @Scheduled(fixedDelayString = "${email.scheduler.cron-interval-seconds:10}000")
    public void runDispatchJob() {
        logger.info("Executing Email Scheduler Dispatch Job...");

        // 1. Fetch all dispatchable emails from DB
        //    QUEUED is included to recover emails that were in-memory queue when app restarted
        LocalDateTime now = LocalDateTime.now();
        List<Email> readyEmails = emailRepository.findByStatusInAndScheduledTimeLessThanEqual(
                List.of(EmailStatus.PENDING, EmailStatus.RETRIED, EmailStatus.QUEUED), now
        );

        if (!readyEmails.isEmpty()) {
            logger.info("Found {} emails ready to dispatch.", readyEmails.size());
            for (Email email : readyEmails) {
                // Deduplicated insert into in-memory priority queue
                queueManager.addEmail(email);
            }
        }

        // 2. Poll and dispatch all emails from the Priority Queue
        dispatchQueuedEmails();
    }

    /**
     * Drains the Priority Queue and dispatches each email via the transaction service.
     * Each email is dispatched in its own transaction so failures are isolated.
     */
    public void dispatchQueuedEmails() {
        while (!queueManager.isEmpty()) {
            Email queuedEmail = queueManager.pollEmail();
            if (queuedEmail == null) continue;

            // Delegates to a separate @Service bean so Spring's @Transactional
            // proxy is applied correctly (avoids self-invocation bypass).
            dispatchTransactionService.processEmailDispatch(queuedEmail.getId());
        }
    }
}
