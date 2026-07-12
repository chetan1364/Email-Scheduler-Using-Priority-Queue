package com.emailscheduler.dto;

import com.emailscheduler.model.EmailPriority;
import com.emailscheduler.model.EmailStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class EmailRequestDTO {

    private String recipients;
    private String cc;
    private String bcc;
    private String subject;
    private String body;
    private EmailPriority priority;
    private String scheduledTime; // Bound as String from datetime-local input
    private EmailStatus status; // DRAFT or PENDING
    private List<MultipartFile> attachments;

    public EmailRequestDTO() {
    }

    public String getRecipients() {
        return recipients;
    }

    public void setRecipients(String recipients) {
        this.recipients = recipients;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public String getBcc() {
        return bcc;
    }

    public void setBcc(String bcc) {
        this.bcc = bcc;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public EmailPriority getPriority() {
        return priority;
    }

    public void setPriority(EmailPriority priority) {
        this.priority = priority;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(String scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public EmailStatus getStatus() {
        return status;
    }

    public void setStatus(EmailStatus status) {
        this.status = status;
    }

    public List<MultipartFile> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<MultipartFile> attachments) {
        this.attachments = attachments;
    }
}
