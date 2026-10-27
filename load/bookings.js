import http from 'k6/http';
import {check} from 'k6';
import exec from 'k6/execution';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const EVENT_ID = __ENV.EVENT_ID || 1;
const FIRST_SEAT = Number(__ENV.FIRST_SEAT || 1);

export const options = {
    scenarios: {
        read_seats: {
            executor: 'ramping-arrival-rate',
            exec: 'readSeats',
            startRate: 5, timeUnit: '1s',
            preAllocatedVUs: 20, maxVUs: 200,
            stages: [
                {target: 20, duration: '1m'},
                {target: 20, duration: '2m'}
            ],
        },
        book: {
            executor: 'ramping-arrival-rate',
            exec: 'book',
            startRate: 1, timeUnit: '1s',
            preAllocatedVUs: 10, maxVUs: 100,
            stages: [
                {target: 5, duration: '1m'},
                {target: 5, duration: '2m'}
            ],
        },
    },
    thresholds: {
        'http_req_failed': ['rate<0.01'],
        'http_req_duration{scenario:read_seats}': ['p(95)<200'],
        'http_req_duration{scenario:book}': ['p(95)<300'],
    }
}

export function readSeats() {
    const res = http.get(`${BASE}/events/${EVENT_ID}/seats`,
        {tags: {name: 'GET /events/{id}/seats'}});
    check(res, {'seats 200': (r) => r.status === 200});
}

export function book() {
    const seatId = FIRST_SEAT + exec.scenario.iterationInTest;
    const res = http.post(`${BASE}/bookings`,
        JSON.stringify({seatId, email: `load${seatId}@test.dk`}),
        {headers: {'Content-Type': 'application/json'}, tags: {name: 'POST /bookings'}});
    check(res, {'booked 201': (r) => r.status === 201});
}