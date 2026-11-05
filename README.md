# booking-lab

A ticket-booking service I built as a **performance lab**. The service itself is deliberately simple. The point of the project is to load it, measure where it breaks, explain why, fix it and measure again.

Every number below comes from a recorded run. The raw k6 output is in [`load/`](load/).

## Stack

Java 21 · Spring Boot 4.1 · PostgreSQL 17 · Flyway · Kafka (KRaft) · Testcontainers · Micrometer · Prometheus · Grafana · k6 · Docker Compose

## Domain

```
events 1 ── * seats 1 ── * bookings (state: NEW | PAID | CANCELLED)
```

A seat is free when it has no active (`NEW` / `PAID`) booking. I keep the booking **history** instead of a `seats.available` flag, so there is a single source of truth and no need to keep two tables in sync. The trade-offs are recorded in [ADR-0001](booking-service/docs/adr/0001-seat-occupancy.md).

| Endpoint | What it does |
|---|---|
| `GET /events/{id}/seats` | Seats of an event, each with an `occupied` flag computed from active bookings |
| `POST /bookings` | Books a seat: `201`, `409` if the seat is taken, `404` if the seat or event is deleted |
| `GET /bookings/{id}` | One booking |

## Run it

```bash
docker compose up -d                       # Postgres, Kafka, Kafka UI, Prometheus, Grafana
cd booking-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=load
```

- Grafana: http://localhost:3000. The provisioned **booking-service RED** dashboard shows rate, errors, p50/p95/p99, the HikariCP pool and GC.
- Prometheus: http://localhost:9090
- Tests: `./mvnw test` runs integration tests against real Postgres and Kafka in Testcontainers.

### Load test

```bash
k6 run -e READ_EVENT_ID=1 -e FIRST_SEAT=1 load/bookings.js
```

The test uses the open model (`ramping-arrival-rate`): k6 sends N requests per second whether the service keeps up or not, the way real traffic does. Reads ramp in steps up to 12,000 req/s, and bookings run at 10% of the read rate.

For the 20M-row dataset there is a narrower profile: a 2-minute JIT warm-up, then 1,000 → 1,500 → 2,000 → 2,500 req/s with p95 reported per step. The script refreshes the list of free seats, resets `pg_stat_statements` and saves the output:

```bash
load/run-steps.sh <event_id> <name>
```

## Findings

In every run k6, the app, Postgres and Docker share one machine's CPU, so the absolute numbers are lower than on separate machines. Findings 1 and 2 ran on my desktop PC and finding 3 on a MacBook Air M3. Compare numbers within a finding, not across them.

### 1. The query planner thought the SSD was a spinning disk

**Symptom.** The service saturated at **~2,000 req/s**. Above that, p95 jumped to ~200 ms, the Hikari pool (10 connections) was fully busy, and at 4,000 req/s the throughput *dropped*: 189 requests were waiting for a connection, and Postgres was using ~90% CPU.

**Cause.** `EXPLAIN ANALYZE` showed that the `EXISTS (active booking)` check ran as a *hashed SubPlan*, which is a **sequential scan of the whole `bookings` table on every request** (10.4 ms per query). Adding indexes did not change the plan. Postgres's default `random_page_cost = 4` models a spinning disk, so the planner judged index lookups to be too expensive.

**Fix.** One line in `docker-compose.yml`: `random_page_cost=1.1`, the usual value for SSD storage.

| | Before | After |
|---|---|---|
| Seats query (`EXPLAIN ANALYZE`) | 10.4 ms (seq scan) | **0.21 ms** (index, ~50×) |
| p95 at 2,000 req/s | ~200 ms | **~1 ms** |
| Saturation point (knee) | ~2,000 req/s | **~8,800 req/s** |
| Healthy ceiling (p95 ≈ 1 ms) | below 2,000 req/s | **~6,600 req/s** |

Little's law checked out on both sides. Before the fix, 10 connections / 2,160 req/s ≈ 4.6 ms of connection hold time per request, which is the ceiling we observed. The new bottleneck at ~8,800 req/s is the machine's CPU (~81%) together with the pool of 10 connections. Pushing further, to 13,200 req/s, shows overload collapse: throughput falls from a ~10,100 peak to ~8,300.

### 2. Turning off Open Session in View *doubled* connection acquisitions

Disabling `spring.jpa.open-in-view` did not improve latency. It also raised connection acquisitions per request from **1.0 to 1.9**: without OSIV and without a transaction, the two SQL statements on the read path each borrowed their own connection. With `@Transactional(readOnly = true)` on the read path it went back to **1.0** (measured as `hikaricp_connections_usage_seconds_count / http_server_requests_seconds_count`), while OSIV stays off.

### 3. Volume: 20.9M bookings

[`load/seed-volume.sql`](load/seed-volume.sql) generates a reproducible dataset (`setseed`):

- 10,000 events across ±10 years, of which 90% are active and 5% deleted;
- 2,000,000 seats;
- **20,887,595 bookings**. Popularity is skewed, with a few hot events and a long cold tail (up to 43 bookings per seat). Each seat has a consistent history: bookings fall into non-overlapping time slots, only the last one can be active, and each cancellation happens before the next booking.

| Step | Time |
|---|---|
| 2M seats | 9.2 s |
| 20.9M bookings | 99 s (~210k rows/s) |

Six invariants were checked at full volume, with 0 violations: at most one active booking per seat, the active booking is the latest, every booking lies within its seat's window, `updated_at` is valid, cancellations happen before the next booking, and there is no unpaid booking for a past event.

On disk, `bookings` is 1.85 GB of heap plus 615 MB of indexes. The `seat_id` index is only 168 MB against the 447 MB primary key, because B-tree deduplication (PG 13+) collapses repeated keys.

The first generator run produced only 10.7M rows. `least(event_date - x, now())` had clamped every future event's `created_at` to the same `now()`, which is constant inside a transaction, so those events got a zero-length booking window. A prediction of the total (~21M) is what caught it.

#### What the volume costs

I compared two events in the same database, with the same number of seats and the same JSON size. The hot event has ~43 bookings per seat and the cold one almost none, so the only difference is how many booking rows the `EXISTS` subplan reads. Both runs include bookings at 10% of the read rate, all answered `201`.

| | Hot event | Cold event |
|---|---|---|
| p95 at 1,000 req/s | 46 ms | **1.7 ms** |
| p95 at 1,500 req/s | 1.35 s | **48 ms** |
| p95 at 2,000 req/s | 2.37 s | 1.16 s |
| Peak read throughput | ~1,460 req/s | **~2,080 req/s** |
| Mean SQL time | 3.42 ms | 0.74 ms |
| Pages per query | 1,048 | 782 |

- **One query is the whole problem.** In the hot run the seats query took ~97% of all database time.
- **The limit is CPU, not the pool.** At the hot event's knee the machine's CPU was at ~100% and Hikari at 10/10. The pool alone would allow ~2,900 req/s (10 connections / 3.42 ms). CPU contention stretches each query, and that fills the pool, so the full pool is a symptom. Above the knee throughput collapses, from ~1,460 to ~900 req/s.
- **My prediction was wrong.** I wrote down ~30 ms p95 at 2,000 req/s before measuring and got 2.37 s. My estimate covered the SQL only, and 2,000 req/s is above capacity, where queueing decides the latency.
- **Cheaper SQL has a limit.** SQL became ~4.6× cheaper on the cold event, but the ceiling rose only ~1.4×. The rest of the CPU goes to Java, k6 and the bookings, so an index on the hot event should not be expected to go far beyond the cold event's ceiling on this machine.

A dirty run is worth recording too. An earlier cold run peaked at ~2,400 req/s, but 99.4% of its bookings got `409` because a wrapper script had not refreshed the seat list. Without the inserts the service did less work, so I discarded it and reran.

**Next:** `EXPLAIN (ANALYZE, BUFFERS)` for both events, to explain why even an event with no bookings reads ~4 pages per seat, then indexes with a before/after run.

## Roadmap

- [x] Service, migrations, integration tests
- [x] Observability (RED dashboard, pool metrics) and baseline load tests
- [x] Planner fix: ~4× throughput
- [x] 20M-row dataset, `pg_stat_statements`
- [ ] Query plans and indexes at volume (`EXPLAIN (ANALYZE, BUFFERS)`, composite, covering and partial indexes)
- [ ] **Double booking.** The current check-then-insert lets two concurrent requests book the same seat (see ADR-0001). I will reproduce it under load and compare locking, a constraint and isolation levels by throughput, error rate and p99.
- [ ] Transactional outbox + Kafka consumers
