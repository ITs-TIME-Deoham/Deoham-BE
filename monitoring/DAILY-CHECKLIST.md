# 일일 운영 체크리스트

시스템 건강 유지를 위한 일일 모니터링 및 운영 작업

## 아침 스탠드업 (근무 시작)

**소요 시간**: 업무 시작 시점에 약 10분

### 시스템 상태 확인

- [ ] **Grafana Operator 대시보드 접근**
  ```
  URL: http://localhost:3000/d/operator
  예상: 전체 상태가 초록색
  노란색/빨간색이면: 즉시 조사
  ```

- [ ] **중요 Alert 확인**
  ```
  Grafana → Alerting → Alert Groups
  "Firing" alert 찾기
  모든 발동 중인 alert을 incident 로그에 기록
  예상: 중요 alert 0개
  ```

- [ ] **지난 24시간 트렌드 보기**
  ```
  Grafana: 시간 범위를 "Last 24h"로 변경
  확인 항목:
  - 에러율 트렌드 (flat하고 낮아야 함 < 0.1%)
  - 지연시간 트렌드 (안정적이어야 함)
  - 트래픽 패턴 (요일로 정상인가?)
  ```

- [ ] **모니터링 스택 상태 확인**
  ```bash
  # 모든 서비스 실행 중인지 확인
  docker-compose ps
  예상: 모든 서비스가 "Up" 표시
  
  # 주요 헬스 엔드포인트 확인
  curl http://localhost:8080/actuator/health
  curl http://localhost:9090/-/healthy
  curl http://localhost:3100/ready
  curl http://localhost:3000/api/health
  ```

### 문서 확인

- [ ] **어제 Incident 로그 검토**
  ```
  파일: monitoring/incidents/YYYY-MM-DD.log
  확인: 반복되는 문제가 있나?
  확인: 모든 이슈가 해결되었나?
  확인: 오늘 해야 할 action item이 있나?
  ```

- [ ] **최근 배포 검토**
  ```bash
  git log --oneline -n 5
  확인: 지난 24시간에 배포된 것이 있나?
  있다면: 문제가 없는지 더 주의 깊게 모니터링
  ```

### 요약

**시스템 상태**: ✓ 정상 / ⚠ 주의 / ✗ 긴급

**메모**:
```
(발견된 이슈, 중요 alert 상태 작성)
```

---

## 근무 시간 중

**빈도**: 업무 시간 중 30분마다

### 빠른 상태 확인

- [ ] **요청 비율 및 에러율**
  ```
  Grafana → Operator Dashboard → "Request Metrics" 패널
  확인:
  - 요청 비율: 이 시간에 정상인가?
  - 에러율: < 0.1%?
  - 갑작스러운 변화 (spike 또는 drop)?
  ```

- [ ] **API 지연시간**
  ```
  Grafana → Operator Dashboard → "Latency" 패널
  확인:
  - p95 지연시간: < 500ms?
  - 평소보다 느린 엔드포인트가 있나?
  ```

- [ ] **리소스 사용량**
  ```
  Grafana → Operator Dashboard → "Resources" 패널
  확인:
  - CPU: < 50%?
  - 메모리: < 70%?
  - 디스크: < 70%?
  ```

- [ ] **Alert 상태**
  ```
  Grafana → Alerting → Alerts
  확인: 마지막 확인 이후 새로운 firing alert이 있나?
  있으면: 즉시 조사
  ```

### 이슈 조치

**노란색 또는 빨간색 지표가 있으면**:
1. 시간과 문제 기록
2. 관련 대시보드에서 상세 정보 확인
3. [ALERT-PROCEDURES.md](./ALERT-PROCEDURES.md) 검토해서 대응 단계 확인
4. 조사 및 결과 문서화

### 근무 종료 요약

**근무 종료 시간 (~오후 5시)**:

- [ ] **일일 메트릭 리포트 작성**
  ```
  파일 생성: monitoring/daily-reports/YYYY-MM-DD-report.txt
  포함 내용:
  - 최고 요청 비율 (req/min)
  - 최고 에러율 (%)
  - 최고 p95 지연시간 (ms)
  - 발동된 alert (지속시간 포함)
  - Incident (있으면)
  - 리소스 사용량 최고값
  ```

- [ ] **야간 운영 관심사 확인**
  ```
  떠나기 전에:
  - 현재 발동 중인 alert이 있나?
  - 오늘 밤 예정된 유지보수가 있나?
  - 알려진 진행 중인 이슈가 있나?
  - 디스크 공간이 밤새 찼을까? (사용량 확인)
  ```

- [ ] **야간 근무자에게 인수인계**
  ```
  #incident 채널에 메시지 발송:
  "Shift handoff YYYY-MM-DD
  상태: ✓ 정상 / ⚠ 주의 / ✗ 긴급
  현재 이슈: [목록]
  야간 관심사: [목록]
  - [당직 엔지니어 이름]"
  ```

---

## 배포 전 체크리스트

**모든 변경 배포 전** (5-10분)

### 모니터링 준비 확인

- [ ] **메트릭 수집 확인**
  ```bash
  curl http://localhost:8080/actuator/prometheus | head -10
  예상: 최근 타임스탐프가 있는 메트릭
  비어있으면: 배포하지 말 것, 메트릭 미작동
  ```

- [ ] **로그 수집 확인**
  ```bash
  docker logs app | tail -5
  예상: 최근 로그 항목
  비어있으면: 배포하지 말 것, 로깅 미작동
  ```

- [ ] **Alert 시스템 준비**
  ```bash
  curl http://localhost:9090/api/v1/rules | jq '.data.groups | length'
  예상: > 0 alert group 로드됨
  0이면: 배포하지 말 것, alert 미로드
  ```

- [ ] **대시보드 접근 가능**
  ```bash
  curl -u admin:admin http://localhost:3000/api/health
  예상: status OK
  다운되면: 수정될 때까지 배포 금지
  ```

### 배포 전 상태

**기준 메트릭 설정**:
- [ ] Operator Dashboard 스크린샷 촬영
- [ ] 현재값 기록:
  - 에러율: ____%
  - p95 지연시간: ____ms
  - 요청 비율: ____req/min
  - CPU 사용률: ____%
  - 메모리 사용률: ____%

**모니터링에 배포 마크 추가**:
```bash
# Grafana에 배포 marker annotation 추가
# 배포 시간과 이슈의 상관관계 파악에 도움
```

---

## 배포 후 체크리스트

**변경 배포 후** (15-30분 모니터링)

### 즉시 확인 (배포 후 5분)

- [ ] **애플리케이션 헬스 확인**
  ```bash
  curl http://localhost:8080/actuator/health
  예상: {"status":"UP"}
  ```

- [ ] **에러율 변경 없음**
  ```
  Grafana → Error rate 패널
  예상: 여전히 < 0.1%, 새로운 spike 없음
  배포 전보다 높으면: 즉시 조사
  ```

- [ ] **지연시간 변경 없음**
  ```
  Grafana → Latency 패널
  예상: 배포 전과 유사
  배포 전 p95 + 100ms보다 크면: 조사
  ```

- [ ] **새로운 exception 없음**
  ```bash
  docker logs app | grep -i "exception\|error" | tail -10
  예상: ERROR 항목 없음 (WARN은 괜찮음)
  새로운 오류면: 변경사항 확인, 롤백 필요할 수 있음
  ```

### 확장된 모니터링 (배포 후 15분)

- [ ] **트래픽 패턴 정상**
  ```
  Grafana → Request Rate 패널
  예상: 이 시간에 정상적인 요청 비율
  안정적 패턴 표시 (spike 또는 drop 아님)
  ```

- [ ] **데이터베이스 성능 정상**
  ```bash
  # 느린 쿼리 수 확인
  docker exec postgres psql -c \
    "SELECT COUNT(*) FROM pg_stat_statements WHERE mean_time > 100;"
  예상: 기준선과 유사
  ```

- [ ] **리소스 사용량 정상**
  ```
  Grafana → Resource 패널
  예상:
  - CPU: 기준선의 ±10%
  - 메모리: 기준선의 ±20%
  - 디스크: 변경 없음
  ```

- [ ] **연쇄 alert 없음**
  ```
  Grafana → Alerts
  예상: 배포 전과 같은 alert
  새로운 alert 발동이면: 근본 원인 조사
  ```

### 최종 확인 (배포 후 30분)

- [ ] **전체 헬스 확인**
  ```bash
  # 포괄적인 헬스 체크 실행
  ./scripts/health-check.sh
  예상: 모든 체크 통과
  ```

- [ ] **배포 후 메트릭 비교**
  ```
  Operator Dashboard 스크린샷 촬영
  배포 전 스크린샷과 비교
  예상: 중요 변화 없음
  작은 개선 사항은 좋은 신호!
  ```

- [ ] **프로덕션 배포 가능**
  - [ ] 모든 체크 통과
  - [ ] 새로운 오류 없음
  - [ ] 성능 허용 가능
  - [ ] #deployments에 포스트: "Deployment successful"

### 롤백 트리거

**다음 조건일 때 자동 롤백**:
- [ ] 에러율 > 1% (배포 전 < 0.1%)
- [ ] p95 지연시간 > 2초 (배포 전 < 500ms)
- [ ] 반복된 OOM/메모리 오류
- [ ] 데이터베이스 연결 고갈
- [ ] 사용자의 광범위한 이슈 보고

**롤백 명령어**:
```bash
# 이전 버전에 대한 git 태그가 있다고 가정
git revert HEAD
./gradlew bootJar -x test
docker build -t deoham-app .
docker-compose up -d

# 시작 및 메트릭 수집 대기 (5분)
sleep 300

# 메트릭이 정상으로 돌아왔는지 확인
curl http://localhost:9090/api/v1/query?query=up
```

---

## 주간 검토 (금요일 오후)

**소요 시간**: 주말 앞 약 30-45분

### 메트릭 분석

- [ ] **주간 SLA 계산**
  ```
  가용성 = (가동 시간 / 168) × 100
  목표: 99.9% (최대 8.64시간 다운타임)
  실제: ____%
  통과: ✓ 예 / ✗ 아니오
  ```

- [ ] **에러율 트렌드**
  ```
  Grafana: "Last 7 days"로 변경
  - 최고 에러율: ____%
  - 최저 에러율: ____%
  - 평균: ____%
  트렌드: ↑ 상승 / → 안정 / ↓ 하락
  ```

- [ ] **지연시간 트렌드**
  ```
  Grafana: 지난 7일간 p95 지연시간
  - 최고 p95: ____ms
  - 최저 p95: ____ms
  - 평균: ____ms
  트렌드: ↑ 상승 / → 안정 / ↓ 하락
  ```

- [ ] **리소스 트렌드**
  ```
  최고 CPU: ___%  (목표: < 70%)
  최고 메모리: ___%  (목표: < 85%)
  최고 디스크: ___%  (목표: < 90%)
  우려 트렌드: ✓ 예 / ✗ 아니오
  ```

### Incident 검토

- [ ] **이번주 모든 Incident 나열**
  ```
  monitoring/incidents/ 폴더에서
  건수: ____ incidents
  ```

- [ ] **Incident 분석**
  ```
  각 incident에 대해:
  - 근본 원인: _____________
  - 심각도: 중요 / 경고 / 정보
  - 해결 시간: _____ 분
  - 이전 이슈의 반복?: 예 / 아니오
  ```

- [ ] **MTTR (평균 복구 시간)**
  ```
  해결 시간의 합 / Incident 수
  MTTR: _____ 분
  목표: 경고 < 30분
         중요 < 5분
  ```

### 용량 계획

- [ ] **성장 지표**
  ```
  요청 비율 성장: 지난주 대비 +___% 
  사용자 수 성장: 지난주 대비 +___% 
  데이터 볼륨 성장: 지난주 대비 +___% 
  필요 용량 예상: _____ 개월
  ```

- [ ] **리소스 사용량 트렌드**
  ```
  CPU: ↑ 상승 / → 안정 / ↓ 하락
  메모리: ↑ 상승 / → 안정 / ↓ 하락
  디스크: ↑ 상승 / → 안정 / ↓ 하락
  
  상승하면: 용량 검토 일정 잡기
  ```

### 팀 커뮤니케이션

- [ ] **주간 리포트 발송**
  ```
  Slack #weekly-reports 채널:
  
  "Weekly Ops Report - [Date 주간]
  
  가용성: 99.95% ✓
  Incident: 2개 (중요 0개, 경고 2개)
  평균 응답 시간: 8분 ✓
  
  하이라이트:
  - DB 최적화로 지연시간 15% 개선
  - 캐시의 메모리 누수 해결
  
  우려사항:
  - 디스크 사용량 주 5% 증가
  
  다음주:
  - 이전 로그 정리
  - 확장 전략 검토"
  ```

---

## 월간 검토 (월말 마지막 날)

**소요 시간**: 약 1-2시간

### SLA 준수 리포트

- [ ] **월간 가용성 계산**
  ```
  총 시간: 720 (30일 × 24시간)
  다운타임: _____ 시간
  가용성: (720 - ___) / 720 × 100 = ___%
  
  SLA 목표: 99.9%
  실제: ___%
  상태: ✓ 통과 / ✗ 미달
  
  미달한 경우:
  - 근본 원인 분석
  - Action item 생성
  - Incident 검토에 문서화
  ```

- [ ] **에러율 준수**
  ```
  목표: < 0.1% 5xx 에러
  실제: ___%
  상태: ✓ 통과 / ✗ 미달
  ```

- [ ] **지연시간 준수**
  ```
  목표: 시간대의 99% p95 < 500ms
  실제: ___%
  상태: ✓ 통과 / ✗ 미달
  ```

### Incident 분석

- [ ] **모든 Incident 분류**
  ```
  총 incident: ____
  심각도별:
  - 중요: ____ (목표: 0)
  - 경고: ____ (목표: < 5)
  - 정보: ____ (목표: 없음)
  
  컴포넌트별:
  - 애플리케이션: ____
  - 데이터베이스: ____
  - 인프라: ____
  - 제3자: ____
  
  근본 원인별:
  - 설정: ____
  - 리소스 부족: ____
  - 코드 버그: ____
  - 외부: ____
  ```

- [ ] **상위 반복 이슈**
  ```
  1. ______________ (____% incident)
  2. ______________ (____% incident)
  3. ______________ (____% incident)
  
  Action item:
  - [ ] 이슈 1: [예방 조치]
  - [ ] 이슈 2: [예방 조치]
  - [ ] 이슈 3: [예방 조치]
  ```

### 성능 분석

- [ ] **성능 개선 확인**
  ```
  메트릭: _____________
  이전: ____ 
  이후: ____
  개선: +___% / -__% (개선/악화)
  ```

- [ ] **성능 하락 확인**
  ```
  메트릭: _____________
  이전: ____
  이후: ____
  하락: +___% (악화)
  근본 원인: _______________
  적용된 해결책: _______________
  ```

### 운영 개선

- [ ] **프로세스 개선**
  ```
  이번 달 잘 작동한 것:
  - [Runbook이 incident X 해결에 시간 절약]
  - [Alert Y가 사용자 영향 전에 문제 포착]
  
  개선할 수 있는 것:
  - [Issue Z 디버그에 시간 오래 소요]
  - [Problem A에 대한 alert 누락]
  
  다음 달 변경사항:
  - [ ] [조건]에 대한 alert 추가
  - [ ] [Incident type]에 대한 runbook 개선
  - [ ] [Manual process] 자동화
  ```

### 용량 계획

- [ ] **저장소 분석**
  ```
  Prometheus 데이터: __GB (목표: < 50GB)
  Loki 로그: __GB (목표: < 30GB)
  데이터베이스: __GB (목표: 디스크의 < 70%)
  
  성장률:
  - Prometheus: +__GB/주
  - Loki: +__GB/주
  
  꽉 차기까지의 개월: ____
  조치 필요: ✓ 예 / ✗ 아니오
  ```

### 팀 커뮤니케이션

- [ ] **리더십에 월간 리포트 발송**
  ```
  파일: monitoring/monthly-reports/YYYY-MM-monthly.md
  
  포함:
  - SLA 준수 상태
  - Incident 요약
  - 성능 메트릭
  - 용량 예측
  - 개선 action
  - 예산 영향 (해당하면)
  ```

---

## 정기 유지보수 작업

### 일일 (근무 종료)

- [ ] 디스크 공간 사용량 < 70% 확인
- [ ] 오류 로그에서 패턴 검토
- [ ] 데이터베이스의 hanging connection 확인

### 주간 (금요일)

- [ ] 이전 로그 파일 정리 (> 30일)
- [ ] 모든 백업 완료 확인
- [ ] 데이터베이스 VACUUM ANALYZE 실행

### 월간 (첫 월요일)

- [ ] alert 임계값 vs 실제 메트릭 검토
- [ ] runbook 검토 및 업데이트
- [ ] 용량 계획 검토
- [ ] 모니터링 스택 버전 업데이트 확인

### 분기별 (분기 첫 날)

- [ ] 전체 재해 복구 테스트
  - [ ] 백업에서 복원
  - [ ] 모든 서비스 시작 확인
  - [ ] 메트릭/로그 가용성 확인
- [ ] 모니터링 시스템 보안 감사
- [ ] 모니터링 비용 검토
- [ ] 다음 분기 용량 예측

### 연간 (1월 1일)

- [ ] 모니터링 스택 주요 버전 업그레이드
- [ ] 운영 문서 업데이트
- [ ] 새 기능/변경사항에 대한 팀 교육
- [ ] 비즈니스 요구사항에 따른 SLA 조정

---

## 빠른 참고 명령어

```bash
# 헬스 체크
curl http://localhost:8080/actuator/health
curl http://localhost:9090/-/healthy
curl http://localhost:3100/ready
curl http://localhost:3000/api/health

# 최근 메트릭 보기
curl http://localhost:8080/actuator/prometheus | head -30

# 최근 로그 보기
docker logs app | tail -20

# 에러율 쿼리
curl 'http://localhost:9090/api/v1/query?query=rate(http_requests_total{status=~"5.."}[5m])'

# 디스크 공간 확인
docker exec app du -sh logs/

# 데이터베이스 연결
docker exec postgres psql -c "SELECT count(*) FROM pg_stat_activity;"

# JVM 메모리 사용량
docker stats app --no-stream

# Prometheus 스크래프 상태
curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets[0]'

# Alert 상태
curl http://localhost:9090/api/v1/rules | jq '.data.groups[0].rules[0]'

# Loki 로그 쿼리
curl 'http://localhost:3100/loki/api/v1/query_range?query={job="deoham"}&start=<timestamp>&end=<timestamp>'
```

---

## Incident 로그 템플릿

**파일**: `monitoring/incidents/YYYY-MM-DD.log`

```
=== Incident 리포트 ===
날짜: YYYY-MM-DD
시간: HH:MM UTC
심각도: 중요 / 경고 / 정보

Alert: [Alert 이름]
조건: [트리거된 것]

영향:
- 영향받은 사용자: [예상치]
- 영향받은 서비스: [목록]
- 수익 영향: [있으면]

타임라인:
- HH:MM - Alert 발동
- HH:MM - 조사 시작
- HH:MM - 근본 원인 파악
- HH:MM - 해결책 적용
- HH:MM - Alert 해결

근본 원인:
[상세 설명]

해결:
[해결을 위해 수행한 것]

예방:
[반복 방지 방법]

Action Item:
- [ ] [Task 1]
- [ ] [Task 2]
```

---

## 연락처 정보

**당직 엔지니어**: [이름/전화]
**당직 매니저**: [이름/전화]
**Platform Team Slack**: #incidents
**Escalation 경로**: 당직 엔지니어 → 매니저 → VP 엔지니어링

**중요 링크**:
- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090
- Loki: http://localhost:3100
- 운영 가이드: monitoring/OPERATIONS-GUIDE.md
- Alert 절차: monitoring/ALERT-PROCEDURES.md
- 문제 해결: monitoring/TROUBLESHOOTING-PLAYBOOK.md

---

**마지막 업데이트**: 2024-01-XX  
**버전**: 1.0  
**관리팀**: Platform Engineering Team  
**다음 검토**: [날짜]
