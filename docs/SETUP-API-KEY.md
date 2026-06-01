# Claude(Anthropic) API 키 발급·주입 가이드

> 자연어 어시스턴트의 **실제 동작(슬라이스 4 E2E)** 에 필요한 `ANTHROPIC_API_KEY` 발급·설정 절차.
> 슬라이스 1~3(도구 + tool_use 루프)은 키 없이 목으로 검증 완료. 이 키는 villa 백엔드가
> Claude를 직접 호출하기 위한 입장권이며, Claude Code/Pro 구독과는 **별도 청구**(사용량 과금)다.

---

## 1. 키 발급 (콘솔)

1. https://console.anthropic.com 접속 → 로그인/가입 (Claude.ai 구독과 별개 계정 체계)
2. **Billing**(결제) → 결제수단 등록 + 크레딧 충전(최소 $5면 이 과제는 충분, 테스트 수십 번 = 몇 센트)
3. **API keys** → **Create Key** → 이름 지정(예: `villa-reservation`) → 생성
4. `sk-ant-...` 키가 **이때 한 번만** 전체 노출됨 → 안전한 곳에 복사(다시 못 봄, 분실 시 재발급)

> ⚠️ 키는 비밀번호다. 코드/yml/깃/로그/채팅에 평문으로 남기지 말 것(이 레포의 guard 훅이 커밋을 차단).

## 2. 모델 ID 확인 (중요)
- `application.yml`의 `anthropic.model` 기본값은 `claude-opus-4-8`(프로젝트 설정값).
- **실제 API는 정확한 모델 ID 문자열**을 요구한다. 콘솔의 *Models* 문서에서 현재 사용 가능한 정식 ID를
  확인해 `ANTHROPIC_MODEL` 또는 yml 값으로 맞춘다(별칭/날짜 suffix가 붙는 경우가 있음).
- 비용 절감 시 더 가벼운 모델로 교체 가능 — **교체 후 eval 셋 재실행**으로 품질 회귀 확인.

## 3. 로컬 주입 (개발/데모)

### Windows PowerShell (현재 세션만)
```powershell
$env:ANTHROPIC_API_KEY = "sk-ant-..."
cd backend; .\gradlew.bat bootRun
```
### Windows (영구 — 사용자 환경변수)
```powershell
setx ANTHROPIC_API_KEY "sk-ant-..."
# 새 터미널을 열어야 적용됨
```
### macOS / Linux (현재 세션)
```bash
export ANTHROPIC_API_KEY="sk-ant-..."
cd backend && ./gradlew bootRun
```
### IDE(IntelliJ 등)
- Run Configuration → Environment variables → `ANTHROPIC_API_KEY=sk-ant-...`

## 4. EC2 운영 주입 (권한 600 EnvironmentFile)
```bash
# 키 파일(평문 금지 위치, 600)
printf 'ANTHROPIC_API_KEY=sk-ant-...\n' | sudo tee /etc/villa.env >/dev/null
sudo chmod 600 /etc/villa.env

# 기동 시 로드
set -a; source /etc/villa.env; set +a
nohup java -Xms128m -Xmx256m -jar ~/villa/villa-reservation-0.0.1.jar > ~/villa/app.log 2>&1 &
```
### systemd(재시작 안정성)
```ini
# /etc/systemd/system/villa.service
[Service]
EnvironmentFile=/etc/villa.env
ExecStart=/usr/bin/java -Xms128m -Xmx256m -jar /home/ec2-user/villa/villa-reservation-0.0.1.jar
Restart=on-failure
User=ec2-user
[Install]
WantedBy=multi-user.target
```

## 5. 동작 확인 (헬스 체크)
```bash
# 로컬
curl -s -X POST http://localhost:8082/villa/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"다음 달 첫째 주 주말에 우리 가족 2박 비어?"}'
# 운영(Nginx 경유)
curl -s -X POST https://mkgalaxy.kr/villa/api/ai/chat \
  -H "Content-Type: application/json" -d '{"message":"오늘 예약 있어?"}'
```
- 키 미설정 시: `{"reply":"AI 어시스턴트가 아직 설정되지 않았습니다(...)"}` → 키만 넣으면 동작.
- 401/403: 키 오타·권한·크레딧 부족 / 404·400: 모델 ID 확인.

## 6. 비용·한도 관리
- 사용량(토큰) 과금. 콘솔 *Usage*에서 모니터링, *Limits*에서 월 상한(budget) 설정 권장.
- 이 앱은 `max-tokens: 1024`, `max-tool-loops: 5`로 호출당 비용·지연을 제한한다.
- 프롬프트 캐싱(긴 system 프롬프트)으로 반복 호출 비용 절감 가능(후속 개선 항목).

## 7. 보안 체크리스트
- [ ] 키를 코드/yml/깃/로그에 평문으로 두지 않았다(환경변수만)
- [ ] EnvironmentFile 권한 600
- [ ] 키 분실/유출 시 콘솔에서 **즉시 Revoke 후 재발급**
- [ ] 공개 레포에 키 흔적 없음(guard 훅 + .gitignore의 .env 제외로 이중 방어)
