package com.emailscheduler.scheduler;

import com.emailscheduler.model.Email;
import com.emailscheduler.model.EmailStatus;
import com.emailscheduler.model.EmailStatusLog;
import com.emailscheduler.repository.EmailRepository;
import com.emailscheduler.repository.EmailStatusLogRepository;
import com.emailscheduler.service.MailDispatchService;
import com.emailscheduler.service.NotificationService;
import com.emailscheduler.util.RetryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Handles transactional email dispatch logic in a separate bean so that
 * Spring's AOP proxy applies @Transactional correctly.
 *
 * This avoids the self-invocation problem that occurs when EmailDispatchJob
 * calls @Transactional methods on itself (bypassing the proxy).
 */
@Service
public class EmailDispatchTransactionService {

    private static final Logger logger = LoggerFactory.getLogger(EmailDispatchTransactionService.class);

    private final EmailRepository emailRepository;
    private final EmailStatusLogRepository statusLogRepository;
    private final MailDispatchService mailDispatchService;
    private final NotificationService notificationService;

    public EmailDispatchTransactionService(EmailRepository emailRepository,
                                           EmailStatusLogRepository statusLogRepository,
                                           MailDispatchService mailDispatchService,
                                           NotificationService notificationService) {
        this.emailRepository = emailRepository;
        this.statusLogRepository = statusLogRepository;
        this.mailDispatchService = mailDispatchService;
        this.notificationService = notificationService;
    }

    /**
     * Dispatches a single email within a transaction.
     * @Transactional keeps the Hibernate session open so lazy collections
     * (e.g. attachments) can be loaded and all DB saves are committed atomically.
     */
    @Transactional
    public void processEmailDispatch(Long emailId) {
        // Re-fetch inside the transaction so all lazy associations are accessible
        Email email = emailRepository.findById(emailId).orElse(null);
        if (email == null) return;

        if (email.getStatus() == EmailStatus.CANCELLED) {
            logger.info("Skipping email ID {} because it was cancelled.", email.getId());
            logStatusChange(email, EmailStatus.CANCELLED, "Email skipped from dispatch queue because it was cancelled.");
            return;
        }

        // Eagerly initialize attachments while the session is open
        email.getAttachments().size();

        // Mark as PROCESSING
        updateStatus(email, EmailStatus.PROCESSING, "Email sending in progress.");

        try {
            mailDispatchService.sendEmail(email);

            // Success
            updateStatus(email, EmailStatus.SENT, "Email successfully sent via SMTP.");
            notificationService.sendSuccessNotification(email.getSender(), email);

        } catch (Exception e) {
            logger.error("Error dispatching email ID {}: {}", email.getId(), e.getMessage());

            if (RetryHandler.canRetry(email)) {
                int nextRetryCount = email.getRetryCount() + 1;
                LocalDateTime nextRetryTime = RetryHandler.calculateNextRetryTime(email);

                email.setRetryCount(nextRetryCount);
                email.setScheduledTime(nextRetryTime);
                email.setStatus(EmailStatus.RETRIED);
                emailRepository.save(email);

                logStatusChange(email, EmailStatus.RETRIED, "Sending failed. Rescheduled. Error: " + e.getMessage());
                notificationService.sendRetryNotification(email.getSender(), email, nextRetryTime, e.getMessage());

                logger.info("Email ID {} rescheduled for retry #{} at {}", email.getId(), nextRetryCount, nextRetryTime);
            } else {
                // Out of retries → FAILED
                updateStatus(email, EmailStatus.FAILED, "Email sending failed. Maximum retries reached. Error: " + e.getMessage());
                notificationService.sendFailureNotification(email.getSender(), email, e.getMessage());

                logger.info("Email ID {} failed permanently after max retries.", email.getId());
            }
        }
    }

    private void updateStatus(Email email, EmailStatus status, String message) {
        email.setStatus(status);
        emailRepository.save(email);
        logStatusChange(email, status, message);
    }

    private void logStatusChange(Email email, EmailStatus status, String message) {
        EmailStatusLog log = new EmailStatusLog(email, status, LocalDateTime.now(), message);
        statusLogRepository.save(log);
    }
}
