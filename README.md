# Fraud Detection Engine

AI-powered transaction fraud detection system built with Java Spring Boot.

## Tech Stack
- Java 17, Spring Boot 3
- PostgreSQL, Spring Data JPA, Hibernate
- Claude AI (Anthropic) — for fraud explainability
- Docker — for deployment

## How it works
Incoming transaction → Rule Engine → AI Analysis → Alert

## Fraud Rules Implemented
- Amount threshold check (flags transactions above ₹50,000)
- Country mismatch detection (flags new countries for an account)
- Velocity check (flags 5+ transactions within 5 minutes)

## Design Patterns Used
- Strategy Pattern — each fraud rule is an independent 
  class implementing FraudRule interface
- Layered Architecture — Controller → Service → Repository

## How to Run
1. Clone the repo
2. Copy src/main/resources/application.properties.example 
   to application.properties and fill in your credentials
3. Run: mvn spring-boot:run
4. Test: POST http://localhost:8080/api/transactions
