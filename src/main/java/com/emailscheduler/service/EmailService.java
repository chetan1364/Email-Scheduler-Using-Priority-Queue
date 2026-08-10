package com.emailscheduler.service;

import com.emailscheduler.model.*;

import com.emailscheduler.repository.EmailRepository;
import com.emailscheduler.repository.EmailStatusLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EmailService {

    private final EmailRepository emailRepository;
    private final EmailStatusLogRepository statusLogRepository;

    public EmailService(EmailRepository emailRepository, EmailStatusLogRepository statusLogRepository) {
        this.emailRepository = emailRepository;
        this.statusLogRepository = statusLogRepository;
    }

    public List<Email> getEmailsBySender(User sender) {
        return emailRepository.findBySenderOrderByCreatedAtDesc(sender);
    }

    public Optional<Email> getEmailById(Long id) {
        return emailRepository.findById(id);
    }

    @Transactional
    public Email saveEmail(Email email) {
        boolean isNew = email.getId() == null;
        if (isNew) {
            email.setCreatedAt(LocalDateTime.now());
            if (email.getRetryCount() == 0) {
                email.setRetryCount(0);
            }
        }
        
        Email savedEmail = emailRepository.save(email);

        // Log initial state
        String message = isNew ? "Email created with status: " + email.getStatus() : "Email updated with status: " + email.getStatus();
        logStatusChange(savedEmail, email.getStatus(), message);

        return savedEmail;
    }

    @Transactional
    public Email updateEmail(Long id, Email updatedEmail) {
        Email existing = emailRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Email not found."));

        // Only allow edits for Drafts, Pending, or Retried emails
        if (existing.getStatus() != EmailStatus.DRAFT &&
            existing.getStatus() != EmailStatus.PENDING &&
            existing.getStatus() != EmailStatus.RETRIED) {
            throw new IllegalStateException("Only drafts, pending, or retried emails can be edited.");
        }

        existing.setRecipients(updatedEmail.getRecipients());
        existing.setCc(updatedEmail.getCc());
        existing.setBcc(updatedEmail.getBcc());
        existing.setSubject(updatedEmail.getSubject());
        existing.setBody(updatedEmail.getBody());
        existing.setPriority(updatedEmail.getPriority());
        existing.setScheduledTime(updatedEmail.getScheduledTime());
        existing.setTimezoneOffset(updatedEmail.getTimezoneOffset());
        existing.setStatus(updatedEmail.getStatus()); // Could change from Draft to Pending
        
        // Clear previous attachments and add new ones if managed via cascade
        existing.getAttachments().clear();
        if (updatedEmail.getAttachments() != null) {
            for (EmailAttachment attachment : updatedEmail.getAttachments()) {
                existing.addAttachment(attachment);
            }
        }

        Email saved = emailRepository.save(existing);
        logStatusChange(saved, saved.getStatus(), "Email details updated.");
        return saved;
    }

    @Transactional
    public void cancelEmail(Long id) {
        Email email = emailRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Email not found."));

        if (email.getStatus() != EmailStatus.PENDING &&
            email.getStatus() != EmailStatus.RETRIED &&
            email.getStatus() != EmailStatus.QUEUED) {
            throw new IllegalStateException("Only pending, retried, or queued emails can be cancelled.");
        }

        email.setStatus(EmailStatus.CANCELLED);
        emailRepository.save(email);
        logStatusChange(email, EmailStatus.CANCELLED, "Email scheduled dispatch cancelled by user.");
    }

    @Transactional
    public void deleteEmail(Long id) {
        Email email = emailRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Email not found."));
        emailRepository.delete(email);
    }

    public List<EmailStatusLog> getStatusLogs(Long emailId) {
        Email email = emailRepository.findById(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Email not found."));
        return statusLogRepository.findByEmailOrderByTimestampDesc(email);
    }

    public List<Email> filterUserEmails(Long userId, EmailStatus status, EmailPriority priority, String search, LocalDateTime start, LocalDateTime end) {
        String safeSearch = search == null ? "" : search;
        LocalDateTime safeStart = start != null ? start : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime safeEnd = end != null ? end : LocalDateTime.of(2099, 12, 31, 23, 59);
        return emailRepository.filterEmails(userId, status, priority, safeSearch, safeStart, safeEnd);
    }

    public List<Email> filterAllEmails(EmailStatus status, EmailPriority priority, String search) {
        String safeSearch = search == null ? "" : search;
        return emailRepository.filterAllEmails(status, priority, safeSearch);
    }

    private void logStatusChange(Email email, EmailStatus status, String message) {
        EmailStatusLog log = new EmailStatusLog(email, status, LocalDateTime.now(), message);
        statusLogRepository.save(log);
    }

    // Analytics Methods
    public Map<String, Long> getStatusDistribution() {
        Map<String, Long> dist = new HashMap<>();
        for (EmailStatus status : EmailStatus.values()) {
            dist.put(status.name(), emailRepository.countByStatus(status));
        }
        return dist;
    }

    public Map<String, Long> getPriorityDistribution() {
        Map<String, Long> dist = new HashMap<>();
        for (EmailPriority priority : EmailPriority.values()) {
            dist.put(priority.name(), emailRepository.countByPriority(priority));
        }
        return dist;
    }

    public long getSentEmailsToday() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        return emailRepository.countSentEmailsInTimeRange(startOfDay, endOfDay);
    }

    public double getFailureRate() {
        long sent = emailRepository.countByStatus(EmailStatus.SENT);
        long failed = emailRepository.countByStatus(EmailStatus.FAILED);
        long totalDispatched = sent + failed;
        if (totalDispatched == 0) return 0.0;
        return ((double) failed / totalDispatched) * 100.0;
    }
}
