
import { check, group, sleep } from 'k6';
import { triggerJob } from './requests/jobrunr.js';

export const options = {
    stages: [
        { duration: '5m', target: 50 }, // simulate ramp-up of traffic from 1 to 60 users over 5 minutes.
        { duration: '10m', target: 100 }, // stay at 100 users for 10 minutes
        { duration: '5m', target: 0 }, // ramp-down to 0 users
    ],
    thresholds: {
        'http_req_duration': ['p(99)<1500'], // 99% of requests must complete below 1.5s
    },
};

export default function () {
    let qvPassAuth = triggerJob();
    console.log(qvPassAuth);
    sleep(1);
}  