# 오버나이트 코드 리뷰 리포트 - 2026-07-15

## 브랜치 / PR
- 베이스 브랜치: feat/82/map
- 작업 브랜치: chore/overnight-review-20260715 (로컬 커밋 완료)
- PR: **오픈 실패 — 이 세션의 GitHub 계정(leesuyong849)에 ITs-TIME-ONDO/ONDO-BE 푸시 권한이 없음**
  - `git push` → 403 "Permission to ITs-TIME-ONDO/ONDO-BE.git denied to leesuyong849"
  - GitHub API(브랜치 생성) → 403 "Resource not accessible by integration"
  - 대신 전체 작업을 **git bundle + 커밋별 patch 파일**로 첨부했음. 적용 방법:
    ```bash
    git fetch <bundle파일 경로> chore/overnight-review-20260715:chore/overnight-review-20260715
    # 또는
    git checkout -b chore/overnight-review-20260715 origin/feat/82/map && git am patches/*.patch
    ```
  - 다음 오버나이트 실행 전에 GitHub App/계정에 이 저장소 write 권한을 부여해 주세요.

## 체크리스트별 결과

### 1. Swagger 설명 점검
- 발견한 문제:
  - **OpenApiConfig가 인증을 "Supabase JWT"로 안내** — 실제로는 카카오 OAuth 후 서버가 자체 발급하는 HS256 JWT. Swagger 첫 화면부터 잘못된 안내였음.
  - `PUT /api/user/profile` 설명이 "업데이트된 프로필 정보를 반환합니다"라고 했으나 실제로는 204 No Content(본문 없음) 반환.
  - `POST /api/user/profile`("프로필 생성")은 실제로 유저를 생성하지 않고 기존 유저의 프로필을 채우는 동작인데 설명이 모호했고, 404(사용자 없음) 응답 문서 누락.
  - `POST /api/cards/{cardId}/applies` 문서가 "Applies to an OPEN card"라고만 되어 있으나, 실제로는 **신청 즉시 자동 수락(ACCEPTED)되고 카드가 MATCHED로 전환**됨(선착순 매칭). FE가 오해하기 쉬운 가장 큰 불일치.
  - 카드 cancel/complete/retry 문서에 상태 전이 조건(어떤 상태에서만 가능한지)과 403/409 발생 조건 설명 부족.
  - `POST /api/auth/refresh`가 리프레시 토큰을 회전(rotation)시켜 기존 토큰이 무효화된다는 설명 누락(클라이언트가 새 refreshToken을 저장하지 않으면 다음 갱신 실패).
  - `POST /api/auth/logout` 응답 코드 문서 누락.
- 수정한 파일: `OpenApiConfig`, `UserController`, `AuthController`, `CardControllerDocs`, `CardApplyControllerDocs`
- 남은 제안:
  - 채팅 관련 docs 인터페이스(ChatMessage/ChatRoom/ChatTranslation/LiveLocation)는 이미 STOMP 프로토콜까지 포함해 상세히 잘 작성되어 있어 손대지 않았음.
  - `GET /api/cards/nearby`의 응답 필드 `has_seen_card_view_onboarding`(snake_case)은 다른 응답 필드(camelCase)와 네이밍이 어긋남 — FE와 협의 후 정리 권장.

### 2. 프로젝트 구조 점검
- 발견한 문제:
  - `ProfileResponse`, `ProfileUpdateRequest`가 `auth.dto`에 있었으나 실제 사용처는 전부 user 도메인(UserController/UserReadService) → **`user.dto`로 이동**.
  - `auth.dto.ProfileUpdateResponse` — 어디서도 사용되지 않는 죽은 코드 → **삭제**.
  - `notification.service.NotificationWriteService` — 메서드가 하나도 없는 빈 인터페이스 → **삭제**.
- 수정한 파일: 위 3개 + import 갱신(`UserController`, `UserReadService`, `UserReadServiceImpl`, `UserControllerTest`)
- 남은 제안(이동하지 않고 리포트만):
  - `notice`, `push` 패키지는 entity만 있고 controller/service/repository가 없음 — 미구현 기능으로 보여 그대로 둠.
  - 서비스 계층 구조가 도메인마다 3가지 스타일로 갈림: card는 `CardReadService`+`DefaultCardReadService`(같은 패키지), user/notification은 `service`+`service.impl`, chat/report는 인터페이스 없이 구현 클래스만. 셋 다 동작에는 문제없으나 팀 컨벤션을 하나로 정하는 것을 권장(개인적으로는 단일 구현이면 인터페이스 없이 클래스만 두는 chat 스타일 추천).
  - 응답 봉투(`ApiResponse<T>`) 사용도 컨트롤러마다 갈림: chat/notification은 봉투 사용, card/user/report/auth 콜백은 raw DTO 반환. CLAUDE.md에는 "모든 응답이 봉투를 거친다"고 되어 있으나 실제로는 아님. 카카오 콜백은 FE 협의로 의도적으로 되돌린 이력(c374897)이 있어 **일절 변경하지 않음** — FE와 함께 정리 필요.

### 3. 예외 처리 공통화
- 발견한 문제:
  - "현재 사용자 UUID 꺼내기 + 없으면 401" 로직이 **8개 컨트롤러에 복붙**되어 있었음 (`currentUserId()` 프라이빗 헬퍼 5곳, 인라인 orElseThrow 블록 9곳, STOMP용 `resolveUserId()` 2곳).
  - **UserController는 인증 실패 시 `IllegalStateException`을 던져 500으로 응답**하던 실질 버그 — 이제 401(UNAUTHORIZED)로 응답.
  - 두 STOMP 컨트롤러에 동일한 `@MessageExceptionHandler` 쌍이 중복 선언.
  - `ChatTranslationService.requireParticipant()`가 `ChatRoomAccessService.requireParticipant()`와 동일한 검증을 중복 구현.
- 수정 내용:
  - `AuthenticationUtils`에 `requiredPrincipal()` / `requiredUserId()` / `requiredUserId(Authentication)` / `requiredUserId(Principal)` 추가 — 실패 시 일관되게 `BusinessException(UNAUTHORIZED)` → GlobalExceptionHandler를 거쳐 표준 봉투로 401 응답.
  - 신규 `global.exception.StompExceptionHandler`(@ControllerAdvice)로 STOMP 예외 처리 통합.
  - ChatTranslationService는 ChatRoomAccessService에 위임.
- 수정한 파일: `AuthenticationUtils`, `StompExceptionHandler`(신규), 컨트롤러 8개, `AuthService`, `ChatTranslationService`
- 남은 제안: 없음 (도메인 서비스 내부의 상태 검증들은 이미 BusinessException + ErrorCode로 일관되게 작성되어 있었음)

### 4. SOLID 원칙 검토
- 적용한 것:
  - **ReportService.closeChatRoomBetweenUsers가 `cardApplyRepository.findAll()`로 전체 테이블을 메모리에 올려 필터링** — 데이터가 쌓이면 신고 API가 급격히 느려지는 구조. 동일 조건의 JPQL(`findByStatusAndUserPair`)로 대체 (기존 필터 조건과 논리적으로 동일).
  - `MetricsRegistry`(626줄)가 도메인별 복붙 메서드 덩어리로 SRP/DRY 위반 — 공통 헬퍼로 통합해 약 230줄 축소, 공개 API·메트릭 이름은 그대로 유지.
  - ISP 관점에서 빈 인터페이스 `NotificationWriteService` 제거(2번 항목과 중복).
- 적용하지 않고 제안만 (근거 포함):
  - **DefaultCardWriteService.createCard의 하드코딩** `radiusM(11100000)`, `city("korea")` — 명백한 임시값으로 보이나 의미를 알 수 없어 동작 변경 위험이 있어 그대로 둠. 상수화 또는 요청 파라미터화 필요.
  - **MetricsAspect.extractEndpointName**의 클래스명 문자열 매칭 if-체인 — OCP 위반(새 컨트롤러마다 수정 필요)이지만 메트릭 라벨이 대시보드와 결합되어 있을 수 있어 미변경. `@Timed` 표준 어노테이션 기반으로 대체 검토 권장.
  - **ChatMessageService가 8개 의존성**을 가짐(메시지 저장 + 알림 + 브로드캐스트 + 메트릭) — 알림/브로드캐스트를 도메인 이벤트(ApplicationEventPublisher)로 분리하면 SRP가 개선되나 트랜잭션 경계가 바뀌는 큰 수술이라 미적용.
  - **AuthService.generateDefaultNickname의 최대 1000회 existsByNickname 루프** — 실제로는 kakaoId가 유니크해서 첫 시도에 끝나지만, UUID suffix 방식으로 바꾸면 루프 자체가 불필요.

### 5. 디자인 패턴 적용 검토
- 적용한 것:
  - **Template Method**: `MetricsRegistry.recordTimed()/recordTimedRun()` — 5개 서비스 메서드에 반복되던 `Timer.Sample + try/성공기록/catch/실패기록/rethrow` 보일러플레이트를 템플릿으로 캡슐화. 메트릭 이름/태그 불변, 관련 단위 테스트 40건 통과로 검증.
- 이미 잘 적용되어 있던 것:
  - **Strategy**: `TranslationProvider` 인터페이스 + `DummyTranslationProvider`(@Primary) — 실제 번역 제공자 교체 준비 완료.
  - 카드 read/write 서비스 분리(CQRS 유사) 등.
- 적용하지 않고 제안만:
  - `ChatMessageService.resolveContent`의 messageType 분기 — 타입이 3개뿐이고 로직이 짧아 Strategy 도입은 과설계로 판단.
  - `ChatMessageService`/`ChatRoomService`의 `notifyOtherParticipant`/`resolveOpponent`가 "상대 참여자 찾기"를 각자 구현 — `ChatRoomAccessService`로 모으는 것을 다음 정리 때 권장(이번엔 시그니처 차이로 보류).

## 변경된 파일 목록
총 31개 파일, +285 / -563 (순감소 278줄)

**신규**: `global/exception/StompExceptionHandler.java`
**삭제**: `auth/dto/ProfileUpdateResponse.java`, `notification/service/NotificationWriteService.java`
**이동**: `auth/dto/ProfileResponse.java` → `user/dto/`, `auth/dto/ProfileUpdateRequest.java` → `user/dto/`
**수정(주요)**: `MetricsRegistry`(-397줄 규모 정리), `AuthenticationUtils`(+공통 메서드), 컨트롤러 10개(중복 제거·Swagger), `ReportService`+`CardApplyRepository`(전체 스캔 제거), `DefaultCardRead/WriteService`·`UserRead/WriteServiceImpl`(메트릭 템플릿), `OpenApiConfig`, docs 인터페이스 2개

커밋 (5개, 의미 단위):
1. `FIX: Swagger 문서 실제 동작 불일치 및 누락 설명 보완`
2. `REF: 프로필 DTO를 user 도메인으로 이동 및 미사용 클래스 제거`
3. `REF: 인증 주체 조회/예외 처리를 공통 유틸로 통합`
4. `REF: MetricsRegistry 중복 제거 및 신고 처리 전체 테이블 스캔 개선`
5. `REF: 메트릭 계측 보일러플레이트를 Template Method로 추출`

## 빌드/테스트 상태
- `compileJava` / `compileTestJava`: ✅ 매 커밋마다 성공 확인
- 단위/웹 계층 테스트 (Docker 불필요): ✅ **56건 전부 통과**
  - MetricsRegistryPhase34Test, CardServiceMetricsTest, UserServiceMetricsTest, ChatMessageServiceMetricsTest (40건 — 이번 메트릭 리팩터링을 직접 검증)
  - AuthControllerTest, CardControllerTest, ReportControllerTest, UserControllerTest (16건)
- Testcontainers 통합 테스트 (ChatTranslationServiceTest, ReportServiceTest, WebSocket 테스트 등): ⚠️ **이 환경에 Docker가 없어 실행 불가** — 로컬에서 `./gradlew test` 1회 실행 권장. (참고: 이 환경에서는 gradle wrapper 배포판 다운로드도 프록시에 막혀 시스템 gradle 8.14.3 + 신규 설치한 JDK 17로 빌드함)
- Flyway 마이그레이션/엔티티: 일절 변경 없음 (ddl-auto=validate 안전)

## 사용자가 아침에 확인해야 할 것
1. **PR 리뷰 후 머지 여부 결정** — 머지는 하지 않았음. 특히 아래 두 가지는 동작이 바뀐 부분이니 확인:
   - UserController 인증 실패 응답이 500 → **401**로 바뀜 (FE에서 500을 기대하던 코드가 없는지)
   - ReportService의 채팅방 종료 조회가 findAll() → 조건 JPQL로 바뀜 (ReportServiceTest를 Docker 있는 곳에서 한 번 돌려 확인 권장)
2. **Docker 있는 환경에서 `./gradlew test` 전체 실행** — Testcontainers 테스트는 여기서 못 돌렸음.
3. 제안 사항 검토:
   - `submitApply` 자동 수락 정책이 의도인지 (문서에는 실제 동작대로 적어 두었음)
   - createCard의 `radiusM(11100000)` / `city("korea")` 하드코딩
   - `has_seen_card_view_onboarding` snake_case 네이밍
   - 서비스 인터페이스 스타일(3가지 혼재) 통일 방향
