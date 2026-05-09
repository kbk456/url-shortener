# URL Shortener — 시스템 설계 실습 프로젝트

9장 『요즘 개발자를 위한 시스템 설계』에서 설계한 URL 단축 서비스를 실제로 구현한 실습 프로젝트.
단순 기능 구현이 아니라 **Cache Aside, Kafka 비동기 처리, Redis Rate Limiting, Eventual Consistency**를 직접 체험하는 것이 목표다.

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Database | MySQL 8 |
| Cache | Redis 7 |
| Message Queue | Kafka (Confluent 7.4) |
| Build | Gradle 8 |
| 부하 테스트 | k6 |

---

## 아키텍처

```
[Client]
   │
   ▼
[Spring Boot API]
   ├── POST /api/shorten  ← Rate Limit (Redis) + Malicious Check
   ├── GET  /{shortCode}  ← Cache Aside (Redis) → 302 Redirect
   └── GET  /api/stats/{shortCode}
            │
            ├── [MySQL]   url / url_click_log
            ├── [Redis]   short-url:{shortCode} (Cache)
            └── [Kafka]   url.click.events (비동기 클릭 로그)
                    │
                    ▼
            [ClickEventConsumer]
                    └── url_click_log INSERT + click_count++
```

---

## 핵심 설계 포인트

**shortCode 생성** — MySQL AUTO_INCREMENT id → Base62 변환. 충돌 없음, 구현 단순.

**302 Redirect** — 301은 브라우저 캐싱으로 클릭 추적 불가. 매 요청마다 서버를 거쳐야 Kafka 이벤트를 발행할 수 있다.

**Cache Aside** — `short-url:{shortCode}` 키로 Redis 조회 → Miss 시 MySQL → Redis 저장.

**Eventual Consistency** — 클릭 로그와 click_count는 Kafka Consumer가 비동기로 처리. 즉시 일관성을 포기하고 처리량을 얻는다.

---

## 실행 방법

### 1. 인프라 실행

```bash
docker-compose up -d
```

MySQL, Redis, Zookeeper, Kafka가 순서대로 실행된다.

### 2. 앱 빌드 & 실행

```bash
# Gradle Wrapper 생성 (최초 1회)
gradle wrapper

# 실행
./gradlew bootRun
```

앱이 시작되면 `src/main/resources/db/schema.sql`이 자동 실행되어 테이블이 생성된다.

---

## API 사용법

### 단축 URL 생성

```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://www.google.com"}'
```

```json
{
  "shortCode": "1",
  "shortUrl": "http://localhost:8080/1",
  "originalUrl": "https://www.google.com",
  "expiresAt": null,
  "createdAt": "2026-05-09T10:00:00"
}
```

### 리다이렉트

```bash
curl -v http://localhost:8080/1
# < HTTP/1.1 302 Found
# < Location: https://www.google.com
```

### 통계 조회

```bash
curl http://localhost:8080/api/stats/1
```

```json
{
  "shortCode": "1",
  "originalUrl": "https://www.google.com",
  "clickCount": 5,
  "createdAt": "2026-05-09T10:00:00",
  "expiresAt": null,
  "active": true
}
```

---

## 성능 테스트

```bash
# k6 설치 (macOS)
brew install k6

# Redirect 부하 테스트 (redirects:0 으로 302만 측정)
k6 run k6/redirect-test.js

# Rate Limit 테스트 (분당 10회 초과)
for i in $(seq 1 15); do
  curl -s -o /dev/null -w "%{http_code}\n" \
  -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://test.com"}'
done
```

자세한 내용은 [docs/performance-test.md](docs/performance-test.md) 참조.

---

## 설계 트레이드오프

| 결정 | 선택 | 포기한 것 |
|------|------|----------|
| shortCode 생성 | 카운터 + Base62 | 순차 노출 위험 |
| Redirect | 302 | 브라우저 캐싱 (성능) |
| 클릭 로그 | Kafka 비동기 | 즉시 일관성 |
| Rate Limit | Redis 고정 윈도우 | 윈도우 경계 정확도 |
| 악성 URL 차단 | 도메인 블랙리스트 | 실시간 탐지 |

---

## 문서

- [시스템 설계](docs/system-design.md)
- [API 명세](docs/api-spec.md)
- [성능 테스트 가이드](docs/performance-test.md)
- [회고](docs/retrospective.md)
