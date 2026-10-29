WITH ev AS (
    INSERT INTO bookings.events (name, event_date)
        VALUES ('load-test', now() + interval '30 days')
        RETURNING id)
INSERT
INTO bookings.seats (number, event_id, price)
SELECT 'L-' || n, ev.id, 100.00
FROM ev,
     generate_series(1, 200) AS n;

WITH ev AS (
    INSERT INTO bookings.events (name, event_date)
        VALUES ('load-test', now() + interval '30 days')
        RETURNING id)
INSERT
INTO bookings.seats (number, event_id, price)
SELECT 'L-' || n, ev.id, 100.00
FROM ev,
     generate_series(1, 250_000) AS n;