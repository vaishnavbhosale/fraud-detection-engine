# FraudShield — AI-Powered Fraud Detection Engine

> Real-time transaction fraud detection system built with Java Spring Boot, combining rule-based logic with Google Gemini AI for human-readable fraud explainability.

---

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
┌─────────────────┐
│  Rule Engine    │  ← Strategy Pattern: runs all rules in sequence
│  ┌───────────┐  │
│  │AmountRule │  │  ← Flags transactions above ₹50,000
│  │CountryRule│  │  ← Flags new country for an account
│  │VelocityRule│ │  ← Flags 5+ transactions in 5 minutes
│  │GraphRule  │  │  ← Flags suspicious merchant/circular patterns
│  └───────────┘  │
└────────┬────────┘
         │
    ┌────┴─────┐
    │          │
    ▼          ▼
APPROVED    FLAGGED
    │          │
    ▼          ▼
 Save TX   Gemini AI Analysis
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
| AI | Google Gemini API (gemini-1.5-flash) |
| Email | JavaMailSender (SMTP) |
| Documentation | Swagger UI (springdoc-openapi) |
| Build | Maven |

---

## Features

- **Rule Engine** — Strategy Pattern with 4 independent fraud rules, each implementing a `FraudRule` interface. New rules can be added without modifying existing code (Open/Closed Principle)
- **Sliding Window Velocity Check** — detects card-testing fraud by counting transactions in a rolling 5-minute window using Spring Data JPA derived queries
- **Graph Analysis** — detects suspicious merchant clustering, fan-in (money mule), and circular transaction patterns using JPQL aggregate queries
- **AI Explainability** — Google Gemini AI analyzes flagged transactions and returns structured risk scores (1–10), fraud categories, and natural language explanations for compliance audit trails
- **JWT Authentication** — stateless token-based auth, server always determines fraud status regardless of client input
- **Real-time Alerts** — email notifications via JavaMailSender when risk score exceeds threshold
- **Analytics Dashboard** — aggregate fraud stats, flagged percentage, and breakdown by rule
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
| `VelocityRule` | 5+ transactions in 5 minutes | Card testing fraud |
| `GraphRule` | 3+ accounts → same merchant in 1 hour | Scam vendor / money mule / circular fraud |

---

## Design Patterns

**Strategy Pattern — Rule Engine**
Each fraud rule is an independent class implementing the `FraudRule` interface:
```java
public interface FraudRule {
    RuleResult evaluate(Transaction tx);
}
```
Spring automatically collects all `@Component` implementations into `List<FraudRule>` — adding a new rule requires zero changes to the engine.

**Layered Architecture**
```
Controller → Service → Repository
```
Each layer has a single responsibility. Swapping PostgreSQL for another DB only touches the Repository layer.

**Fail-Fast Evaluation**
Rule engine stops at the first suspicious result — efficient and produces clear single-reason flagging.

---

## Getting Started

### Prerequisites
- Java 17+
- PostgreSQL
- Maven
- Google Gemini API key ([aistudio.google.com](https://aistudio.google.com))
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
```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```
Fill in your credentials:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/frauddb
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD

gemini.api.key=YOUR_GEMINI_KEY

spring.mail.username=YOUR_GMAIL
spring.mail.password=YOUR_APP_PASSWORD
alert.email=YOUR_GMAIL

jwt.secret=YOUR_JWT_SECRET_MIN_32_CHARS
jwt.expiration=86400000

app.admin.username=admin
app.admin.password=admin123
```

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
```bash
POST /api/auth/login
{
  "username": "admin",
  "password": "admin123"
}
```

**Step 2 — Submit a suspicious transaction**
```bash
POST /api/transactions
Authorization: Bearer <token>

{
  "accountId": "ACC001",
  "receiverAccountId": "ACC999",
  "amount": 95000.00,
  "currency": "INR",
  "merchant": "Unknown Vendor",
  "country": "US",
  "timestamp": "2026-06-26T10:30:00"
}
```

**Step 3 — Get AI fraud report**
```bash
GET /api/transactions/{id}/fraud-report
Authorization: Bearer <token>
```

**Step 4 — Check analytics**
```bash
GET /api/analytics/fraud-stats
Authorization: Bearer <token>
```

---

## Project Structure

```
src/main/java/com/vaishnav/fraud_detection/
├── controller/
│   ├── AuthController.java
│   ├── TransactionController.java
│   └── AnalyticsController.java
├── service/
│   ├── TransactionService.java
│   ├── AIAnalysisService.java
│   ├── GraphAnalysisService.java
│   └── AlertService.java
├── rules/
│   ├── FraudRule.java          ← Interface
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
│   └── FraudStatsResponse.java
├── repository/
│   ├── TransactionRepository.java
│   └── FraudLogRepository.java
├── security/
│   ├── JwtUtil.java
│   ├── JwtFilter.java
│   └── SecurityConfig.java
└── config/
    ├── AppConfig.java
    └── SwaggerConfig.java
```

---

## Author

**Vaishnav Bhosale**
[GitHub](https://github.com/vaishnavbhosale) · [LinkedIn](https://www.linkedin.com/in/vaishnavbharatbhosale/) · vaishnavbharatbhosale@gmail.com