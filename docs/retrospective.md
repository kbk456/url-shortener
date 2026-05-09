# 회고 (Retrospective)

> 부하 테스트 완료 후 이 문서를 채운다.

---

## 성능 측정 결과

| 항목 | 목표 | 실측 |
|------|------|------|
| Redirect p95 | < 100ms | - |
| Redirect p99 | < 300ms | - |
| Shorten p95  | < 500ms | - |
| 에러율 | < 1% | - |
| Cache Hit Rate | > 90% | - |

---

## 병목 발견

### 발견한 병목
- (측정 후 기록)

### 원인 분석
- (측정 후 기록)

---

## 설계 트레이드오프 회고

### 302 vs 301
- 302 선택으로 클릭 추적 가능해짐
- 대신 모든 리다이렉트 요청이 서버를 거침 → 트래픽 높을 때 부하 증가
- 개선 방향: 인기 URL은 CDN 엣지에서 302 처리

### Cache Aside vs Write-Through
- Cache Aside는 캐시 미스 시 DB를 거치므로 첫 요청이 느림
- Write-Through는 쓰기 시 캐시에도 저장 (이미 구현됨)
- 문제: 캐시가 재시작되면 Miss가 집중 발생 (Cache Stampede 가능성)
- 개선 방향: 인기 URL 예열(cache warming) 배치 추가

### Kafka Eventual Consistency
- click_count가 실시간으로 정확하지 않음
- Consumer Lag이 클 때 stats API의 숫자가 크게 밀릴 수 있음
- 이 트레이드오프는 의도적. 클릭 집계는 통계 목적이므로 수 초 지연 허용

### Rate Limit 고정 윈도우
- 윈도우 경계에서 최대 20회(2배) 요청이 허용되는 문제
- 개선 방향: 슬라이딩 윈도우 알고리즘으로 교체

---

## 다음 개선 과제

- [ ] Cache Stampede 방지 (mutex lock 또는 probabilistic early expiration)
- [ ] Kafka Consumer 멱등성 처리 (click_log 중복 방지)
- [ ] 슬라이딩 윈도우 Rate Limiting
- [ ] Google Safe Browsing API 연동
- [ ] 사용자 인증 + 개인 단축 URL 목록
- [ ] MySQL Read Replica 분리
