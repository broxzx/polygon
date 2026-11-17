import http from 'k6/http';
import {check} from 'k6';


export const options = {
    scenarios: {
        race: {executor: 'shared-iterations', vus: 200, iterations: 200, maxDuration: '30s'},
    },
};


export default function () {
    const BASE = __ENV.BASE_URL || 'http://localhost:8080';
    const res = http.post(`${BASE}/bookings`,
        JSON.stringify({seatId: Number(__ENV.SEAT_ID), email: `race${__VU}@test.dk`}),
        {headers: {'Content-Type': 'application/json'}});
    check(res, {'201': r => r.status === 201, '409': r => r.status === 409});
}