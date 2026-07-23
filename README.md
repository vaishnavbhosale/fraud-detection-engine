# FraudShield — AI-Powered Fraud Detection Engine

> Real-time transaction fraud detection system built with Java Spring Boot, combining rule-based logic with Groq-hosted LLaMA 3.3 70B for human-readable fraud explainability.
## Live API
[Base URL](https://fraud-detection-11qq.onrender.com)    
[Swagger UI](https://fraud-detection-11qq.onrender.com/swagger-ui.html)

## Architecture

```
POST /api/transactions
        │
        ▼
┌─────────────────┐
│   JWT Filter    │  ← Validates Bearer token on every request
└────────┬────────┘
         │
         ▼
┌──────────────────────┐
│     Rule Engine      │  ← Strategy Pattern: fail-fast, stops at first suspicious rule
│  ┌─────────────────┐ │
│  │  AmountRule     │ │  ← Flags transactions above ₹50,000
│  │  CountryRule    │ │  ← Flags new country for an account
│  │  VelocityRule   │ │  ← Flags 5+ transactions in 5 minutes
│  │  GraphRule      │ │  ← Flags suspicious merchant/fan-in/circular patterns
│  └─────────────────┘ │
└──────────┬───────────┘
           │
      ┌────┴─────┐
      │          │
      ▼          ▼
  APPROVED    FLAGGED
      │          │
      ▼          ▼
   Save TX   Groq AI Analysis
                │
                ▼
          FraudLog saved
          (risk score, category,
           explanation, recommendation)
                │
                ▼
          Risk Score ≥ 7?
                │
                ▼
          Email Alert fired
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Security | Spring Security + JWT (jjwt) |
| Database | PostgreSQL + Spring Data JPA + Hibernate |
| AI | Groq API (llama-3.3-70b-versatile) |
| Email | JavaMailSender (SMTP) |
| Validation | Bean Validation (Jakarta) |
| Documentation | Swagger UI (springdoc-openapi) |
| Build | Maven |

---

## Features

- **Rule Engine** — Strategy Pattern with 4 independent fraud rules, each implementing a `FraudRule` interface. New rules added without modifying existing engine code (Open/Closed Principle)
- **Sliding Window Velocity Check** — detects card-testing fraud by counting transactions in a rolling 5-minute window using Spring Data JPA derived queries
- **Graph Analysis** — detects suspicious merchant clustering, fan-in (money mule), and circular transaction patterns using JPQL aggregate queries on existing PostgreSQL data
- **AI Explainability** — Groq-hosted LLaMA 3.3 70B analyzes flagged transactions returning structured risk scores (1–10), fraud categories, and natural language explanations for compliance audit trails
- **JWT Authentication** — stateless token-based auth via Spring Security, server always determines fraud status regardless of client input
- **Real-time Alerts** — email notifications via JavaMailSender when AI risk score exceeds threshold of 7
- **Analytics Dashboard** — aggregate fraud stats, flagged percentage, and breakdown by triggered rule
- **Global Exception Handling** — `@RestControllerAdvice` returns clean JSON error responses with correct HTTP status codes for all error scenarios
- **Input Validation** — Bean Validation (`@Valid`, `@NotBlank`, `@Positive`) on all transaction fields with meaningful error messages
- **API Documentation** — interactive Swagger UI with JWT auth support

---

## API Endpoints

### Auth
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| POST | `/api/auth/login` | Get JWT token | ❌ |

### Transactions
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| POST | `/api/transactions` | Submit a transaction | ✅ |
| GET | `/api/transactions/{id}/fraud-report` | Get AI fraud report | ✅ |

### Analytics
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| GET | `/api/analytics/fraud-stats` | Fraud statistics | ✅ |

### Docs
| Method | Endpoint | Description |
|---|---|---|
| GET | `/swagger-ui.html` | Interactive API docs |

---

## Fraud Rules

| Rule | Logic | Pattern Detected |
|---|---|---|
| `AmountRule` | Amount > ₹50,000 | High-value transaction |
| `CountryMismatchRule` | Country differs from account history | Account takeover / travel fraud |
| `VelocityRule` | 5+ transactions in 5 minutes (sliding window) | Card testing fraud |
| `GraphRule` | 3+ accounts → same merchant in 1 hr, OR 3+ accounts → same receiver in 30 min, OR A→B→A transfer in 24 hr | Scam vendor / money mule (fan-in) / circular fraud |

---

## Design Patterns & Principles

**Strategy Pattern — Rule Engine**
Each fraud rule is an independent class implementing the `FraudRule` interface:
```java
public interface FraudRule {
    RuleResult evaluate(Transaction tx);
}
```
Spring automatically collects all `@Component` implementations into `List<FraudRule>` — adding a new rule is a new class only, zero changes to `RuleEngine`.

**Layered Architecture**
```
Controller → Service → Repository
```
Each layer has a single responsibility. Swapping PostgreSQL for another DB only touches the Repository layer.

**Fail-Fast Evaluation**
Rule engine stops at the first suspicious result — efficient and produces a clear single-reason flag per transaction.

**Open/Closed Principle**
`RuleEngine` is closed for modification — open for extension. New fraud patterns become new classes, not edits to existing ones.

---

## Getting Started

### Prerequisites
- Java 17+
- PostgreSQL
- Maven
- Groq API key ([console.groq.com](https://console.groq.com))
- Gmail App Password (for email alerts)

### Setup

**1. Clone the repository**
```bash
git clone https://github.com/vaishnavbhosale/fraud-detection-engine.git
cd fraud-detection-engine
```

**2. Create PostgreSQL database**
```sql
CREATE DATABASE frauddb;
```

**3. Configure environment**

`application.properties` already ships in the repo pre-wired to read from environment variables — there's no `.example` file to copy. Set the following before running:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/frauddb
export DB_USERNAME=postgres
export DB_PASSWORD=YOUR_PASSWORD

export GROQ_API_KEY=YOUR_GROQ_KEY

export MAIL_USERNAME=YOUR_GMAIL
export MAIL_PASSWORD=YOUR_APP_PASSWORD

export JWT_SECRET=YOUR_JWT_SECRET_MIN_32_CHARS
```

`jwt.expiration`, `alert.email` (defaults to `MAIL_USERNAME`), and the admin login (`admin` / `admin123`) are hardcoded in `application.properties` and can be edited there directly if you want to change them.

**4. Run**
```bash
mvn spring-boot:run
```

**5. Open Swagger UI**
```
http://localhost:8080/swagger-ui.html
```

---

## Testing the API

**Step 1 — Get JWT token**
```
POST /api/auth/login
Body: { "username": "admin", "password": "admin123" }
```

**Step 2 — Submit a suspicious transaction**
```
POST /api/transactions
Authorization: Bearer <token>

{
  "accountId": "ACC001",
  "receiverAccountId": "ACC999",
  "amount": 95000.00,
  "currency": "INR",
  "merchant": "Unknown Vendor",
  "country": "US",
  "timestamp": "2026-06-27T10:30:00"
}
```

**Step 3 — Get AI fraud report**
```
GET /api/transactions/{id}/fraud-report
Authorization: Bearer <token>
```

**Step 4 — Check analytics**
```
GET /api/analytics/fraud-stats
Authorization: Bearer <token>
```

**Step 5 — Test validation**
```
POST /api/transactions with "amount": -500
→ 400 Bad Request: "amount: Amount must be greater than 0"

GET /api/transactions/9999/fraud-report
→ 404 Not Found: "Transaction not found with id: 9999"
```

---

## Project Structure

```
src/main/java/com/vaishnav/fraud_detection/
├── controller/
│   ├── AuthController.java
│   ├── TransactionController.java
│   ├── AnalyticsController.java
│   └── HomeController.java         ← GET / health-check landing route
├── service/
│   ├── TransactionService.java
│   ├── AIAnalysisService.java
│   ├── GraphAnalysisService.java
│   └── AlertService.java
├── rules/
│   ├── FraudRule.java              ← Interface
│   ├── RuleEngine.java
│   ├── RuleResult.java
│   ├── AmountRule.java
│   ├── CountryMismatchRule.java
│   ├── VelocityRule.java
│   └── GraphRule.java
├── model/
│   ├── Transaction.java
│   ├── FraudLog.java
│   ├── AIFraudReport.java
│   ├── LoginRequest.java
│   ├── ErrorResponse.java
│   ├── FraudStatsResponse.java
│   └── TransactionStatus.java
├── repository/
│   ├── TransactionRepository.java
│   └── FraudLogRepository.java
├── security/
│   ├── JwtUtil.java
│   ├── JwtFilter.java
│   └── SecurityConfig.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── InvalidTransactionException.java
└── config/
    ├── AppConfig.java
    └── SwaggerConfig.java
```
---

## Author

**Vaishnav Bhosale**
[GitHub](https://github.com/vaishnavbhosale) · [LinkedIn](https://www.linkedin.com/in/vaishnavbharatbhosale/) ·
[GMail](vaishnavbharatbhosale@gmail.com)