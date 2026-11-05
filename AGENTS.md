# AGENTS.md

2026 명지대 AI 창의적 아이디어 해커톤 백엔드. AI 에이전트(Claude, Codex 등)는 이 파일을 먼저 읽는다.

## 프로젝트 개요
- **주제**: 서대문구를 비롯한 서울의 역사·문화·예술·관광·생활자원을 소재로 한 새로운 AI 기반 K-콘텐츠 서비스
- **한 줄 요약**: 서대문에 온 유학생이 앱을 열면 지금에 맞는 장소를 추천받고, 내 언어와 내 나라 이야기로 장소를 이해하고, 방문 도장과 한 줄 흔적을 남기고, 하루를 엽서 한 장으로 간직한다
- **컨셉 — 세 겹의 발자취**: 서대문의 발자취(장소 이야기) · 먼저 온 유학생의 발자취(흔적 지도·한걸음) · 나의 발자취(도장·기록·엽서)
- **타깃**: 한국 생활 1~3개월, 서대문 거주 학부 유학생. 장소 이름은 알아도 의미를 몰라 그냥 지나친다
- **핵심 장면**: 베트남 유학생 린이 토요일 오후 독립문역에서 내려 걷다가 "여기가 뭔데?" 하고 지나친다 → 앱을 열면 질문 없이 지금 갈 곳과 그 이유가 준비돼 있다
- 스택: Java 25 · Spring Boot 4.1 · Spring AI 2.0 (Anthropic) · Gradle Groovy · MySQL 8.4 (JPA, 테스트는 H2)
- 기획 원문은 팀 노션. 이 파일에는 백엔드가 지켜야 할 것만 둔다

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

## 기능과 API (P0)
기능마다 들어갈 패키지가 정해져 있다. 새 코드는 해당 패키지에 만든다.

| 기능 | 패키지 | 엔드포인트 | AI가 하는 일 |
|---|---|---|---|
| F1 처음 설정 | `domain/user` | `POST /api/users`, `GET /api/users/{userId}` | - |
| F2 지금 추천 | `domain/recommend` | `GET /api/now` | 규칙으로 거른 후보 중 장소 선택 + 추천 이유 |
| F3 맞춤 장면 | `domain/scene` | `POST /api/scenes` | 모국어 + 쉬운 한국어 이야기(3~4문장), 내 나라 역사와 한 줄 연결 |
| F4 현장 해설 | `domain/place` | `GET /api/places/nearby`, `POST /api/explanations` | 저장된 해설을 번역만 한다 |
| 사진 (F4·F6·F7) | `domain/photo` | `POST /api/photos` | - |
| F5 방문 도장·기록 | `domain/visit` | `POST /api/visits`, `GET /api/users/{userId}/visits` | - |
| F6 흔적 지도·한걸음 | `domain/review`, `domain/visit` | `GET /api/map/pins`, `GET·POST /api/places/{placeId}/reviews` | 한걸음 번역, 욕설·광고 필터 |
| F7 회고 엽서 | `domain/postcard` | `POST /api/postcards`, `GET /api/postcards/{postcardId}` | 사진 4장 + 소감 + 그림체 → 엽서 이미지 (OpenAI) |
| 장소·역 데이터 | `domain/place` | (시드 데이터 적재) | - |
| 공통 | `global`, `domain/llm` | `GET /api/health` | - |

범위 밖(만들지 않는다): 장면 삽화, TTS, 단어장, 가족용 공유 페이지, 추가 질문 채팅, 혼잡도, 축제 추천.

## 작업 시작 전에
여러 사람이 각자 AI 에이전트로 동시에 작업한다. 충돌을 줄이려고 다음을 지킨다.
1. 작업할 기능의 패키지가 이미 있으면 먼저 읽고 그 패턴을 따른다. 없으면 아래 패키지 구조대로 만든다
2. 다른 도메인이 필요하면 그 도메인의 service 메서드를 쓴다. 아직 없으면 필요한 메서드만 최소로 추가한다 (구조를 갈아엎지 않는다)
3. `global/`, `domain/llm/`, `prompts.yml`의 기존 항목은 모두가 쓰는 코드다. 고쳐야 하면 기존 동작을 깨지 않게 추가만 한다
4. API 형식을 정했으면 구현과 같은 변경에서 `docs/api.md`의 해당 엔드포인트 섹션을 채운다

## 패키지 구조
```
com.hackathon
├── global/
│   ├── config/      AppProperties(app.*), JpaConfig(auditing), LlmProperties, AnthropicConfig, OpenApiConfig, WebConfig
│   ├── common/      Nationality, Language — 여러 도메인이 같이 쓰는 코드값
│   ├── entity/      BaseTimeEntity — createdAt·updatedAt 자동 (서울 시간)
│   ├── util/        SeoulTime(오늘·지금, Asia/Seoul), GeoUtils(거리 m)
│   ├── response/    ApiResult, ApiResponse, SuccessType, ErrorCode, CommonErrorType
│   └── exception/   DomainException, GlobalExceptionHandler
└── domain/
    ├── llm/         LlmClient(ChatClient 호출은 여기만), PromptRegistry, 범용 샘플 API(/api/chat 등)
    └── {기능}/      controller · service · repository · entity · dto/{request,response} · exception
```
- 도메인 패키지는 위 구조를 따른다. 다른 도메인의 엔티티를 연관관계로 묶지 말고 id(`Long placeId`, `String userId`)로 참조한다 — 병렬 작업 충돌을 줄인다
- 다른 도메인 데이터가 필요하면 그 도메인의 service 메서드를 호출한다 (repository를 직접 쓰지 않는다)

## 공통 코드값
- 국적 `Nationality`: `VN, CN, JP, MN, UZ, US, OTHER` / 언어 `Language`: `KO, EN, VI, ZH, JA, MN, UZ`. API에서는 상수 이름 그대로 주고받는다
- 시간·날짜는 `SeoulTime.now()/today()`를 쓴다 (`LocalDateTime.now()` 직접 금지 — 컨테이너는 UTC)
- 엔티티는 `BaseTimeEntity`를 상속한다
- 사용자는 로그인 없는 익명 사용자. `userId`는 서버가 만든 UUID 문자열이고 프론트가 기기에 저장한다

## 응답·예외 컨벤션
- 모든 JSON 응답은 `ApiResult { resultType, httpStatusCode, code(실패 시), message, data }`
- 성공: `return ApiResponse.of(SuccessType.SUCCESS, data);` (생성은 `SuccessType.CREATED`)
- 실패: `throw new XxxException(XxxErrorType.YYY, "detail");` → 코드는 `{타입명}_{상수명}` 자동 (예: `REVIEW_INAPPROPRIATE`)
  - 도메인마다 `exception/XxxErrorType`(`ErrorCode` 구현 enum) + `XxxException`(`DomainException` 상속)을 만든다. `IllegalArgumentException` 등 직접 던지지 않는다
  - 새 ErrorType enum은 `OpenApiConfig.ERROR_TYPES`에 추가한다 (Swagger 오류 예시)
  - `detail`은 로그에만 남는다. 응답 message에 API 키·프롬프트 원문·모델 원본 오류를 넣지 않는다
- 검증 실패 422 `COMMON_VALIDATION_FAILED`, 본문/파라미터 오류 400 `COMMON_INVALID_REQUEST`, 없는 리소스 404 `{도메인}_NOT_FOUND`
- 한걸음이 부적절하면 422 `REVIEW_INAPPROPRIATE`

## 서비스 원칙 (기획에서 온 규칙 — 코드·프롬프트 모두)
- **개인정보**: 위치(lat/lng)는 요청 처리에만 쓰고 저장·로그하지 않는다. 지도·한걸음에는 장소 단위 집계와 날짜까지만 보여주고 개인의 위치·방문 시각은 내보내지 않는다
- **AI 정확성**: DB에 있는 장소·정보만 근거로 쓴다. 추천은 규칙으로 거른 후보 안에서만 고르고, 후보 밖 id가 오면 버린다. 현장 해설은 저장된 원문을 **번역만** 한다
- **민감한 역사**: 식민지 역사가 얽힌 장소(서대문형무소 등)는 누군가를 탓하지 않고 "기억과 평화의 공간"으로 담담하게 전한다. 이 원칙은 해당 프롬프트 system에 고정한다
- **시연 안정성**: 모든 AI 기능은 목 응답이 있어야 한다. 위치·날씨·시간은 요청 파라미터로 시뮬레이션할 수 있게 한다 (발표장은 실내). 번역·장면은 장소·언어별로 캐싱한다
- **응답 시간**: 추천·장면은 몇 초 안. 엽서처럼 오래 걸리는 생성은 비동기 + 상태 조회
- **샘플 데이터**: 지도가 비어 보이지 않게 장소별 한걸음 3~5개, 국적이 섞인 방문 기록을 시드로 넣는다. 샘플임을 구분할 수 있게 한다

## LLM 규칙
- 프롬프트 문자열은 Java 코드에 쓰지 않는다. `src/main/resources/prompts.yml`에만 (model, temperature, max-tokens, system, defaults). 장소 데이터 같은 입력값은 user 메시지로 넘긴다
- ChatClient는 `LlmClient`에서만 호출한다 (목 모드 분기·타임아웃·예외 변환·로깅이 거기 있다). 도메인 서비스는 `PromptRegistry.resolve()` → `LlmClient.call/entity()`를 쓴다
- 구조화 출력은 record DTO + `static mock()` + `LlmClient.entity(...)`
- 새 AI 기능은 한 세트로 추가한다: **promptKey + 요청/응답 DTO + 서비스 메서드 + 엔드포인트 + mock 응답 + 테스트 + docs/api.md** → `/ai-feature <설명>`
- 프롬프트에 언어·국적을 넣을 때는 `Language.getKoreanName()`, `Nationality.getKoreanName()`을 변수로 넘긴다
- Spring AI는 2.0 API 기준 (옵션은 불변 builder, `.mutate()`, Jackson 3 = `tools.jackson`). 1.x 예제를 그대로 쓰지 않는다
- 엽서 이미지는 OpenAI 이미지 API. 키는 `spring.ai.openai.api-key`(환경변수 `OPENAI_API_KEY`). 키가 없거나 목 모드면 대체 이미지로 동작해야 한다
  - OpenAI 스타터를 추가하면 ChatModel이 둘이 되어 `ChatClient.Builder` 자동 설정이 깨질 수 있다. `spring.ai.model.chat=anthropic`, `spring.ai.model.image=openai`로 역할을 고정한다

## 외부 설정 (`app.*`, `AppProperties`)
| 키 | 용도 | 없을 때 |
|---|---|---|
| `app.storage.dir` | 사진·엽서 저장 디렉터리 (기본 `./uploads`) | 기본값 |
| `app.weather.kma-service-key` | 기상청 초단기실황 (공공데이터포털 Decoding 키) | 날씨 시뮬레이션/기본값 |
| `app.demo.default-station` | 위치·역이 없을 때 기준 역 (기본 `dongnimmun`) | 기본값 |

키는 `application.yml`(gitignore) 또는 환경변수(`APP_WEATHER_KMA_SERVICE_KEY`)로 넣는다. AI 키는 `spring.ai.anthropic.api-key`(`ANTHROPIC_API_KEY`), `spring.ai.openai.api-key`(`OPENAI_API_KEY`).

## 속도 규칙
- 계획 승인 루프 없이 바로 구현한다
- 변경 후 `./gradlew compileJava` 통과를 확인한다 (.java 수정 시 훅이 자동 실행)
- API 요청/응답 형식이 바뀌면 `docs/api.md`를 같은 변경에서 갱신한다
- 엔드포인트마다 목 모드 성공 테스트 1개 이상 (`LlmControllerTest` 패턴)
- 데모 전에는 `/demo-check`로 전 엔드포인트를 확인한다

## 절대 규칙
1. API 키·`.env`·`src/main/resources/application.yml`을 커밋하지 않는다. `application.yml`은 gitignore 대상이고 팀원끼리 직접 공유한다 (키 패턴은 pre-commit이 차단)
2. `application.yml`의 API 키 값은 읽지 않는다 (`spring.ai.anthropic.api-key`, `spring.ai.openai.api-key`, `app.weather.kma-service-key` 등)
   - 파일 전체를 `cat`·Read로 열지 않는다. 설정을 확인하거나 고칠 땐 필요한 구간만 보고, 키가 있는 줄은 빼고 본다 (예: `grep -v -i 'key' application.yml`)
   - 키 값을 응답·로그·커밋 메시지·문서·테스트에 옮겨 적지 않는다. `.env`와 `ANTHROPIC_API_KEY`·`OPENAI_API_KEY` 같은 환경변수 값도 출력하지 않는다 (`env`, `printenv`, `echo $..._KEY` 금지)
   - 키가 필요한 동작은 목 모드(`LLM_MOCK=true`)로 확인한다. 실제 키 확인이 필요하면 사람에게 요청한다
3. force push 하지 않는다
4. 커밋은 사용자가 요청할 때만 한다
