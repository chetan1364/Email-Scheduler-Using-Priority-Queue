package com.emailscheduler.dto;

import com.emailscheduler.model.Email;
import com.emailscheduler.model.EmailPriority;
import com.emailscheduler.model.EmailStatus;

import java.time.LocalDateTime;

public class EmailResponseDTO {

    private Long id;
    private String recipients;
    private String subject;
    private EmailPriority priority;
    private LocalDateTime scheduledTime;
    private EmailStatus status;
    private int retryCount;

    public EmailResponseDTO() {
    }

    public EmailResponseDTO(Email email) {
        this.id = email.getId();
        this.recipients = email.getRecipients();
        this.subject = email.getSubject();
        this.priority = email.getPriority();
        this.scheduledTime = email.getScheduledTime();
        this.status = email.getStatus();
        this.retryCount = email.getRetryCount();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecipients() {
        return recipients;
    }

    public void setRecipients(String recipients) {
        this.recipients = recipients;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public EmailPriority getPriority() {
        return priority;
    }

    public void setPriority(EmailPriority priority) {
        this.priority = priority;
    }

    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public EmailStatus getStatus() {
        return status;
    }

    public void setStatus(EmailStatus status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}
