# 모니터링 스택 문제 해결 가이드

## Phase 7: 문제 진단 및 해결

이 가이드는 일반적인 모니터링 스택 문제에 대한 체계적인 문제 해결 절차를 제공합니다.

---

## 1. 빠른 진단 체크리스트

### 초기 단계 (모든 문제)

1. **Docker 컨테이너 확인**
   ```bash
   docker ps
   docker ps -a  # 모든 컨테이너 표시
   ```

2. **컨테이너 로그 확인**
   ```bash
   docker logs deoham-be-prometheus-1
   docker logs deoham-be-loki-1
   docker logs deoham-be-grafana-1
   docker logs deoham-be-app-1
   ```

3. **서비스 상태 확인**
   ```bash
   # Prometheus
   curl http://localhost:19090/-/healthy
   
   # Loki
   curl http://localhost:3100/ready
   
   # Grafana
   curl http://localhost:3000/api/health
   ```

4. **검증 스크립트 실행**
   ```bash
   ./monitoring/validate-stack.ps1
   ```

---

## 2. 문제 카테고리

## 카테고리 A: 컨테이너 문제

### 문제: 컨테이너가 시작되지 않음

**증상:**
- 컨테이너가 즉시 종료됨
- `docker ps -a`에서 상태가 "Exited"
- 헬스체크가 통과하지 않음

**진단:**
```bash
# 1. 컨테이너 로그 확인
docker logs deoham-be-<service>-1 --tail 50

# 2. 설정 파일 확인
docker logs deoham-be-<service>-1 2>&1 | grep -i "error\|invalid\|fail"

# 3. 볼륨 확인
docker inspect deoham-be-<service>-1 | grep -A 10 "Mounts"

# 4. 포트 충돌 확인
netstat -an | grep LISTEN | grep <port>
```

**일반적인 원인 및 해결책:**

| 원인 | 해결책 |
|------|--------|
| 설정 문법 오류 | YAML 온라인 검증기 또는 린터로 검증 |
| 포트 이미 사용 중 | `compose.yaml`에서 포트 변경 |
| 볼륨 마운트 경로 없음 | 디렉토리 생성: `mkdir -p ./logs` |
| 디스크 공간 부족 | 디스크 정리: `docker system prune` |
| 메모리 부족 | Docker 메모리 제한 설정에서 증가 |

**해결 단계:**

1. **설정 오류의 경우:**
   ```bash
   # YAML 검증
   yamllint monitoring/prometheus.yml
   
   # 또는 온라인 검증기 사용
   # https://www.yamllint.com/
   ```

2. **포트 충돌의 경우:**
   ```bash
   # compose.yaml에서 포트 변경
   # 예: 19090:9090 → 19091:9090
   
   # Compose 재시작
   docker compose down
   docker compose up -d
   ```

3. **볼륨 문제의 경우:**
   ```bash
   # 누락된 디렉토리 생성
   mkdir -p ./logs
   mkdir -p ./monitoring
   chmod 777 ./logs
   
   # 컨테이너 재시작
   docker compose restart <service>
   ```

### 문제: 컨테이너가 자주 충돌함

**증상:**
- 컨테이너가 반복적으로 재시작됨
- `docker ps`에서 상태가 "Restarting"
- 헬스체크 실패

**진단:**
```bash
# 1. 재시작 정책 확인
docker inspect deoham-be-<service>-1 | grep -A 5 "RestartPolicy"

# 2. 최근 로그 확인
docker logs deoham-be-<service>-1 --tail 100 --timestamps

# 3. 리소스 사용량 확인
docker stats deoham-be-<service>-1

# 4. 디스크 사용량 확인
docker exec deoham-be-<service>-1 df -h
```

**일반적인 원인 및 해결책:**

| 원인 | 해결책 |
|------|--------|
| 메모리 부족 (OOM) | `compose.yaml`에서 메모리 제한 증가 |
| 디스크 공간 부족 | 이전 로그 및 데이터 정리 |
| 헬스체크 실패 | 헬스체크 타임아웃 연장 |
| 의존성 준비 미완료 | 의존하는 서비스 시작 대기 |

**해결 단계:**

1. **OOM 문제의 경우:**
   ```yaml
   # compose.yaml에서 메모리 증가
   deploy:
     resources:
       limits:
         memory: 2048M  # 현재값에서 증가
   ```

2. **디스크 공간의 경우:**
   ```bash
   # 정리
   docker system prune -a
   
   # 또는 수동으로 이전 볼륨 제거
   docker volume ls
   docker volume rm <volume_name>
   ```

3. **헬스체크 실패의 경우:**
   ```yaml
   # compose.yaml에서 타임아웃 조정
   healthcheck:
     timeout: 10s    # 5초에서 증가
     retries: 10     # 5에서 증가
   ```

---

## 카테고리 B: 메트릭 수집 문제

### 문제: Prometheus가 메트릭을 수집하지 않음

**증상:**
- Grafana 대시보드에 "데이터 없음" 표시
- Prometheus `/targets` 페이지에 "Down" 표시
- 메트릭 수가 0 또는 증가하지 않음

**진단:**
```bash
# 1. Prometheus 대상 확인
curl http://localhost:19090/api/v1/targets | jq '.'

# 2. 특정 작업 상태 확인
curl http://localhost:19090/api/v1/targets?job_name=deoham-app | jq '.data.activeTargets'

# 3. 스크래프 오류 확인
curl http://localhost:19090/api/v1/query?query=up

# 4. 메트릭 엔드포인트 확인
curl http://localhost:8080/actuator/prometheus | head -20

# 5. Prometheus 로그 확인
docker logs deoham-be-prometheus-1 | grep -i "scrape\|error"
```

**일반적인 원인 및 해결책:**

| 원인 | 진단 명령어 | 해결책 |
|------|-----------|--------|
| 앱이 응답하지 않음 | `curl http://localhost:8080` | 애플리케이션 시작, 헬스 엔드포인트 확인 |
| 메트릭 경로 잘못됨 | `/actuator/prometheus` URL 확인 | `prometheus.yml` 스크래프 경로 업데이트 |
| 네트워크 연결 문제 | `docker exec deoham-be-prometheus-1 curl http://app:8080` | DNS 또는 네트워크 설정 수정 |
| 방화벽 차단 | 포트 접근 확인 | 포트 8080을 Prometheus에 개방 |
| 메트릭 엔드포인트 비활성화 | `application.yml` 확인 | Actuator 엔드포인트 활성화 |

**해결 단계:**

1. **애플리케이션이 실행되지 않는 경우:**
   ```bash
   # 앱 상태 확인
   docker ps | grep deoham-be-app-1
   
   # 필요하면 앱 시작
   docker compose up -d app
   
   # 시작 대기
   sleep 10
   
   # 메트릭 엔드포인트 검증
   curl http://localhost:8080/actuator/prometheus
   ```

2. **메트릭 엔드포인트가 비활성화된 경우:**
   ```yaml
   # application.yml 또는 application-prod.yml에서
   management:
     endpoints:
       web:
         exposure:
           include: health,info,prometheus,metrics
   ```

3. **네트워크 문제의 경우:**
   ```bash
   # 컨테이너 간 연결 테스트
   docker compose exec prometheus curl http://app:8080/actuator/prometheus
   
   # 호스트명 확인
   docker compose exec prometheus nslookup app
   ```

4. **스크래프 설정이 잘못된 경우:**
   ```yaml
   # prometheus.yml에서 확인:
   scrape_configs:
     - job_name: 'deoham-app'
       metrics_path: '/actuator/prometheus'  # 올바른 경로
       static_configs:
         - targets: ['localhost:8080']       # 올바른 호스트
   ```

### 문제: 메트릭에 간격이 있거나 드물다

**증상:**
- 메트릭이 있지만 데이터 포인트가 누락됨
- 그래프에서 긴 데이터 없음 기간
- Alert 평가가 간헐적으로 실패

**진단:**
```bash
# 1. 메트릭 수 확인
curl http://localhost:19090/api/v1/query?query=count(ALERTS)

# 2. 스크래프 지속시간 확인
curl http://localhost:19090/api/v1/query?query=scrape_duration_seconds

# 3. 스크래프 오류 확인
curl http://localhost:19090/api/v1/query?query=scrape_samples_post_metric_relabeling

# 4. Prometheus 상태 확인
curl http://localhost:19090/api/v1/status/config

# 5. 스크래프 로그 검토
docker logs deoham-be-prometheus-1 --tail 100 | grep -i "scrape"
```

**일반적인 원인 및 해결책:**

| 원인 | 해결책 |
|------|--------|
| 스크래프 간격이 너무 김 | `prometheus.yml`에서 `scrape_interval` 감소 |
| 애플리케이션 타임아웃 | `prometheus.yml`에서 `scrape_timeout` 증가 |
| 높은 카디널리티 메트릭 | 메트릭 재라벨링을 구현해 카디널리티 감소 |
| 저장소 문제 | 디스크 공간 확인 및 TSDB 상태 확인 |

**해결 단계:**

1. **데이터가 드문 경우:**
   ```yaml
   # prometheus.yml에서
   global:
     scrape_interval: 15s    # 30초에서 감소
     scrape_timeout: 10s     # 기본값에서 연장
   ```

2. **타임아웃 문제의 경우:**
   ```yaml
   # prometheus.yml에서
   scrape_configs:
     - job_name: 'deoham-app'
       scrape_timeout: 15s    # 타임아웃 증가
   ```

3. **저장소 문제의 경우:**
   ```bash
   # 디스크 공간 확인
   docker exec deoham-be-prometheus-1 df -h
   
   # Prometheus 데이터 디렉토리 크기 확인
   docker exec deoham-be-prometheus-1 du -sh /prometheus
   
   # 필요하면 정리
   docker volume prune
   ```

---

## 카테고리 C: 로그 수집 문제

### 문제: Loki에 로그가 나타나지 않음

**증상:**
- Loki 쿼리가 결과 반환 안 함
- `logs/application.log` 파일이 존재하지만 Promtail이 읽지 않음
- Grafana에서 로그 표시 안 됨

**진단:**
```bash
# 1. 로그 파일 존재 확인
ls -lh ./logs/application.log

# 2. 로그 파일 권한 확인
stat ./logs/application.log

# 3. Promtail 로그 확인
docker logs deoham-be-promtail-1 | tail -50

# 4. Loki에 직접 쿼리
curl http://localhost:3100/loki/api/v1/query?query=\{job=\"promtail\"\}

# 5. Promtail 설정 확인
cat monitoring/promtail-config.yml

# 6. 로그 수집 테스트
echo "test log line" >> ./logs/application.log
sleep 5
curl http://localhost:3100/loki/api/v1/query?query=\{job=\"promtail\"\}
```

**일반적인 원인 및 해결책:**

| 원인 | 해결책 |
|------|--------|
| 로그 파일 없음 | 애플리케이션이 실행 중인지, 로깅하는지 확인 |
| Promtail이 로그 파일에 접근할 수 없음 | 파일 권한 확인: `chmod 644 ./logs/application.log` |
| Promtail의 로그 파일 경로 잘못됨 | `monitoring/promtail-config.yml`에서 경로 업데이트 |
| Loki가 실행 중이 아님 | Loki 시작: `docker compose up -d loki` |
| 네트워크 연결 | Promtail에서 Loki 연결 확인 |

**해결 단계:**

1. **로그 파일이 없는 경우:**
   ```bash
   # logs 디렉토리 생성
   mkdir -p ./logs
   
   # 애플리케이션 시작
   docker compose up -d app
   
   # 애플리케이션 시작 대기
   sleep 10
   
   # 로그 파일 생성 확인
   ls -la ./logs/application.log
   ```

2. **권한 문제의 경우:**
   ```bash
   # 파일 권한 수정
   chmod 644 ./logs/application.log
   chmod 755 ./logs
   
   # Promtail 재시작
   docker compose restart promtail
   ```

3. **Promtail 설정이 잘못된 경우:**
   ```yaml
   # monitoring/promtail-config.yml에서 확인:
   scrape_configs:
     - job_name: system
       static_configs:
         - targets:
             - localhost
           labels:
             job: promtail
   ```

4. **Loki가 응답하지 않는 경우:**
   ```bash
   # Loki 상태 확인
   curl http://localhost:3100/ready
   
   # Loki 재시작
   docker compose restart loki
   
   # 준비 대기
   sleep 10
   
   # 다시 테스트
   curl http://localhost:3100/ready
   ```

### 문제: 로그 수집 레이턴시가 높음

**증상:**
- Grafana에 로그가 5분 이상 지연되어 나타남
- Promtail 로그가 느린 처리량 표시
- Promtail에서 CPU 사용률 높음

**진단:**
```bash
# 1. Promtail 성능 확인
docker stats deoham-be-promtail-1

# 2. Loki 수집 레이트 확인
curl http://localhost:3100/loki/api/v1/query?query=loki_distributor_lines_received_total

# 3. 로그 볼륨 확인
wc -l ./logs/application.log

# 4. Promtail 로그에서 오류 확인
docker logs deoham-be-promtail-1 | grep -i "error\|fail"
```

**일반적인 원인 및 해결책:**

| 원인 | 해결책 |
|------|--------|
| 애플리케이션 로깅이 너무 자세함 | 로그 레벨을 WARN으로 감소 |
| Promtail 리소스 제한이 너무 낮음 | `compose.yaml`에서 메모리 증가 |
| Loki 수집 레이트 제한됨 | Loki 설정에서 `ingestion_rate_mb` 증가 |
| 네트워크 혼잡 | 네트워크 메트릭 확인, 로그 볼륨 감소 |

**해결 단계:**

1. **로그 볼륨 감소:**
   ```xml
   <!-- logback-spring.xml에서 -->
   <springProfile name="prod">
     <logger name="com.deoham" level="INFO" />
     <root level="WARN" />
   </springProfile>
   ```

2. **리소스 제한 증가:**
   ```yaml
   # compose.yaml에서
   promtail:
     deploy:
       resources:
         limits:
           memory: 256M        # 128MB에서 증가
   ```

3. **Loki 수집 조정:**
   ```yaml
   # loki-config.yml에서
   limits_config:
     ingestion_rate_mb: 16     # 8에서 증가
     ingestion_burst_size_mb: 32
   ```

---

## 카테고리 D: 대시보드 및 쿼리 문제

### 문제: Grafana 대시보드가 "데이터 없음"을 표시함

**증상:**
- 모든 패널이 비어있거나 오류 표시
- 데이터소스가 "Down" 상태 표시
- 쿼리 편집기에 빨간 오류 표시

**진단:**
```bash
# 1. Grafana 데이터소스 확인
curl http://localhost:3000/api/datasources \
  -H "Authorization: Bearer admin:admin"

# 2. 데이터소스 연결 테스트
curl http://localhost:3000/api/datasources/1/health \
  -H "Authorization: Bearer admin:admin"

# 3. Prometheus 연결 상태 확인
docker logs deoham-be-grafana-1 | grep -i "datasource\|error"

# 4. Prometheus에서 직접 메트릭 테스트
curl http://localhost:19090/api/v1/query?query=up
```

**일반적인 원인 및 해결책:**

| 원인 | 해결책 |
|------|--------|
| Prometheus가 다운되거나 연결할 수 없음 | Prometheus 시작, 네트워크 확인 |
| Loki가 다운되거나 연결할 수 없음 | Loki 시작, 네트워크 확인 |
| 데이터소스 URL이 잘못됨 | Grafana에서 데이터소스 URL 업데이트 |
| 아직 메트릭/로그 수집 안 됨 | 데이터 수집 대기 |

**해결 단계:**

1. **Prometheus에 연결할 수 없는 경우:**
   ```bash
   # Prometheus 시작
   docker compose up -d prometheus
   
   # 시작 대기
   sleep 10
   
   # 상태 확인
   curl http://localhost:19090/-/healthy
   
   # Grafana 새로고침
   ```

2. **데이터소스 URL이 잘못된 경우:**
   ```bash
   # Grafana 로그인 (admin/admin)
   # 설정 → 데이터 소스로 이동
   # Prometheus URL 업데이트: http://prometheus:9090
   # Loki URL 업데이트: http://loki:3100
   # 저장 및 테스트
   ```

3. **아직 데이터가 수집되지 않은 경우:**
   ```bash
   # 메트릭 수집 대기 (1-2분)
   sleep 120
   
   # 메트릭 수 확인
   curl http://localhost:19090/api/v1/query?query=count(ALERTS)
   
   # 대시보드 새로고침
   ```

### 문제: 쿼리가 느림

**증상:**
- 대시보드 로드에 10초 이상 소요
- "쿼리 타임아웃" 오류 메시지
- 쿼리 중 CPU 사용률 급증

**진단:**
```bash
# 1. 쿼리 지속 시간 확인
curl "http://localhost:19090/api/v1/query_range?query=up&start=$(date +%s -d '1 hour ago')&end=$(date +%s)&step=15s"

# 2. 느린 로그 확인
docker logs deoham-be-grafana-1 | grep -i "slow\|timeout"

# 3. Prometheus 쿼리 통계 확인
curl http://localhost:19090/api/v1/status/tsdb

# 4. 카디널리티 확인
curl http://localhost:19090/api/v1/query?query=count\(ALERTS\)
```

**일반적인 원인 및 해결책:**

| 원인 | 해결책 |
|------|--------|
| 높은 카디널리티의 복잡한 쿼리 | 쿼리 단순화, 집계 사용 |
| 쿼리 범위가 너무 큼 | 시간 범위를 최근 24시간으로 감소 |
| Prometheus 리소스 부족 | Prometheus 리소스 증가 또는 보관 기간 감소 |
| Grafana 네트워크 레이턴시 | 네트워크 연결 확인 |

**해결 단계:**

1. **쿼리 최적화:**
   ```promql
   # 나쁜 예: 높은 카디널리티
   http_server_requests_seconds_bucket
   
   # 좋은 예: 집계됨
   sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
   ```

2. **시간 범위 제한:**
   ```bash
   # 대시보드에서 기본 범위 설정
   Dashboard Settings → Time Picker → Default → 최근 24시간
   ```

3. **Prometheus 리소스 증가:**
   ```yaml
   # compose.yaml에서
   prometheus:
     deploy:
       resources:
         limits:
           memory: 2048M       # 1024MB에서 증가
   ```

---

## 카테고리 E: Alert 문제

### 문제: Alert가 발생하지 않음

**증상:**
- Alert 규칙이 "Inactive" 표시 (임계값 초과해도)
- AlertManager가 alert 수신 안 함
- Prometheus에서 수동 트리거 쿼리는 작동

**진단:**
```bash
# 1. Alert 규칙 상태 확인
curl http://localhost:19090/api/v1/rules | jq '.data.groups[].rules[] | select(.type=="alert")'

# 2. Alert 상태 확인
curl http://localhost:19090/api/v1/alerts

# 3. 규칙 평가 확인
curl http://localhost:19090/api/v1/query?query=ALERTS

# 4. 규칙 로그 확인
docker logs deoham-be-prometheus-1 | grep -i "alert\|rule"

# 5. 규칙 문법 검증
curl http://localhost:19090/api/v1/status/config | jq '.data.yaml'
```

**일반적인 원인 및 해결책:**

| 원인 | 해결책 |
|------|--------|
| 규칙 문법 오류 | 규칙 YAML 파일 검증 |
| Alert 조건이 충족되지 않음 | 임계값 및 메트릭 가용성 확인 |
| `for` 지속시간이 너무 김 | Alert 규칙에서 `for` 지속시간 감소 |
| AlertManager가 설정되지 않음 | Prometheus에서 AlertManager 설정 |

**해결 단계:**

1. **규칙 검증:**
   ```bash
   # 규칙 파일 문법 확인
   docker exec deoham-be-prometheus-1 cat /etc/prometheus/rules/prometheus-rules.yml | grep -A 5 "alert:"
   
   # Prometheus UI에서 확인
   # http://localhost:19090/alerts
   ```

2. **메트릭 가용성 확인:**
   ```bash
   # 메트릭 존재 확인
   curl http://localhost:19090/api/v1/query?query=jvm_memory_used_bytes
   
   # 없으면 메트릭 수집 대기
   sleep 30
   ```

3. **Alert 지속시간 감소:**
   ```yaml
   # prometheus-rules.yml에서
   - alert: MemoryUsageHigh
     expr: ...
     for: 30s          # 2m에서 감소 (테스트용)
   ```

### 문제: Alert가 너무 많이 발생함

**증상:**
- 사소한 문제로 alert 발생
- 경보 피로도 높음
- 정당한 alert 무시됨

**진단:**
```bash
# 1. Alert 빈도 확인
curl http://localhost:19090/api/v1/query?query=ALERTS

# 2. Alert 히스토리 분석
curl http://localhost:19090/api/v1/query_range?query=ALERTS&start=...&end=...

# 3. 임계값 검토
docker exec deoham-be-prometheus-1 cat /etc/prometheus/rules/prometheus-rules.yml | grep -E "threshold|>"
```

**일반적인 원인 및 해결책:**

| 원인 | 해결책 |
|------|--------|
| 임계값이 너무 민감함 | 임계값 증가 |
| `for` 지속시간이 너무 짧음 | `for` 지속시간을 5-10분으로 증가 |
| 메트릭 스파이크가 정상 | 평균 또는 백분위수 사용 |

**해결 단계:**

1. **임계값 조정:**
   ```yaml
   # 이전: 너무 민감함
   - alert: MemoryUsageHigh
     expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.80
   
   # 이후: 더 합리적
   expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.90
   ```

2. **지속시간 증가:**
   ```yaml
   # 이전: 즉시 alert
   for: 1m
   
   # 이후: 짧은 스파이크 허용
   for: 10m
   ```

---

## 3. 긴급 절차

### 모든 것이 다운된 경우

1. **기본 헬스 확인**
   ```bash
   docker ps -a
   docker compose status
   ```

2. **모든 것 재시작**
   ```bash
   docker compose down
   docker compose up -d
   ```

3. **상태 확인**
   ```bash
   ./monitoring/validate-stack.ps1
   ```

4. **로그 검토**
   ```bash
   docker compose logs --tail 100
   ```

### 디스크 공간이 중대할 경우

1. **사용량 확인**
   ```bash
   du -sh ./monitoring/*
   du -sh ./logs
   ```

2. **정리**
   ```bash
   # 이전 로그 제거
   find ./logs -name "*.log.*" -mtime +7 -delete
   
   # Docker 정리
   docker system prune -a
   ```

3. **데이터 볼륨 확인**
   ```bash
   docker volume ls
   docker volume inspect <volume_name>
   ```

---

## 4. 디버그 모드

### 상세 로깅 활성화

```yaml
# compose.yaml 또는 환경 변수에서
prometheus:
  environment:
    - GOGC=75  # 가비지 컬렉션 최적화

loki:
  environment:
    - GOGC=50

grafana:
  environment:
    - GF_LOG_LEVEL=debug
```

### 디버그 정보 수집

```bash
# 디버그 번들 생성
mkdir debug-bundle

# 로그 수집
docker logs deoham-be-prometheus-1 > debug-bundle/prometheus.log
docker logs deoham-be-loki-1 > debug-bundle/loki.log
docker logs deoham-be-grafana-1 > debug-bundle/grafana.log

# 상태 수집
docker ps > debug-bundle/docker-status.txt
docker stats --no-stream > debug-bundle/resource-usage.txt

# 설정 수집
cp monitoring/*.yml debug-bundle/
cp compose.yaml debug-bundle/
cp src/main/resources/application*.yml debug-bundle/

# 진단 수집
./monitoring/validate-stack.ps1 > debug-bundle/validation.log 2>&1
```

---

## 5. 도움말 받기

### 지원을 위한 유용한 명령어

```bash
# 전체 시스템 상태
docker compose status
docker stats

# Prometheus 진단
curl http://localhost:19090/api/v1/status/tsdb
curl http://localhost:19090/api/v1/status/config

# Loki 진단
curl http://localhost:3100/api/prom/tail
curl http://localhost:3100/config

# 애플리케이션 상태
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/prometheus
```

### Escalation 시점

- 재시작 시도에도 컨테이너가 계속 충돌함
- 디스크 공간이 빠르게 계속 채워짐
- 서비스 간 네트워크 연결 문제
- 데이터 손실 또는 손상 의심

---

## 6. 참고 자료

- Prometheus 문제 해결: https://prometheus.io/docs/prometheus/latest/troubleshooting/
- Grafana 문제 해결: https://grafana.com/docs/grafana/latest/troubleshooting/
- Loki 문제 해결: https://grafana.com/docs/loki/latest/troubleshooting/
- Docker 디버깅: https://docs.docker.com/config/containers/logging/

---

**마지막 업데이트:** 2026-07-11

---
