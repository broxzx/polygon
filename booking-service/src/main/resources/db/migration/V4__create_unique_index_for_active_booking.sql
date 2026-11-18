CREATE UNIQUE INDEX idx__unique_active_booking ON bookings.bookings (seat_id)
    WHERE state in ('NEW', 'PAID');
