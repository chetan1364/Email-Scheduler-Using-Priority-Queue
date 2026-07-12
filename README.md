# Email Scheduler Using Priority Queue

A full-stack Spring Boot web application that implements an email dispatch pipeline using a custom priority queue data structure. The application schedules emails based on priority levels, scheduled execution times, and creation timestamps, serving them via SMTP.

---

## Technical Stack
- **Backend**: Java 17+, Spring Boot 3.3.2
- **Modules**: Spring Web (MVC), Spring Security, Spring Data JPA, Spring Mail, Spring Scheduler
- **Database**: MySQL (Default) / H2 In-Memory (Fallback developer profile)
- **Frontend**: HTML5, Vanilla CSS, Bootstrap 5, Thymeleaf
- **Build Tool**: Maven

---

## Core DSA Scheduling Logic
The core scheduling algorithm runs in the background using Java's `PriorityQueue` (specifically, a thread-safe `PriorityBlockingQueue` to handle concurrency) ordered by a custom comparator `PriorityComparator`. 

### Email Ordering Algorithm:
1. **Priority Rank**: High priority (`HIGH`) comes before medium priority (`MEDIUM`), which comes before low priority (`LOW`).
2. **Scheduled Time**: If priorities are equal, emails with an earlier scheduled delivery time (`scheduledTime`) are executed first.
3. **Creation Time (Tie-breaker)**: If both the priority and the scheduled time are identical, the older email (based on the `createdAt` timestamp) is processed first.

### Dispatch States Lifecycle:
- `DRAFT` (Saved draft; not ready to send)
- `PENDING` (Scheduled and waiting for execution time)
- `QUEUED` (Picked up by scheduler and loaded into Priority Queue)
- `PROCESSING` (Currently dispatching via SMTP)
- `SENT` (Successfully dispatched)
- `RETRIED` (Dispatched failed, rescheduled for a backoff attempt)
- `FAILED` (Permanently failed, maximum retries reached)
- `CANCELLED` (Cancelled by user before dispatch)

---

## Getting Started

### 1. Database Configuration
By default, the application runs on MySQL. Ensure a database named `email_scheduler` exists:
```sql
CREATE DATABASE email_scheduler;
```
Configure your credentials in [application.properties](file:///C:/Users/cheta/.gemini/antigravity/scratch/email-scheduler/src/main/resources/application.properties):
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/email_scheduler?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

#### Developer H2 Fallback
If you do not have MySQL running locally, you can run the app in H2 In-memory DB mode by uncommenting the H2 lines in `application.properties` and commenting out the MySQL lines.

### 2. SMTP Configurations
The application uses SMTP for mail dispatch. Configure your mail server details in `application.properties`:
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
```

---

## Running the Application

### Seeded Credentials
When the application starts, it seeds default login credentials for quick testing:
- **Admin User**:
  - **Email**: `admin@emailscheduler.com`
  - **Password**: `admin123`
  - **Role**: `ADMIN`
- **Standard User**:
  - **Email**: `user@emailscheduler.com`
  - **Password**: `user123`
  - **Role**: `USER`

### 1. Build and Run Tests
```bash
mvn clean test
```

### 2. Run the Application
```bash
mvn spring-boot:run
```
Once started, navigate to `http://localhost:8080` in your web browser.

---

## Features
- **User Authentication**: Secure signup/signin using Spring Security, with role-based restrictions.
- **Compose Interface**: Interactive email editor supporting body details, multiple attachments, and local scheduling calendar dates.
- **Live Monitoring Dashboard**: Live status card summaries, search queries filtering, and side panel notifications.
- **Admin Control Console**: Live priority queue diagnostic viewer, account management toggles, database metrics charts, and SMTP configuration checks.
