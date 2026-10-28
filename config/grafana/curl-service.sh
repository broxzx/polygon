echo 'Sending requests...'
while true; do curl -s localhost:8080/events/1/seats >/dev/null; done