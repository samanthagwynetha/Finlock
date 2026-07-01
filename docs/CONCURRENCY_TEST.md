# Concurrency Test - Distributed Locking Verification

## Why This Test Exists

`@Transactional` alone does not prevent a race condition when two separate HTTP requests hit the same wallet at nearly the same time. Without an additional layer of protection, the following sequence is possible:

1. Request A reads wallet balance: ₱70
2. Request B reads wallet balance: ₱70 (before A has saved anything)
3. Request A checks: ₱70 ≥ ₱50 - proceeds
4. Request B checks: ₱70 ≥ ₱50 - proceeds
5. Both deduct ₱50 and save final balance could end up at ₱-30, with ₱100 transferred out of an account that only ever held ₱70

This is a classic race condition, and it is the kind of bug that has caused real financial losses in production payment systems. FinLock prevents it using a **Redis-based distributed lock**: before any transfer touches a wallet's balance, it must first acquire an exclusive lock on that wallet's ID. If a second request tries to acquire a lock already held by another in-flight transfer, it is immediately rejected with a `409 Conflict` rather than being allowed to read stale data.

## How the Lock Works

- Implemented in `DistributedLockService` using Redis's `SET key value NX` command (`setIfAbsent` in Spring Data Redis), which is atomic, Redis guarantees only one caller can successfully set a given key, even if many requests arrive at the exact same instant.
- Locks are acquired in a **consistent order** (by comparing wallet UUID strings) regardless of which wallet is the sender or receiver, to prevent deadlocks between two transfers moving money in opposite directions between the same pair of wallets.
- Locks are released in a `finally` block, guaranteeing release whether the transfer succeeds or fails (e.g. due to insufficient balance).
- Locks have a 5-second TTL as a safety net, in case a server crash ever prevents the `finally` block from running.

## The Test

**Setup:** Juan's wallet balance was confirmed at exactly ₱70.00.

**Action:** Two transfer requests, each for ₱50.00 to the same recipient, were fired at the same instant using backgrounded `curl` processes in bash:

```bash
curl -s -X POST http://localhost:8080/api/transfers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: race-test-A" \
  -d '{"recipientEmail":"maria@finlock.com","currency":"PHP","amount":50.00}' &

curl -s -X POST http://localhost:8080/api/transfers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: race-test-B" \
  -d '{"recipientEmail":"maria@finlock.com","currency":"PHP","amount":50.00}' &

wait
```

Since Juan only had ₱70, it should be mathematically impossible for both ₱50 transfers to succeed — at most one can.

## Result

**Request A** (`race-test-A`):
```json
{
  "success": false,
  "message": "Another transfer is already in progress. Please try again.",
  "data": null
}
```

**Request B** (`race-test-B`):
```json
{
  "success": true,
  "message": "Transfer completed successfully",
  "data": {
    "transactionId": "990f6567-cec3-49fe-aa01-c53d5fd7abb2",
    "recipientEmail": "maria@finlock.com",
    "amount": 50.00,
    "currency": "PHP",
    "senderNewBalance": 20.0000,
    "status": "COMPLETED"
  }
}
```

**Final balance verification (queried directly from PostgreSQL):**
```
 balance  |       email
----------+-------------------
   20.0000 | juan@finlock.com
 1030.0000 | maria@finlock.com
```

## What This Proves

- Exactly one of the two simultaneous requests succeeded; the other was correctly rejected before it could read or act on stale balance data.
- Juan's final balance (₱20.00) is exactly ₱70.00 − ₱50.00 consistent with only one transfer having executed.
- No double-spend occurred. No negative balance occurred. No money was created or destroyed.
- The system behaved correctly under genuine concurrent load, not just in sequential manual testing.

This confirms the distributed lock is doing its job: serializing access to a shared resource (a wallet's balance) across concurrent requests, which is the core guarantee any real money-movement system must provide.
