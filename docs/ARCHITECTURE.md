# 별장 예약 시스템 — 서비스 흐름도 (Architecture & Service Flow)

> **이 문서의 목적**: 코드를 한 줄씩 읽지 않고도 "어떤 화면의 어떤 기능이 → 어떤 API로 → 어떤
> 컨트롤러·서비스를 거쳐 → 어떤 테이블을 조회/변경하는지"를 한눈에 파악하기 위한 설계서.
> AI 도구로 만든 시스템을 사람이 **경계와 불변식 단위로** 검증·인수인계할 때 쓰는 산출물 형식.

---

## 1. 시스템 개요 (토폴로지)

```
[브라우저]
   │  https://mkgalaxy.kr/villa
   ▼
[Nginx 리버스 프록시]  ── HTTPS 종단, 외부 노출
   │  → localhost:8082
   ▼
[Spring Boot 앱]  (context-path: /villa, port 8082)
   ├── 정적 SPA 서빙 (React 빌드 결과 = src/main/resources/static)
   ├── REST API  /villa/api/*
   └── (신규) AI  /villa/api/ai/chat ──→ [Anthropic Claude API] (외부)
   │
   ▼
[JPA / Hibernate]
   ▼
[H2 파일 DB]  backend/data/villa-reservation.mv.db   ← 가족 실데이터
```

- 단일 JAR 배포: 프론트(React/Vite) 빌드물을 백엔드 static으로 복사 → `./gradlew buildAll`
- 인증: 전역 로그인 없음. 예약별 **비밀번호(BCrypt)** 로 수정/취소 권한만 확인.

---

## 2. 기능 흐름 매핑 (화면 → API → 코드 → DB)

| # | 화면 / 트리거 | HTTP | Controller 메서드 | Service 메서드 | Repository / 쿼리 | 테이블 | 핵심 규칙 |
|---|---------------|------|-------------------|----------------|-------------------|--------|-----------|
| 1 | 캘린더 월 진입/이동 | `GET /api/reservations?year&month` | getReservations | getReservationsByMonth | findByStatusAndDateRange (`status=ACTIVE AND checkIn<endDate AND checkOut>startDate`) | reservation | ACTIVE만 |
| 2 | 예약 칸 클릭(상세) | `GET /api/reservations/{id}` | getReservation | getReservation | findById | reservation | 없으면 400 |
| 3 | 예약 폼 저장 | `POST /api/reservations` | createReservation | createReservation | **countConflictingNew** → save | reservation | 충돌 0건 + BCrypt 해시, 충돌 시 **409** |
| 4 | 수정/취소 전 본인확인 | `POST /api/reservations/{id}/verify-password` | verifyPassword | verifyPassword | findById + BCrypt.matches | reservation | 비번 일치 여부만 반환 |
| 5 | 상세 모달 수정 | `PUT /api/reservations/{id}` | updateReservation | updateReservation | **countConflicting(excludeId)** → save | reservation | 자기 자신 제외하고 충돌검증 |
| 6 | 체크아웃 모달 | `POST /api/reservations/{id}/checkout` | checkout | checkout | save(상태=CHECKED_OUT) + memo save | reservation, checkout_memo | 예약기간 내만, 조기퇴실 시 checkOut=오늘 |
| 7 | 상세 모달 취소 | `DELETE /api/reservations/{id}` | cancelReservation | cancelReservation | memo delete + save(상태=CANCELLED) | reservation, checkout_memo | 비번 확인, 연관 메모 삭제 |
| 8 | 예약 폼의 이전 메모 노출 | `GET /api/checkout-memos/latest` | getLatestCheckoutMemo | getLatestCheckoutMemo | findTopByReservationStatusOrderByCreatedAtDesc(CHECKED_OUT) | checkout_memo | 없으면 204 |
| 9 | 헤더 체크아웃 버튼 활성화 | `GET /api/reservations/active-today` | getActiveReservationsForToday | getActiveReservationsForToday | findByStatus...CheckIn≤today≤CheckOut | reservation | 오늘 진행중 |
| 10 | **(신규) 챗 모달** | `POST /api/ai/chat` | AiController.chat | AiAssistantService | (읽기전용 tool) check_availability=findByStatusAndDateRange / list_reservations / active_today | reservation | LLM tool use, **쓰기 없음** |

---

## 3. 프론트 데이터 흐름 (React)

```
App.tsx (마운트 / 월 변경)
  └ loadData(): Promise.all[ fetchReservations, fetchLatestCheckoutMemo, fetchActiveReservationsToday ]
        ├─ Calendar  ← reservations 렌더링 (날짜 칸에 예약 표시)
        ├─ ReservationForm ← 날짜 from-to 선택 완료 시 노출, latestMemo 전달
        └─ Header ← activeToday로 체크아웃 버튼 활성/비활성
  handleDayClick: 예약칸 → 상세모달 / 빈날 → 날짜선택(충돌 구간이면 리셋)
  모달: ReservationDetailModal(수정·취소) · CheckoutModal · (신규) AiChatModal
api/reservationApi.ts: 모든 호출은 fetch + handleResponse 패턴, base = /villa/api
```

---

## 4. 도메인 불변식 (절대 깨지면 안 되는 규칙)

1. **충돌 = 반개구간**: `checkInDate < otherCheckOut AND checkOutDate > otherCheckIn`
   → **체크아웃일 == 다음 체크인일은 충돌 아님** (같은 날 퇴실 후 입실 가능). 진실 원천: ReservationRepository.
2. **날짜 유효성**: `checkOutDate > checkInDate` (최소 1박).
3. **상태 필터**: 조회·충돌검증은 **ACTIVE만**. CANCELLED/CHECKED_OUT 제외.
4. **권한**: 수정·체크아웃·취소는 예약 비밀번호(BCrypt) 일치 필요.
5. **예외 포맷**: GlobalExceptionHandler가 `{error:"..."}` 일관 응답. 충돌=409, 검증=400.

---

## 5. (신규) AI 어시스턴트 흐름

```
사용자 자연어 ("다음 달 첫째 주 비어?")
  → AiController POST /api/ai/chat {message | messages[]}
  → AiAssistantService
       system 프롬프트(오늘날짜 + 가구매핑 + 규칙) + tools[3개] 동봉
       → Claude Messages API
       → stop_reason=tool_use ? → ReservationToolService(읽기전용) 실행 → tool_result append → 재호출 (최대 5회)
       → end_turn → 자연어 응답
  도구는 #2 기능표의 기존 Repository를 호출만 → 충돌식 복제 없음, 불변식 자동 보장
```
- 가구 매핑(코드엔 없음 → 프롬프트 주입): 부모님=황용귀·김경임 / 형네=황대한·박정인 / 본인=황민국·배지현 / 기타=외부손님

---

## 6. 이 산출물을 어떻게 쓰나 (봇 프로젝트 적용법)

- **인수인계·검증**: 사람은 전체 소스를 읽지 않고 **이 표의 "핵심 규칙" 열과 불변식(4장)** 만 집중 검증한다.
- **AI에게 생성시키고 사람이 대조**: "코드 분석해서 이 형식의 흐름표를 채워줘" → 결과를 실제 코드와 spot-check.
- **코인봇/주식봇**: 같은 형식으로 (전략 평가 → 주문 실행 → 체결 → 포지션/로그 테이블) 흐름표 1장씩.
  핵심 로직(전략·리스크·주문 멱등성)과 불변식만 표로 뽑으면, 라인 단위로 안 읽어도 구조를 장악할 수 있다.
