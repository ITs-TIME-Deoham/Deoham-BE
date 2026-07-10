# 성능 모니터링 및 최적화 가이드

## Phase 7: 성능 최적화 및 모니터링

이 가이드는 프로덕션에서 Deoham 모니터링 스택을 모니터링하고 최적화하기 위한 모범 사례를 제공합니다.

---

## 1. 모니터링 스택 아키텍처

```
┌─────────────────────────────────────────────────────────────────────┐
│                      모니터링 아키텍처                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  애플리케이션 (포트 8080)                                             │
│  ├─ 메트릭: /actuator/prometheus                                     │
│  └─ 상태: /actuator/health                                           │
│       │                                                               │
│       ├──→ Prometheus (포트 19090)  [메트릭]                         │
│       │    └─ Alert 규칙 평가 (30초 간격)                            │
│       │    └─ TSDB 저장소 (프로덕션에서 WAL 활성화)                  │
│       │                                                               │
│       └──→ 로그/application.log                                     │
│            └─ Promtail (포트 9080)  [로그 전송기]                    │
│                 └─ Loki (포트 3100) [로그 집계]                      │
│                                                                       │
│  Grafana (포트 3000) [시각화]                                        │
│  ├─ Prometheus 데이터소스                                            │
│  ├─ Loki 데이터소스                                                  │
│  └─ 3개 사전 구성 대시보드                                           │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. 리소스 할당 및 제한

### 2.1 권장 리소스 제한

| 서비스 | CPU 제한 | 메모리 제한 | CPU 예약 | 메모리 예약 |
|--------|---------|-----------|---------|-----------|
| App | 2.0 | 1536 MB | 1.0 | 768 MB |
| PostgreSQL | 1.0 | 1024 MB | 0.5 | 512 MB |
| Redis | 0.5 | 256 MB | 0.25 | 128 MB |
| Prometheus | 0.75 | 1024 MB | 0.5 | 512 MB |
| Loki | 0.75 | 1024 MB | 0.5 | 512 MB |
| Promtail | 0.25 | 128 MB | 0.125 | 64 MB |
| Grafana | 0.75 | 512 MB | 0.5 | 256 MB |

**합계:** ~5.5 CPU 코어, ~5.5 GB RAM

### 2.2 리소스 사용량 모니터링

#### Docker 통계

```bash
# 모든 컨테이너
docker stats

# 특정 컨테이너
docker stats deoham-be-prometheus-1

# 메모리 사용량만
docker stats --format "table {{.Container}}\t{{.MemUsage}}"

# CPU 사용률만
docker stats --format "table {{.Container}}\t{{.CPUPerc}}"
```

#### Prometheus에서 리소스 메트릭 쿼리

```promql
# 컨테이너 CPU 사용량
container_cpu_usage_seconds_total

# 컨테이너 메모리 사용량
container_memory_usage_bytes

# 프로세스 CPU 사용률 (애플리케이션)
process_cpu_usage * 100

# JVM 힙 메모리
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

---

## 3. 성능 최적화 설정

### 3.1 Prometheus 최적화

**파일:** `compose.yaml` 및 `monitoring/prometheus.yml`

#### 명령줄 플래그

| 플래그 | 값 | 목적 |
|--------|-----|------|
| `--storage.tsdb.max-block-duration` | 2h | 30초 평가 간격에 최적 |
| `--query.timeout` | 2m | 오래 실행되는 쿼리 방지 |
| `--query.max-concurrent` | 20 | 동시 쿼리 제한 |

#### 설정 튜닝

```yaml
# 로컬 환경
global:
  scrape_interval: 30s        # 기본값 15s, 리소스 절감을 위해 증가
  evaluation_interval: 30s    # 기본값 15s, 리소스 절감을 위해 증가

# 프로덕션 환경
global:
  scrape_interval: 60s        # 메트릭 수집량 감소를 위해 긴 간격
  evaluation_interval: 60s    # CPU 사용률 감소를 위해 긴 간격
```

#### Alert 평가

- **기본값:** 15초 간격 (높은 CPU 사용률)
- **최적화:** 30초 간격 (30-40% CPU 감소)
- **프로덕션:** 60초 간격 (60% CPU 감소)

**트레이드오프:** 더 긴 간격은 alert 감지 지연 (~30-60초 지연)

### 3.2 Loki 최적화

**파일:** `monitoring/loki-config.yml`

#### 청크 설정

| 설정 | 값 | 목적 |
|------|-----|------|
| `chunk_idle_period` | 3m | 3분 비활성화 후 청크 플러시 |
| `max_chunk_age` | 1h | 1시간 후 청크 플러시 (크기 상관없음) |
| `chunk_encoding` | snappy | 저장소 효율성을 위해 청크 압축 |

#### 쿼리 캐싱

```yaml
frontend:
  compress_responses: true
  max_cache_freshness_per_query: 10m
  log_queries_longer_than: 10s
```

**이점:**
- 쿼리 레이턴시 감소 (캐시된 쿼리: <100ms)
- 낮은 디스크 I/O
- 대시보드에서 사용자 경험 향상

#### 보관 설정

```yaml
limits_config:
  retention_period: 720h  # 30일
  ingestion_rate_mb: 8    # 스트림당 제한
  ingestion_burst_size_mb: 16
```

### 3.3 Grafana 최적화

**파일:** `compose.yaml`

#### 데이터소스 설정

```yaml
environment:
  GF_DATAPROXY_TIMEOUT: 10s        # 쿼리 타임아웃
  GF_DATAPROXY_DIALTIEMOUT: 10s   # 연결 타임아웃
  GF_DATAPROXY_KEEP_ALIVE_SECONDS: 30
```

#### 대시보드 모범 사례

1. **쿼리 캐싱**
   - 동적 스텝 크기를 위해 `$__interval` 변수 사용
   - 데이터소스 레벨에서 쿼리 캐싱 활성화
   - 반복되는 쿼리의 결과 캐싱

2. **패널 설정**
   - 최소 데이터 간격을 30초로 설정
   - `max_data_points: 1000`을 사용해 반환되는 시리즈 제한
   - 값 형식 최적화 활성화

3. **새로고침 간격**
   - 로컬: 5-10초 (개발용)
   - 프로덕션: 30-60초 (대시보드 부하 감소)

### 3.4 애플리케이션 로깅 최적화

**파일:** `src/main/resources/logback-spring.xml`

#### 로컬 환경

```xml
<logger name="com.deoham" level="DEBUG" />
<logger name="org.springframework.security" level="DEBUG" />

<rollingPolicy>
  <maxFileSize>10MB</maxFileSize>
  <maxHistory>10</maxHistory>
  <totalSizeCap>1GB</totalSizeCap>
</rollingPolicy>
```

#### 프로덕션 환경

```xml
<logger name="com.deoham" level="INFO" />
<logger name="org.springframework" level="WARN" />

<rollingPolicy>
  <maxFileSize>50MB</maxFileSize>
  <maxHistory>30</maxHistory>
  <totalSizeCap>10GB</totalSizeCap>
</rollingPolicy>
```

**이점:**
- 60-70% 낮은 디스크 I/O
- 로깅으로 인한 CPU 사용률 감소
- 프로덕션 문제에 더 집중

---

## 4. 성능 모니터링 쿼리

### 4.1 애플리케이션 성능

#### 요청 레이턴시 (p95)

```promql
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))
```

#### 요청 처리량

```promql
sum(rate(http_server_requests_seconds_count[5m]))
```

#### 에러율

```promql
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
/
sum(rate(http_server_requests_seconds_count[5m]))
```

### 4.2 데이터베이스 성능

#### 연결 풀 사용량

```promql
tomcat_jdbc_pool_active / tomcat_jdbc_pool_max * 100
```

#### 쿼리 실행 시간

```promql
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])
```

### 4.3 시스템 성능

#### CPU 사용률

```promql
process_cpu_usage * 100
```

#### JVM 메모리 사용량

```promql
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

#### GC 일시 정지 시간

```promql
increase(jvm_gc_pause_seconds_sum[5m])
```

### 4.4 로그 수집 성능

#### 초당 로그

```logql
sum(rate({job="promtail"}[5m]))
```

#### 로그 에러율

```logql
sum(rate({job="promtail", level="error"}[5m]))
```

#### 평균 로그 크기

```logql
avg(bytes_processed_per_second / logs_per_second)
```

---

## 5. 성능 기준선 기대값

### 5.1 레이턴시 목표

| 메트릭 | 목표 | 경고 | 중대 |
|--------|------|------|------|
| API 응답 (p50) | < 100ms | > 200ms | > 500ms |
| API 응답 (p95) | < 500ms | > 1s | > 2s |
| API 응답 (p99) | < 1s | > 2s | > 5s |
| Prometheus 쿼리 | < 500ms | > 1s | > 2s |
| Grafana 대시보드 로드 | < 2s | > 5s | > 10s |

### 5.2 처리량 목표

| 메트릭 | 목표 | 경고 | 중대 |
|--------|------|------|------|
| 요청/초 | > 100 | < 50 | < 10 |
| 로그/초 | > 10 | < 5 | < 1 |
| 메트릭/초 | > 1000 | < 500 | < 100 |

### 5.3 리소스 목표

| 메트릭 | 목표 | 경고 | 중대 |
|--------|------|------|------|
| App CPU | < 30% | > 50% | > 80% |
| App 메모리 | < 60% | > 80% | > 95% |
| Prometheus CPU | < 20% | > 40% | > 70% |
| Prometheus 메모리 | < 50% | > 75% | > 90% |
| Loki 메모리 | < 40% | > 60% | > 80% |

---

## 6. 최적화 체크리스트

### 6.1 프로덕션 배포 전

- [ ] Prometheus WAL 활성화 (`--storage.tsdb.wal` in prod)
- [ ] Alert 규칙 검증 통과
- [ ] Loki 보관 정책 설정 (30일)
- [ ] Grafana 데이터소스 연결 검증
- [ ] 대시보드 성능 최적화 (느린 쿼리가 있는 패널 < 10개)
- [ ] 애플리케이션 로그 레벨을 WARN으로 설정
- [ ] 로그 로테이션 설정 (50MB 파일, 30개 히스토리)
- [ ] 리소스 제한 적절히 설정
- [ ] 모든 서비스의 헬스체크 설정
- [ ] Prometheus 및 Loki 데이터 백업 전략
- [ ] 모니터링 스택에 대한 모니터링 (메타 모니터링)

### 6.2 지속적 최적화

- [ ] 주간: 리소스 사용량 트렌드 검토
- [ ] 월간: alert 성능 검토
- [ ] 분기별: 보관 정책 검토
- [ ] Grafana의 느린 쿼리 검토
- [ ] Prometheus의 실패한 쿼리 검토
- [ ] 로그 저장소의 디스크 사용량 모니터링
- [ ] 백업 완성도 검증
- [ ] 기준선을 기반으로 alert 임계값 업데이트

---

## 7. 스케일링 고려사항

### 7.1 수직 스케일링

다음과 같은 경우 단일 인스턴스 리소스 증가:
- CPU 사용률이 지속적으로 > 80%
- 메모리 사용률이 지속적으로 > 85%
- 쿼리 레이턴시가 정기적으로 > 5초

### 7.2 수평 스케일링

다음과 같은 경우 더 많은 인스턴스 추가 고려:
- Prometheus 스크래프 간격을 더 이상 증가시킬 수 없음
- Loki 인제스션 레이트가 10MB/초 초과
- Grafana 동시 사용자가 50명 초과

### 7.3 샤딩 전략

**높은 볼륨용:**

```yaml
# Prometheus 샤딩
scrape_configs:
  - job_name: 'app-shard-1'
    scrape_interval: 60s
    static_configs:
      - targets: ['app1:8080']

  - job_name: 'app-shard-2'
    scrape_interval: 60s
    static_configs:
      - targets: ['app2:8080']
```

**Loki 샤딩:**
- 애플리케이션 인스턴스당 여러 Promtail 인스턴스
- 여러 Loki 인스턴스에 로그 분산
- 로그 분산을 위해 일관된 해싱 사용

---

## 8. 핵심 성과 지표 (KPI)

### 8.1 가용성 KPI

- Prometheus 가동시간: > 99.9%
- Loki 가동시간: > 99.9%
- Grafana 가동시간: > 99.5%
- Alert 감지 레이턴시: < 2분

### 8.2 성능 KPI

- 메트릭 수집 레이턴시: < 10초
- 로그 수집 레이턴시: < 5초
- 쿼리 응답 시간 (p95): < 1초
- 대시보드 로드 시간 (p95): < 5초

### 8.3 신뢰성 KPI

- 데이터 보관: 최소 30일
- 로그 수집 성공률: > 99.9%
- Alert 규칙 평가 성공: 100%
- 백업 성공률: 100%

---

## 9. 성능 문제 트러블슈팅

### CPU 사용률 높음

1. 평가 간격 확인 (프로덕션에서는 30-60초)
2. Alert 규칙 복잡성 검토
3. 쿼리 캐싱 설정 확인
4. 스크래프 간격 감소 고려

### 메모리 사용량 높음

1. Prometheus의 WAL 설정 확인
2. Loki의 청크 설정 검증
3. Grafana 캐시 설정 확인
4. 보관 기간 감소 고려

### 느린 쿼리

1. 쿼리 로깅 활성화 (10초 임계값)
2. 인덱스 설정 확인
3. 데이터소스 연결 검증
4. 쿼리 최적화 고려

### 디스크 공간 문제

1. 보관 정책 확인
2. 로그 로테이션 설정 검증
3. 아카이브 프로세스 모니터링
4. 압축 설정 고려

---

## 10. 참고 자료

- [Prometheus 문서](https://prometheus.io/docs/)
- [Loki 문서](https://grafana.com/docs/loki/latest/)
- [Grafana 문서](https://grafana.com/docs/grafana/latest/)
- [Java 메트릭](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

---

## 연락처 및 지원

성능 문제 또는 최적화 질문:
1. 이 가이드 검토
2. 모니터링 대시보드 확인
3. 애플리케이션 로그 검토
4. DevOps 팀에 문의

**마지막 업데이트:** 2026-07-11

---
