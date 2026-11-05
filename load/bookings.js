import http from 'k6/http';
import {check} from 'k6';
import exec from 'k6/execution';
import {SharedArray} from 'k6/data';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const READ_EVENT_ID = __ENV.READ_EVENT_ID || 1;
const FIRST_SEAT = Number(__ENV.FIRST_SEAT || 1);
const PROFILE = __ENV.PROFILE || 'ramp';

const FREE_SEATS = __ENV.FREE_SEATS
    ? new SharedArray('free seats', () => open(__ENV.FREE_SEATS).split('\n').filter(l => l.trim() !== '').map(Number))
    : null;

const STEPS = [1000, 1500, 2000, 2500];
const stepStages = (scale) => [
    {target: 100 * scale, duration: '30s'},
    {target: 100 * scale, duration: '90s'}, // JIT warm-up
    ...STEPS.flatMap(rps => [
        {target: rps * scale, duration: '15s'},
        {target: rps * scale, duration: '90s'},
    ]),
];

function stepTag() {
    if (PROFILE !== 'steps') return 'all';
    const t = (Date.now() - exec.scenario.startTime) / 1000;
    if (t < 120) return 'warmup';
    const i = Math.floor((t - 120) / 105);
    if (i >= STEPS.length) return 'end';
    return (t - 120) % 105 < 15 ? 'ramp' : String(STEPS[i]);
}

const stepThresholds = (scenario, limit) => PROFILE !== 'steps' ? {} : Object.fromEntries(
    STEPS.map(rps => [`http_req_duration{scenario:${scenario},step:${rps}}`, [`p(95)<${limit}`]]));

const RAMP = {
    read: [
        {target: 100, duration: '30s'}, // JIT warm-up
        {target: 2000, duration: '15s'}, {target: 2000, duration: '45s'},
        {target: 4000, duration: '15s'}, {target: 4000, duration: '45s'},
        {target: 6000, duration: '15s'}, {target: 6000, duration: '45s'},
        {target: 8000, duration: '15s'}, {target: 8000, duration: '45s'},
        {target: 12000, duration: '15s'}, {target: 12000, duration: '45s'},
    ],
    book: [
        {target: 10, duration: '30s'}, // JIT warm-up
        {target: 200, duration: '15s'}, {target: 200, duration: '45s'},
        {target: 400, duration: '15s'}, {target: 400, duration: '45s'},
        {target: 600, duration: '15s'}, {target: 600, duration: '45s'},
        {target: 800, duration: '15s'}, {target: 800, duration: '45s'},
        {target: 1200, duration: '15s'}, {target: 1200, duration: '45s'},
    ],
};

const READ_STAGES = PROFILE === 'steps' ? stepStages(1) : RAMP.read;
const BOOK_STAGES = PROFILE === 'steps' ? stepStages(0.1) : RAMP.book;

export const options = {
    scenarios: {
        read_seats: {
            executor: 'ramping-arrival-rate',
            exec: 'readSeats',
            startRate: 25, timeUnit: '1s',
            preAllocatedVUs: 150, maxVUs: 1500,
            stages: READ_STAGES,
        },
        book: {
            executor: 'ramping-arrival-rate',
            exec: 'book',
            startRate: 3, timeUnit: '1s',
            preAllocatedVUs: 15, maxVUs: 1000,
            stages: BOOK_STAGES,
        },
    },
    thresholds: {
        'http_req_failed': ['rate<0.01'],
        'http_req_duration{scenario:read_seats}': ['p(95)<200'],
        'http_req_duration{scenario:book}': ['p(95)<300'],
        ...stepThresholds('read_seats', 200),
        ...stepThresholds('book', 300),
    }
}

export function readSeats() {
    const res = http.get(`${BASE}/events/${READ_EVENT_ID}/seats`,
        {tags: {name: 'GET /events/{id}/seats', step: stepTag()}});
    check(res, {'seats 200': (r) => r.status === 200});
}

export function book() {
    const n = exec.scenario.iterationInTest;
    const seatId = FREE_SEATS ? FREE_SEATS[n % FREE_SEATS.length] : FIRST_SEAT + n;
    const res = http.post(`${BASE}/bookings`,
        JSON.stringify({seatId, email: `load${seatId}@test.dk`}),
        {headers: {'Content-Type': 'application/json'}, tags: {name: 'POST /bookings', step: stepTag()}});
    check(res, {'booked 201': (r) => r.status === 201});
}