import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { triggerJob } from './requests/jobrunr.js';


export const options = {
    stages: [
        { duration: '5m', target: 100 }, // simulate ramp-up of traffic from 1 to 60 users over 5 minutes.
        { duration: '10m', target: 500 }, // stay at 500 users for 10 minutes
        { duration: '3m', target: 50 }, // ramp-up to 50 users over 3 minutes (peak hour starts)
        { duration: '2m', target: 5000 }, // stay at 5000 users for short amount of time (peak hour)
        { duration: '3m', target: 1000 }, // ramp-down to 600 users over 3 minutes (peak hour ends)
        { duration: '10m', target: 700 }, // continue at 700 for additional 10 minutes
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