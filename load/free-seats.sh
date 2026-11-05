#!/usr/bin/env bash
set -euo pipefail
COUNT="${1:-100000}"
OUT="$(dirname "$0")/free-seats.txt"
PGPASSWORD=booking psql -h localhost -U booking -d booking -At -v ON_ERROR_STOP=1 -c "
  select s.id
  from bookings.seats s
  join bookings.events e on e.id = s.event_id
  where not s.deleted and not e.deleted
    and not exists (select 1 from bookings.bookings b
                    where b.seat_id = s.id and not b.deleted and b.state in ('NEW', 'PAID'))
  order by random()
  limit ${COUNT};" > "$OUT.tmp"
mv "$OUT.tmp" "$OUT"
echo "free seats: $(wc -l < "$OUT" | tr -d ' ') -> $OUT"
