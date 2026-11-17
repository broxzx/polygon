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