# 서대문 발자취 API

서대문에 온 유학생에게 지금 갈 장소를 추천하고, 모국어와 내 나라 이야기로 장소를 풀어주고, 방문 도장·한걸음·회고 엽서로 발자취를 남기게 하는 서비스의 백엔드.
Java 25 · Spring Boot 4.1 · Spring AI 2.0 (Anthropic) · MySQL 8.4.

기능·패키지·서비스 원칙은 [AGENTS.md](AGENTS.md)에 있다.

## 실행

```bash
bash scripts/setup-hooks.sh                  # clone 후 1회 (git 훅)
docker compose up -d mysql                   # DB (localhost:3307 / hackathon / hackathon)
LLM_MOCK=true ./gradlew bootRun              # 목 모드 — API 키 없이 고정 응답
./gradlew bootRun                            # 실제 호출 — application.yml의 api-key에 키 기입
```

- JDK 25가 없으면 Gradle이 첫 빌드 때 자동으로 내려받는다 (실행용 JDK는 17 이상이면 된다)
- Swagger UI: http://localhost:8080/swagger-ui.html
- 프론트용 명세: [docs/api.md](docs/api.md)
- DB 설계: [docs/db.md](docs/db.md) — 엔티티를 만들거나 고치기 전에 반드시 읽는다

### Docker

```bash
LLM_MOCK=true docker compose up -d --build   # 목 모드
docker compose up -d --build                 # 실제 호출 — 키는 application.yml 또는 셸/.env의 ANTHROPIC_API_KEY
docker compose down
```

- `application.yml`은 이미지 빌드 때 jar에 들어간다. 고친 뒤에는 `--build`로 다시 빌드한다. 키가 든 이미지이므로 레지스트리에 올리지 않는다
- `api`는 `mysql`이 healthy가 된 뒤 뜬다. DB 데이터는 `mysql-data` 볼륨에 남고, 지우려면 `docker compose down -v`
- 환경변수가 yml보다 우선하므로 셸에 `LLM_MOCK`가 남아 있으면 `application.yml`의 `llm.mock`이 무시된다

### AWS 배포

EC2 한 대에서 `compose.yml`에 Caddy(HTTPS, `compose.prod.yml`)를 얹어 띄운다. 지금 서버는 **`https://3-36-178-51.sslip.io`**. 자세한 건 [docs/deploy.md](docs/deploy.md).

```bash
scripts/deploy-ec2.sh ubuntu@3.36.178.51 -i ~/.ssh/take-the-moo-key.pem   # 배포·재배포
```

### 목 모드

`llm.mock=true`(환경변수 `LLM_MOCK=true`)면 Anthropic API를 호출하지 않고 `[MOCK]`으로 시작하는 고정 응답을 준다.
응답 형식은 실제와 같으므로 프론트는 크레딧·네트워크 없이 먼저 연동할 수 있다. `GET /api/health`의 `data.mock`으로 확인한다.

## curl 예시

```bash
curl localhost:8080/api/health

curl -X POST localhost:8080/api/chat -H 'Content-Type: application/json' \
  -d '{"message":"자기소개 해줘","params":{"target":"해커톤 참가자"}}'

curl -N 'localhost:8080/api/chat/stream?message=자기소개%20해줘'

curl -X POST localhost:8080/api/analyze -H 'Content-Type: application/json' \
  -d '{"input":"회의록: 프론트 로그인 완료, 백엔드 LLM 연동 중"}'

curl -F file=@photo.png -F question="뭐가 보여?" localhost:8080/api/image
```

## 작업 방법

1. AGENTS.md의 "기능과 API" 표에서 작업할 기능의 패키지를 확인한다 (에이전트는 AGENTS.md를 자동으로 읽는다)
2. AI가 들어가는 기능은 **/ai-feature <설명>** 으로 한 세트를 만든다
   (promptKey + DTO + 서비스 + 엔드포인트 + mock 응답 + 테스트 + docs/api.md, 끝에 목 모드 curl 확인)
3. 요청/응답 형식을 정하면 `docs/api.md`에 먼저 적어 프론트와 맞춘다
4. **/demo-check** — 데모 전 목 모드로 전 엔드포인트 점검

## 구조

```
src/main/java/com/hackathon
├── global/{config, common, entity, util, response, exception}   설정·공통 코드값·BaseTimeEntity·ApiResult·예외 처리
└── domain/
    ├── llm/{controller, service, dto, prompt, exception}       LlmClient·PromptRegistry·범용 샘플 API
    └── {user, recommend, scene, place, photo, visit, review, postcard, journey}/   기능별 도메인 (AGENTS.md 참고)
src/main/resources/{application.yml, prompts.yml}
```

- 응답·예외 규칙, LLM 규칙은 [AGENTS.md](AGENTS.md)
- `.claude/` — 권한 설정, `.java` 수정 시 자동 `compileJava` 훅, `/ai-feature`·`/demo-check` 커맨드
- `.githooks/pre-commit` — `.env`·API 키(`sk-ant-…`)·설정 파일 리터럴 시크릿 커밋 차단
- `.github/workflows/ci.yml` — main push·PR마다 컴파일만 확인 (`application.yml`이 gitignore라 테스트는 로컬에서). 차단·배포 없이 확인만

## 설정 요약

| 키 | 기본값 | 설명 |
|---|---|---|
| `spring.ai.anthropic.api-key` | (없음) | Anthropic API 키 — `application.yml`(gitignore)에 직접 또는 환경변수 `ANTHROPIC_API_KEY` |
| `spring.ai.openai.api-key` | (없음) | OpenAI API 키 — `application.yml`(gitignore)에 직접 또는 환경변수 `OPENAI_API_KEY` |
| `LLM_MOCK` / `llm.mock` | `false` | 목 모드 |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | `jdbc:mysql://localhost:3307/hackathon` / `hackathon` / `hackathon` | MySQL 접속 (compose 개발용 계정) |
| `DB_PORT` | `3307` | compose가 여는 MySQL 호스트 포트 |
| `llm.timeout.connect` / `read` / `request` | 5s / 60s / 120s | LLM 호출 연결 / 응답 대기 / 전체 상한 |
| `llm.max-retries` | 1 | SDK 재시도 횟수 |
| `app.storage.dir` | `./uploads` | 사진·엽서 저장 디렉터리 |
| `app.weather.kma-service-key` / `APP_WEATHER_KMA_SERVICE_KEY` | (없음) | 기상청 초단기실황 키 (Decoding 키). 없으면 시뮬레이션·기본 날씨 |
| `app.tour.service-key` / `APP_TOUR_SERVICE_KEY` | (없음) | TourAPI 키 (Decoding 키). 장소를 매일 04:00에 DB로 동기화. 없으면 건너뜀 |
| `app.tour.sync-enabled` | `true` | 장소 동기화 켜기. 발표 전에는 `false` |
| `app.station.api-key` / `APP_STATION_API_KEY` | (없음) | T Data 지하철역 좌표정보 인증키. 서버 시작 시(비어 있으면)와 장소 동기화 때 역을 DB로 동기화 |
| `app.demo.default-station` | `독립문` | 위치·역을 못 받았을 때 기준 역 |
| `app.demo.password` / `APP_DEMO_PASSWORD` | (없음) | 시연 계정 `demo_linh`의 비밀번호. 없으면 시연 계정을 만들지 않는다 |
| `app.visit.stamp-radius-meters` | `200` | 방문 도장을 줄 장소 반경(미터) |
