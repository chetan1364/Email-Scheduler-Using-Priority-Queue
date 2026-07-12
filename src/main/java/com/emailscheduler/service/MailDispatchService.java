package com.emailscheduler.service;

import com.emailscheduler.model.Email;
import com.emailscheduler.model.EmailAttachment;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class MailDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(MailDispatchService.class);
    private final JavaMailSender mailSender;

    // Must match the authenticated SMTP account — Gmail rejects mismatched From addresses
    @Value("${spring.mail.username}")
    private String fromAddress;

    public MailDispatchService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Dispatches an email using Spring Mail.
     * Supports CC, BCC, HTML body, and File Attachments.
     */
    public void sendEmail(Email email) throws Exception {
        logger.info("Attempting to send email ID: {} to {}", email.getId(), email.getRecipients());
        
        MimeMessage message = mailSender.createMimeMessage();
        
        // true flag indicates multipart message (for attachments/HTML)
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // From address MUST match the authenticated Gmail account (chetanchaudhari1364@gmail.com)
        // Gmail silently drops or rejects emails with a mismatched From header
        helper.setFrom(fromAddress);
        
        // Process To
        String[] toArray = splitAddresses(email.getRecipients());
        if (toArray.length > 0) {
            helper.setTo(toArray);
        } else {
            throw new IllegalArgumentException("No valid recipient addresses found.");
        }

        // Process CC
        String[] ccArray = splitAddresses(email.getCc());
        if (ccArray.length > 0) {
            helper.setCc(ccArray);
        }

        // Process BCC
        String[] bccArray = splitAddresses(email.getBcc());
        if (bccArray.length > 0) {
            helper.setBcc(bccArray);
        }

        helper.setSubject(email.getSubject());
        
        // true flag enables HTML body rendering
        helper.setText(email.getBody(), true);

        // Process Attachments
        if (email.getAttachments() != null) {
            for (EmailAttachment attachment : email.getAttachments()) {
                File file = new File(attachment.getFilePath());
                if (file.exists() && file.isFile()) {
                    FileSystemResource fileSystemResource = new FileSystemResource(file);
                    helper.addAttachment(attachment.getFileName(), fileSystemResource);
                    logger.debug("Attached file: {} from path: {}", attachment.getFileName(), attachment.getFilePath());
                } else {
                    logger.warn("Attachment file not found: {}", attachment.getFilePath());
                }
            }
        }

        mailSender.send(message);
        logger.info("Email ID {} successfully dispatched.", email.getId());
    }

    private String[] splitAddresses(String addresses) {
        if (addresses == null || addresses.trim().isEmpty()) {
            return new String[0];
        }
        return addresses.split("\\s*,\\s*");
    }
}
