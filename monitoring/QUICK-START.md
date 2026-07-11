# 모니터링 스택 빠른 시작 가이드

## Phase 7: 팀을 위한 빠른 시작

이 가이드는 최적화된 모니터링 스택을 빠르게 시작할 수 있도록 도와줍니다.

---

## 1. 첫 설정 (5분)

### Step 1: 설치 검증

```bash
# 검증 스크립트 실행
./monitoring/validate-stack.ps1

# 예상 출력: "All validation checks passed!"
```

### Step 2: 모니터링 UI 접속

| 서비스 | URL | 로그인 정보 |
|--------|-----|-----------|
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:19090 | - |
| Loki | http://localhost:3100 | - |

### Step 3: 애플리케이션 상태 확인

```bash
# 애플리케이션 상태 확인
curl http://localhost:8080/actuator/health

# 애플리케이션 메트릭 확인
curl http://localhost:8080/actuator/prometheus | head -20
```

---

## 2. 일일 운영 (10분)

### 헬스체크

```bash
# 빠른 상태 확인
docker ps | grep "deoham-be"

# 전체 검증 (선택사항)
./monitoring/validate-stack.ps1
```

### 대시보드 확인

1. **Grafana 열기:** http://localhost:3000
2. **로그인:** admin / admin
3. **대시보드 보기:**
   - 애플리케이션 상태 대시보드
   - 성능 메트릭 대시보드
   - 비즈니스 메트릭 대시보드

### Alert 모니터링

```bash
# 활성 alert 확인
curl http://localhost:19090/api/v1/alerts

# Prometheus UI에서 보기: http://localhost:19090/alerts
```

---

## 3. 자주 하는 작업

### 애플리케이션 메트릭 확인

```bash
# 특정 메트릭 쿼리
curl "http://localhost:19090/api/v1/query?query=http_server_requests_seconds_bucket" | jq '.'

# 또는 Prometheus UI 사용: http://localhost:19090/graph
```

### 로그 검색

```bash
# curl 사용
curl "http://localhost:3100/loki/api/v1/query?query=%7Bjob%3D%22promtail%22%7D" | jq '.'

# 또는 Grafana UI 사용: Explore → Loki
```

### 리소스 사용량 확인

```bash
# Docker 통계
docker stats --no-stream

# 또는 Grafana에서 확인: Dashboards → System Metrics
```

### 서비스 재시작

```bash
# 모든 서비스 재시작
docker compose down
docker compose up -d

# 헬스체크 대기
sleep 30

# 검증
./monitoring/validate-stack.ps1
```

---

## 4. 성능 모니터링

### 주의할 주요 메트릭

| 메트릭 | 정상 | 경고 | 중대 |
|--------|------|------|------|
| API 응답시간 (p95) | < 500ms | > 1s | > 2s |
| 에러율 | < 1% | > 5% | > 10% |
| 메모리 사용률 | < 60% | > 80% | > 95% |
| CPU 사용률 | < 30% | > 50% | > 80% |
| 로그 볼륨 | < 10/초 | > 50/초 | > 100/초 |

### 성능 확인

```bash
# Grafana에서 보기:
# 1. 애플리케이션 성능 대시보드
# 2. 시스템 리소스 대시보드
# 3. 비즈니스 메트릭 대시보드
```

---

## 5. 문제 해결 (3단계)

### 뭔가 잘못되었을 때

**Step 1:** 검증 실행
```bash
./monitoring/validate-stack.ps1
```

**Step 2:** 로그 확인
```bash
docker compose logs --tail 50
```

**Step 3:** 가이드 참고
- `monitoring/TROUBLESHOOTING-GUIDE.md` 참고
- 목차에서 문제 찾기
- 해결 단계 따라하기

### 자주 있는 문제 빠른 해결법

| 문제 | 빠른 해결법 |
|------|-----------|
| 대시보드에 데이터 없음 | 1분 대기 후 새로고침 |
| 컨테이너 시작 안 됨 | 로그 확인: `docker logs <container>` |
| CPU 사용률 높음 | 확인: `docker stats` |
| 디스크 용량 부족 | 실행: `docker system prune` |

---

## 6. 설정 파일

### 주요 파일 및 용도

| 파일 | 용도 | 수정할 때 |
|------|------|---------|
| `compose.yaml` | 컨테이너 설정 및 리소스 | 서비스 추가 또는 포트 변경 |
| `monitoring/prometheus.yml` | 메트릭 수집 | 스크래프 간격 또는 대상 변경 |
| `monitoring/prometheus-rules.yml` | Alert 규칙 | Alert 생성/수정 |
| `monitoring/loki-config.yml` | 로그 집계 | 보관 기간 또는 성능 변경 |
| `logback-spring.xml` | 애플리케이션 로깅 | 로그 레벨 또는 로테이션 변경 |

---

## 7. 성능 최적화 체크리스트

### 프로덕션 배포 전

- [ ] 검증 스크립트 실행 및 모든 체크 통과
- [ ] PERFORMANCE-GUIDE.md 검토
- [ ] application.yml에서 로그 레벨을 WARN으로 설정
- [ ] 백업 전략 구성
- [ ] 팀 교육 (문제 해결)
- [ ] Alert 절차 테스트
- [ ] 모니터링 대시보드 설정

### 정기 유지보수

- [ ] 주간: 리소스 사용량 검토
- [ ] 주간: 에러율 확인
- [ ] 월간: 디스크 사용량 검토
- [ ] 월간: 백업 검증
- [ ] 분기별: Alert 임계값 업데이트

---

## 8. 문서 맵

```
monitoring/
├── QUICK-START.md                 ← 현재 위치
├── PERFORMANCE-GUIDE.md           ← 상세 최적화
├── TROUBLESHOOTING-GUIDE.md       ← 문제 해결
├── PHASE-7-COMPLETION.md          ← 전체 상세정보
├── validate-stack.ps1             ← 검증 스크립트
├── prometheus.yml                 ← 메트릭 설정
├── prometheus-rules.yml           ← Alert 규칙
├── loki-config.yml               ← 로그 설정
├── promtail-config.yml           ← 로그 수집기
├── dashboards/                    ← 사전 구성 대시보드
└── grafana-*.yml                  ← Grafana 설정
```

---

## 9. 도움말 받기

### 자가 진단 리소스

1. **성능 문제**
   - 읽기: `PERFORMANCE-GUIDE.md`
   - 검색: "latency", "CPU", "memory"
   - 실행: 최적화 체크리스트 따라하기

2. **에러 또는 실패**
   - 실행: `./monitoring/validate-stack.ps1`
   - 읽기: `TROUBLESHOOTING-GUIDE.md`
   - 찾기: 문제 카테고리에서 자신의 에러 찾기
   - 따라하기: 해결 단계

3. **설정 질문**
   - 확인: 해당 YAML 파일 주석
   - 읽기: Docker Compose 모범 사례
   - 검토: 예제 설정

### 지원 워크플로우

```
문제 발생
    ↓
검증 스크립트 실행
    ↓
로그 확인 (docker logs)
    ↓
문제 해결 가이드 검색
    ↓
제안된 해결책 시도
    ↓
수정 검증 (스크립트 재실행)
    ↓
문제 기록 (향후 참고)
```

---

## 10. 유용한 명령어

### 빠른 접근 명령어

```bash
# 전체 검증
./monitoring/validate-stack.ps1

# 서비스 상태 확인
docker ps

# 모든 로그 보기
docker compose logs --tail 100

# 리소스 사용량
docker stats --no-stream

# 모든 서비스 재시작
docker compose down && docker compose up -d

# 이전 데이터 삭제
docker system prune -a

# 컨테이너 검사
docker inspect <container_id>

# Prometheus 접속
curl http://localhost:19090/api/v1/status/tsdb

# Loki 접속
curl http://localhost:3100/api/prom/tail

# 애플리케이션 상태 확인
curl http://localhost:8080/actuator/health
```

### PromQL 빠른 쿼리 예제

```bash
# CPU 사용률
curl "http://localhost:19090/api/v1/query?query=process_cpu_usage*100"

# 메모리 사용률
curl "http://localhost:19090/api/v1/query?query=jvm_memory_used_bytes{area=\"heap\"}/jvm_memory_max_bytes{area=\"heap\"}"

# 요청 비율
curl "http://localhost:19090/api/v1/query?query=rate(http_server_requests_seconds_count[5m])"

# 에러율
curl "http://localhost:19090/api/v1/query?query=rate(http_server_requests_seconds_count{status=\"500\"}[5m])"
```

---

## 11. 빠른 참고

### 헬스체크 엔드포인트

```bash
# 애플리케이션
curl http://localhost:8080/actuator/health

# Prometheus
curl http://localhost:19090/-/healthy

# Loki
curl http://localhost:3100/ready

# Grafana
curl http://localhost:3000/api/health
```

### 로그 접근

```bash
# 최근 앱 로그
tail -f ./logs/application.log

# 마지막 N줄
tail -n 100 ./logs/application.log

# 로그 검색
grep ERROR ./logs/application.log

# Docker 로그
docker logs deoham-be-app-1 --follow
```

### 컨테이너 검사

```bash
# 실행 중인 컨테이너 표시
docker ps

# 모든 컨테이너 표시
docker ps -a

# 컨테이너 상세정보
docker inspect deoham-be-app-1

# 컨테이너 통계
docker stats deoham-be-app-1
```

---

## 12. FAQ

**Q: 메트릭은 얼마나 자주 수집되나요?**
A: 기본 30초마다 (prometheus.yml에서 설정 가능)

**Q: 로그는 얼마나 오래 보관되나요?**
A: 기본 30일 (loki-config.yml에서 설정 가능)

**Q: 관리자 암호를 변경할 수 있나요?**
A: 네, Grafana: Settings → Preferences → Change Password

**Q: 사용자 정의 대시보드를 추가하려면?**
A: JSON을 `monitoring/dashboards/`에 복사하고 Grafana 재시작

**Q: 모니터링 빈도를 높일 수 있나요?**
A: 네, prometheus.yml의 scrape_interval을 줄이면 됨

**Q: 필요한 디스크 공간은 얼마인가요?**
A: 메트릭 ~100MB/일 + 로그 ~500MB/일

**Q: Alert를 비활성화할 수 있나요?**
A: 네, prometheus-rules.yml의 alert 규칙을 주석 처리

**Q: 최소 하드웨어 요구사항은?**
A: RAM 4GB, CPU 2코어, 디스크 20GB (30일 보관 기준)

---

## 13. 모범 사례

### 해야 할 것 (DO)

- ✓ 설정 변경 후 검증 실행
- ✓ 정기적으로 리소스 사용량 모니터링
- ✓ 문서 최신 유지
- ✓ 비프로덕션에서 먼저 Alert 테스트
- ✓ 최소 30일 로그 보관 유지
- ✓ 매일 메트릭 검토
- ✓ 문제 해결 단계 문서화

### 하지 말아야 할 것 (DON'T)

- ✗ 검증 경고 무시
- ✗ 테스트 없이 프로덕션 설정 수정
- ✗ 헬스체크 비활성화
- ✗ 에러 Alert 무시
- ✗ 프로덕션에서 로그 레벨을 DEBUG로 설정
- ✗ 백업 건너뛰기
- ✗ 팀 공지 없이 주요 변경

---

## 14. 다음 단계

### 처음 사용자

1. Section 1 (설정) 완료
2. Section 2 (일일 운영)에 익숙해지기
3. Section 5 (문제 해결) 북마크
4. Section 8 (문서 맵) 읽기

### 운영팀

1. PERFORMANCE-GUIDE.md 읽기
2. Alert 설정 이해
3. 긴급 절차 연습
4. Alert 라우팅 설정

### 개발자

1. 메트릭 엔드포인트 이해
2. 사용 가능한 메트릭 검토
3. 사용자 정의 메트릭 추가 방법 학습
4. 로그 상관관계 이해

---

## 15. 지원 연락처

문제나 질문이 있으면:

1. **첫 번째:** 이 가이드 및 문서 참고
2. **두 번째:** 문제 해결 가이드 확인
3. **세 번째:** 모니터링 대시보드 검토
4. **마지막:** DevOps 팀에 문의

---

**빠른 링크:**
- [Prometheus 문서](https://prometheus.io/docs/)
- [Grafana 문서](https://grafana.com/docs/grafana/)
- [Loki 문서](https://grafana.com/docs/loki/)
- [Docker Compose 문서](https://docs.docker.com/compose/)

**마지막 수정:** 2026-07-11

---
