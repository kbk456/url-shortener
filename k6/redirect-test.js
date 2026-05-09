import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '10s', target: 50 },
    { duration: '60s', target: 200 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<100', 'p(99)<300'],
    errors: ['rate<0.01'],
  },
};

const BASE_URL = 'http://localhost:8080';

export function setup() {
  const testUrls = [
    'https://www.google.com',
    'https://www.github.com',
    'https://www.stackoverflow.com',
    'https://www.youtube.com',
    'https://www.reddit.com',
  ];

  const shortCodes = [];
  for (const url of testUrls) {
    const res = http.post(`${BASE_URL}/api/shorten`,
        JSON.stringify({ originalUrl: url }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    if (res.status === 200) {
      shortCodes.push(JSON.parse(res.body).shortCode);
    }
  }
  console.log(`Setup created ${shortCodes.length} short URLs: ${shortCodes}`);
  return { shortCodes };
}

export default function (data) {
  const { shortCodes } = data;
  if (shortCodes.length === 0) return;

  const shortCode = shortCodes[Math.floor(Math.random() * shortCodes.length)];

  // redirects: 0 — measure only the 302 response, not the final destination
  const res = http.get(`${BASE_URL}/${shortCode}`, { redirects: 0 });

  const ok = check(res, {
    'status 302':         (r) => r.status === 302,
    'has Location header':(r) => r.headers['Location'] !== undefined,
  });

  errorRate.add(!ok);
  sleep(0.1);
}
