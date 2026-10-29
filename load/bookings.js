import http from 'k6/http';
import {check} from 'k6';
import exec from 'k6/execution';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const READ_EVENT_ID = __ENV.READ_EVENT_ID || 1;
const FIRST_SEAT = Number(__ENV.FIRST_SEAT || 1);

export const options = {
    scenarios: {
        read_seats: {
            executor: 'ramping-arrival-rate',
            exec: 'readSeats',
            startRate: 25, timeUnit: '1s',
            preAllocatedVUs: 150, maxVUs: 1500,
            stages: [
                {target: 100, duration: '30s'}, // warm-up the service
                {target: 2000, duration: '15s'}, {target: 2000, duration: '45s'},
                {target: 4000, duration: '15s'}, {target: 4000, duration: '45s'},
                {target: 6000, duration: '15s'}, {target: 6000, duration: '45s'},
                {target: 8000, duration: '15s'}, {target: 8000, duration: '45s'},
                {target: 12000, duration: '15s'}, {target: 12000, duration: '45s'},
            ],
        },
        book: {
            executor: 'ramping-arrival-rate',
            exec: 'book',
            startRate: 3, timeUnit: '1s',
            preAllocatedVUs: 15, maxVUs: 1000,
            stages: [
                {target: 10, duration: '30s'}, // warm-up the service
                {target: 200, duration: '15s'}, {target: 200, duration: '45s'},
                {target: 400, duration: '15s'}, {target: 400, duration: '45s'},
                {target: 600, duration: '15s'}, {target: 600, duration: '45s'},
                {target: 800, duration: '15s'}, {target: 800, duration: '45s'},
                {target: 1200, duration: '15s'}, {target: 1200, duration: '45s'},
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
    const res = http.get(`${BASE}/events/${READ_EVENT_ID}/seats`,
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