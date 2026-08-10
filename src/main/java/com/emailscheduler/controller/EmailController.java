package com.emailscheduler.controller;

import com.emailscheduler.dto.EmailRequestDTO;
import com.emailscheduler.model.*;
import com.emailscheduler.service.AuthService;
import com.emailscheduler.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/emails")
public class EmailController {

    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);
    private static final String UPLOAD_DIR = "C:/Users/cheta/.gemini/antigravity/scratch/email-scheduler/uploads/";

    private final EmailService emailService;
    private final AuthService authService;

    public EmailController(EmailService emailService, AuthService authService) {
        this.emailService = emailService;
        this.authService = authService;
    }

    @GetMapping("/compose")
    public String composePage(Model model) {
        model.addAttribute("emailRequest", new EmailRequestDTO());
        model.addAttribute("priorities", EmailPriority.values());
        return "compose";
    }

    @PostMapping("/compose")
    public String scheduleEmail(
            @ModelAttribute("emailRequest") EmailRequestDTO requestDTO,
            @RequestParam("action") String action,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            User user = authService.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found."));

            Email email = new Email();
            email.setSender(user);
            email.setRecipients(requestDTO.getRecipients());
            email.setCc(requestDTO.getCc());
            email.setBcc(requestDTO.getBcc());
            email.setSubject(requestDTO.getSubject());
            email.setBody(requestDTO.getBody());
            email.setPriority(requestDTO.getPriority());

            // Parse scheduled time and convert from user's local time to UTC for storage.
            // timezoneOffset is in minutes-behind-UTC (e.g., IST = -330).
            // localTime.plusMinutes(-330) subtracts 5h30m, converting IST to UTC correctly.
            if (requestDTO.getScheduledTime() != null && !requestDTO.getScheduledTime().isEmpty()) {
                LocalDateTime localTime = LocalDateTime.parse(requestDTO.getScheduledTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                int offset = (requestDTO.getTimezoneOffset() != null) ? requestDTO.getTimezoneOffset() : 0;
                if (offset != 0) {
                    localTime = localTime.plusMinutes(offset);
                }
                email.setTimezoneOffset(offset);
                email.setScheduledTime(localTime);
            } else {
                // No scheduled time provided — send immediately in UTC
                email.setScheduledTime(LocalDateTime.now(ZoneOffset.UTC));
                email.setTimezoneOffset(0);
            }

            // Determine status based on button clicked
            if ("draft".equalsIgnoreCase(action)) {
                email.setStatus(EmailStatus.DRAFT);
            } else {
                email.setStatus(EmailStatus.PENDING);
            }

            // Handle file attachments upload
            handleAttachments(requestDTO.getAttachments(), email);

            emailService.saveEmail(email);

            return "redirect:/dashboard?success=Email saved successfully!";
        } catch (Exception e) {
            logger.error("Error scheduling email: ", e);
            model.addAttribute("error", "Error creating email request: " + e.getMessage());
            model.addAttribute("priorities", EmailPriority.values());
            return "compose";
        }
    }

    @GetMapping("/{id}")
    public String emailDetailPage(@PathVariable("id") Long id, Model model) {
        Email email = emailService.getEmailById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Email ID: " + id));

        List<EmailStatusLog> logs = emailService.getStatusLogs(id);

        model.addAttribute("email", email);
        model.addAttribute("logs", logs);
        return "detail";
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable("id") Long id, Model model) {
        Email email = emailService.getEmailById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Email ID: " + id));

        // Only DRAFT, PENDING, or RETRIED can be edited
        if (email.getStatus() != EmailStatus.DRAFT &&
            email.getStatus() != EmailStatus.PENDING &&
            email.getStatus() != EmailStatus.RETRIED) {
            return "redirect:/emails/" + id + "?error=Cannot edit email in " + email.getStatus() + " state.";
        }

        EmailRequestDTO requestDTO = new EmailRequestDTO();
        requestDTO.setRecipients(email.getRecipients());
        requestDTO.setCc(email.getCc());
        requestDTO.setBcc(email.getBcc());
        requestDTO.setSubject(email.getSubject());
        requestDTO.setBody(email.getBody());
        requestDTO.setPriority(email.getPriority());
        if (email.getScheduledTime() != null) {
            requestDTO.setScheduledTime(email.getLocalScheduledTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        requestDTO.setTimezoneOffset(email.getTimezoneOffset());

        model.addAttribute("emailId", id);
        model.addAttribute("emailRequest", requestDTO);
        model.addAttribute("priorities", EmailPriority.values());
        model.addAttribute("currentStatus", email.getStatus());
        return "compose"; // Reuse compose template
    }

    @PostMapping("/{id}/edit")
    public String updateEmail(
            @PathVariable("id") Long id,
            @ModelAttribute("emailRequest") EmailRequestDTO requestDTO,
            @RequestParam("action") String action,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        try {
            Email existing = emailService.getEmailById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Email ID: " + id));

            Email updated = new Email();
            updated.setRecipients(requestDTO.getRecipients());
            updated.setCc(requestDTO.getCc());
            updated.setBcc(requestDTO.getBcc());
            updated.setSubject(requestDTO.getSubject());
            updated.setBody(requestDTO.getBody());
            updated.setPriority(requestDTO.getPriority());

            // Parse scheduled time and convert from user's local time to UTC for storage.
            if (requestDTO.getScheduledTime() != null && !requestDTO.getScheduledTime().isEmpty()) {
                LocalDateTime localTime = LocalDateTime.parse(requestDTO.getScheduledTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                // Use offset from the submitted form; fall back to the existing email's offset if not provided.
                int offset = (requestDTO.getTimezoneOffset() != null) ? requestDTO.getTimezoneOffset() : (existing.getTimezoneOffset() != null ? existing.getTimezoneOffset() : 0);
                if (offset != 0) {
                    localTime = localTime.plusMinutes(offset);
                }
                updated.setTimezoneOffset(offset);
                updated.setScheduledTime(localTime);
            } else {
                updated.setScheduledTime(LocalDateTime.now(ZoneOffset.UTC));
                updated.setTimezoneOffset(existing.getTimezoneOffset() != null ? existing.getTimezoneOffset() : 0);
            }

            if ("draft".equalsIgnoreCase(action)) {
                updated.setStatus(EmailStatus.DRAFT);
            } else {
                updated.setStatus(EmailStatus.PENDING);
            }

            // Copy over existing attachments and handle new ones
            List<EmailAttachment> combinedAttachments = new ArrayList<>(existing.getAttachments());
            updated.setAttachments(combinedAttachments);
            handleAttachments(requestDTO.getAttachments(), updated);

            emailService.updateEmail(id, updated);

            return "redirect:/emails/" + id + "?success=Email updated successfully!";
        } catch (Exception e) {
            logger.error("Error updating email: ", e);
            model.addAttribute("emailId", id);
            model.addAttribute("error", "Error updating email: " + e.getMessage());
            model.addAttribute("priorities", EmailPriority.values());
            return "compose";
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancelEmail(@PathVariable("id") Long id) {
        try {
            emailService.cancelEmail(id);
            return "redirect:/emails/" + id + "?success=Email cancelled successfully.";
        } catch (IllegalStateException e) {
            return "redirect:/emails/" + id + "?error=" + e.getMessage();
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteEmail(@PathVariable("id") Long id) {
        try {
            emailService.deleteEmail(id);
            return "redirect:/dashboard?success=Email deleted successfully.";
        } catch (Exception e) {
            return "redirect:/emails/" + id + "?error=Error deleting email: " + e.getMessage();
        }
    }

    private void handleAttachments(List<MultipartFile> files, Email email) throws IOException {
        if (files == null || files.isEmpty()) return;

        // Ensure target directory exists
        File targetDir = new File(UPLOAD_DIR);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.trim().isEmpty()) continue;

            // Generate unique filename to avoid overwrites
            String uniqueName = UUID.randomUUID().toString() + "_" + originalName;
            String filePath = UPLOAD_DIR + uniqueName;

            file.transferTo(new File(filePath));

            EmailAttachment attachment = new EmailAttachment(originalName, filePath);
            email.addAttachment(attachment);
        }
    }
}
