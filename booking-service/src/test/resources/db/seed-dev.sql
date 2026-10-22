BEGIN;

TRUNCATE bookings.bookings, bookings.seats, bookings.events RESTART IDENTITY CASCADE;

INSERT INTO bookings.events (name, event_date, deleted)
VALUES ('Concert: Ilya Live', NOW() + INTERVAL '30 days', FALSE),
       ('Empty Hall', NOW() + INTERVAL '60 days', FALSE),
       ('Cancelled Festival', NOW() + INTERVAL '90 days', TRUE);

INSERT INTO bookings.seats (event_id, number, price, deleted)
VALUES (1, 'A-1', 49.90, FALSE),
       (1, 'A-2', 49.90, FALSE),
       (1, 'A-3', 49.90, FALSE),
       (1, 'A-4', 79.00, FALSE),
       (1, 'A-5', 79.00, FALSE),
       (1, 'A-6', 79.00, TRUE);

INSERT INTO bookings.bookings (seat_id, state, email)
VALUES (1, 'PAID', 'carl@example.com'),
       (2, 'NEW', 'dina@example.com'),
       (3, 'CANCELLED', 'egor@example.com'),
       (4, 'CANCELLED', 'anya@example.com'),
       (4, 'PAID', 'boris@example.com');

COMMIT;