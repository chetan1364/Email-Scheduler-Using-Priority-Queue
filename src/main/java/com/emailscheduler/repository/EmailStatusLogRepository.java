package com.emailscheduler.repository;

import com.emailscheduler.model.Email;
import com.emailscheduler.model.EmailStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailStatusLogRepository extends JpaRepository<EmailStatusLog, Long> {
    List<EmailStatusLog> findByEmailOrderByTimestampDesc(Email email);
}
