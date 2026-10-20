CREATE SCHEMA bookings;

CREATE TABLE bookings.events
(
    id         INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name       VARCHAR                  NOT NULL,
    active     boolean                           DEFAULT TRUE NOT NULL,
    deleted    boolean                           DEFAULT FALSE NOT NULL,
    event_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE bookings.seats
(
    id         INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    number     varchar                             NOT null,
    event_id   INT REFERENCES bookings.events (id) NOT NULL,
    price      NUMERIC(10, 2)                      NOT NULL,
    deleted    boolean                                      DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE            NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE            NOT NULL DEFAULT NOW()
);
CREATE INDEX idx__seats_event_id_fk ON bookings.seats (event_id);

CREATE TABLE bookings.bookings
(
    id         INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    state      VARCHAR                            NOT NULL CHECK (state IN ('NEW', 'PAID', 'CANCELLED')),
    seat_id    INT REFERENCES bookings.seats (id) NOT NULL,
    email      VARCHAR                            NOT NULL,
    deleted    boolean                                     DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE           NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE           NOT NULL DEFAULT NOW()
);
CREATE INDEX idx__bookings_seat_id_fk ON bookings.bookings (seat_id);
