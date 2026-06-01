---
name: deploy-ec2
description: Build the single Spring Boot JAR (frontend integrated) and deploy it to the memory-constrained EC2 instance — resource check, swap, JVM heap tuning, secret-safe key injection, and health check — recording every step as the README's 'Linux 운영 흔적'. Use when deploying the villa app to EC2 or deciding deploy-vs-local-demo. Generates SSH commands for the owner to run.
---

# deploy-ec2 — EC2 빌드·배포 + Linux 운영 흔적 기록

NHN 자격요건 'Linux 환경에서 개발 및 운영 경험'을 정면으로 충족하는 절차. 모든 명령과 수치를
`docs/ops-log.md`에 남겨 README의 운영 흔적으로 사용한다. **명령은 오너가 SSH에서 실행**하고
결과를 가져오면 본 절차가 다음 단계를 안내한다(이 도구는 EC2 셸을 직접 갖지 않는다).

## 0. 사전 점검 (결정 게이트)
```bash
free -h && ps aux --sort=-%mem | head -10 && df -h
```
- 기준: available < ~500Mi 또는 swap 0 → **먼저 스왑 추가**(아래 1단계). 너무 빠듯하면 로컬 데모로 대체.
- 2026-06-01 점검 결과: available 410Mi, swap 0 → 스왑 추가 필요로 판정됨.

## 1. 스왑 2GB (메모리 안전화 + 운영 흔적)
```bash
sudo fallocate -l 2G /swapfile || sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h     # 전/후 수치를 ops-log.md에 기록
```

## 2. 로컬 빌드 (프론트 통합 단일 JAR)
```bash
cd backend && ./gradlew buildAll      # frontend(npm run build) → dist → static 복사 후 build
ls build/libs/villa-reservation-0.0.1.jar
```

## 3. 전송 + 키 주입 (평문 금지)
```bash
scp backend/build/libs/villa-reservation-0.0.1.jar ec2-user@mkgalaxy.kr:~/villa/   # 또는 EC2 퍼블릭 IP
# 키는 코드/yml에 두지 않는다 — 권한 600 EnvironmentFile
printf 'ANTHROPIC_API_KEY=sk-ant-...\n' | sudo tee /etc/villa.env >/dev/null
sudo chmod 600 /etc/villa.env
```

## 4. 기동 + JVM 힙 상한
```bash
set -a; source /etc/villa.env; set +a
nohup java -Xms128m -Xmx256m -jar ~/villa/villa-reservation-0.0.1.jar > ~/villa/app.log 2>&1 &
```

## 5. 헬스체크 + 메모리 재확인
```bash
sleep 5
# 앱 직접(로컬)
curl -s http://localhost:8082/villa/index.html | head -c 200
curl -s -X POST http://localhost:8082/villa/api/ai/chat -H "Content-Type: application/json" -d '{"message":"오늘 예약 있어?"}'
# 운영 URL(Nginx 리버스 프록시 경유) — https://mkgalaxy.kr/villa
curl -s https://mkgalaxy.kr/villa/index.html | head -c 200
free -h && ps aux --sort=-%mem | head -5
dmesg | grep -i oom | tail -5         # OOM-killer 흔적 점검
```

## 6. (선택) systemd 등록 — 재시작 안정성
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
```bash
sudo systemctl daemon-reload && sudo systemctl enable --now villa
sudo systemctl status villa --no-pager
```

## ops-log.md 기록 형식 (README용)
```
### [조치명]
- 배경: (왜 필요)
- 명령: (실행)
- 결과: (free -h 전/후, curl 응답)
- 판단: (왜 이 선택)
```

## 주의
- `data/villa-reservation.mv.db`(가족 실데이터) 덮어쓰기 금지 — 별도 경로/백업.
- 봇들과 메모리 경합 → 배포 직후 `free -h` 필수. OOM 발생 시 -Xmx 하향 또는 로컬 데모 전환.
