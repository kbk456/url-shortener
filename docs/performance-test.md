# Performance Test

## 사전 준비

```bash
# 인프라 실행
docker-compose up -d

# k6 설치 (macOS)
brew install k6

# 앱 실행
./gradlew bootRun
```


## 테스트 1: Redirect (GET /{shortCode})

```bash
k6 run k6/redirect-test.js
```

**시나리오**
- setup()에서 테스트용 단축 URL 5개 생성
- 0→50 VU (10s) → 50→200 VU (60s) → 200→0 VU (10s)
- `redirects: 0` 옵션으로 302 응답만 측정

**주요 관찰 지점**
- `http_req_duration` p95 < 100ms, p99 < 300ms 기준 통과 여부
- Redis Cache Hit Rate 확인
  ```bash
  redis-cli info stats | grep keyspace
  redis-cli info stats | grep hit
  ```
- Cache Miss 시 응답 시간 vs Cache Hit 시 응답 시간 비교

---

## Redis 캐시 모니터링

```bash
# 캐시 히트/미스 확인
redis-cli info stats | grep -E "keyspace_hits|keyspace_misses"

# 저장된 키 확인
redis-cli keys "short-url:*"

# 특정 키 확인
redis-cli get "short-url:1"

# TTL 확인
redis-cli ttl "short-url:1"
```

## Kafka 모니터링

```bash
# 토픽 메시지 확인
docker exec -it url-shortener-kafka \
  kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic url.click.events --from-beginning

# Consumer Lag 확인
docker exec -it url-shortener-kafka \
  kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group url-shortener-group
```

## MySQL 확인

```sql
-- 단축 URL 목록
SELECT id, short_code, click_count, is_active, created_at FROM url;

-- 클릭 로그
SELECT * FROM url_click_log ORDER BY clicked_at DESC LIMIT 20;

-- 클릭 수 집계
SELECT short_code, COUNT(*) as log_count FROM url_click_log GROUP BY short_code;
```

## 기록 항목

테스트 후 아래 항목을 `docs/retrospective.md`에 기록한다.

- p95, p99 응답 시간
- Cache Hit Rate
- 실패율
- 병목 지점
- Kafka Consumer Lag
