package com.emailscheduler.controller;

import com.emailscheduler.model.*;
import com.emailscheduler.scheduler.EmailPriorityQueueManager;
import com.emailscheduler.service.AuthService;
import com.emailscheduler.service.EmailService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AuthService authService;
    private final EmailService emailService;
    private final EmailPriorityQueueManager queueManager;

    public AdminController(AuthService authService, EmailService emailService, EmailPriorityQueueManager queueManager) {
        this.authService = authService;
        this.emailService = emailService;
        this.queueManager = queueManager;
    }

    @GetMapping
    public String viewAdminPanel(
            @RequestParam(value = "status", required = false) EmailStatus status,
            @RequestParam(value = "priority", required = false) EmailPriority priority,
            @RequestParam(value = "search", required = false) String search,
            Model model) {

        // Fetch User accounts list
        List<User> users = authService.getAllUsers();

        // Fetch system wide emails
        List<Email> systemEmails = emailService.filterAllEmails(status, priority, search);

        // Fetch live queue monitoring
        List<Email> liveQueue = queueManager.getQueueSnapshot();

        // Fetch analytics
        Map<String, Long> statusDistribution = emailService.getStatusDistribution();
        Map<String, Long> priorityDistribution = emailService.getPriorityDistribution();
        long sentToday = emailService.getSentEmailsToday();
        double failureRate = emailService.getFailureRate();

        model.addAttribute("users", users);
        model.addAttribute("systemEmails", systemEmails);
        model.addAttribute("liveQueue", liveQueue);
        model.addAttribute("queueSize", queueManager.size());
        model.addAttribute("statusDistribution", statusDistribution);
        model.addAttribute("priorityDistribution", priorityDistribution);
        model.addAttribute("sentToday", sentToday);
        model.addAttribute("failureRate", failureRate);

        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPriority", priority);
        model.addAttribute("searchQuery", search);

        return "admin";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUserStatus(@PathVariable("id") Long id) {
        try {
            authService.toggleUserStatus(id);
        } catch (Exception e) {
            // handle err
        }
        return "redirect:/admin";
    }

    @PostMapping("/users/{id}/role")
    public String updateUserRole(@PathVariable("id") Long id, @RequestParam("role") Role role) {
        try {
            authService.updateUserRole(id, role);
        } catch (Exception e) {
            // handle err
        }
        return "redirect:/admin";
    }

    @PostMapping("/queue/clear")
    public String clearQueue() {
        queueManager.clear();
        return "redirect:/admin?success=Queue cleared.";
    }
}
