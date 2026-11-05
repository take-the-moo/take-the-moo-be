# Hackathon LLM API

주제가 무엇이든 `prompts.yml`과 DTO만 바꿔서 바로 쓰는 범용 LLM API 서버.
Java 25 · Spring Boot 4.1 · Spring AI 2.0 (Anthropic) · DB 없음.

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

### Docker

```bash
LLM_MOCK=true docker compose up -d --build   # 목 모드
docker compose up -d --build                 # 실제 호출 — 키는 application.yml 또는 셸/.env의 ANTHROPIC_API_KEY
docker compose down
```

- `application.yml`은 이미지 빌드 때 jar에 들어간다. 고친 뒤에는 `--build`로 다시 빌드한다. 키가 든 이미지이므로 레지스트리에 올리지 않는다
- `api`는 `mysql`이 healthy가 된 뒤 뜬다. DB 데이터는 `mysql-data` 볼륨에 남고, 지우려면 `docker compose down -v`
- 환경변수가 yml보다 우선하므로 셸에 `LLM_MOCK`가 남아 있으면 `application.yml`의 `llm.mock`이 무시된다

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

## 당일 순서 (18시 주제 공개 후)

1. **AGENTS.md** — "주제", "핵심 장면" 빈칸을 채운다 (에이전트가 매번 읽는 컨텍스트)
2. **prompts.yml** — `chat`/`analyze`/`image`의 system 프롬프트를 주제에 맞게 고친다. 필요하면 키를 추가한다
3. **/ai-feature <설명>** — Claude Code에서 새 기능을 한 세트로 추가한다
   (promptKey + DTO + 서비스 + 엔드포인트 + mock 응답 + 테스트 + docs/api.md, 끝에 목 모드 curl 확인)
   - 구조화 출력이 필요하면 `AnalysisResult`처럼 record DTO + `mock()`만 새로 만들면 된다
4. **/demo-check** — 데모 전 목 모드로 전 엔드포인트 점검

## 구조

```
src/main/java/com/hackathon
├── global/{config, response, exception}   ApiResult·ErrorCode·GlobalExceptionHandler·CORS·Swagger·Anthropic 클라이언트
└── domain/llm/{controller, service, dto, prompt, exception}
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
