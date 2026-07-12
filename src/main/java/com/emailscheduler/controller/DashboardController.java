package com.emailscheduler.controller;

import com.emailscheduler.model.*;
import com.emailscheduler.service.AuthService;
import com.emailscheduler.service.EmailService;
import com.emailscheduler.service.NotificationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final EmailService emailService;
    private final AuthService authService;
    private final NotificationService notificationService;

    public DashboardController(EmailService emailService, AuthService authService, NotificationService notificationService) {
        this.emailService = emailService;
        this.authService = authService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String viewDashboard(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "status", required = false) EmailStatus status,
            @RequestParam(value = "priority", required = false) EmailPriority priority,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        // Get currently logged-in user
        User user = authService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userDetails.getUsername()));

        // Convert search ranges
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        // Fetch filtered emails for the logged-in user
        List<Email> emails = emailService.filterUserEmails(user.getId(), status, priority, search, startDateTime, endDateTime);

        // Fetch User notifications
        List<Notification> notifications = notificationService.getNotificationsForUser(user);

        // Calculate user metrics
        long totalCount = emails.size();
        long sentCount = emails.stream().filter(e -> e.getStatus() == EmailStatus.SENT).count();
        long pendingCount = emails.stream().filter(e -> e.getStatus() == EmailStatus.PENDING || e.getStatus() == EmailStatus.RETRIED || e.getStatus() == EmailStatus.QUEUED).count();
        long failedCount = emails.stream().filter(e -> e.getStatus() == EmailStatus.FAILED).count();

        // Pass objects to Thymeleaf model
        model.addAttribute("user", user);
        model.addAttribute("emails", emails);
        model.addAttribute("notifications", notifications);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("sentCount", sentCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("failedCount", failedCount);

        // Retain search values in the form
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPriority", priority);
        model.addAttribute("searchQuery", search);
        model.addAttribute("selectedStartDate", startDate);
        model.addAttribute("selectedEndDate", endDate);

        return "dashboard";
    }

    @GetMapping("/notifications/read")
    public String markNotificationsRead(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            User user = authService.findByEmail(userDetails.getUsername()).orElse(null);
            if (user != null) {
                notificationService.markAllAsRead(user);
            }
        }
        return "redirect:/dashboard";
    }
}
