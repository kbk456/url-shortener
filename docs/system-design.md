# System Design

## 아키텍처 개요

```
[Client]
   │ POST /api/shorten
   │ GET  /{shortCode}
   ▼
[Spring Boot API]
   ├── Rate Limiting (Redis INCR)       ← POST /api/shorten only
   ├── Malicious URL Check (Blacklist)
   ├── UrlService
   │     ├── Cache Aside (Redis)        ← GET /{shortCode} hot path
   │     └── MySQL (url, url_click_log)
   └── ClickEventProducer (Kafka)       ← fire-and-forget
          │
          ▼
   [Kafka: url.click.events]
          │
          ▼
   [ClickEventConsumer]
          └── INSERT url_click_log
              UPDATE url.click_count
```

## 핵심 설계 결정

### 1. shortCode 생성: MySQL AUTO_INCREMENT + Base62

**왜 Base62인가?**
- 해시(MD5/SHA) 방식은 충돌 가능성이 있고 길이가 길다.
- UUID는 사람이 읽기 어렵다.
- 카운터 방식은 충돌이 없고, Base62로 변환하면 7자리로 3.5조 개를 표현할 수 있다.

**트레이드오프**
- 장점: 충돌 없음, 구현 단순, 순서 예측 가능
- 단점: 순차 증가하므로 경쟁사가 생성 속도를 추정 가능

### 2. 302 Redirect (301 아님)

- 301은 브라우저가 결과를 캐싱해 이후 요청이 서버에 도달하지 않는다.
- 302를 사용해야 매 클릭마다 서버를 거쳐 Kafka로 이벤트를 발행할 수 있다.
- 단점: 301보다 서버 부하가 높다. 이 트레이드오프를 의도적으로 선택했다.

### 3. Cache Aside 패턴 (Redis)

```
GET /{shortCode}
  1. Redis에서 short-url:{shortCode} 조회
  2. Hit  → originalUrl 반환, Kafka 발행
  3. Miss → MySQL 조회 → Redis 저장 → 반환, Kafka 발행
```

- Redis key: `short-url:{shortCode}`
- Redis value: `{ "urlId": 1, "originalUrl": "...", "expiresAt": "..." }` (JSON)
- TTL: expiresAt 기준. expiresAt 없으면 7일.

### 4. 클릭 추적: Kafka + Eventual Consistency

- Redirect API에서 직접 DB INSERT하면 응답 지연 증가.
- Kafka로 fire-and-forget 발행 → Consumer가 비동기로 INSERT + click_count 증가.
- click_count는 즉시 일관성이 아닌 최종 일관성. stats 조회 시 약간의 지연 허용.

**알려진 문제**
- Consumer 실패 후 재처리 시 click_log 중복 저장 가능. 멱등성 처리 미구현 (학습 목적).

### 5. Rate Limiting: Redis INCR

```
key: rate-limit:{ip}
INCR → count
count == 1 이면 TTL 60초 설정
count > 10 이면 429 반환
```

- 슬라이딩 윈도우 방식보다 단순한 고정 윈도우 방식.
- 트레이드오프: 윈도우 경계에서 최대 2배 요청이 허용될 수 있음.

### 6. 악성 URL 차단: 도메인 블랙리스트

- 생성 시점에 차단. 리다이렉트 시점 차단은 이미 DB에 저장 후라 비효율.
- 초기 구현: application.yml에 블랙리스트 도메인 목록.
- 개선 방향: Google Safe Browsing API 연동.


## 데이터베이스 스키마

```sql
url
  id              BIGINT PK AUTO_INCREMENT
  short_code      VARCHAR(10) UNIQUE
  original_url    TEXT
  original_url_hash VARCHAR(64)  -- SHA-256, 중복 감지용
  is_active       BOOLEAN
  click_count     BIGINT
  created_at      DATETIME

url_click_log
  id          BIGINT PK
  short_code  VARCHAR(10)
  ip_address  VARCHAR(45)
  user_agent  VARCHAR(500)
  referer     VARCHAR(500)
  clicked_at  DATETIME
```

## 확장 방향 (미구현)

- 로드 밸런서 + 다중 API 서버 인스턴스
- MySQL Read Replica (읽기/쓰기 분리)
- Kafka 파티션 증가로 Consumer 병렬화
- 사용자 인증 + 개인 대시보드
- Google Safe Browsing API 연동
