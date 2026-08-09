# Email Scheduler Using Priority Queue

A full-stack Spring Boot web application that implements an intelligent email dispatch pipeline using a custom priority queue data structure. Emails are scheduled and dispatched based on priority level, scheduled execution time, and creation timestamp — powered by **Neon PostgreSQL** (cloud database) and the **Brevo HTTP API** (transactional email), deployable on **Render.com** with zero-configuration.

---

## Live Demo

> Deployed on [Render.com](https://render.com) — see [Deployment](#deployment) section below.

---

## Technical Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 17, Spring Boot 3.3.2 |
| **Modules** | Spring Web (MVC), Spring Security, Spring Data JPA, Spring Scheduler |
| **Database** | [Neon PostgreSQL](https://neon.tech) (serverless cloud PostgreSQL) |
| **Email API** | [Brevo](https://brevo.com) Transactional Email HTTP API (300 free emails/day) |
| **Frontend** | HTML5, Vanilla CSS, Bootstrap 5, Thymeleaf |
| **Build Tool** | Maven |
| **Deployment** | [Render.com](https://render.com) |

---

## Core DSA Scheduling Logic

The core scheduling algorithm runs in the background using Java's `PriorityBlockingQueue` (thread-safe) ordered by a custom `PriorityComparator`.

### Email Ordering Algorithm
1. **Priority Rank** — `HIGH` > `MEDIUM` > `LOW`
2. **Scheduled Time** — Earlier delivery time executes first (if priorities are equal)
3. **Creation Time (tie-breaker)** — Older email is processed first (if both priority and scheduled time match)

### Email Status Lifecycle

```
DRAFT → PENDING → QUEUED → PROCESSING → SENT
                                      ↘ RETRIED → FAILED
                  ↘ CANCELLED
```

| Status | Description |
|--------|-------------|
| `DRAFT` | Saved draft; not yet scheduled |
| `PENDING` | Scheduled and waiting for execution time |
| `QUEUED` | Picked up by the scheduler and loaded into the Priority Queue |
| `PROCESSING` | Currently dispatching via Brevo API |
| `SENT` | Successfully delivered |
| `RETRIED` | Dispatch failed; rescheduled with exponential backoff |
| `FAILED` | Permanently failed — maximum retries reached |
| `CANCELLED` | Cancelled by user before dispatch |

---

## Getting Started (Local Development)

### Prerequisites
- Java 17+
- Maven 3.9+
- A [Neon](https://neon.tech) free account (PostgreSQL)
- A [Brevo](https://brevo.com) free account (email API)

### 1. Clone the Repository
```bash
git clone https://github.com/chetan1364/Email-Scheduler-Using-Priority-Queue.git
cd Email-Scheduler-Using-Priority-Queue
```

### 2. Configure application.properties
Copy the template and fill in your credentials:
```bash
cp src/main/resources/application.properties.template src/main/resources/application.properties
```

Edit `application.properties`:

```properties
# Neon PostgreSQL — paste your JDBC URL from neon.tech
spring.datasource.url=jdbc:postgresql://ep-xxx.us-east-1.aws.neon.tech/neondb?sslmode=require
spring.datasource.driver-class-name=org.postgresql.Driver

# Brevo HTTP API — from app.brevo.com → SMTP & API → API Keys
brevo.api.key=xkeysib-your-api-key
brevo.sender.email=your-verified@email.com
brevo.sender.name=PriorityMail Scheduler
```

> **Note**: `application.properties` is in `.gitignore` — your real credentials will never be committed.

### 3. Run the Application
```bash
mvn spring-boot:run
```
Navigate to `http://localhost:8080` in your browser.

### Seeded Login Credentials

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@emailscheduler.com` | `admin123` |
| User | `user@emailscheduler.com` | `user123` |

---

## Deployment

This project is configured for one-click deployment on **Render.com** using the included [`render.yaml`](render.yaml).

### Step 1 — Set Up External Services

**Neon PostgreSQL** ([neon.tech](https://neon.tech)):
1. Create a project → go to **Connection Details**
2. Select **JDBC** format and copy the URL:
   ```
   jdbc:postgresql://ep-xxx.region.aws.neon.tech/neondb?sslmode=require
   ```

**Brevo Email API** ([app.brevo.com](https://app.brevo.com)):
1. Go to **SMTP & API → API Keys** → Generate a key
2. Go to **Senders & IPs → Senders** → Add and verify your sender email

### Step 2 — Deploy on Render

1. Go to [render.com](https://render.com) → **New → Web Service**
2. Connect your GitHub repository
3. Render auto-reads `render.yaml` — confirm the settings:
   - **Build**: `mvn clean package -DskipTests`
   - **Start**: `java -Dserver.port=$PORT -jar target/email-scheduler-0.0.1-SNAPSHOT.jar`
4. In the **Environment** tab, add:

| Variable | Value |
|----------|-------|
| `NEON_DATABASE_URL` | Your Neon JDBC URL |
| `BREVO_API_KEY` | Your Brevo API key |
| `BREVO_SENDER_EMAIL` | Your verified sender email |

5. Click **Create Web Service** — first build takes ~3–5 minutes

> Hibernate auto-creates all database tables on first boot via `ddl-auto=update`.

---

## Features

- **User Authentication** — Secure signup/login with Spring Security and role-based access control (Admin / User)
- **Email Composer** — Rich HTML email editor with CC/BCC, file attachments, priority selection, and scheduled delivery date picker
- **Live Dashboard** — Real-time status summary cards, filter pipeline (status/priority/date range), and alert notification log
- **Admin Console** — Priority queue viewer, user account management, system metrics cards, and email registry table
- **Priority Queue Engine** — Custom DSA-backed `PriorityBlockingQueue` with scheduled background dispatch jobs
- **Retry Logic** — Exponential backoff retries with configurable max-retry limit
- **Brevo Integration** — HTTP API dispatch with To/CC/BCC, HTML body, and Base64-encoded file attachments
