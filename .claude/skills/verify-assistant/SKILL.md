---
name: verify-assistant
description: Run the natural-language reservation assistant against a battery of edge-case prompts and check each answer against the domain rules, producing a PASS/FAIL verification log for the README. Use when validating the AI assistant after a change, or when building the "검증 깊이" section of the README — ambiguous dates, conflict boundary, CANCELLED leakage, LLM hallucination.
---

# verify-assistant — 자연어 어시스턴트 검증 루프

이 스킬은 NHN 과제의 핵심 평가축("AI 출력을 어떤 기준으로 검증·수정했는가")을 **반복 가능한 절차**로 만든다.
구현이 바뀔 때마다 같은 케이스를 돌려 회귀를 잡고, 결과를 README에 그대로 붙일 검증 로그로 남긴다.

## 사전 조건
- 백엔드 기동: `cd backend && ./gradlew bootRun` (8082, context `/villa`)
- `ANTHROPIC_API_KEY` 환경변수 설정
- 엔드포인트: `POST /villa/api/ai/chat` — 로컬 `http://localhost:8082`, 운영 `https://mkgalaxy.kr`(Nginx 경유). body `{ "message": "<자연어>" }` (멀티턴 시 `messages` 배열) → `{ "reply": "<응답>" }`
- 정답 기준은 **reservation-domain-expert**의 규칙(반개구간 충돌, 가구 매핑, 날짜 해석)

## 절차
1. **테스트 데이터 준비** — 알려진 예약 몇 건을 seed(예: 12/6–12/7 형네, 12/10–12/12 부모님). 각 케이스의 정답을 도메인 규칙으로 미리 계산.
2. **케이스 배터리 실행** — 아래 카테고리를 각각 `curl`로 던지고 응답 수집.
3. **판정** — 응답을 도메인 정답과 대조해 PASS/FAIL. FAIL이면 원인(프롬프트 vs 코드) 분류.
4. **수정 → 재실행** — 회귀 없는지 전체 재실행.
5. **로그 기록** — `docs/verification-log.md`에 케이스별로 누적, README 검증 섹션에서 인용.

## 케이스 배터리 (최소 세트)
### A. 충돌 경계
- "12월 7일 입실해서 1박 가능?" (앞 예약 퇴실일==입실일) → **가능** (반개구간)
- "12월 6일부터 2박 비어?" (기존 예약과 정확히 겹침) → **불가 + 누구 예약인지**
### B. 모호한 날짜
- "다음 달 첫째 주 주말에 우리 가족 2박 비어?" → 가정한 구간을 응답에 **명시**
- "담주 화요일부터 2박" → checkIn/checkOut 명시 후 판정
### C. 상태 누수
- 취소된 예약 기간을 물었을 때 → "예약 없음/가능"(CANCELLED 제외)
### D. 환각
- 존재하지 않는 사람/예약을 단정하지 않는가 → tool_result 범위 내 응답만
### E. 운영 실패
- 키 미설정/네트워크 오류 시 → 사용자 친화적 fallback 문구(500 노출 금지)

## 예시 호출
```bash
curl -s -X POST http://localhost:8082/villa/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"다음 달 첫째 주 주말에 우리 가족 2박 비어?"}'
```

## 로그 형식 (README 인용용)
```
### [케이스] 충돌 경계 — 퇴실일==입실일
- 입력: "12월 7일 입실해서 1박 가능?"
- 기대: 가능 (반개구간; 앞 예약 12/7 퇴실)
- 실제: <응답>
- 판정: PASS/FAIL
- 조치: (FAIL 시 system 프롬프트/코드 수정 내용)
```
> 평가자는 PASS 목록이 아니라 **FAIL→원인→수정**의 흔적을 본다. 실패를 숨기지 말 것.
