# FraudShield — AI-Powered Fraud Detection Engine

> Real-time transaction fraud detection built with Java Spring Boot. Six weighted rules decide APPROVED / FLAGGED / BLOCKED, and a Groq-hosted LLaMA 3.3 70B model writes the human-readable explanation. **The rules decide, the AI only explains.**

## Live API

[Base URL](https://fraudshield-5cu6.onrender.com)
[Swagger UI](https://fraudshield-5cu6.onrender.com/swagger-ui.html)
[Web demo](https://fraudshield-5cu6.onrender.com/app.html)

The demo runs on a free Render instance, so the first request after a quiet period can take 30–60 seconds while it wakes up.

---

## Web demo

A small single-page frontend is served by the Spring Boot app itself, so there is no separate frontend to build or deploy. It lives in `src/main/resources/static/app.html` and is opened at `/app.html`.

What it does:
- Log in with the demo admin account (the fields are pre-filled).
- Send a transaction and see the result as a colored badge: green APPROVED, amber FLAGGED, red BLOCKED.
- For flagged or blocked transactions it also shows the rules that fired, the rule score, and the AI risk score, category, explanation and recommendation.
- Live statistics: totals, flagged percentage and a count per rule.

Things to try:

| Send | Expected |
|---|---|
| Amount 500, a new account | APPROVED |
| Amount 95000 | FLAGGED (amount rule, 30 points) |
| Amount 95000 about 7 times quickly on the same account | BLOCKED (amount + velocity, 60 points) |

Notes: the login token is kept only in memory, so closing the tab logs you out. Text coming from the server is added to the page with `textContent`, not `innerHTML`, so a malicious merchant name cannot inject script. Every click sends a fresh `Idempotency-Key`.

---

## How a transaction is processed

```
POST /api/transactions   (optional header: Idempotency-Key)
        │
        ▼
 JWT filter ─────────────► 401/403 if the token is missing or invalid
        │
        ▼
 Request DTO + validation ─► 400 if invalid (clients cannot send id, status or timestamp)
        │
        ▼
 Per-account lock ─────────► one request per account at a time (stops the velocity race)
        │
        ├─ Idempotency-Key already used? ──► return the original transaction
        │
        ▼
 Rule engine: runs ALL rules, adds up their weights
        │
        ▼
   score < 30 ─► APPROVED     30–59 ─► FLAGGED     60+ ─► BLOCKED
        │
        ▼
 Save transaction (server sets the timestamp)
        │
        ▼  (FLAGGED or BLOCKED only)
 Groq AI analysis ─► sanitized prompt, validated answer, timeouts, safe fallback
        │
        ▼
 FraudLog saved: rule names, rule score, AI risk score, category, explanation
        │
        ▼
 Email alert if AI risk score ≥ 7  OR  the rules say BLOCKED
```

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Security | Spring Security + JWT (jjwt) |
| Database | PostgreSQL + Spring Data JPA + Hibernate |
| AI | Groq API (llama-3.3-70b-versatile) |
| Email | JavaMailSender (SMTP) |
| Validation | Bean Validation (Jakarta) |
| Docs | Swagger UI (springdoc-openapi) |
| Tests | JUnit 5, Mockito, Spring Boot Test, H2 (in-memory, tests only) |
| Load testing | Apache JMeter |
| Build | Maven |

---

## Fraud rules and scoring

Every rule runs on every transaction. A rule that fires adds its weight to the total score.

| Rule | Name | Weight | Fires when |
|---|---|---|---|
| `AmountRule` | `AMOUNT` | 30 | amount > ₹50,000 |
| `VelocityRule` | `VELOCITY` | 30 | 5 or more earlier transactions by the account in the last 5 minutes |
| `CountryMismatchRule` | `COUNTRY_MISMATCH` | 20 | country differs from the account's previous transaction |
| `MerchantRule` | `MERCHANT` | 15 | 3+ different accounts already have FLAGGED/BLOCKED payments to this merchant in the last hour |
| `FanInRule` | `FAN_IN` | 25 | 3+ different accounts sent money to the same receiver in the last 30 minutes |
| `CircularRule` | `CIRCULAR` | 40 | money went A → B and back B → A within 24 hours |

| Total score | Status |
|---|---|
| 0–29 | `APPROVED` |
| 30–59 | `FLAGGED` |
| 60 or more | `BLOCKED` |

Why weights instead of "first rule wins": a transaction that breaks three rules is more dangerous than one that breaks one, and weak signals (a popular merchant, a country change) should only matter when they add up with others. For example, a country change alone scores 20 and stays approved, but a large amount (30) plus a country change (20) is flagged.

**A false-positive fix worth knowing about:** the first version flagged any merchant paid by 3+ accounts in an hour, which flags every popular shop. The merchant check now only counts payments that were *already* flagged or blocked, and it carries a low weight.

The rule weights are starting values chosen by reasoning about how strong each signal is. They are **not** tuned on labeled fraud data (see Limitations).

---

## Correctness and security work

| Problem found | Fix | Proof |
|---|---|---|
| **Mass assignment:** the API bound JSON straight into the database entity, so a client could send `"id": 1` and overwrite an existing row, or set its own `status` and `timestamp` | Separate `TransactionRequest` DTO; the server sets id, status and timestamp | `TransactionBindingTest`, `TransactionServiceTest` |
| **Velocity could be dodged** with old client timestamps | Timestamp is set by the server | `TransactionServiceTest` |
| **Race condition:** 10 parallel requests for one account were all approved when the limit is 5, because "count" and "save" were separate steps | Per-account lock around count → decide → save | `VelocityConcurrencyTest` (10 parallel requests → exactly 5 approved) |
| **Duplicate submissions** created duplicate transactions | `Idempotency-Key` header, checked inside the lock, plus a unique database constraint as a safety net | `IdempotencyTest` (sequential and 10 parallel requests) |
| **Prompt injection:** the merchant name went straight into the AI prompt | Text sanitized and length-limited, prompt marks user fields as data not instructions | `PromptSanitizerTest`, `AIAnalysisServiceTest` |
| **Untrusted AI output:** a missing score crashed the request with a `NullPointerException` | AI answer is validated (score 1–10, allowed recommendation, length limits); a bad answer falls back to a safe default | `AIReportValidatorTest` |
| **AI call could hang** | Connect (3 s) and read (8 s) timeouts | `RestTemplateTimeoutTest` |
| **A fooled AI could silence the alert** | A BLOCKED transaction sends the alert regardless of the AI score | `TransactionServiceTest` |

---

## Performance (JMeter)

Test plan: `evaluation/load-test.jmx` — 20 virtual users, each logs in once and then sends 50 transactions, random accounts and amounts.

| Metric | Result |
|---|---|
| Transactions sent | 1,000 |
| Errors | 0.00% |
| Throughput | ~138 requests/second |
| Average response time | 84 ms |
| Median | 65 ms |
| 95th percentile | 188 ms |
| 99th percentile | 343 ms |
| Slowest request | 1,368 ms |

**Read these numbers with care:** JMeter, the app and PostgreSQL all ran on the same laptop. Almost every request was approved (small amounts, a different account each time), so the slow AI path and the per-account lock under contention were **not** exercised. Treat this as a baseline, not a capacity claim.

---

## Testing

```bash
mvn test
```

49 automated tests:

- **Unit tests** for every rule, the rule engine, the scoring, the sanitizer, the AI answer validator and the alert logic
- **Integration tests** (`@SpringBootTest` with an in-memory H2 database via the `test` profile — no PostgreSQL needed to run them)
- **Concurrency tests** that fire parallel requests from a thread pool
- **A timeout test** against a deliberately slow local server

---

## API

### Auth
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/auth/login` | Get a JWT token | No |

### Transactions
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/transactions` | Submit a transaction (optional `Idempotency-Key` header) | Yes |
| GET | `/api/transactions/{id}/fraud-report` | Fraud report for a flagged/blocked transaction | Yes |

### Analytics
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/api/analytics/fraud-stats` | Totals (approved / flagged / blocked) and a count per rule | Yes |

---

## Getting started

### Prerequisites
- Java 21
- PostgreSQL
- Maven
- A Groq API key ([console.groq.com](https://console.groq.com))
- A Gmail app password (for email alerts)

### Setup

**1. Clone**
```bash
git clone https://github.com/vaishnavbhosale/fraud-detection-engine.git
cd fraud-detection-engine
```

**2. Create the database**
```sql
CREATE DATABASE frauddb;
```

**3. Set the environment variables**

`application.properties` reads everything secret from environment variables.

Linux / macOS:
```bash
export DB_URL=jdbc:postgresql://localhost:5432/frauddb
export DB_USERNAME=postgres
export DB_PASSWORD=YOUR_PASSWORD
export GROQ_API_KEY=YOUR_GROQ_KEY
export MAIL_USERNAME=YOUR_GMAIL
export MAIL_PASSWORD=YOUR_APP_PASSWORD
export JWT_SECRET=YOUR_JWT_SECRET_MIN_32_CHARS
```

Windows PowerShell:
```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/frauddb"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="YOUR_PASSWORD"
$env:GROQ_API_KEY="YOUR_GROQ_KEY"
$env:MAIL_USERNAME="YOUR_GMAIL"
$env:MAIL_PASSWORD="YOUR_APP_PASSWORD"
$env:JWT_SECRET="YOUR_JWT_SECRET_MIN_32_CHARS"
```

For a quick local try-out, `GROQ_API_KEY=dummy-key` works: the AI call fails and the app uses its safe fallback report.

**4. Run**
```bash
mvn spring-boot:run
```

**5. Open Swagger UI:** `http://localhost:8080/swagger-ui.html`

---

## Trying it out

**1. Log in**
```
POST /api/auth/login
{ "username": "admin", "password": "admin123" }
```

**2. Submit a transaction** (send the token as `Authorization: Bearer <token>`)
```
POST /api/transactions
Idempotency-Key: pay-001        ← optional; the same key twice returns the same transaction

{
  "accountId": "ACC001",
  "receiverAccountId": "ACC999",
  "amount": 95000,
  "currency": "INR",
  "merchant": "Unknown Vendor",
  "country": "IN"
}
```
The response has `status: "FLAGGED"` (amount 30 points). `id`, `status` and `timestamp` are always set by the server; sending them has no effect.

**3. Read the report:** `GET /api/transactions/{id}/fraud-report` returns the rule names that fired (`ruleNames`), the rule score (`ruleScore`), and the AI's score, category and explanation.

**4. Check the stats:** `GET /api/analytics/fraud-stats`

**5. Validation:** an `amount` of `-500` returns `400 Bad Request: "amount: Amount must be greater than 0"`.

---

## Project structure

```
src/main/java/com/vaishnav/fraud_detection/
├── controller/    AuthController, TransactionController, AnalyticsController, HomeController
├── dto/           TransactionRequest
├── service/       TransactionService, AIAnalysisService, AIReportValidator,
│                  PromptSanitizer, GraphAnalysisService, AlertService
├── rules/         FraudRule (interface), RuleEngine, RuleResult, RiskResult,
│                  AmountRule, VelocityRule, CountryMismatchRule,
│                  MerchantRule, FanInRule, CircularRule
├── model/         Transaction, FraudLog, AIFraudReport, TransactionStatus, ...
├── repository/    TransactionRepository, FraudLogRepository
├── security/      JwtUtil, JwtFilter, SecurityConfig
├── exception/     GlobalExceptionHandler, ResourceNotFoundException, InvalidTransactionException
└── config/        AppConfig (RestTemplate with timeouts), SwaggerConfig

src/test/          unit, integration, concurrency and timeout tests (H2 profile in resources/)
src/main/resources/static/   app.html (the web demo page)
evaluation/        load-test.jmx (JMeter plan)
```

Design notes: each rule is its own class implementing `FraudRule` (Strategy pattern). Spring collects every `@Component` rule into the engine, so a new rule is a new class with a name and a weight, and `RuleEngine` is not edited.

---

## Limitations (known and deliberate)

- **Detection accuracy has not been measured.** There is no labeled fraud data behind the rule weights or thresholds; they are reasoned starting values. A proper evaluation (precision and recall on a labeled dataset such as PaySim) is the next step.
- **The per-account lock works inside one running copy of the app.** With several instances behind a load balancer, the velocity race would return; the fix would be a database advisory lock or a shared lock (for example Redis).
- **Idempotency keys:** reusing a key with a *different* request body returns the first transaction instead of an error, and two requests with the same key but different accounts are only protected by the database unique constraint.
- **AI latency:** the AI call happens before the response is returned, so a flagged request can wait up to the 8-second timeout. Running it in the background would remove that wait.
- **Prompt cleaning is a mitigation, not a guarantee.** Plain words still pass through, which is why the design makes sure the AI cannot change a decision or silence an alert.
- **Stored reasons are limited to about 255 characters** (a plain text column); many long rule messages together could exceed it.
- **The admin login (`admin` / `admin123`) is a demo credential** kept in `application.properties`; a real deployment needs a user store and hashed passwords.
- **`ddl-auto=update`** is convenient for a demo; production should use migrations (for example Flyway).
- **The load test is a local baseline** (see the note under Performance).
- **The web page is a minimal demo:** it has no registration, the demo admin credentials are pre-filled, and the token is not persisted.

---

## Author

**Vaishnav Bhosale**
[GitHub](https://github.com/vaishnavbhosale) · [LinkedIn](https://www.linkedin.com/in/vaishnavbharatbhosale/) · [Email](mailto:vaishnavbharatbhosale@gmail.com)
