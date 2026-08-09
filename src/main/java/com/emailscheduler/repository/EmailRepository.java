package com.emailscheduler.repository;

import com.emailscheduler.model.Email;
import com.emailscheduler.model.EmailPriority;
import com.emailscheduler.model.EmailStatus;
import com.emailscheduler.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {

    List<Email> findBySenderOrderByCreatedAtDesc(User sender);

    List<Email> findByStatusInAndScheduledTimeLessThanEqual(List<EmailStatus> statuses, LocalDateTime scheduledTime);

    @Query("SELECT e FROM Email e WHERE e.sender.id = :senderId " +
           "AND (:status IS NULL OR e.status = :status) " +
           "AND (:priority IS NULL OR e.priority = :priority) " +
           "AND (LOWER(e.recipients) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.subject) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND e.scheduledTime >= :startDate " +
           "AND e.scheduledTime <= :endDate " +
           "ORDER BY e.createdAt DESC")
    List<Email> filterEmails(
            @Param("senderId") Long senderId,
            @Param("status") EmailStatus status,
            @Param("priority") EmailPriority priority,
            @Param("search") String search,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT e FROM Email e WHERE " +
           "(:status IS NULL OR e.status = :status) " +
           "AND (:priority IS NULL OR e.priority = :priority) " +
           "AND (LOWER(e.recipients) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.subject) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.sender.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY e.createdAt DESC")
    List<Email> filterAllEmails(
            @Param("status") EmailStatus status,
            @Param("priority") EmailPriority priority,
            @Param("search") String search
    );

    long countByStatus(EmailStatus status);

    long countByPriority(EmailPriority priority);

    long countBySenderId(Long senderId);

    long countBySenderIdAndStatus(Long senderId, EmailStatus status);

    @Query("SELECT COUNT(e) FROM Email e WHERE e.status = 'SENT' AND e.scheduledTime >= :startOfDay AND e.scheduledTime < :endOfDay")
    long countSentEmailsInTimeRange(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
}
