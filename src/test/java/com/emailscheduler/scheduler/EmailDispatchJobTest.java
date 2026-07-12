package com.emailscheduler.scheduler;

import com.emailscheduler.model.Email;
import com.emailscheduler.model.EmailPriority;
import com.emailscheduler.model.EmailStatus;
import com.emailscheduler.model.User;
import com.emailscheduler.repository.EmailRepository;
import com.emailscheduler.repository.EmailStatusLogRepository;
import com.emailscheduler.service.MailDispatchService;
import com.emailscheduler.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EmailDispatchJobTest {

    private EmailRepository emailRepository;
    private EmailStatusLogRepository statusLogRepository;
    private EmailPriorityQueueManager queueManager;
    private MailDispatchService mailDispatchService;
    private NotificationService notificationService;

    private EmailDispatchTransactionService dispatchTransactionService;
    private EmailDispatchJob dispatchJob;

    @BeforeEach
    public void setUp() {
        emailRepository = mock(EmailRepository.class);
        statusLogRepository = mock(EmailStatusLogRepository.class);
        queueManager = new EmailPriorityQueueManager();
        mailDispatchService = mock(MailDispatchService.class);
        notificationService = mock(NotificationService.class);

        dispatchTransactionService = new EmailDispatchTransactionService(
                emailRepository,
                statusLogRepository,
                mailDispatchService,
                notificationService
        );

        dispatchJob = new EmailDispatchJob(
                emailRepository,
                queueManager,
                dispatchTransactionService
        );
    }

    @Test
    public void testEmailsAreAddedToQueueAndDispatched() throws Exception {
        User sender = new User();
        sender.setEmail("sender@example.com");

        Email email1 = new Email();
        email1.setId(1L);
        email1.setStatus(EmailStatus.PENDING);
        email1.setScheduledTime(LocalDateTime.now().minusMinutes(1));
        email1.setSender(sender);
        email1.setPriority(EmailPriority.HIGH);

        Email email2 = new Email();
        email2.setId(2L);
        email2.setStatus(EmailStatus.RETRIED);
        email2.setScheduledTime(LocalDateTime.now().minusMinutes(1));
        email2.setSender(sender);
        email2.setPriority(EmailPriority.LOW);
        email2.setRetryCount(1);
        email2.setMaxRetries(3);

        when(emailRepository.findByStatusInAndScheduledTimeLessThanEqual(any(), any()))
                .thenReturn(List.of(email1, email2));
        when(emailRepository.findById(1L)).thenReturn(Optional.of(email1));
        when(emailRepository.findById(2L)).thenReturn(Optional.of(email2));

        dispatchJob.runDispatchJob();

        // Both emails should have been saved at least once (PROCESSING → SENT)
        verify(emailRepository, atLeast(2)).save(any(Email.class));
    }

    @Test
    public void testQueuedEmailsRecoveredAfterRestart() throws Exception {
        // Emails stuck in QUEUED (e.g. app restarted, in-memory queue was lost)
        // must be picked up again in the next scheduler tick.
        User sender = new User();
        sender.setEmail("sender@example.com");

        Email stuckEmail = new Email();
        stuckEmail.setId(5L);
        stuckEmail.setStatus(EmailStatus.QUEUED);
        stuckEmail.setScheduledTime(LocalDateTime.now().minusMinutes(5));
        stuckEmail.setSender(sender);
        stuckEmail.setPriority(EmailPriority.HIGH);
        stuckEmail.setRecipients("recipient@example.com");

        when(emailRepository.findByStatusInAndScheduledTimeLessThanEqual(any(), any()))
                .thenReturn(List.of(stuckEmail));
        when(emailRepository.findById(5L)).thenReturn(Optional.of(stuckEmail));

        dispatchJob.runDispatchJob();

        // Verify the stuck QUEUED email was dispatched
        verify(emailRepository, atLeastOnce()).save(stuckEmail);
        verify(mailDispatchService).sendEmail(stuckEmail);
    }

    @Test
    public void testDispatchQueuedEmailsSuccess() throws Exception {
        User sender = new User();
        sender.setEmail("sender@example.com");

        Email email = new Email();
        email.setId(10L);
        email.setStatus(EmailStatus.QUEUED);
        email.setSender(sender);
        email.setRecipients("recipient@example.com");

        queueManager.addEmail(email);
        when(emailRepository.findById(10L)).thenReturn(Optional.of(email));

        dispatchJob.dispatchQueuedEmails();

        // Verify PROCESSING → SENT flow
        verify(emailRepository, atLeastOnce()).save(email);
        assertEquals(EmailStatus.SENT, email.getStatus());
        verify(mailDispatchService).sendEmail(email);
        verify(notificationService).sendSuccessNotification(sender, email);
    }
}
