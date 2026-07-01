# OWASP Top 10 Security Review — FinLock

**Review Date:** July 2026
**OWASP Version:** 2021
**Reviewer:** Samantha Gwyneth Arsua

This document records a systematic review of FinLock against the OWASP Top 10 most critical web application security risks. Each item includes the specific protections in place and the verification method used to confirm them.

---

## A01: Broken Access Control | PASS

**Risk:** Users acting outside their intended permissions — accessing or modifying another user's data by guessing or supplying a different ID.

**Protections:**
- Every endpoint derives user identity from the JWT token via `@AuthenticationPrincipal`, never from client-supplied IDs in the request body or path
- `WalletService` and `TransferService` always scope all queries by `user.getId()` — it is architecturally impossible to query another user's wallet by passing a different ID

**Verification:**
Authenticated as Juan, called `GET /api/wallets/me` — response contained only Juan's wallet. Maria's wallet never appeared, regardless of any parameter manipulation.

---

## A02: Cryptographic Failures | PASS

**Risk:** Sensitive data exposed due to weak or missing encryption.

**Protections:**
- Passwords hashed with BCrypt (work factor 10) — one-way, salted, slow-by-design. Never stored in plain text.
- Wallet balances encrypted at rest with AES-256-GCM via a JPA `AttributeConverter` — transparent to the application layer
- JWT tokens signed with HMAC-SHA384

**Verification:**
Direct database query confirmed passwords stored as `$2a$10$...` BCrypt hashes. Wallet balance column showed Base64-encoded ciphertext (`DalVigfHXGf4185C3qTKhjiXGCUWph2nXQIzTmQbdwaG7g==`), not readable numbers.

---

## A03: Injection | PASS (defense in depth)

**Risk:** Untrusted input executed as SQL or other code.

**Protections:**
- All database access via Spring Data JPA / Hibernate using parameterized queries (prepared statements) — user input is never concatenated into SQL strings
- Input validation (`@Email`, `@NotBlank`, `@Pattern`) rejects malformed input at the controller layer before it reaches any query

**Verification:**
Submitted `admin@finlock.com OR 1=1--` as the email field. Response: `400 Validation failed — Invalid email format`. The injection payload was caught by `@Email` validation before reaching the database layer.

---

## A04: Insecure Design | PASS

**Risk:** Fundamental architectural flaws that can't be patched away.

**Protections:**
- Rate limiting on login (5 attempts/minute/email) — prevents brute force by design
- Redis distributed locking on transfers — prevents race conditions by design
- Idempotency keys on transfers — prevents duplicate financial operations by design
- Self-transfer blocked, insufficient balance checked server-side — business rules never trust client assumptions

**Verification:**
Concurrent curl test (Day 20): two simultaneous ₱50 transfers fired at a wallet with ₱70 balance. Exactly one succeeded; the other was correctly rejected. Final balance confirmed at ₱20, not ₱-30. See [CONCURRENCY_TEST.md](CONCURRENCY_TEST.md) for full documentation.

---

## A05: Security Misconfiguration | PASS

**Risk:** Default credentials, verbose error messages leaking internals, unnecessary features enabled.

**Protections:**
- `GlobalExceptionHandler` intercepts all exceptions — raw stack traces never reach clients
- CSRF disabled deliberately (correct for stateless JWT APIs — CSRF attacks target cookie-based sessions, which FinLock doesn't use)
- No default or hardcoded credentials anywhere in the codebase

**Verification:**
Submitted malformed JSON to `POST /api/auth/register`. Response: `400 Malformed JSON request. Please check your request body.` — no class names, file paths, line numbers, or internal details leaked.

---

## A06: Vulnerable and Outdated Components | PASS

**Risk:** Libraries with known CVEs, abandoned dependencies, major version gaps.

**Protections:**
- All dependencies pinned to explicit versions — no floating `latest` references
- Spring Boot 3.5.14 — recent, actively maintained release
- All directly-used dependencies confirmed minor-version-behind at most via `mvn versions:display-dependency-updates`

**Verification:**
`mvn versions:display-dependency-updates` showed all direct dependencies within 1-2 minor versions of current. No major version gaps. Note: this checks freshness, not CVEs — a dedicated tool like OWASP Dependency-Check would be the next step for a production audit.

---

## A07: Identification and Authentication Failures | PASS

**Risk:** Weak passwords, no brute-force protection, account enumeration, predictable tokens.

**Protections:**
- Minimum 8-character password enforced via `@Size` validation
- BCrypt password hashing (verified under A02)
- Rate limiting on login — 5 attempts per minute per email
- Generic error message: `"Invalid email or password"` — same message whether the email exists or not
- JWT tokens expire after 24 hours, signed with HMAC-SHA384

**Verification:**
Tested login with a non-existent email and with a valid email + wrong password. Both returned exactly `"Invalid email or password"` — no account enumeration possible. An attacker cannot determine which emails are registered from error messages alone.

---

## A08: Software and Data Integrity Failures | PASS

**Risk:** Tampered tokens accepted, unverified data trusted, compromised dependencies auto-applied.

**Protections:**
- JWT tokens signed with HMAC-SHA384 — any modification to header, payload, or signature invalidates the token
- AES-256-GCM provides authenticated encryption — tampered ciphertext fails the GCM integrity tag check, not just decryption
- All dependencies version-pinned (no auto-update from unverified sources)

**Verification:**
Took a valid JWT token, replaced the first character of the signature segment with `X`, and attempted to use it. Response: `HTTP/1.1 403` — token rejected correctly. The JJWT library's `verifyWith()` call detected the signature mismatch.

Note: appending a character to the *end* of the full token string is not a meaningful tampering test due to Base64URL padding behavior. Modifying actual signature bytes is the correct test.

---

## A09: Security Logging and Monitoring Failures | PASS

**Risk:** Attacks go undetected because no audit trail exists.

**Protections:**
- `AuditLogService` records all security-relevant events to the `audit_logs` table
- Events captured: `LOGIN_SUCCESS`, `LOGIN_FAILED`, `LOGIN_RATE_LIMITED`, `TRANSFER_COMPLETED`, `TRANSFER_FAILED`
- Every entry includes timestamp, IP address, and human-readable description
- Logging is fail-safe — a logging failure never blocks the actual business operation

**Verification:**
Fired 6 rapid failed login attempts. Audit log confirmed 5 `LOGIN_FAILED` entries within 500ms of each other (clearly automated), followed immediately by `LOGIN_RATE_LIMITED` — a security team monitoring this table would instantly recognize a brute-force pattern.

```
LOGIN_RATE_LIMITED | Login rate limit exceeded for email: juan@finlock.com | 2026-07-01 17:12:58
LOGIN_FAILED       | Failed login attempt for email: juan@finlock.com      | 2026-07-01 17:12:57
LOGIN_FAILED       | Failed login attempt for email: juan@finlock.com      | 2026-07-01 17:12:57
LOGIN_FAILED       | Failed login attempt for email: juan@finlock.com      | 2026-07-01 17:12:57
LOGIN_FAILED       | Failed login attempt for email: juan@finlock.com      | 2026-07-01 17:12:57
LOGIN_FAILED       | Failed login attempt for email: juan@finlock.com      | 2026-07-01 17:12:57
```

---

## A10: Server-Side Request Forgery (SSRF) | NOT APPLICABLE

**Risk:** Attacker tricks the server into fetching internal URLs on their behalf.

**Finding:**
FinLock contains no feature where user-supplied URLs are fetched server-side. There is no HTTP client in the application that accepts user input as a URL target. This attack surface simply does not exist in the current design.

This is the correct finding to document — SSRF requires a specific attack surface that FinLock does not expose.

---

## Summary

| # | Vulnerability | Status | Key Control |
|---|---|---|---|
| A01 | Broken Access Control | PASS | JWT-derived identity, no client-supplied IDs |
| A02 | Cryptographic Failures | PASS | BCrypt + AES-256-GCM + HMAC-SHA384 |
| A03 | Injection | PASS | JPA parameterized queries + input validation |
| A04 | Insecure Design | PASS | Rate limiting + distributed locking + idempotency |
| A05 | Security Misconfiguration | PASS | No stack traces leaked, stateless, no defaults |
| A06 | Vulnerable Components | PASS | All dependencies recent, minor versions only |
| A07 | Auth Failures | PASS | BCrypt + rate limiting + no account enumeration |
| A08 | Data Integrity Failures | PASS | JWT signature validation blocks tampering |
| A09 | Logging Failures | PASS | Full audit trail with brute-force pattern detection |
| A10 | SSRF | N/A | No user-supplied URL fetching in design |

**Result: 9/9 applicable controls passing. 1/10 not applicable by design.**
