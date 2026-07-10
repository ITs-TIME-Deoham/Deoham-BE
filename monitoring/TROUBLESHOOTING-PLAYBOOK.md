# 문제 해결 플레이북

일반적인 모니터링 문제에 대한 단계별 진단 절차입니다.

## 목차

1. [메트릭이 수집되지 않음](#메트릭이-수집되지-않음)
2. [Loki에서 로그 누락](#loki에서-로그-누락)
3. [대시보드에 데이터 없음](#대시보드에-데이터-없음)
4. [Alert가 발동하지 않음](#alert가-발동하지-않음)
5. [서비스에 연결할 수 없음](#서비스에-연결할-수-없음)
6. [메모리 누수](#메모리-누수)
7. [데이터베이스 성능 문제](#데이터베이스-성능-문제)
8. [네트워크 연결 문제](#네트워크-연결-문제)

---

## 메트릭이 수집되지 않음

### 증상

- Prometheus 대시보드에 "데이터 없음" 표시
- Grafana 패널에 빈 그래프 표시
- 메트릭 엔드포인트가 데이터 반환 안 함
- 어제는 모니터링이 작동했는데 오늘 멈춤

### 진단 절차

**Step 1: 애플리케이션이 실행 중인지 확인 (1분)**

```bash
# 앱 실행 상태 확인
curl http://localhost:8080/actuator/health

# 예상 응답:
# {"status":"UP","components":{...}}

# 연결 거부된 경우:
docker ps | grep app
docker logs app | tail -20
```

**앱이 실행되지 않는 경우**:
- 재시작: `docker restart app`
- 로그에서 시작 오류 확인
- 오류가 있으면 [서비스에 연결할 수 없음](#서비스에-연결할-수-없음) 참고

**Step 2: 메트릭 엔드포인트가 노출되어 있는지 확인 (1분)**

```bash
# Prometheus 엔드포인트 접근 가능 여부 확인
curl http://localhost:8080/actuator/prometheus

# 예상 응답:
# # HELP process_cpu_usage ...
# process_cpu_usage 0.15
# ...
```

**엔드포인트가 404 또는 빈 응답을 반환하는 경우**:
- 설정에서 Prometheus 엔드포인트가 활성화되어 있는지 확인:
  ```yaml
  # application.yml
  management:
    endpoints:
      web:
        exposure:
          include: health,prometheus,env  # 'prometheus' 포함 필수
  ```
- 앱 재시작: `docker restart app`
- 다시 확인: `curl http://localhost:8080/actuator/prometheus`

**Step 3: Prometheus가 스크래프하는지 확인 (2분)**

```bash
# Prometheus 대상 확인
curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {labels, lastScrapeTime}'

# job="app" 대상 확인
# "lastScrapeTime"은 최근이어야 함 (지난 15초 내)
```

**대상이 "down"을 표시하는 경우**:
- 대상 상세정보 확인: `curl http://localhost:9090/api/v1/targets | jq '.data.droppedTargets'`
- 오류가 "droppedTargets"에 표시됨
- 일반적인 오류:
  - `refused by Prometheus` → 방화벽 확인
  - `context deadline exceeded` → 앱이 너무 느려서 응답 안 함
  - `invalid content type` → 메트릭 엔드포인트가 잘못된 형식 반환

**네트워크 문제 해결**:
```bash
# Prometheus 컨테이너에서 앱으로 연결 테스트
docker exec prometheus nc -zv app 8080

# Docker 네트워크 확인
docker network ls
docker network inspect bridge  # app과 prometheus가 같은 네트워크에 있는지 확인
```

**Step 4: Prometheus 설정 확인 (1분)**

```bash
# prometheus.yml 보기
docker exec prometheus cat /etc/prometheus/prometheus.yml

# job_name: "app"인 scrape_config 찾기
# 대상이 올바른 host:port를 가리키는지 확인
```

**설정이 잘못된 경우**:
```yaml
# 올바른 설정 예:
scrape_configs:
  - job_name: 'app'
    static_configs:
      - targets: ['app:8080']  # 'app'은 Docker 서비스 이름
```

**Step 5: 스크래프 수동 테스트**

```bash
# 스크래프를 수동으로 실행해서 디버그
curl http://localhost:8080/actuator/prometheus -v

# 응답 헤더 확인
# Content-Type: text/plain; version=0.0.4; charset=utf-8
# Status: 200 OK

# 상태가 200이 아니거나 content type이 잘못된 경우:
# Step 2 다시 참고 (메트릭 엔드포인트 문제)
```

### 일반적인 원인 및 해결책

| 증상 | 원인 | 해결책 |
|------|------|--------|
| `refused by Prometheus` | 방화벽 차단 | `docker network` 확인 |
| 연결 타임아웃 | 앱이 느림/멈춤 | 앱 재시작, 로그 확인 |
| Invalid content type | 잘못된 엔드포인트 형식 | `/actuator/prometheus` 사용, `/metrics` 아님 |
| 404 Not Found | 엔드포인트 비활성화 | `exposure.include`에 `prometheus` 추가 |
| 빈 응답 | 아직 메트릭 생성 안 됨 | 앱에 트래픽 보내서 메트릭 생성 |

### 예방법

1. **배포 후 테스트**:
   ```bash
   sleep 30  # 스크래프 사이클 대기
   curl http://localhost:9090/api/v1/query?query=up
   # app 작업 상태가 1이어야 함
   ```

2. **스크래프 성공 모니터링**:
   - Grafana: `up{job="app"}` 패널 추가
   - `up == 0`일 때 5분 이상이면 Alert

3. **정기적 헬스 체크**:
   ```bash
   # cron 작업에 추가
   curl -f http://localhost:8080/actuator/health || alert
   curl -f http://localhost:9090/-/healthy || alert
   ```

---

## Loki에서 로그 누락

### 증상

- Loki에 "데이터 없음" 표시
- Promtail이 실행 중이지만 로그가 나타나지 않음
- 파일에는 로그가 있지만 Loki에는 없음
- 어제는 로그가 있었는데 오늘은 없음

### 진단 절차

**Step 1: 애플리케이션이 로그를 기록하는지 확인 (1분)**

```bash
# 애플리케이션 로그 파일이 존재하고 최근 항목이 있는지 확인
tail -f logs/application.log

# 타임스탐프가 있는 로그 줄이 보여야 함
# 파일이 없거나 비어 있으면:
# - 앱이 로그를 작성하지 않을 수 있음
# - logback 설정 확인
```

**파일에 로그가 없는 경우**:
- logback 설정 확인: `src/main/resources/logback-spring.xml`
- 로그 레벨이 ERROR_ONLY가 아닌지 확인
- 테스트 로그 생성: `curl http://localhost:8080/actuator/health` (로그 트리거)
- 파일 권한 확인: `ls -la logs/application.log`

**Step 2: Promtail이 실행 중인지 확인 (1분)**

```bash
# Promtail 컨테이너가 실행 중인지 확인
docker ps | grep promtail

# 실행되지 않는 경우:
docker-compose up -d promtail

# Promtail 로그에서 오류 확인
docker logs promtail | tail -20

# 다음을 찾기:
# "ERROR" 메시지
# "failed to" 메시지
# "connection refused"
```

**Promtail이 실행되지 않는 경우**:
- Docker 오류 확인: `docker logs promtail | grep ERROR`
- 재시작: `docker restart promtail`
- compose 파일 확인: `docker-compose.yml`

**Step 3: Promtail 설정 확인 (1분)**

```bash
# 컨테이너 내 Promtail 설정 보기
docker exec promtail cat /etc/promtail/config.yml

# 다음을 포함해야 함:
# scrape_configs:
#   - job_name: deoham
#     static_configs:
#       - targets:
#           - localhost
#         labels:
#           job: deoham
```

**Promtail이 올바른 파일을 감시하는지 확인**:
```bash
# Promtail 설정에서 다음 찾기:
# file_sd_configs가 로그 파일을 가리킴
# __path__에 logs/application.log 포함

# 컨테이너 내 파일 경로 확인
docker exec promtail ls -la /var/log/deoham/application.log
# 또는 로그가 마운트된 곳
```

**Step 4: Promtail이 로그 파일을 읽을 수 있는지 확인 (1분)**

```bash
# 앱 관점에서 파일 권한 확인
docker exec app ls -la logs/application.log

# 읽을 수 있어야 함 (-w--w----)
# 그렇지 않으면 권한 수정:
chmod 644 logs/application.log

# Promtail 재시작
docker restart promtail
```

**Step 5: Promtail → Loki 연결 확인 (2분)**

```bash
# Promtail 컨테이너에서 Loki 연결 테스트
docker exec promtail nc -zv loki 3100

# 예상: nc: connect to loki port 3100 (tcp) succeeded!

# 연결 거부된 경우:
docker network inspect bridge | grep -A5 loki
# loki 서비스가 promtail과 같은 네트워크에 있는지 확인

# Loki 실행 중인지 확인:
docker ps | grep loki
docker logs loki | tail -10
```

**Loki가 응답하지 않는 경우**:
- Loki 재시작: `docker restart loki`
- Loki 헬스 확인: `curl http://localhost:3100/ready`
- Loki 로그 확인: `docker logs loki | grep ERROR`

**Step 6: 로그가 성공적으로 전송되었는지 확인 (2분)**

```bash
# Promtail 로그에서 성공적인 전송 확인
docker logs promtail | grep -E "sent|entries to"

# 다음과 같은 메시지가 보여야 함:
# "sent 42 entries to Loki"
# "batch of 50 entries submitted"

# 보이지 않는 경우:
docker logs promtail | grep -E "ERROR|error|failed"
# 연결 또는 쓰기 오류 찾기
```

**Step 7: Loki에 직접 쿼리 (2분)**

```bash
# Loki에 직접 쿼리 (Grafana 무시)
curl 'http://localhost:3100/loki/api/v1/query_range?query={job="deoham"}&start=1609459200&end=1609545600'

# 다음을 반환해야 함:
# {"status":"success","data":{"result":[{"stream":{"job":"deoham"},"values":[[timestamp,"log line"]...]}]}}

# 빈 결과를 반환하는 경우:
# 로그가 이 시간 범위에 없을 수 있음
# 최근 타임스탐프로 시도:
curl 'http://localhost:3100/loki/api/v1/query_range?query={job="deoham"}&start='$(date +%s)'000&end='$(date -u -d '+10 minutes' +%s)'000'
```

### 일반적인 원인 및 해결책

| 증상 | 원인 | 해결책 |
|------|------|--------|
| Promtail 실행 안 됨 | 컨테이너 충돌 | 로그 확인, 재시작 |
| Loki에 연결할 수 없음 | 네트워크 문제 | Docker 네트워크 확인 |
| 로그 파일 권한 거부 | 파일을 읽을 수 없음 | `chmod 644 logs/application.log` |
| Loki 저장소 꽉 참 | 디스크 공간 문제 | 이전 로그 정리 |
| 파일에는 로그가 있지만 Loki에는 없음 | Promtail이 파일을 감시하지 않음 | Promtail 설정 확인, 재시작 |

### 예방법

1. **일일 로그 검증**:
   ```bash
   # monitoring/DAILY-CHECKLIST.md에 추가:
   - curl 'http://localhost:3100/loki/api/v1/query_range?query={job="deoham"}' | jq
   - 최근 로그 나타나는지 확인
   ```

2. **Promtail 헬스 모니터링**:
   ```bash
   # Promtail이 10분 동안 로그를 전송하지 않으면 Alert
   ```

3. **파일 권한 정기 확인**:
   ```bash
   # 주기적 권한 확인
   ls -la logs/application.log | grep -v rw-r
   ```

---

## 대시보드에 데이터 없음

### 증상

- Grafana 패널에 "데이터 없음" 표시
- 메트릭이 존재해도 그래프가 비어있음
- 특정 대시보드가 깨짐, 다른 건 작동
- 시간 범위 변경 후 시작됨

### 진단 절차

**Step 1: Prometheus에 데이터가 있는지 확인 (1분)**

```bash
# 대시보드 패널과 같은 PromQL로 쿼리
# 대시보드 패널에서 PromQL 복사

curl 'http://localhost:9090/api/v1/query_range?query=up&start=1609459200&end=1609545600&step=15s'

# 다음을 반환하면:
# {"status":"success","data":{"result":[{"metric":{"job":"app"},"values":[[ts,"1"]]}]}}
# → Prometheus에 데이터가 있음, Grafana 문제

# 다음을 반환하면:
# {"status":"success","data":{"result":[]}}
# → Prometheus에 데이터가 없음
```

**Prometheus에 데이터가 있는 경우**:
- 문제는 Grafana datasource 또는 쿼리
- Step 2로 계속

**Prometheus에 데이터가 없는 경우**:
- 문제는 메트릭 수집
- [메트릭이 수집되지 않음](#메트릭이-수집되지-않음) 참고

**Step 2: Grafana Datasource 확인 (1분)**

```bash
# Grafana datasource 설정 확인
curl -u admin:admin http://localhost:3000/api/datasources

# Prometheus datasource를 상태 OK로 반환해야 함
# URL이 올바른 Prometheus를 가리키는지 확인

# datasource가 다운된 경우:
curl -u admin:admin http://localhost:3000/api/datasources/1/health

# 다음을 반환해야 함: {"status":"ok"}
```

**datasource 상태가 ok가 아닌 경우**:
- Prometheus 실행 중인지 확인: `docker ps | grep prometheus`
- 연결성 확인: `docker exec grafana nc -zv prometheus 9090`
- Grafana의 Prometheus URL 확인: `http://prometheus:9090` (localhost 아님)
- Grafana 재시작: `docker restart grafana`

**Step 3: 패널 PromQL 쿼리 확인 (1분)**

```bash
# Grafana에서:
# 1. 데이터 없는 대시보드 열기
# 2. 패널 제목 클릭 → 수정
# 3. 쿼리 탭 보기
# 4. PromQL 표현식 복사

# Prometheus Graph 탭에 붙여넣기 및 실행
http://localhost:9090/graph

# PromQL이 Prometheus에서 데이터를 반환하면:
# → Grafana 패널 설정 문제
```

**PromQL 문법 오류가 있는 경우**:
- 일반적인 실수: counter 메트릭에 `rate()` 누락
- 올바름: `rate(http_requests_total[5m])`
- 잘못됨: `http_requests_total[5m]` (문법 오류)

**Step 4: 시간 범위 확인 (1분)**

```bash
# 대시보드 시간 범위 선택기 확인 (우측 상단)
# 현재 시간 범위: 최근 5분? 최근 1시간?

# 메트릭이 축적되려면 시간 필요
# 최소 2개의 데이터 포인트 필요
# 15초 스크래프 간격 기준:
# - 최근 30초: 아무것도 표시 안 될 수 있음
# - 최근 5분: 데이터 표시 되어야 함
# - 최근 1시간: 확실히 데이터 있음

# 시간 범위가 "최근 30초"이면:
# "최근 1시간"으로 변경해서 데이터 확인
```

**Step 5: 메트릭이 존재하는지 확인 (1분)**

```bash
# 메트릭에 직접 쿼리
curl http://localhost:9090/api/v1/query?query=http_request_duration_seconds

# 다음을 확인:
# {"status":"success","data":{"result":[{"metric":{...},"value":[ts,"value"]}]}}

# 결과 배열이 비어있으면:
# 메트릭이 없거나 아직 데이터 포인트 없음
# 트래픽 생성해야 메트릭 생성
```

**메트릭이 없는 경우**:
- 트래픽 생성: `for i in {1..10}; do curl http://localhost:8080/actuator/health; done`
- 30초 대기 (스크래프 사이클)
- Grafana 새로고침

**Step 6: 레이블 필터 확인 (1분)**

```bash
# 일부 쿼리에는 레이블 필터가 있음: job="app", method="GET"

# 사용 가능한 레이블 값 쿼리:
curl 'http://localhost:9090/api/v1/label/job/values'
# 반환: ["app", "prometheus", "alertmanager"]

# 레이블 필터가 존재하지 않는 값을 사용하면:
# 쿼리가 데이터 반환 안 함

# PromQL의 필터 문법 확인:
# 올바름: {job="app"} 또는 {job=~"app|system"}
# 잘못됨: {job:="app"} (잘못된 연산자)
```

### 일반적인 원인 및 해결책

| 증상 | 원인 | 해결책 |
|------|------|--------|
| 항상 "데이터 없음" | Prometheus datasource 다운 | Prometheus 재시작, URL 확인 |
| 특정 패널 데이터 없음 | 잘못된 PromQL 문법 | Prometheus UI에서 문법 확인 |
| 새 쿼리 데이터 없음 | 아직 메트릭 생성 안 됨 | 트래픽 생성, 1분 대기 |
| 시간 범위가 비어있음 | 시간 범위가 너무 짧음 | 최근 1시간으로 변경 |
| 쿼리 작동, 패널 미작동 | Grafana 캐시 | 캐시 삭제: Cmd+R 새로고침 |

### 예방법

1. **편집 후 대시보드 테스트**:
   - 쿼리 변경
   - Prometheus에서 먼저 데이터 반환하는지 확인
   - 그 다음 Grafana에 추가

2. **시간 범위 매크로 사용**:
   - 타임스탐프 하드코딩하지 않기
   - `$__from`과 `$__to` Grafana 변수 사용
   - 대시보드 시간 범위 선택기가 작동하도록 함

3. **데이터 검증 쿼리 추가**:
   ```
   대시보드: Status 패널에 다음 추가:
   쿼리: count(up{job="app"})
   표시: 0 또는 1 (app이 스크래프되는지 여부)
   ```

---

## Alert가 발동하지 않음

### 증상

- Alert 조건이 충족되어도 발동 안 됨
- 이전에 발동하던 alert이 멈춤
- Prometheus에서 alert이 "Inactive"로 표시
- 알림 수신 안 됨

### 진단 절차

**Step 1: Alert 규칙이 존재하는지 확인 (1분)**

```bash
# 모든 alert 규칙 나열
curl http://localhost:9090/api/v1/rules

# HTTPErrorRateHigh 같은 alert 찾기
# 출력에 있어야 함

# 없으면:
# - Alert 규칙 파일이 로드되지 않음
# - 파일 존재 확인: ls monitoring/prometheus-rules.yml
# - 파일 문법 확인: yamllint prometheus-rules.yml
```

**alert 규칙 파일을 찾을 수 없는 경우**:
- prometheus.yml에서 올바른 rule_files 경로 확인:
  ```yaml
  rule_files:
    - '/etc/prometheus/rules/*.yml'
  ```
- alert 규칙을 올바른 디렉토리에 배치
- Prometheus 재시작: `docker restart prometheus`

**Step 2: Alert 규칙 문법 확인 (1분)**

```bash
# PromQL 표현식 검증
# alert 조건을 Prometheus Graph에 복사

# 예: HTTPErrorRateHigh는 다음을 포함:
# (sum(rate(http_requests_total{status=~"5.."}[5m])) 
#  / sum(rate(http_requests_total[5m]))) * 100 > 5

# http://localhost:9090/graph에 붙여넣기
# 실행하고 오류 확인

# Prometheus에서 문법 오류가 있으면:
# PromQL 표현식 수정
```

**Step 3: Alert 상태 확인 (1분)**

```bash
# Prometheus UI: Alerts 탭
# http://localhost:9090/alerts

# 자신의 alert 찾기 (예: HTTPErrorRateHigh)
# 상태 열 확인:

# - "Inactive": 조건 충족 안 됨 (OK)
# - "Pending": 조건 충족, "for" 지속시간 대기 중
# - "Firing": 조건 충족 시간이 지남, 발동 해야 함

# "Inactive"이지만 조건이 충족되면:
# → 평가가 지연될 수 있음
# → Prometheus 평가 간격이 느릴 수 있음
```

**Step 4: Alert "For" 지속시간 확인 (1분)**

```bash
# Alert는 "for" 지속시간 동안 Pending 상태여야 함

# 예:
# Alert 발동 조건: error_rate > 5% for 5 minutes

# 타임라인:
# 12:00 - error_rate = 3% (alert 없음)
# 12:01 - error_rate = 6% (Pending 시작)
# 12:05 - error_rate = 5.5% (Pending 만료, 발동)
# 12:06 - error_rate = 2% (해결)

# Alert가 Pending 상태이지만 Firing이 아니면:
# 더 기다려야 할 수도 있음
```

**Step 5: Alert 표현식 직접 테스트 (2분)**

```bash
# 높은 에러율을 수동으로 생성해서 테스트

# 5xx 에러 생성:
for i in {1..100}; do 
  curl http://localhost:8080/api/nonexistent 2>/dev/null
done

# Alert 조건 및 "for" 지속시간 대기 (5분)
# Prometheus alerts 페이지 확인
# Alert이 "Firing"이어야 함

# 계속 발동하지 않으면:
# → Alert 규칙 문법 오류
# → 시간 범위 문제
```

**Step 6: Alert 알림 확인 (1분)**

```bash
# Alert는 발동하지만 알림을 받지 못함?

# Prometheus에서 alert 이름 클릭 → Rule 보기
# "Annotation" 섹션 보기
# 레이블 및 annotation이 있어야 함

# Grafana:
# Alerting → Contact Points
# 알림 채널이 설정되어 있는지 확인
# 테스트 클릭 (Test 버튼)

# 테스트 알림이 실패하면:
# → 알림 채널 설정이 잘못됨
```

### 일반적인 원인 및 해결책

| 증상 | 원인 | 해결책 |
|------|------|--------|
| Alert 규칙 찾을 수 없음 | 규칙 파일이 로드 안 됨 | rule_files 경로 확인, 재시작 |
| 규칙에 문법 오류 | 유효하지 않은 PromQL | 표현식 수정, Graph 탭에서 테스트 |
| 조건 충족해도 발동 안 됨 | "For" 지속시간이 너무 길거나 | Alert 규칙에서 "for" 시간 감소 (테스트용 5분→1분) |
| 발동하지만 알림 없음 | 채널이 설정 안 됨 | Grafana에서 contact point 추가 |
| 잘못된 채널로 알림 전송 | 잘못된 알림 채널 선택 | Grafana 알림 정책 확인 |

### 예방법

1. **새 alert 테스트**:
   ```bash
   # 새 alert 추가 후:
   # 1. 규칙 파일에 추가
   # 2. Prometheus 재시작
   # 3. 조건을 수동으로 트리거
   # 4. 5-10분 내에 발동하는지 확인
   ```

2. **Alert 인프라 모니터링**:
   ```bash
   # 대시보드에 다음을 표시하는 대시보드 생성:
   - ALERTS{alertstate="firing"} (발동 중인 alert 수)
   - ALERTS_FOR_STATE{alertstate="pending"} (pending 상태)
   ```

3. **정기적 alert 테스트**:
   ```bash
   # 매월: 조건을 생성해서 각 alert 테스트
   # 결과를 incident 로그에 문서화
   ```

---

## 서비스에 연결할 수 없음

### 증상

- Prometheus, Grafana, Loki에 연결할 수 없음
- `Connection refused` 오류
- 포트가 수신 대기 중이 아님
- 서비스 컨테이너가 실행 중이지만 응답 없음

### 진단 절차

**Step 1: 서비스가 실행 중인지 확인 (30초)**

```bash
# 모든 컨테이너 확인
docker-compose ps

# 모든 서비스가 "Up"이어야 함
# "Exited" 또는 "Restart" 상태면:
# 서비스가 충돌했거나 unhealthy
```

**서비스가 실행되지 않는 경우**:
```bash
# 시작
docker-compose up -d <service_name>

# 시작 로그 확인
docker logs <service_name> | tail -50
```

**Step 2: 포트가 수신 대기 중인지 확인 (1분)**

```bash
# 포트가 실제로 수신 대기 중인지 확인
docker exec <service_name> ss -tlnp | grep <port>

# 예:
docker exec prometheus ss -tlnp | grep 9090
# 다음과 같이 표시되어야 함: tcp ... :9090 ... LISTEN

# LISTEN이 표시 안 되면:
# 포트가 노출되지 않음 또는 서비스가 연결 수락 안 함
```

**Step 3: 로컬 연결 테스트 (1분)**

```bash
# 서비스 컨테이너에서 접근 시도
docker exec <service_name> curl http://localhost:<port>/health

# 예:
docker exec prometheus curl http://localhost:9090/-/healthy

# 여기서 작동하지만 호스트에서 작동 안 하면:
# → Docker 네트워크 문제
```

**Step 4: Docker 네트워크 확인 (1분)**

```bash
# 모든 서비스가 같은 네트워크에 있는지 확인
docker network inspect bridge | grep -A2 '"Name"'

# 다음을 나열해야 함: app, prometheus, grafana, loki, promtail
# 모두 같은 네트워크에

# 누락된 것이 있으면:
# docker-compose up -d <service_name>
```

**Step 5: 서비스 로그 확인 (2분)**

```bash
# 시작 오류 찾기
docker logs <service_name> | grep -i -E "error|exception|panic" | head -20

# 일반적인 시작 오류:
# - "Address already in use" → 포트 충돌
# - "Connection refused" → 의존성에 연결 불가
# - "Configuration error" → 설정 파일 유효하지 않음
# - "Out of memory" → 컨테이너에 메모리 부족
```

**"Address already in use"인 경우**:
```bash
# 포트를 사용 중인 프로세스 찾기
lsof -i :<port>          # Mac/Linux
netstat -ano | findstr :<port>  # Windows

# 프로세스 종료 또는 다른 포트에서 재시작
docker-compose down
# docker-compose.yml에서 포트 변경
docker-compose up -d
```

**Step 6: 서비스 재시작 (1분)**

```bash
# 문제 있는 서비스 재시작
docker-compose restart <service_name>

# 시작 대기 (10-30초)
sleep 30

# 상태 확인
docker-compose ps
docker logs <service_name> | tail -20
```

### 일반적인 원인 및 해결책

| 서비스 | 증상 | 원인 | 해결책 |
|--------|------|------|---------|
| Prometheus | 포트 9090 수신 안 함 | 설정 오류 | prometheus.yml 문법 확인, 재시작 |
| Grafana | 포트 3000 수신 안 함 | 인증 문제 | 암호 재설정, 재시작 |
| Loki | 503 반환 | 저장소 꽉 참 | 이전 로그 삭제, 디스크 증가 |
| Promtail | 로그 전송 안 함 | 설정 오류 | loki-config.yml 확인, 재시작 |
| App | 포트 8080 수신 안 함 | 앱 충돌 | 로그에서 exception 확인, 재시작 |

### 예방법

1. **헬스 체크 엔드포인트**:
   ```bash
   # 모니터링에 추가:
   - Prometheus: curl http://localhost:9090/-/healthy
   - Grafana: curl http://localhost:3000/api/health
   - Loki: curl http://localhost:3100/ready
   ```

2. **자동 재시작**:
   ```yaml
   # docker-compose.yml
   services:
     prometheus:
       restart: unless-stopped  # 충돌 시 자동 재시작
   ```

3. **컨테이너 상태 모니터링**:
   ```bash
   # 실행되지 않는 컨테이너에 alert
   docker-compose ps | grep -v "Up"
   ```

---

## 메모리 누수

### 증상

- Heap 사용량이 지속적으로 증가
- 메모리가 기준선으로 돌아가지 않음
- 애플리케이션이 시간이 지남에 따라 느려짐
- 결국 OutOfMemoryError (OOM) 발생
- 재시작하면 임시 해결됨

### 진단 절차

**Step 1: 메모리 누수 패턴 확인 (5분)**

```bash
# 시간에 따른 heap 모니터링
docker stats app --no-stream | watch

# 메모리 사용량 기록:
# 시작: 500MB
# 1시간: 800MB
# 2시간: 1100MB
# 지속적 증가 = 메모리 누수
# 톱니 패턴 = 정상적 GC
```

**톱니 패턴 (정상)**:
```
1800MB ▀▀▀▀▄
1600MB ▀▀▄▀▄
1400MB ▀▄▀▄▀
1200MB ▄▀▄▀▄
 800MB ▄▀▄▀▄

15분 GC 간격으로 정상
메모리가 증가 후 GC가 내려옴
```

**선형 증가 (메모리 누수)**:
```
2000MB                    ▀▀▀▀▀
1800MB                ▀▀▀▀▀
1600MB            ▀▀▀▀▀
1400MB        ▀▀▀▀▀
1200MB    ▀▀▀▀▀

지속적 증가, 사이클마다 기준선 상승
객체가 GC되지 않음을 나타냄
```

**Step 2: Heap 덤프 생성 (2분)**

```bash
# 메모리가 높을 때 heap 캡처
docker exec app jmap -dump:live,format=b,file=heap.bin 1
# (PID 1은 컨테이너의 app)

# 컨테이너에서 추출
docker cp app:/heap.bin ./heap.bin

# Eclipse MAT (데스크탑 도구) 또는 jhat (명령줄)으로 분석
jhat -J-Xmx2g heap.bin
# http://localhost:7000에서 접근
```

**Step 3: Heap 덤프 분석 (5-10분)**

```bash
# jhat 웹 UI에서:
1. "Show Heap Histogram" 찾기
2. "Total Memory"로 정렬
3. 예상 밖의 큰 객체 찾기 (애플리케이션 클래스 찾기)
4. 의심 클래스 예:
   - 100,000개 요소를 포함하는 ArrayList
   - 무제한 증가하는 HashMap
   - 데이터를 축적하는 String 배열
```

**Step 4: 최근 코드 변경 검토 (5분)**

```bash
# 메모리 누수가 시작된 시간 파악
git log --oneline -n 20

# 그 주변 커밋 확인
git diff <commit> <previous_commit>

# 다음을 찾기:
# - 새로운 cache 구현
# - remove/evict 없는 새 collection
# - 절대 제거되지 않는 새 listener
# - 데이터를 축적하는 새 Thread
```

### 일반적인 메모리 누수 패턴

**패턴 1: 무제한 Cache**
```java
// 잘못된 예: Cache가 무한정 커짐
private static final Map<String, Object> cache = new HashMap<>();

public void processCard(String cardId) {
    if (!cache.containsKey(cardId)) {
        cache.put(cardId, expensiveCompute(cardId));
    }
}

// 수정: 제한된 cache에 eviction
private final LoadingCache<String, Object> cache = 
    CacheBuilder.newBuilder()
        .maximumSize(10000)  # 제한됨
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build(new CacheLoader<String, Object>() {...});
```

**패턴 2: 제거되지 않는 Listener**
```java
// 잘못된 예: Listener가 절대 제거되지 않음
@PostConstruct
public void init() {
    eventBus.register(this);  # 등록해제 없음
}

// 수정: 종료 시 listener 제거
@PreDestroy
public void shutdown() {
    eventBus.unregister(this);
}
```

**패턴 3: ThreadLocal이 정리되지 않음**
```java
// 잘못된 예: ThreadLocal이 정리 안 됨
private static final ThreadLocal<List> items = new ThreadLocal<>();

public void add(String item) {
    items.get().add(item);  # get()이 null이면 NPE이지만...
}

// 수정: 항상 정리
try {
    # 작업
} finally {
    items.remove();  # 중요!
}
```

**패턴 4: List가 누적됨**
```java
// 잘못된 예: List가 계속 커짐
private final List<String> allUserIds = new ArrayList<>();

public void loadUser(String userId) {
    allUserIds.add(userId);  # 영원히!
    # user 가져오기...
}

// 수정: 제한된 큐 사용 또는 이전 항목 제거
```

### 메모리 누수 해결

**즉시 해결** (임시):
```bash
# 애플리케이션 재시작
docker restart app

# 메모리를 모니터링해서 누수 확인
# 메모리가 기준선에서 시작해야 함
```

**영구 해결** (근본 원인 찾기 및 수정):

1. **누수 원인 파악**:
   - heap 덤프 분석 사용
   - git 로그에서 최근 변경 확인
   - 메모리 스파이크 타이밍을 배포와 연결

2. **코드 수정**:
   - cache에 제한 추가
   - listener를 올바르게 제거
   - thread local 정리
   - finally 블록에서 리소스 닫기

3. **수정 테스트**:
   ```bash
   # 수정 배포
   # 24시간+ 메모리 모니터링
   # 축적이 없는지 확인
   ```

### 예방법

1. **코드 리뷰 중점**:
   - 제한이 없는 cache 찾기
   - 제거되지 않는 listener 찾기
   - 정리되지 않는 thread local 찾기
   - 누적되는 collection 찾기

2. **자동 감지**:
   - 큰 데이터셋으로 unit test 실행
   - test 정리 후 메모리 확인
   - heap 증가 > 시간당 20%이면 alert

3. **프로덕션 모니터링**:
   - heap 증가율을 표시하는 대시보드
   - 증가율 > 임계값이면 alert
   - 정기적 heap 분석 일정 잡기

---

## 데이터베이스 성능 문제

### 증상

- API 응답이 느림 (p95 > 1초)
- 데이터베이스 CPU 높음
- 연결 풀이 고갈됨
- 느린 쿼리 로그가 계속 쌓임
- 사용자가 타임아웃 보고

### 진단 절차

**Step 1: 쿼리 성능 확인 (2분)**

```bash
# 느린 쿼리 로그 활성화 (아직 안 된 경우)
# 데이터베이스에서:
SET log_min_duration_statement = 100;  # 100ms 이상 쿼리 로그

# 느린 쿼리 확인
SELECT query, calls, mean_time
FROM pg_stat_statements
WHERE mean_time > 100
ORDER BY mean_time DESC
LIMIT 10;

# 가장 느린 쿼리 파악
```

**Step 2: 쿼리 계획 분석 (2분)**

```sql
-- 가장 느린 쿼리에 대해 실행 계획 가져오기
EXPLAIN ANALYZE
SELECT * FROM cards WHERE status = 'active' AND user_id = 123;

-- 다음을 찾기:
-- - Sequential Scan (느림, Index Scan이어야 함)
-- - 높은 행 수 (쿼리가 너무 많은 행 반환)
-- - Nested loops (N+1 문제)

-- 예:
-- Sequential Scan on cards  <- 나쁜 예: 전체 테이블 스캔
-- Index Scan using idx_cards_status <- 좋은 예: 인덱스 사용
```

**Step 3: 누락된 인덱스 확인 (2분)**

```sql
-- 검색되지만 인덱스 없는 컬럼 찾기
SELECT schemaname, tablename, attname
FROM pg_stats
WHERE schemaname NOT LIKE 'pg_%'
ORDER BY null_frac DESC;

-- 느린 쿼리를 가지는 테이블 확인
-- WHERE/JOIN 컬럼에 인덱스가 있는지 확인

-- 예: user_id WHERE 절에 인덱스 없음
EXPLAIN SELECT * FROM cards WHERE user_id = 123;
-- Sequential Scan이 표시되면, 인덱스 생성:
CREATE INDEX idx_cards_user_id ON cards(user_id);
```

**Step 4: Lock 확인 (1분)**

```sql
-- 오래 실행되고 있는 트랜잭션 찾기
SELECT pid, query, query_start, state_change
FROM pg_stat_activity
WHERE query_start < NOW() - INTERVAL '5 minutes'
AND state != 'idle';

-- 발견되면 오래 실행 중인 쿼리 종료:
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE pid <> pg_backend_pid()
AND query_start < NOW() - INTERVAL '10 minutes';
```

**Step 5: 연결 풀 확인 (1분)**

```bash
# 연결 사용량 모니터링
docker exec postgres psql -c "SELECT count(*) FROM pg_stat_activity;"

# 설정된 최대 연결 수 확인
docker exec postgres psql -c "SHOW max_connections;"

# 제한에 가까우면 연결이 가용성을 기다리는 상태
# API 타임아웃 유발
```

**Step 6: 최근 스키마 변경 검토 (1분)**

```bash
# 적용된 마이그레이션 확인
docker exec app ./gradlew flywayInfo

# 인덱스를 추가/제거/수정하는 마이그레이션이 있나?
# nullable 컬럼을 추가하는 마이그레이션이 스캔에 영향을 미치나?
```

### 일반적인 원인 및 해결책

| 증상 | 원인 | 해결책 |
|------|------|--------|
| 테이블에 Sequential scan | 인덱스 누락 | WHERE 컬럼에 CREATE INDEX |
| 계획에 Nested loops | N+1 쿼리 | JOIN 사용, loop 대신 |
| 높은 행 수 반환됨 | 잘못된 쿼리 | WHERE 절로 필터링 추가 |
| 연결 풀 꽉 참 | 느린 쿼리 | 느린 쿼리 최적화 또는 풀 증가 |
| Lock 타임아웃 | 오래 실행 중인 트랜잭션 | 오래된 쿼리 종료 |
| 디스크 I/O 높음 | 테이블/인덱스 bloat | VACUUM, REINDEX 실행 |

### 빠른 성능 튜닝

**누락된 인덱스 추가**:
```sql
-- 일반적 검색 패턴의 경우
CREATE INDEX idx_cards_user_id_status 
ON cards(user_id, status) 
WHERE status = 'active';  # Partial index

-- 정렬을 위해
CREATE INDEX idx_cards_created_at 
ON cards(created_at DESC);  # ORDER BY DESC 용
```

**N+1 쿼리 최적화**:
```java
# 잘못된 예: N+1 쿼리
List<Card> cards = cardRepository.findAll();
for (Card card : cards) {
    card.getUser();  # 각 card마다 별도 쿼리!
}

# 수정: 단일 쿼리에서 JOIN
@Query("SELECT c FROM Card c JOIN FETCH c.user")
List<Card> findAllWithUser();
```

**결과 셋 제한**:
```java
# 잘못된 예: 무제한 쿼리
Page<Card> search(String keyword) {
    return repo.findByKeyword(keyword);  # 1M 결과 반환 가능
}

# 수정: Paginate
@Query("SELECT c FROM Card c WHERE c.name LIKE %:keyword%")
Page<Card> search(@Param("keyword") String keyword, Pageable pageable);
# 사용법: search("test", PageRequest.of(0, 50))
```

### 예방법

1. **코드 리뷰 중점**:
   - n+1 패턴 찾기
   - WHERE/JOIN 컬럼에 인덱스 존재 확인
   - 무제한 쿼리에 LIMIT 확인

2. **자동 느린 쿼리 alert**:
   ```sql
   # alert 생성
   SELECT query, mean_time FROM pg_stat_statements
   WHERE mean_time > 1000;  # 1초 초과 alert
   ```

3. **정기적 인덱스 유지보수**:
   ```bash
   # 주간 유지보수
   ANALYZE;  # 테이블 통계 업데이트
   VACUUM;   # 공간 회수
   REINDEX;  # bloat > 30%이면 인덱스 재구성
   ```

---

## 네트워크 연결 문제

### 증상

- 서비스들이 서로 도달할 수 없음
- "Connection refused" 오류
- DNS 해석 실패
- 간헐적 연결 타임아웃
- 한 컨테이너는 다른 것에 연결 가능하지만 세 번째는 불가

### 진단 절차

**Step 1: Docker 네트워크 확인 (1분)**

```bash
# Docker 네트워크 나열
docker network ls

# 사용 중인 네트워크 검사
docker network inspect bridge  # 또는 사용자 정의 네트워크 이름

# app, prometheus, grafana, loki, promtail이 표시되어야 함
# 모두 같은 네트워크에

# 연결 안 된 경우:
docker network connect <network_name> <container_name>
```

**Step 2: 컨테이너 연결성 테스트 (2분)**

```bash
# 한 컨테이너에서 다른 것을 테스트
docker exec prometheus nc -zv app 8080

# 예상: nc: connect to app port 8080 (tcp) succeeded!

# "Connection refused" 표시 시:
# - app이 port 8080을 수신 대기하지 않음
# - 방화벽이 차단
# - 컨테이너 이름 잘못됨

# 모든 연결 테스트:
docker exec prometheus nc -zv postgres 5432
docker exec prometheus nc -zv redis 6379
docker exec prometheus nc -zv loki 3100
```

**Step 3: DNS 해석 테스트 (1분)**

```bash
# 컨테이너에서 서비스 이름 해석
docker exec prometheus nslookup app

# app의 IP 주소를 반환해야 함

# "Host not found" 표시 시:
# - 서비스 이름 잘못됨
# - 서비스가 같은 네트워크에 없음
# - Docker DNS 미작동
```

**Step 4: 방화벽 규칙 확인 (1분)**

```bash
# docker-compose.yml에서 포트 매핑 확인
# 예:
# ports:
#   - "8080:8080"  # host:container

# 호스트에서 각 서비스 테스트
curl http://localhost:8080  # app
curl http://localhost:9090  # prometheus
curl http://localhost:3000  # grafana
curl http://localhost:3100  # loki
```

**Step 5: 서비스 설정 확인 (1분)**

```yaml
# docker-compose.yml
services:
  app:
    container_name: app
    ports:
      - "8080:8080"
    networks:
      - monitoring  # 같은 네트워크에 있어야 함

  prometheus:
    container_name: prometheus
    networks:
      - monitoring
    # 설정이 서비스 이름으로 app을 참조:
    # targets: ['app:8080']

# 서비스가 같은 네트워크에 없으면:
networks:
  monitoring:
    driver: bridge
```

### 일반적인 원인 및 해결책

| 증상 | 원인 | 해결책 |
|------|------|--------|
| "Connection refused" | 서비스가 수신 대기하지 않음 | 서비스 재시작, 포트 확인 |
| "No route to host" | 컨테이너가 네트워크에 없음 | `docker network connect` |
| "Host not found" (DNS) | 잘못된 서비스 이름 | docker-compose.yml에서 container_name 확인 |
| 간헐적 타임아웃 | 네트워크 혼잡 | 리소스 제한 확인, 메모리 증가 |
| 한 경로는 작동, 다른 건 안 됨 | 비대칭 라우팅 | 네트워크 토폴로지 확인, docker daemon 재시작 |

### 예방법

1. **docker-compose 네트워크 사용**:
   ```yaml
   networks:
     monitoring:
       driver: bridge
   
   services:
     app:
       networks: [monitoring]
     prometheus:
       networks: [monitoring]
   ```

2. **시작 시 연결성 검증**:
   ```bash
   # 시작
   docker-compose up -d
   sleep 10
   
   # 모든 중요 연결 테스트
   docker exec prometheus nc -zv app 8080 || exit 1
   docker exec app nc -zv postgres 5432 || exit 1
   ```

3. **네트워크 오류 모니터링**:
   - 로그의 "connection refused" 오류에 alert
   - DNS 해석 실패에 alert
   - 연결 실패율 추적

---

## 요약

이 플레이북은 가장 일반적인 모니터링 문제를 다룹니다. 새로운 문제를 마주쳤을 때:

1. **증상** 섹션으로 시작
2. **진단 절차**를 단계별로 따르기
3. **일반적인 원인 및 해결책** 표 참고
4. 해결되지 않으면 엔지니어링 팀으로 escalate

여기서 다루지 않는 문제의 경우:
- 서비스별 문서 확인 (Prometheus, Grafana, Loki)
- 애플리케이션 로그: `docker logs <service_name> | grep ERROR`
- 시스템 로그: `journalctl -xe`

---

**마지막 업데이트**: 2024-01-XX  
**버전**: 1.0  
**관리팀**: Platform Engineering Team  
**연락처**: ops-support@company.com
