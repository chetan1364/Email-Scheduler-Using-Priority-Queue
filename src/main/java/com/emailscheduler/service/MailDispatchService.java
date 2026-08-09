package com.emailscheduler.service;

import com.emailscheduler.model.Email;
import com.emailscheduler.model.EmailAttachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

@Service
public class MailDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(MailDispatchService.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate;

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Value("${brevo.sender.name}")
    private String senderName;

    public MailDispatchService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Dispatches an email via the Brevo Transactional Email HTTP API.
     * Supports CC, BCC, HTML body, and Base64-encoded file attachments.
     * API docs: https://developers.brevo.com/reference/sendtransacemail
     */
    public void sendEmail(Email email) throws Exception {
        logger.info("Attempting to dispatch email ID: {} to {} via Brevo API", email.getId(), email.getRecipients());

        // ── Build request headers ──────────────────────────────────────────
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // ── Build JSON payload ─────────────────────────────────────────────
        Map<String, Object> payload = new LinkedHashMap<>();

        // Sender
        Map<String, String> sender = new LinkedHashMap<>();
        sender.put("name", senderName);
        sender.put("email", senderEmail);
        payload.put("sender", sender);

        // To
        String[] toAddresses = splitAddresses(email.getRecipients());
        if (toAddresses.length == 0) {
            throw new IllegalArgumentException("No valid recipient addresses found.");
        }
        payload.put("to", buildAddressList(toAddresses));

        // CC
        String[] ccAddresses = splitAddresses(email.getCc());
        if (ccAddresses.length > 0) {
            payload.put("cc", buildAddressList(ccAddresses));
        }

        // BCC
        String[] bccAddresses = splitAddresses(email.getBcc());
        if (bccAddresses.length > 0) {
            payload.put("bcc", buildAddressList(bccAddresses));
        }

        // Subject + HTML body
        payload.put("subject", email.getSubject());
        payload.put("htmlContent", email.getBody());

        // Attachments (Base64-encoded)
        if (email.getAttachments() != null && !email.getAttachments().isEmpty()) {
            List<Map<String, String>> attachmentList = new ArrayList<>();
            for (EmailAttachment attachment : email.getAttachments()) {
                File file = new File(attachment.getFilePath());
                if (file.exists() && file.isFile()) {
                    byte[] fileBytes = Files.readAllBytes(file.toPath());
                    String base64Content = Base64.getEncoder().encodeToString(fileBytes);
                    Map<String, String> attachMap = new LinkedHashMap<>();
                    attachMap.put("name", attachment.getFileName());
                    attachMap.put("content", base64Content);
                    attachmentList.add(attachMap);
                    logger.debug("Attached file: {} ({} bytes)", attachment.getFileName(), fileBytes.length);
                } else {
                    logger.warn("Attachment file not found, skipping: {}", attachment.getFilePath());
                }
            }
            if (!attachmentList.isEmpty()) {
                payload.put("attachment", attachmentList);
            }
        }

        // ── Send HTTP POST to Brevo ────────────────────────────────────────
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.exchange(BREVO_API_URL, HttpMethod.POST, requestEntity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("Email ID {} successfully dispatched via Brevo. Response: {}", email.getId(), response.getBody());
        } else {
            throw new RuntimeException("Brevo API returned non-2xx status: " + response.getStatusCode() + " — " + response.getBody());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String[] splitAddresses(String addresses) {
        if (addresses == null || addresses.trim().isEmpty()) {
            return new String[0];
        }
        return addresses.split("\\s*,\\s*");
    }

    /**
     * Converts an array of email address strings into Brevo's list-of-objects format:
     * [{ "email": "user@example.com" }, ...]
     */
    private List<Map<String, String>> buildAddressList(String[] addresses) {
        List<Map<String, String>> list = new ArrayList<>();
        for (String addr : addresses) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("email", addr.trim());
            list.add(entry);
        }
        return list;
    }
}
