Status: `Accepted`
Date: `2026-10-25`

# Context
Booking lab contains `seats` and `bookings` tables which represent a seat in the event and bookings for a seat respectively. 
A user is able to book and pay for a seat for a particular event. Seat should have only 1 active booking. 
Seat is deemed available if it doesn't have bookings with state `NEW` or `PAID`.

# Decision
The `bookings` table contains state (`NEW`, `PAID`, `CANCELLED`) which represents whether seat is available. 
This way we can have a history of bookings who and when booked what.
With a boolean flag we would need to keep data in the `bookings` and `seats` table consistent.

# Alternatives
1. Boolean flag `available` in the `seats` table which represents whether the seat has an active booking. 
The problem with this solution is that we need to keep data consistent between `bookings` and `seats` tables.

2. Create a counter of active bookings in the `events` table. It creates hot-row problem where many bookings querying the same event row.

3. The `seats` table references the active booking in `bookings` table. Here we don't have a log of bookings.

# Consequences
As a tradeoff we need a mechanism to prevent more than one active booking for a seat. Additionally, when the `bookings` table grows
reading seat availability will slow down in terms of performance as we need to query for all bookings with `seat_id`. 
An index on `bookings.seat_id` is expected to be sufficient; to be verified in week 3 with ~20M bookings and EXPLAIN (ANALYZE).

The current check-then-insert in createBooking allows concurrent double booking; the prevention mechanism will be chosen in week 4 by load-testing alternatives.

# Updates

## 2026-11-08 — Read performance at 20.9M bookings
The `seat_id` index alone was not enough: the planner chose a hashed subplan scanning all bookings
(`random_page_cost = 4`), and the `EXISTS` read each seat's full history. Fixed with
`random_page_cost = 1.1` and a composite index `(seat_id, state) WHERE deleted = false` (V2):
the seats query went from 1.46 ms to 0.37 ms on a hot event.

## 2026-11-17 — Double booking reproduced and fixed (first candidate)
200 concurrent bookings for one seat produced 8–10 active bookings (5 runs). With Hikari pool 20 the
result was 20, so the number of double bookings equals the pool size: the first wave of transactions
passes the `exists` check before any of them commits.
Candidate 1: `@Lock(PESSIMISTIC_WRITE)` on the seat lookup → `SELECT … FOR NO KEY UPDATE OF seats`.
Only the seat row is locked (not the event); bookings for one seat are serialised. Result: exactly
1 active booking in 5/5 runs.
Next: compare with optimistic locking (`@Version`), a unique partial index and SERIALIZABLE by
throughput, error rate and p99 before making the final decision.
## 2026-11-17 — Candidate 2: optimistic locking
`@Version` on `seats` (V3) and `@Lock(OPTIMISTIC_FORCE_INCREMENT)` on the seat lookup. A booking does
not modify the seat, so plain `OPTIMISTIC` would only re-check an unchanged version and let both
transactions commit; `FORCE_INCREMENT` turns the read into `UPDATE seats SET version = v + 1 WHERE
version = v` at flush, so only one transaction wins. No retry: the conflict surfaces at commit as
`ObjectOptimisticLockingFailureException` and is mapped to 409. Result: 1 × 201, 199 × 409, 0 × 5xx.

## 2026-11-18 — Candidate 3: partial unique index
V4: `CREATE UNIQUE INDEX CONCURRENTLY idx__unique_active_booking ON bookings (seat_id)
WHERE state IN ('NEW', 'PAID') AND deleted = false` — the same rule as the `exists` check, enforced
by the database. No lock on the seat; the `exists` check stays as a fast path to 409.
Only a violation of this index is mapped to 409 (Hibernate `ConstraintViolationException` with this
constraint name); other integrity violations stay 500. Result: 1 × 201, 199 × 409, 0 × 5xx.
Operational findings:
- `CREATE INDEX CONCURRENTLY` waits for all open transactions, including Flyway's own transactional
  advisory lock, so the migration blocks itself → `spring.flyway.postgresql.transactional-lock: false`.
  An interrupted build leaves an INVALID index that must be dropped before retrying.
- Flyway records that a migration ran, not that the schema still matches: an index dropped by hand
  left V4 marked as successful while double bookings came back.

## 2026-11-19 — Comparison under contention
`booking.concurrency = PESSIMISTIC | OPTIMISTIC | UNIQUE_ONLY` switches the seat lookup; the V4
index exists in all variants (in the first two the duplicate is rejected before the INSERT).
200 concurrent bookings for one seat, 3 runs per variant (the first, JIT warm-up, discarded);
all runs 1 × 201, 199 × 409, 0 × 5xx. `show-sql` was on, so absolute numbers are inflated.

| Variant | median | p95 |
|---|---|---|
| Pessimistic | 99–110 ms | 185–186 ms |
| Optimistic | 68–72 ms | 99–123 ms |
| Unique index only | 64–67 ms | 96–100 ms |

Pessimistic is the slowest because every request, including the 199 that end in 409, takes the
seat row lock before its check, so all of them run one by one. With optimistic locking and the
unique index the losers see the committed booking in parallel and fail fast. Optimistic locking
looks cheap only because conflicts are not retried; with retries each attempt would conflict again.

`FOR UPDATE SKIP LOCKED` was considered and rejected: a client books a specific seat, and skipping
a locked seat would report it as taken while the holder may still roll back. It fits "any free
seat" or job-queue style workloads.

Next: a mixed load (many seats plus one hot seat) to check whether pessimistic locking holds pool
connections while waiting and slows down bookings of other seats. Final decision after that run.
