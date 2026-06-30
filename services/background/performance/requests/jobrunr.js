import http from 'k6/http';
export function triggerJob() {
    const url = `http://localhost:8070/jobs/failure/queue`;
    let response = http.post(url,JSON.stringify({"test":"test"}),{
        headers: { 'Content-Type': 'application/json' },
    });

    return response.body;
}
