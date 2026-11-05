# AWS 배포 (EC2 1대 + docker compose + Caddy HTTPS, 프론트 포함)

해커톤용 가장 단순한 구성. EC2 한 대에서 `compose.yml`(api + mysql)을 그대로 띄운다. 레지스트리·RDS·로드밸런서 없음.
API 키는 `application.yml`에만 있고 gitignore 대상이므로, 배포 스크립트가 로컬 파일을 서버로 직접 보낸다 (커밋·이미지 푸시 금지).

## 지금 떠 있는 서버 (2026-11-06)

| 항목 | 값 |
|---|---|
| 주소 | **`https://3-36-178-51.sslip.io`** — 앱 화면(프론트)과 API가 한 주소. Swagger `/swagger-ui.html`. 예전 `http://3.36.178.51:8080`(API만)도 아직 열려 있다 (탄력적 IP라 재시작해도 유지) |
| 리전·인스턴스 | 서울 `ap-northeast-2` · `i-015be9a3b8ce00377` (`t3.medium`, 이름 `take-the-moo-be`) |
| 보안 그룹 | `take-the-moo-be` — 80·443·8080 전체 공개, 22는 배포한 사람 IP만 |
| HTTPS·경로 | `compose.prod.yml`의 Caddy가 443을 받아 `/api/*`·`/swagger-ui*`·`/v3/api-docs*`는 api로, 그 밖은 프론트(web)로 넘긴다. 인증서는 Let's Encrypt에서 자동 발급·갱신 (`caddy-data` 볼륨) |
| 프론트 | `take-the-moo-fe/storywalk-fe`를 서버 `~/fe`로 보내 `web` 컨테이너(Next.js, 3000)로 띄운다. 같은 출처라 CORS 없이 `/api`를 부른다 |
| 서버 `~/app/.env` | `APP_AUTH_JWT_SECRET`, `COMPOSE_FILE=compose.yml:compose.prod.yml`, `DOMAIN`, `COMPOSE_PROFILES=web` — 스크립트가 만들고 이후 그대로 쓴다. 사람이 넣는 값: `FE_NAVER_MAP_KEY_ID`(네이버 지도 Client ID), `FE_DEMO_STAMP_AT_PLACE`(실내 시연이면 `true`), `APP_DEMO_PASSWORD`(시연 계정) |
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
| 보안 그룹 | SSH `22` 내 IP · HTTP `80`·HTTPS `443` `0.0.0.0/0` (80은 인증서 발급에 필요) |
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

프론트도 같이 올릴 때는 프론트 저장소의 `storywalk-fe` 폴더를 `FE_DIR`로 준다. 한 번 보낸 뒤에는 `FE_DIR` 없이 배포해도 서버에 있는 프론트 소스로 다시 빌드한다:

```bash
FE_DIR=../take-the-moo-fe/storywalk-fe scripts/deploy-ec2.sh ubuntu@3.36.178.51 -i ~/.ssh/take-the-moo-key.pem
```

- **두 저장소 모두 최신 main, 커밋 안 한 변경이 없는 상태에서 보낸다.** 작업 폴더를 그대로 rsync하므로 고치던 코드도 올라간다
- 프론트의 `NEXT_PUBLIC_*` 값은 빌드할 때 들어간다. 서버 `~/app/.env`의 `FE_*` 값을 바꾸면 다시 배포한다
- 네이버 지도가 뜨려면 NCP 콘솔 > Maps > Application의 **Web 서비스 URL에 `https://3-36-178-51.sslip.io`를 등록**한다

하는 일: 로컬 소스를 `~/app`으로 rsync(`application.yml` 포함, `.git`·`build` 제외) → 서버 `~/app/.env`에 JWT 서명 키가 없으면 만든다 → `docker compose up -d --build` → `/api/health`가 200을 줄 때까지 대기.
**지금 체크아웃된 브랜치의 코드가 올라간다.** 보통은 최신 `origin/main`에서 배포한다.
첫 빌드는 5분 정도, 이후 재배포는 1~2분.

- 확인: `https://<a-b-c-d>.sslip.io/api/health` (IP의 점을 `-`로 바꾼 주소), Swagger는 같은 주소의 `/swagger-ui.html`
- 서버에서 `docker compose ...`를 직접 칠 때도 `.env`의 `COMPOSE_FILE` 덕분에 Caddy까지 같이 다룬다
- 로그: `ssh -i ~/.ssh/키.pem ubuntu@<IP> 'cd app && docker compose logs -f api'`
- 중지: `ssh ... 'cd app && docker compose down'` (DB·업로드 파일은 볼륨에 남는다. `down -v`는 지운다)

## 발표 전 체크

- `application.yml`의 `app.tour.sync-enabled=false`로 장소 데이터를 고정한 뒤 재배포한다 (AGENTS.md 장소 동기화 참고)
- 목 모드로 올렸다면 `GET /api/health`의 `data.mock`이 `true`다. 실제 호출로 바꾸려면 `LLM_MOCK` 없이 다시 배포한다
- 앱은 `https://3-36-178-51.sslip.io`로 연다. 프론트는 같은 주소의 `/api`를 부르므로 따로 API 주소를 넣지 않는다
- 발표장이 실내면 서버 `.env`에 `FE_DEMO_STAMP_AT_PLACE=true`를 넣고 다시 배포한다 (방문 도장에 기기 위치 대신 장소 좌표)
- **sslip.io**는 이름 안의 IP로 연결해 주는 무료 DNS다. 도메인을 사면 A 레코드를 이 IP로 걸고 서버 `.env`의 `DOMAIN`만 바꿔 재배포한다
- `caddy-data` 볼륨을 지우면 인증서를 다시 받는다. Let's Encrypt는 같은 이름에 주당 발급 횟수 제한이 있으니 `down -v`를 반복하지 않는다
- 비용: `t3.medium`은 시간당 약 $0.05. 행사 뒤에는 인스턴스를 **중지**한다 (중지 중엔 EBS 비용만, 탄력적 IP는 연결이 끊기면 과금)

- DB 구조가 크게 바뀌어(예: 사용자 ID 형식) 기동이 실패하면, 데이터를 버려도 될 때만 `ssh ... 'cd app && docker compose down -v'` 후 다시 배포한다. 샘플 데이터는 기동 시 다시 들어간다

## 안 될 때

| 증상 | 확인 |
|---|---|
| `ssh: connect ... timed out` | 보안 그룹 22가 내 IP인지 (IP가 바뀌면 다시), 인스턴스가 running인지 |
| `rsync: command not found` | 사용자 데이터가 아직 안 끝났거나 실패. `sudo tail -50 /var/log/cloud-init-output.log` |
| 빌드 중 `Killed` / 137 | 메모리 부족. `free -h`로 스왑 확인, 안 됐으면 `sudo swapon /swapfile` |
| 헬스체크 타임아웃 | `docker compose logs api`. mysql이 healthy 될 때까지 api가 기다리므로 첫 기동은 1분 정도 더 걸린다 |
| https 접속 안 됨 / TLS 오류 | 보안 그룹 80·443, `docker compose logs caddy`. 첫 배포 직후 수십 초는 인증서 발급 중이라 TLS 오류·502가 정상 |
| 502 Bad Gateway | `/api`면 api가 아직 뜨는 중이거나 죽음(`docker compose logs api`), 화면이면 web(`docker compose logs web`) |
| Caddyfile을 고쳤는데 그대로 | 배포 스크립트가 `caddy reload`를 하는지, `docker compose exec caddy cat /etc/caddy/Caddyfile`로 내용 확인 |
| 지도만 안 뜸 | `FE_NAVER_MAP_KEY_ID`가 서버 `.env`에 있는지, 네이버 Web 서비스 URL에 이 주소가 등록됐는지 |
