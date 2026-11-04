BEGIN;
TRUNCATE bookings.bookings, bookings.seats, bookings.events RESTART IDENTITY;
SELECT setseed(0.42);


insert into bookings.events (name, active, deleted, event_date, created_at, updated_at)
with t
         as (select NOW() - interval '10' year +
                    (random() * ((NOW() + interval '10' year) - (NOW() - interval '10' year))) as random_date,
                    random() > 0.1                                                             as active,
                    random() > 0.95                                                            as deleted,
                    n                                                                          as n
             from generate_series(1, 10_000) as n),
     t2 as (select *, least(random_date, now()) - random() * interval '15 days' as created_at
            from t)
select 'event-' || t2.n,
       t2.active,
       t2.deleted,
       t2.random_date,
       t2.created_at,
       t2.created_at
from t2;

with t as (select least(e.created_at + random() * interval '1' day, now()) as created_at,
                  e.id                                                     as event_id,
                  n                                                        as n
           from bookings.events e
                    CROSS JOIN LATERAL generate_series(1, 200) as n)
insert
into bookings.seats (number, event_id, price, deleted, created_at, updated_at)
select 'seat-' || t.n,
       t.event_id,
       round((random() * 100 + 1)::numeric, 2),
       random() > 0.95,
       t.created_at,
       t.created_at
from t;

with t as (select e.id                       as event_id,
                  random()                   as event_popularity,
                  e.event_date               as event_date,
                  least(e.event_date, now()) as window_end
           from bookings.events e),
     s as (select se.id                        as seat_id,
                  t.event_date                 as event_date,
                  se.created_at                as window_start,
                  t.window_end - se.created_at as window_len,
                  case
                      when t.window_end > se.created_at
                          then floor(44 * power(t.event_popularity, 3))::int
                      else 0 end               as cnt
           from bookings.seats se
                    join t on t.event_id = se.event_id),
     b as (select s.seat_id,
                  s.event_date,
                  s.cnt,
                  k                                                            as k,
                  s.window_start + s.window_len * ((k - 1 + random()) / s.cnt) as created_at,
                  s.window_start + s.window_len * (k::float8 / s.cnt)          as slot_end,
                  random()                                                     as r
           from s
                    cross join lateral generate_series(1, s.cnt) as k),
     c as (select b.*,
                  case
                      when b.k < b.cnt then 'CANCELLED'
                      when b.r > 0.8 then 'PAID'
                      when b.r > 0.4 and b.event_date > now() then 'NEW'
                      else 'CANCELLED' end as state
           from b)
insert
into bookings.bookings (state, seat_id, email, deleted, created_at, updated_at)
select c.state,
       c.seat_id,
       'john.doe' || (1 + floor(random() * 100_000)::int) || '@example.com',
       random() > 0.95,
       c.created_at,
       case
           when c.state = 'NEW' then c.created_at
           else c.created_at + random() * (c.slot_end - c.created_at) end
from c;

COMMIT;