# API Specification

## POST /api/shorten — 단축 URL 생성

**Rate Limit**: IP당 분당 10회

**Request**
```json
{
  "originalUrl": "https://www.example.com/very/long/path"
}
```
- `originalUrl`: 필수. http:// 또는 https://로 시작해야 함.
- `expiresAt`: 선택. 없으면 기본 7일 TTL 적용.

**Response 200**
```json
{
  "shortCode": "1",
  "shortUrl": "http://localhost:8080/1",
  "originalUrl": "https://www.example.com/very/long/path",
  "createdAt": "2026-05-09T10:00:00"
}
```

**Response 400** — 유효하지 않은 URL 또는 악성 도메인
```json
{ "code": "MALICIOUS_URL", "message": "Blocked domain: malware.com" }
```

**Response 429** — Rate Limit 초과
```json
{ "code": "RATE_LIMIT_EXCEEDED", "message": "Too many requests. Please try again later." }
```

---

## GET /{shortCode} — 리다이렉트

**Response 302**
```
Location: https://www.example.com/very/long/path
```

**Response 404**
```json
{ "code": "URL_NOT_FOUND", "message": "URL not found: abc123" }
```

---

## GET /api/stats/{shortCode} — 통계 조회

**Response 200**
```json
{
  "shortCode": "1",
  "originalUrl": "https://www.example.com/very/long/path",
  "clickCount": 42,
  "createdAt": "2026-05-09T10:00:00",
  "active": true
}
```

> click_count는 Kafka Consumer가 비동기로 업데이트하므로 실제 클릭 수보다 약간 늦을 수 있다 (Eventual Consistency).

**Response 404**
```json
{ "code": "URL_NOT_FOUND", "message": "URL not found: abc123" }
```
