-- Тестовые данные для локальной ручной проверки. НЕ миграция: в прод не попадает.
-- ВНИМАНИЕ: полностью очищает таблицы bookings.* и сбрасывает id.
-- Запуск: docker compose exec -T postgres psql -U booking -d booking -v ON_ERROR_STOP=1 < scripts/seed-dev.sql

BEGIN;

TRUNCATE bookings.bookings, bookings.seats, bookings.events RESTART IDENTITY CASCADE;

-- id после RESTART IDENTITY предсказуемы: 1, 2, 3
INSERT INTO bookings.events (name, event_date, deleted) VALUES
    ('Concert: Ilya Live',  NOW() + INTERVAL '30 days', FALSE), -- 1: места в разных состояниях
    ('Empty Hall',          NOW() + INTERVAL '60 days', FALSE), -- 2: без мест      → 200 []
    ('Cancelled Festival',  NOW() + INTERVAL '90 days', TRUE);  -- 3: удалено       → 404

INSERT INTO bookings.seats (event_id, number, price, deleted) VALUES
    (1, 'A-1', 49.90, FALSE), -- 1: бронь PAID                    → occupied
    (1, 'A-2', 49.90, FALSE), -- 2: бронь NEW                     → occupied
    (1, 'A-3', 49.90, FALSE), -- 3: только отменённая бронь       → свободно
    (1, 'A-4', 79.00, FALSE), -- 4: Аня отменила, Борис оплатил   → occupied, одна строка
    (1, 'A-5', 79.00, FALSE), -- 5: броней нет                    → свободно
    (1, 'A-6', 79.00, TRUE);  -- 6: удалённое место               → не показывать

INSERT INTO bookings.bookings (seat_id, state, email) VALUES
    (1, 'PAID',      'carl@example.com'),
    (2, 'NEW',       'dina@example.com'),
    (3, 'CANCELLED', 'egor@example.com'),
    (4, 'CANCELLED', 'anya@example.com'),
    (4, 'PAID',      'boris@example.com');

COMMIT;

-- Ожидаемо для GET /events/1/seats: A-1..A-5, occupied = true, true, false, true, false
