---
description: 새 AI 기능을 한 세트(promptKey·DTO·서비스·엔드포인트·mock·테스트·문서)로 추가한다
argument-hint: <기능 설명>
---

새 AI 기능을 추가한다: **$ARGUMENTS**

계획 승인 없이 바로 구현한다. 먼저 AGENTS.md의 LLM 규칙과 기존 `/api/chat`, `/api/analyze` 구현을 참고해 패턴을 맞춘다.

## 순서
1. **이름 정하기** — promptKey(영문 소문자, 예: `quiz`), 엔드포인트 경로(`/api/...`), DTO 이름을 정하고 한 줄로 알린다.
2. **응답 형태 고르기**
   - 자유 텍스트면 `ChatResponseDto` 재사용 + `LlmClient.call(...)`
   - 정해진 JSON이면 새 record DTO + `LlmClient.entity(prompt, input, Dto.class, Dto::mock)` (StructuredOutputValidationAdvisor가 자동 적용)
   - 실시간 표시가 필요하면 `LlmClient.stream(...)` + SSE
3. **prompts.yml** — `llm.prompts.<promptKey>`에 model, temperature, max-tokens, system(`|` 여러 줄, `{변수}`), 필요하면 defaults. 프롬프트 문자열은 Java에 쓰지 않는다.
4. **DTO** — `dto/request/XxxRequestDto` (record, jakarta validation + `@Schema(example)`), 구조화 출력이면 `dto/response/Xxx` (record, 필드마다 `@Schema(description)` — LLM 스키마가 된다) + `static Xxx mock()`.
5. **서비스** — `LlmService`에 메서드 추가: `promptRegistry.resolve(key, params)` → `llmClient.xxx(...)`.
6. **엔드포인트** — `LlmController`(또는 새 컨트롤러)에 `@Operation` 달고 `ApiResponse.of(SuccessType.SUCCESS, data)` 반환.
7. **테스트** — `src/test/.../LlmControllerTest` 패턴대로 목 모드 성공 1개 (ApiResult 필드 + data 핵심 필드 검증).
8. **docs/api.md** — 엔드포인트 섹션 추가 (요청/응답 JSON 예시, 에러 코드).
9. **확인**
   - `./gradlew test` 통과
   - `LLM_MOCK=true ./gradlew bootRun`을 백그라운드로 띄우고 health가 응답할 때까지 기다린 뒤, 새 엔드포인트를 curl로 호출해 결과를 보여준다
   - 서버를 종료한다 (`lsof -ti tcp:8080 | xargs kill`)

끝나면 바뀐 파일 목록과 curl 결과를 짧게 요약한다. 커밋은 하지 않는다.
