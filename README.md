

# FinLock: Distributed Wallet System

A backend REST API system simulating the core infrastructure of a digital payments platform, built as a portfolio project to demonstrate senior-level backend engineering skills.

Think of it as the backend of GCash or Maya handling user authentication, multi-currency wallets, atomic fund transfers, and real-time security controls.

---

## Live Demo

🌐 **Live API:** https://finlock-api.onrender.com

> Note: Free tier — may take 50 seconds to wake up on first request after inactivity.

Try it:
```bash
curl -X POST https://finlock-api.onrender.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Your Name","email":"you@example.com","password":"password123"}'
  
```


---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Security | Spring Security 6 + JWT (HMAC-SHA384) |
| Database | PostgreSQL 16 |
| Cache / Locking | Redis 7 |
| Migrations | Flyway |
| Rate Limiting | Bucket4j |
| Encryption | AES-256-GCM |
| Containerization | Docker + Docker Compose |
| CI/CD | GitHub Actions *(coming soon)* |
| Testing | JUnit 5 |

---

## Architecture

FinLock uses a **Modular Monolith** architecture, one deployable Spring Boot application split into clean, loosely-coupled modules.

```
src/main/java/com/finlock/finlock/
├── auth/           # Registration, login, JWT token management
├── wallet/         # Wallet creation, deposit, withdrawal, balance
├── transaction/    # Fund transfers, idempotency, history
├── audit/          # Immutable audit logging for all sensitive actions
└── common/
    ├── security/   # JWT filter, distributed lock service
    ├── encryption/ # AES-256-GCM converter for data at rest
    ├── exception/  # Global exception handler
    ├── ratelimit/  # Token bucket rate limiter
    └── response/   # Standardized API response wrapper
```

---

## Features

### Authentication
- User registration with BCrypt password hashing
- JWT-based stateless authentication (24-hour expiry)
- Rate limiting: 5 login attempts per minute per account
- No account enumeration — identical error messages for wrong email vs wrong password

### Wallet Management
- Create wallets per currency (PHP, USD, etc.)
- Deposit and withdrawal with `BigDecimal` precision (no floating point math near money)
- Encrypted balances at rest using AES-256-GCM
- One wallet per currency per user enforced at both DB and application layer

### Fund Transfers
- Atomic transfers between users — both wallets update or neither does
- Idempotency keys — duplicate requests return the original result without re-executing
- Redis distributed locking — prevents race conditions on concurrent transfers
- Self-transfer blocked, insufficient balance rejected with correct HTTP semantics (`422`)
- Full transaction history with per-user directional view (`SENT` / `RECEIVED`)

### Security
- OWASP Top 10 reviewed and documented — see [`docs/OWASP_REVIEW.md`](./docs/OWASP_REVIEW.md)
- OWASP ZAP scan: **66 checks passed, 0 failures** — see [`docs/zap_scan_report.html`](./docs/zap_scan_report.html)
- AES-256-GCM encryption for sensitive data at rest
- Secrets managed via environment variables (not hardcoded in config files)
- Immutable audit trail: login attempts, transfers, rate limit hits — all logged with IP and timestamp

---

## Getting Started

### Prerequisites
- Java 21
- Docker + Docker Compose
- Maven

### Run locally

```bash
# Clone the repo
git clone https://github.com/YOUR_USERNAME/finlock.git
cd finlock

# Start PostgreSQL and Redis
docker compose up -d

# Set environment variables (or use the defaults for local dev)
export JWT_SECRET=your-secret-key-at-least-32-characters
export AES_KEY=your-base64-encoded-32-byte-key

# Run the app
mvn spring-boot:run
```

### Environment Variables

| Variable | Description | Required in Production |
|---|---|---|
| `JWT_SECRET` | HMAC key for signing JWT tokens (min 32 chars) | Yes |
| `AES_KEY` | Base64-encoded 32-byte key for AES-256 encryption | Yes |
| `JWT_EXPIRATION` | Token expiry in milliseconds (default: 86400000) | No |

---

## API Reference

### Auth
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/auth/register` | Register a new user | None |
| POST | `/api/auth/login` | Login and receive JWT token | None |

### Wallets
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/wallets` | Create a wallet for a currency | JWT |
| GET | `/api/wallets/me` | Get all your wallets and balances | JWT |
| POST | `/api/wallets/deposit` | Deposit into a wallet | JWT |
| POST | `/api/wallets/withdraw` | Withdraw from a wallet | JWT |

### Transfers
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/transfers` | Transfer funds to another user | JWT + Idempotency-Key header |
| GET | `/api/transfers` | Get your full transaction history | JWT |

### Transfer Request Example
```bash
curl -X POST http://localhost:8080/api/transfers \
  -H "Authorization: Bearer <your_token>" \
  -H "Idempotency-Key: unique-request-id-here" \
  -H "Content-Type: application/json" \
  -d '{
    "recipientEmail": "maria@example.com",
    "currency": "PHP",
    "amount": 500.00
  }'
```

---

## Security Highlights

### Distributed Locking (Race Condition Prevention)
Two simultaneous transfer requests targeting the same wallet are safely serialized using Redis locks. See the live proof: [`docs/CONCURRENCY_TEST.md`](./docs/CONCURRENCY_TEST.md)

### Idempotency
Every transfer requires a client-generated `Idempotency-Key` header. Sending the same key twice returns the original result without re-executing — preventing double charges from network retries.

### Encryption at Rest
Wallet balances are stored as AES-256-GCM ciphertext in PostgreSQL. Direct database access reveals only Base64-encoded ciphertext, not readable financial figures.

### Audit Trail
Every sensitive action (login, transfer, rate limit hit) is recorded immutably with timestamp and IP address.

---

## Documentation

| Document | Description |
|---|---|
| [`docs/OWASP_REVIEW.md`](./docs/OWASP_REVIEW.md) | Manual OWASP Top 10 review with verified findings |
| [`docs/zap_scan_report.html`](./docs/zap_scan_report.html) | OWASP ZAP automated security scan (66 pass, 0 fail) |
| [`docs/CONCURRENCY_TEST.md`](./docs/CONCURRENCY_TEST.md) | Live concurrency test proving distributed lock correctness |

---


## What I Learned Building This

This project was specifically designed to go beyond basic CRUD and tackle the problems that make financial systems genuinely hard:

- **Why floating point fails for money** — and how `BigDecimal` fixes it
- **Why `@Transactional` alone isn't enough** for concurrent balance mutations
- **How distributed locks prevent race conditions** across multiple simultaneous requests
- **Why idempotency keys exist** — the subtle difference between "the same request" and "the same operation"
- **How JWT signature validation actually works** — and what "tampered token" means at the byte level
- **How AES-GCM differs from AES-CBC** — authenticated encryption vs confidentiality only
- **What an OWASP audit actually looks like** in practice, not just in theory

---

*Built with Java 21 + Spring Boot 3.5 + PostgreSQL + Redis*
