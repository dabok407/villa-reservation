# 검증 로그 (Verification Log)

> 이 과제의 평가 핵심은 코드의 화려함이 아니라 **"AI/시스템 출력을 어떤 기준으로 검증·수정했는가"**다.
> 이 문서는 그 과정을 케이스별로 누적 기록하며, README의 '검증 깊이' 섹션의 원천이 된다.
> 각 항목은 **입력 / 기대(도메인 규칙 기준) / 실제 / 판정 / 조치** 형식으로 남긴다.

---

## 슬라이스 1 — `check_availability` 도구 도메인 정확성 (단위, LLM 없음)

검증 대상: `kr.mkgalaxy.villa.ai.tool.ReservationToolService.checkAvailability`
방법: `@DataJpaTest` + 내장 H2(in-memory). 실데이터(`villa-reservation.mv.db`) 미접촉.
실행: `./gradlew test --tests "*ReservationToolServiceTest"` → **5/5 PASS** (2026-06-01)

| # | 케이스 | 입력 | 기대(도메인 규칙) | 판정 |
|---|--------|------|-------------------|------|
| 1 | 충돌 경계 | 기존 12/5–12/7 ACTIVE, 질의 12/7–12/8 | **가능** (반개구간: 체크아웃일==체크인일은 충돌 아님) | PASS |
| 2 | 기간 겹침 | 기존 12/6–12/9(박정인), 질의 12/8–12/10 | **불가** + 충돌자 "박정인" 반환 | PASS |
| 3 | 취소 제외 | CANCELLED 12/10–12/12, 동일 기간 질의 | 가능 (ACTIVE만 조회) | PASS |
| 4 | 체크아웃 제외 | CHECKED_OUT 12/15–12/17, 동일 기간 질의 | 가능 (ACTIVE만 조회) | PASS |
| 5 | 잘못된 기간 | 12/10–12/10 (0박) | IllegalArgumentException | PASS |

**설계 근거(왜 이 구현이 안전한가)**: 충돌 검증식을 새로 작성하지 않고 기존
`ReservationRepository.findByStatusAndDateRange`(= `checkInDate < :endDate AND checkOutDate > :startDate`)를
재사용했다. 덕분에 반개구간 규약과 상태 필터(ACTIVE)가 **단일 진실 원천**에서 자동 보장되어,
"AI 기능이 충돌 규칙을 미묘하게 다르게 구현"하는 흔한 버그 클래스를 구조적으로 차단했다.

**부수 조치**: `build.gradle`에 `options.encoding = 'UTF-8'` 추가 — MS949 환경에서도 한글
문자열/주석/`@DisplayName`이 깨지지 않도록 컴파일 인코딩을 고정(기존 한글 메시지 안전성도 향상).

---

## 슬라이스 2 — `list_reservations` · `active_today` 도구 (단위, LLM 없음)

검증 대상: `ReservationToolService.listReservations(year, month)`, `activeToday(today)`
방법: `@DataJpaTest` + 내장 H2. 실행: `./gradlew test --tests "*ReservationToolServiceTest"` → **11/11 PASS** (슬라이스1 5 + 슬라이스2 6, 회귀 없음, 2026-06-01)

| # | 케이스 | 기대(도메인 규칙) | 판정 |
|---|--------|-------------------|------|
| 6 | 월별 조회 | 해당 월 ACTIVE만 반환, 다른 달 제외 | PASS |
| 7 | 상태 필터 | CANCELLED·CHECKED_OUT 제외 | PASS |
| 8 | 잘못된 월 | month 13 → IllegalArgumentException | PASS |
| 9 | 오늘 진행중 | 체크인일·중간·**체크아웃 당일까지 진행 중**(양 끝 포함) | PASS |
| 10 | 범위 밖 | 체크인 전날·체크아웃 다음날 → 없음 | PASS |
| 11 | null 기준일 | IllegalArgumentException | PASS |

**설계 근거**: 두 도구 모두 기존 Repository 메서드(`findByStatusAndDateRange`,
`findByStatusAndCheckInDateLessThanEqualAndCheckOutDateGreaterThanEqual`)를 **재사용** — 쿼리 복제 0.
`active_today`는 기준일(`today`)을 인자로 받아 `LocalDate.now()` 의존을 제거 → **테스트 결정성** 확보.

> 주의: 충돌검증(반개구간, 체크아웃일 제외)과 active-today(양 끝 포함)는 **의도적으로 다른 경계**다.
> "오늘 머무는 중"은 체크아웃 당일도 포함하는 게 자연스럽기 때문. 기존 시스템 동작을 그대로 따랐다.

---

## 슬라이스 3 — LLM 연계 레이어 (tool_use 루프, 목 기반 단위 검증)

검증 대상: `AiAssistantService`(tool_use 루프), `ToolRegistry`(스키마·실행·입력방어), `SystemPromptBuilder`
방법: 외부 호출(`AnthropicClient`)을 인터페이스로 격리 → **목(Mockito)으로 LLM·네트워크 없이** 루프 검증.
실행: `./gradlew test` → **전체 22/22 PASS** + 컨텍스트 기동 스모크 1 PASS (2026-06-01)

| 영역 | 케이스 | 기대 | 판정 |
|---|---|---|---|
| 루프 | tool_use → 도구 실행 → end_turn | 최종 텍스트 반환, 도구 1회 호출, API 2회 호출 | PASS |
| 루프 | 도구 없이 end_turn | 즉시 텍스트, 도구 미호출 | PASS |
| 루프 | tool_use 무한 반복 | maxToolLoops(2)에서 중단 + fallback | PASS |
| 안전 | API 키 미설정 | 외부 호출 0, 안내 메시지 | PASS |
| 안전 | 외부 호출 예외 | 사용자 친화 fallback(500 노출 X) | PASS |
| 도구 | 스키마 노출 | 읽기전용 3종 | PASS |
| 도구 | 잘못된 입력/미지 도구 | 예외 아닌 **error JSON**(신뢰경계 방어) | PASS |
| 프롬프트 | system 프롬프트 | 오늘날짜·가구매핑·반개구간·환각차단 포함 | PASS |
| 기동 | 전체 컨텍스트 | 신규 ai/ 빈 전부 와이어링 | PASS |

**설계 근거**:
- **외부 호출을 `AnthropicClient` 인터페이스 뒤로 격리** → LLM·키·네트워크 없이 오케스트레이션 로직을 결정적으로 테스트. (실 구현 `RestTemplateAnthropicClient`는 슬라이스 4 E2E에서 검증)
- **입력 신뢰 경계**: LLM이 넘긴 도구 입력의 파싱 오류를 예외로 터뜨리지 않고 `{"error":...}` JSON으로 돌려줌 → 루프가 안전하게 계속.
- **루프 상한·키 부재·외부 예외**를 모두 fallback으로 흡수 → 사용자에게 500/스택트레이스 노출 없음.

**미검증(슬라이스 4 예정)**: 실제 Claude API 호출(실 키 필요) — 모호한 날짜·가구 별칭·환각을 실 LLM으로 N회 반복 평가(eval).

---

## 슬라이스 4 — 실제 E2E (진짜 Claude API 연동)

환경: 실제 Claude API(`claude-sonnet-4-6`), **임시 in-memory DB**(실데이터 보호), 오늘=2026-06-02.
사전: 키 유효성 + 모델 ID를 `/v1/models`로 확인(`claude-sonnet-4-6`·`claude-opus-4-8` 정식 ID 존재).
seed: 형네(황대한) 2026-07-06~07-08, 부모님(황용귀) 2026-07-10~07-12 (둘 다 ACTIVE).
실행: `POST /villa/api/ai/chat` 자연어 5건 → **5/5 PASS** (2026-06-02)

| # | 입력 | 기대(도메인 규칙) | 실제 응답 요약 | 판정 |
|---|------|-------------------|----------------|------|
| 1 | "다음 달 첫째 주에 별장 예약한 사람 있어?" | 다음달=7월 파싱 + 가구매핑 | "7월 첫째 주(7/1~5) 없음" + 황대한=형네·황용귀=부모님 표로 노출 | PASS |
| 2 | "7월 8일부터 1박 가능해?" | **가능**(형네 7/8 퇴실 == 7/8 입실, 반개구간) | "✅ 가능, 충돌 없음" | PASS(핵심) |
| 3 | "7월 7일부터 우리 가족 2박?" | 불가(형네 7/6~8 겹침) | "불가, 형네(황대한) 7/6~8 충돌" | PASS |
| 4 | "형네가 7월에 언제 예약했어?" | 형네→황대한/박정인 매핑 | "형네(황대한) 7/6~7/8" + 부모님도 안내 | PASS |
| 5 | "12월에 예약 있는 날 있어?" | 없음(환각 차단) | "12월 진행 중 예약 없음" | PASS |

**확인된 것**: 실 LLM이 tool을 호출해 우리 DB를 조회하고 — ① 상대적 날짜(다음 달=7월) 파싱,
② 가구 별칭(형네→황대한) 매핑, ③ **충돌 경계(체크아웃일==체크인일 → 가능) 정확 판정**(Q2),
④ 겹침 시 충돌자 명시(Q3), ⑤ 빈 결과를 지어내지 않음(Q5). 모호한 날짜엔 가정 구간을 노출(Q1).

### 적대적 발견 (개선 여지 — 숨기지 않고 기록)
- **[중] 과잉 약속(over-promise)**: Q2 응답이 "예약을 진행할까요?"로 끝남. 그러나 **MVP는 읽기 전용(쓰기 tool 없음)** 이라 실제 예약 생성 불가. 사용자가 "응"하면 수행 못 함 → UX/범위 불일치.
  → **조치(예정)**: system 프롬프트에 "어시스턴트는 조회·안내만 하며 직접 예약 생성/변경은 하지 않는다. 예약은 캘린더 화면에서 진행하도록 안내한다"를 추가하고 재검증.
- **[하] "첫째 주" 해석 편차**: LLM이 7/1~7/5로 가정(가정을 명시한 점은 규칙 준수 ✓). 단 도메인 정의("1일이 포함된 주, 월~일")와 미세하게 다름.
  → **조치(선택)**: 프롬프트에 "N월 첫째 주 = 해당 월 1일이 포함된 월~일 주"로 정의를 고정하면 편차 제거.

> 의미: 시스템은 작아도 **실 LLM이 도메인 불변식(반개구간 경계)을 정확히 지키는지**를 실데이터 없이
> 재현 가능한 시드로 검증했고, 통과/미통과뿐 아니라 **개선점까지 적대적으로 도출**했다.
