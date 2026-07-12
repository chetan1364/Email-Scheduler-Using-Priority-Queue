package com.emailscheduler.service;

import com.emailscheduler.model.Email;
import com.emailscheduler.model.Notification;
import com.emailscheduler.model.User;
import com.emailscheduler.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void sendSuccessNotification(User user, Email email) {
        String msg = String.format("Email sent successfully to '%s' (Subject: %s)", 
                email.getRecipients(), email.getSubject());
        notificationRepository.save(new Notification(user, msg));
    }

    @Transactional
    public void sendFailureNotification(User user, Email email, String error) {
        String msg = String.format("Email permanently failed sending to '%s'. Error: %s (Subject: %s)", 
                email.getRecipients(), error, email.getSubject());
        notificationRepository.save(new Notification(user, msg));
    }

    @Transactional
    public void sendRetryNotification(User user, Email email, LocalDateTime nextAttempt, String error) {
        String msg = String.format("Email send failed to '%s'. Retrying at %s. Error: %s (Subject: %s)", 
                email.getRecipients(), nextAttempt, error, email.getSubject());
        notificationRepository.save(new Notification(user, msg));
    }

    public List<Notification> getNotificationsForUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Notification> getUnreadNotificationsForUser(User user) {
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> unread = notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }
}
