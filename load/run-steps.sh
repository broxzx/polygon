#!/usr/bin/env bash
set -euo pipefail
EVENT="${1:?usage: run-steps.sh <event_id> <name> [free_seats]}"
NAME="${2:?usage: run-steps.sh <event_id> <name> [free_seats]}"
SEATS="${3:-250000}"
cd "$(dirname "$0")/.."

load/free-seats.sh "$SEATS"
PGPASSWORD=booking psql -h localhost -U booking -d booking -At -v ON_ERROR_STOP=1 \
  -c "select pg_stat_statements_reset();" > /dev/null
echo "pg_stat_statements reset"

k6 run -e PROFILE=steps -e READ_EVENT_ID="$EVENT" -e FREE_SEATS=free-seats.txt load/bookings.js \
  | tee "load/run-$(date +%F)-$NAME.txt"
