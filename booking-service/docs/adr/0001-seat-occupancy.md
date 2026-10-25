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