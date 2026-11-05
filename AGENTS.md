# AGENTS.md

해커톤용 범용 LLM API 서버. AI 에이전트(Claude, Codex 등)는 이 파일을 먼저 읽는다.

## 프로젝트 개요
- **주제**: (당일 기입)
- **핵심 장면**: [누가] [언제] [무엇 때문에] 막힌다 → (당일 기입)
- 스택: Java 25 · Spring Boot 4.1 · Spring AI 2.0 (Anthropic) · Gradle Groovy · MySQL 8.4 (JPA, 테스트는 H2)

## 명령어
```bash
./gradlew compileJava              # 빠른 확인 (변경 후 필수)
./gradlew test                     # 테스트 (목 모드로 돈다)
./gradlew build                    # 전체 빌드
docker compose up -d mysql         # 로컬 DB (localhost:3307, bootRun 전에 필요)
LLM_MOCK=true ./gradlew bootRun    # 목 모드 실행 — API 키·크레딧 없이 고정 응답
./gradlew bootRun                  # 실제 호출 — 키는 src/main/resources/application.yml
LLM_MOCK=true docker compose up -d --build   # 컨테이너 실행 (목 모드)
bash scripts/setup-hooks.sh        # clone 후 1회 — git 훅 활성화
```
Swagger: http://localhost:8080/swagger-ui.html · 프론트 명세: `docs/api.md`

## 패키지 구조
```
com.hackathon
├── global/{config, response, exception}      # 공통 응답·예외·CORS·Swagger·Anthropic 클라이언트
└── domain/llm/
    ├── controller/   LlmController(/api/chat, /chat/stream, /analyze, /image), HealthController
    ├── service/      LlmService(기능별 진입점), LlmClient(ChatClient 호출은 여기만)
    ├── prompt/       PromptRegistry(prompts.yml 조회·{변수} 치환), PromptProperties
    ├── dto/request·response/   record DTO. 구조화 출력 DTO에는 static mock()
    └── exception/    LlmErrorType, LlmException
```

## 응답·예외 컨벤션
- 모든 JSON 응답은 `ApiResult { resultType, httpStatusCode, code(실패 시), message, data }`
- 성공: `return ApiResponse.of(SuccessType.SUCCESS, data);`
- 실패: `throw new XxxException(XxxErrorType.YYY, "detail");` → 코드는 `{타입명}_{상수명}` 자동 (예: `LLM_TIMEOUT`)
  - ErrorType은 `ErrorCode` 구현 enum, 예외는 `DomainException` 상속. `IllegalArgumentException` 등 직접 던지지 않는다
  - `detail`은 로그에만 남는다. 응답 message에 API 키·프롬프트 원문·모델 원본 오류를 넣지 않는다
- 검증 실패 422 `COMMON_VALIDATION_FAILED`, 본문/파라미터 오류 400 `COMMON_INVALID_REQUEST`
- SSE는 스트림 전 오류만 ApiResult, 스트림 중 오류는 `event: error` + `{code, message}` 후 종료

## LLM 규칙
- 프롬프트 문자열은 Java 코드에 쓰지 않는다. `src/main/resources/prompts.yml`에만 (model, temperature, max-tokens, system, defaults)
- ChatClient는 `LlmClient`에서만 호출한다 (목 모드 분기·타임아웃·예외 변환·로깅이 거기 있다)
- 새 AI 기능은 한 세트로 추가한다: **promptKey + 요청/응답 DTO + 서비스 메서드 + 엔드포인트 + mock 응답 + 테스트 + docs/api.md** → `/ai-feature <설명>`
- Spring AI는 2.0 API 기준 (옵션은 불변 builder, `.mutate()`, Jackson 3 = `tools.jackson`). 1.x 예제를 그대로 쓰지 않는다

## 속도 규칙
- 계획 승인 루프 없이 바로 구현한다
- 변경 후 `./gradlew compileJava` 통과를 확인한다 (.java 수정 시 훅이 자동 실행)
- API 요청/응답 형식이 바뀌면 `docs/api.md`를 같은 변경에서 갱신한다
- 데모 전에는 `/demo-check`로 전 엔드포인트를 확인한다

## 절대 규칙
1. API 키·`.env`·`src/main/resources/application.yml`을 커밋하지 않는다. `application.yml`은 gitignore 대상이고 팀원끼리 직접 공유한다 (키 패턴은 pre-commit이 차단)
2. force push 하지 않는다
3. 커밋은 사용자가 요청할 때만 한다
