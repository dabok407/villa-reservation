# Villa Reservation — Project Guide

> **한 줄 정의**: 기존 CRUD 기반 가족 별장 예약 시스템을, 자연어로 쓰는 AI 인터페이스로 **AX 전환**한 사례.
> NHN 'AI 전환 백엔드 개발' 포지션 사전과제. 평가 핵심은 코드의 화려함이 아니라
> **"AI 출력을 어떤 기준으로 판단·검증·수정했는가"** — 이 과정을 README에 누적 기록하는 것이 과제의 절반.

## Overview
가족(3가구: 부모님/형네/본인)이 세컨하우스를 겹치지 않게 예약하는 웹앱. 메인은 캘린더,
날짜 from-to 선택 → 예약정보 입력 → 저장 → 조회. 여기에 **자연어 예약 어시스턴트 1개**를 얹는다:
"다음 달 첫째 주 비어?" → LLM이 파싱 → function call로 예약 DB 조회·충돌검증 → 자연어 응답.

## Tech Stack (실측)
- **Backend**: Java **8** / Spring Boot **2.7.18** / **Gradle** (`./gradlew`)
- **ORM/DB**: Spring Data JPA / Hibernate, **H2 파일 모드** (`backend/data/villa-reservation.mv.db`), `ddl-auto: update`
- **Security**: spring-security-crypto — **BCrypt만** (전체 Security 미적용; 예약 비번 1회용)
- **Frontend**: React **19** + TypeScript **5.9** + Vite **8** + Tailwind CSS **4** + remixicon, 순수 `fetch`
- **Server**: port **8082**, context-path **`/villa`** → API `/villa/api/*`, SPA `/villa/*`
- **신규(AI)**: Anthropic **Claude Messages API** + function calling, 호출은 **RestTemplate**(외부 SDK 없음)

## Project Structure
```
villa-reservation/
├── backend/  (Spring Boot, Gradle)
│   └── src/main/java/kr/mkgalaxy/villa/
│       ├── controller/ReservationController.java   # /api/* 8 endpoints
│       ├── service/ReservationService.java         # 비즈니스 로직 + 충돌검증
│       ├── repository/{Reservation,CheckoutMemo}Repository.java
│       ├── entity/{Reservation, ReservationStatus, CheckoutMemo}.java
│       ├── dto/  (Request/Response 7종)
│       ├── exception/{ReservationConflictException, GlobalExceptionHandler}.java
│       ├── config/WebConfig.java                   # SPA forward
│       └── ai/   ← 신규(이 패키지만 추가, 기존 무수정)
│   ├── data/villa-reservation.mv.db                # ★ 가족 실데이터
│   └── build.gradle  (buildAll = 프론트 dist→static 복사 후 단일 JAR)
├── frontend/ (React SPA)  src/{App.tsx, api/, components/{Calendar,Header,ReservationForm,modals/}, types/, utils/}
├── _nhn_context/  PROJECT_BRIEF·KICKOFF·WEEK_PLAN  (전략 문서)
└── .claude/  (이 하네스: agents/, skills/, hooks/, settings.json)
```

## Domain Model
- **Reservation**: reserverName, checkInDate, checkOutDate, adultCount, childCount, description, passwordHash(BCrypt), status, createdAt/updatedAt
- **ReservationStatus**: ACTIVE / CHECKED_OUT / CANCELLED — **조회·충돌검증은 ACTIVE만**
- **CheckoutMemo**: 체크아웃 시 1건 생성, 최신 메모는 다음 예약 화면에 노출
- 예약자명 후보(`RESERVER_NAMES`): 김경임·황용귀·박정인·황대한·배지현·황민국·기타
- **가구↔이름 매핑(코드에 없음, 오너 확정)**: 부모님=황용귀·김경임 / 형네=황대한·박정인 / 본인=황민국·배지현 / 기타=외부 손님. AI는 system 프롬프트로 주입.

## 핵심 불변식 (절대 깨지 말 것)
1. **예약 충돌 = 반개구간**: `checkInDate < :checkOut AND checkOutDate > :checkIn`.
   ⇒ **체크아웃일 == 다음 체크인일은 충돌 아님**(같은 날 퇴실 후 입실 가능). 이 식을 복제/변형하지 말고
   `ReservationRepository.countConflictingNew(...)`를 **호출**한다.
2. **날짜 유효성**: `checkOutDate > checkInDate` (최소 1박). 위반 시 400.
3. 예외 포맷: GlobalExceptionHandler가 `{error:"..."}`로 일관 처리. 충돌=409, 검증=400.

## Build & Run
```bash
cd backend
./gradlew bootRun          # 로컬 기동 (8082, /villa)
./gradlew test             # 테스트
./gradlew build            # 백엔드 빌드(테스트 포함)
./gradlew buildAll         # frontend(npm build)→static 복사→단일 JAR (build/libs/villa-reservation-0.0.1.jar)
# frontend 단독: cd frontend && npm run dev | npm run build
```

## AI 작업 규칙 (어기면 안 됨 — 살아있는 문서, 실수 발견 시 한 줄씩 추가)
1. **기존 `ReservationService`/`ReservationRepository`를 수정하지 않는다.** 새 AI 기능은 `ai/` 패키지에 **추가만**.
2. **충돌 검증식을 복제·변형하지 않는다.** 필요하면 기존 Repository 메서드를 호출.
3. **API 키를 코드/yml에 평문으로 박지 않는다.** `${ANTHROPIC_API_KEY:}` 강제. (PreToolUse 가드가 차단)
4. **`data/villa-reservation.mv.db`(가족 실데이터)를 삭제·덮어쓰지 않는다.** (가드가 차단)
5. **MVP 범위 = 자연어 조회·검증·응답까지.** 예약 생성/수정 tool은 만들지 않는다(사이드이펙트 0).
   추천·예측·요약 리포트도 범위 밖. 새 기능 충동 시 "MVP 필수인가, 면접 한 줄인가?"를 먼저 묻는다.
6. **Java 8 문법만.** var, record, List.of, Map.of, 텍스트블록, stream.toList() 금지.
7. **새 기능은 테스트 없이 머지 금지.** 특히 LLM tool 결과 → DB 조회 매핑은 단위 테스트 필수.
8. **AI 응답의 정답 기준은 도메인 규칙**(reservation-domain-expert). 모호한 날짜는 가정을 응답에 명시.

## AI 어시스턴트 설계 요지 (상세는 .claude/agents/ai-integration-expert.md)
- 구조: **LLM API 연계 + function calling**. RAG 아님(임베딩/벡터검색 없음) — 정직하게 명시.
- 흐름: user 자연어 → Claude(messages+tools) → `stop_reason=tool_use` → 백엔드가 읽기전용 tool 실행 → tool_result append → 재호출 → `end_turn` 응답. 루프 횟수 상한으로 비용·무한루프 차단.
- 읽기 전용 tool 3개: `check_availability`(countConflictingNew), `list_reservations`(getReservationsByMonth), `active_today`.
- system 프롬프트에 박을 것: 오늘 날짜, 가구 매핑, 충돌 규약, 모호성 처리 규칙, 환각 차단. 긴 프롬프트는 prompt caching.
- 모델 기본 **claude-sonnet-4-6**(설정값 `ANTHROPIC_MODEL`, 비용 효율; 데모 품질 위해 opus로 교체 가능). **멀티턴은 서버 무상태** — 클라이언트가 messages 배열로 대화 이력 전달(세션/DB 저장 없음). 챗 UI(React/Tailwind)는 백엔드 MVP 후행.

## 운영 환경 (EC2)
- **운영 URL: https://mkgalaxy.kr/villa** (Nginx 리버스 프록시 → localhost:8082, context `/villa`). AI 공개 엔드포인트 = `https://mkgalaxy.kr/villa/api/ai/chat`. 앱은 8082로만 바인딩, HTTPS/외부노출은 Nginx 담당.
- 같은 EC2에 주식봇/코인봇 가동 중. **2026-06-01 점검: available 410Mi, swap 0** → 무조정 배포 시 OOM 위험.
- 조치 순서: ① 스왑 2GB 추가 ② JVM `-Xmx256m` ③ 빠듯하면 로컬 데모+문서화. 상세는 .claude/skills/deploy-ec2.
- 키 주입은 환경변수/권한600 EnvironmentFile. 점검 수치·조치는 README 'Linux 운영 흔적'으로 기록.

## 하네스 구성 (.claude/)
- **agents/**: pm-orchestrator(기획·문서·조율) · reservation-domain-expert(도메인 진실) · ai-integration-expert(LLM 연계) · backend-developer(Java8/Spring) · qa-verifier(적대적 검증) · frontend-developer(React/Tailwind 챗UI) · devops-ec2(Linux 운영)
- **skills/**: `verify-assistant`(엣지케이스 검증 루프) · `deploy-ec2`(빌드·배포+운영 흔적)
- **hooks/**: `guard.js`(시크릿 평문·실데이터 덮어쓰기 차단, PreToolUse)
- 모델 티어링: 설계·검증 에이전트 opus-4-8 / 기계적 에이전트(frontend, devops) sonnet-4-6.

## 작업 리듬 / 평가축 매핑
- 운전자는 오너, Claude Code는 타이핑. plan mode로 계획 먼저 검토 → 이상하면 실행 전 차단.
- 자격요건: Agentic AI(이 하네스) · 운영 백엔드(빌드·기동) · Linux/EC2(devops-ec2). 우대: LLM 연계(ai-integration-expert) · FE React/Tailwind(frontend-developer). 주요업무: 비개발자 문서화(pm-orchestrator).
