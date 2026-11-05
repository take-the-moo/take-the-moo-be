# AWS 배포 (EC2 1대 + docker compose)

해커톤용 가장 단순한 구성. EC2 한 대에서 `compose.yml`(api + mysql)을 그대로 띄운다. 레지스트리·RDS·로드밸런서 없음.
API 키는 `application.yml`에만 있고 gitignore 대상이므로, 배포 스크립트가 로컬 파일을 서버로 직접 보낸다 (커밋·이미지 푸시 금지).

## 지금 떠 있는 서버 (2026-11-06)

| 항목 | 값 |
|---|---|
| 주소 | `http://3.36.178.51:8080` (탄력적 IP, 재시작해도 유지) · Swagger `/swagger-ui.html` |
| 리전·인스턴스 | 서울 `ap-northeast-2` · `i-015be9a3b8ce00377` (`t3.medium`, 이름 `take-the-moo-be`) |
| 보안 그룹 | `take-the-moo-be` — 8080 전체 공개, 22는 배포한 사람 IP만 |
| SSH 키 | `take-the-moo-key` (`~/.ssh/take-the-moo-key.pem`, 만든 사람 PC에만 있다 — 필요하면 직접 받는다) |
| 로그인 JWT 서명 키 | 서버의 `~/app/.env`에 있는 `APP_AUTH_JWT_SECRET`. 첫 배포 때 스크립트가 만들고 이후 그대로 쓴다. 지우면 모두 다시 로그인해야 한다 |
| 확인용 계정 | `deploy_check` (배포 확인 때 만든 테스트 계정) |

재배포:

```bash
scripts/deploy-ec2.sh ubuntu@3.36.178.51 -i ~/.ssh/take-the-moo-key.pem
```

다른 팀원이 배포하려면 키 파일을 받고, 보안 그룹 22번에 자기 IP를 추가한다:

```bash
aws ec2 authorize-security-group-ingress --region ap-northeast-2 --group-name take-the-moo-be --protocol tcp --port 22 --cidr $(curl -s https://checkip.amazonaws.com)/32
```

## 1. 인스턴스 만들기 (콘솔, 한 번만)

AWS 콘솔 → 리전 **서울(ap-northeast-2)** → EC2 → 인스턴스 시작

| 항목 | 값 |
|---|---|
| AMI | Ubuntu Server 24.04 LTS |
| 인스턴스 유형 | `t3.medium` (4GB). 프리티어 `t3.micro`(1GB)는 이미지 빌드 중 죽는다 |
| 키 페어 | 새로 만들고 `.pem` 저장 → `chmod 400 ~/.ssh/키.pem` |
| 보안 그룹 | SSH `22` 내 IP · 사용자 지정 TCP `8080` `0.0.0.0/0` |
| 스토리지 | 20GB gp3 |
| 고급 세부 정보 → 사용자 데이터 | `scripts/ec2-user-data.sh` 내용을 통째로 붙여넣기 (Docker·rsync 설치, 스왑) |

- 시작 후 2~3분 기다린다 (사용자 데이터가 Docker를 설치하는 시간). 확인: `ssh -i ~/.ssh/키.pem ubuntu@<IP> 'docker compose version'`
- 재부팅해도 IP를 유지하려면 **탄력적 IP**를 하나 만들어 연결한다 (프론트에 알려줄 주소가 바뀌지 않는다)

## 2. 배포 (처음·재배포 모두 같은 명령)

```bash
scripts/deploy-ec2.sh ubuntu@<EC2 IP> -i ~/.ssh/키.pem
```

```bash
LLM_MOCK=true scripts/deploy-ec2.sh ubuntu@<EC2 IP> -i ~/.ssh/키.pem   # 목 모드 (키·크레딧 없이)
```

하는 일: 로컬 소스를 `~/app`으로 rsync(`application.yml` 포함, `.git`·`build` 제외) → 서버 `~/app/.env`에 JWT 서명 키가 없으면 만든다 → `docker compose up -d --build` → `/api/health`가 200을 줄 때까지 대기.
**지금 체크아웃된 브랜치의 코드가 올라간다.** 보통은 최신 `origin/main`에서 배포한다.
첫 빌드는 5분 정도, 이후 재배포는 1~2분.

- 확인: `http://<IP>:8080/api/health`, Swagger `http://<IP>:8080/swagger-ui.html`
- 로그: `ssh -i ~/.ssh/키.pem ubuntu@<IP> 'cd app && docker compose logs -f api'`
- 중지: `ssh ... 'cd app && docker compose down'` (DB·업로드 파일은 볼륨에 남는다. `down -v`는 지운다)

## 발표 전 체크

- `application.yml`의 `app.tour.sync-enabled=false`로 장소 데이터를 고정한 뒤 재배포한다 (AGENTS.md 장소 동기화 참고)
- 목 모드로 올렸다면 `GET /api/health`의 `data.mock`이 `true`다. 실제 호출로 바꾸려면 `LLM_MOCK` 없이 다시 배포한다
- 프론트는 `http://<IP>:8080`을 base URL로 쓴다. HTTPS가 아니므로 **https로 배포된 프론트에서는 브라우저가 혼합 콘텐츠로 차단**한다. 그 경우 도메인 + Caddy(자동 TLS)를 앞에 두어야 한다 — 필요해지면 추가
- 비용: `t3.medium`은 시간당 약 $0.05. 행사 뒤에는 인스턴스를 **중지**한다 (중지 중엔 EBS 비용만, 탄력적 IP는 연결이 끊기면 과금)

- DB 구조가 크게 바뀌어(예: 사용자 ID 형식) 기동이 실패하면, 데이터를 버려도 될 때만 `ssh ... 'cd app && docker compose down -v'` 후 다시 배포한다. 샘플 데이터는 기동 시 다시 들어간다

## 안 될 때

| 증상 | 확인 |
|---|---|
| `ssh: connect ... timed out` | 보안 그룹 22가 내 IP인지 (IP가 바뀌면 다시), 인스턴스가 running인지 |
| `rsync: command not found` | 사용자 데이터가 아직 안 끝났거나 실패. `sudo tail -50 /var/log/cloud-init-output.log` |
| 빌드 중 `Killed` / 137 | 메모리 부족. `free -h`로 스왑 확인, 안 됐으면 `sudo swapon /swapfile` |
| 헬스체크 타임아웃 | `docker compose logs api`. mysql이 healthy 될 때까지 api가 기다리므로 첫 기동은 1분 정도 더 걸린다 |
| 8080 접속 안 됨 | 보안 그룹 8080 인바운드, 컨테이너 `docker compose ps` |
