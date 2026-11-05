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
| F2 지금 추천 | `domain/recommend` (날씨는 `domain/weather`) | `GET /api/now` | 규칙으로 거른 후보 중 장소 선택 + 추천 이유 |
| F3 맞춤 장면 | `domain/scene` | `POST /api/scenes` | 모국어 + 쉬운 한국어 이야기(3~4문장), 내 나라 역사와 한 줄 연결 |
| F4 현장 해설 | `domain/place` | `GET /api/places/nearby`, `POST /api/explanations` | 저장된 해설을 번역만 한다 |
| 사진 (F4·F6·F7) | `domain/photo` | `POST /api/photos` | - |
| F5 방문 도장·기록 | `domain/visit` | `POST /api/visits`, `GET /api/users/{userId}/visits` | - |
| F6 흔적 지도·한걸음 | `domain/review`, `domain/visit` | `GET /api/map/pins`, `GET·POST /api/places/{placeId}/reviews` | 한걸음 번역, 욕설·광고 필터 |
| F7 회고 엽서 | `domain/postcard` | `POST /api/postcards`, `GET /api/postcards/{postcardId}` | 사진 4장 + 소감 + 그림체 → 엽서 이미지 (OpenAI) |
| 장소·역 데이터 | `domain/place` | (TourAPI 스케줄 동기화 + 팀 보강 데이터) | - |
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
│   ├── external/    ExternalHttp(외부 GET+JSON, 키 인코딩·XML 오류 처리), ExternalApiException, PublicDataHeader
│   ├── response/    ApiResult, ApiResponse, SuccessType, ErrorCode, CommonErrorType
│   └── exception/   DomainException, GlobalExceptionHandler
└── domain/
    ├── llm/         LlmClient(ChatClient 호출은 여기만), PromptRegistry, 범용 샘플 API(/api/chat 등)
    ├── place/       Place·Station 엔티티, TourApiClient·StationApiClient, PlaceSyncService·StationSyncService,
    │                DataSyncScheduler(시작 시·매일 동기화), curation/PlaceCurationRegistry(팀 보강 파일)
    ├── weather/     WeatherService.resolve(시뮬레이션 날씨) / current() — 10분 캐시, 실패해도 예외 없음
    └── {기능}/      controller · service · repository · entity · dto/{request,response} · exception
```
- 도메인 패키지는 위 구조를 따른다. 다른 도메인의 엔티티를 연관관계로 묶지 말고 id(`Long placeId`, `String userId`)로 참조한다 — 병렬 작업 충돌을 줄인다
- 다른 도메인 데이터가 필요하면 그 도메인의 service 메서드를 호출한다 (repository를 직접 쓰지 않는다)

## 공통 코드값
- 국적 `Nationality`: `VN, CN, JP, MN, UZ, US, OTHER` / 언어 `Language`: `KO, EN, VI, ZH, JA, MN, UZ`. API에서는 상수 이름 그대로 주고받는다
- 시간·날짜는 `SeoulTime.now()/today()`를 쓴다 (`LocalDateTime.now()` 직접 금지 — 컨테이너는 UTC)
- 엔티티는 `BaseTimeEntity`를 상속한다
- 사용자는 로그인 없는 익명 사용자. `userId`는 서버가 만든 UUID 문자열이고 프론트가 기기에 저장한다

## 장소·역 데이터 (외부 API 동기화)
장소와 지하철역은 CSV·하드코딩 시드가 아니라 **외부 API를 호출해 DB에 동기화**한다. 관리자 API는 만들지 않는다.

### 이미 있는 것 (연동 틀)
- 외부 호출은 `ExternalHttp.getJson()`으로 한다. 쿼리 값을 직접 인코딩하고(Decoding 키의 `+`·`/` 보호), 키가 든 URL·원본 예외는 로그·예외에 남기지 않는다
- TourAPI(서대문구 161건 중 관광지·문화시설·쇼핑 115건 수집)·기상청·T Data(역 784건) 모두 2026-11 실제 키로 응답 형식을 확인했다
- 장소를 쓰는 기능은 `PlaceRepository.findAllByStatus(ACTIVE)`만 후보로 쓴다. 조회용 서비스 메서드(`PlaceService`)는 기능을 만들 때 `domain/place/service`에 추가한다

### 장소: TourAPI
- **실행 시점**: 서버를 켤 때마다 1회 + `@Scheduled(cron = "${app.tour.sync-cron}", zone = "Asia/Seoul")`로 매일 1회. 목록 호출 3번 + 바뀐 개요만이라 가볍다. `app.tour.sync-enabled=false`면 건너뛴다 (발표 전에는 꺼서 데이터를 고정한다)
- **받는 타입** `app.tour.content-type-ids` = 관광지(12)·문화시설(14)·쇼핑(38). **바로 노출하는 타입** `app.tour.auto-active-type-ids` = 관광지·문화시설. 서대문형무소역사관·임시정부기념관은 문화시설로 분류돼 있다
- **노출 규칙** (`Place.applyRules`, 동기화 때마다 다시 계산)
  | 조건 | 상태 |
  |---|---|
  | 보강 파일 `excluded`에 있음 | `EXCLUDED` (공연장·영화관·어린이공원 등) |
  | 보강 파일 `places`에 있음, 또는 자동 노출 타입 | `ACTIVE` |
  | 그 외 (쇼핑 매장 등) | `PENDING_REVIEW` |
  | 이번 목록에 없음 | `INACTIVE` |
- **필드 출처**
  | 출처 | 필드 |
  |---|---|
  | TourAPI | `contentId`(유니크 키), 이름, 주소, 좌표, 개요, 대표 이미지, `modifiedTime` |
  | 보강 파일 (없으면 기본값) | 추천 시간대(문화시설 `MORNING,AFTERNOON` / 그 외 `MORNING,AFTERNOON,EVENING`), 실내 여부(문화시설이면 true), 민감한 역사 여부(false), 현장 해설 원문(null) |
- **보강 파일** `src/main/resources/data/place-curation.json`: `{ excluded: [contentId…], excludedNames: [참고용 이름…], places: [{contentId, name, timeSlots?, indoor?, sensitiveHistory?, explanation?}] }`. 필요한 값만 적는다. 형식은 `PlaceCuration` 주석 참고. **고친 뒤 서버를 다시 켜면 반영된다**
- 현장 해설(`explanation`)이 없는 장소는 해설 기능에서 TourAPI 개요로 대신하거나 후보에서 뺀다 (기능 구현 때 정한다)
- 개요는 `ACTIVE` 장소 중 개요가 없거나 `modifiedTime`이 바뀐 것만 다시 받는다
- **TourAPI에서 사라진 장소**는 지우지 않고 비활성으로 바꾼다 (방문 기록·한걸음이 placeId로 참조한다)
- **실패해도 서버는 계속 돈다**: 호출 실패·키 없음이면 로그만 남기고 기존 DB 데이터를 유지한다
- 테스트·목 모드에서는 외부 호출 없이 동작해야 한다 (키가 비어 있으면 건너뛰므로 테스트는 고정 데이터를 직접 넣는다)
- 지역 필터는 법정동 코드(`lDongRegnCd=11`, `lDongSignguCd=410`)로 한다. 예전 `areaCode`는 쓰지 않는다

### 지하철역: T Data 지하철역 좌표정보
- 엔드포인트 `app.station.base-url`(https) + `apikey`(필수), `startRow`·`rowCnt`. 응답은 JSON 배열 `[{outStnNum, stnKrNm, lineNm, convX, convY}]`이고 `convX` = 경도, `convY` = 위도 (문자열)
- 전체 784건(2026-11 기준)이 `rowCnt=1000` 한 번에 온다
- **실행 시점**: 서버 시작 시 역 테이블이 비어 있으면 1회 + 장소 동기화 스케줄 때 함께 1회 (원천 갱신은 분기 1회). `app.station.sync-enabled=false`면 건너뛴다
- 서울 전체 역을 저장하고 **`outStnNum`(역 외부코드, 행마다 유일)**으로 upsert한다. 환승역은 호선마다 한 행이고, 같은 이름이라도 위치가 다를 수 있다 (예: 신촌 2호선 ≠ 신촌 경의중앙선)
- 가까운 역 계산은 저장된 전체 역에서 `GeoUtils`로 한다. "역 선택" 목록은 기준 역에서 `app.station.selectable-radius-meters`(기본 3km) 안의 역만 보여준다
- 장소와 같이 실패해도 기존 데이터를 유지하고, 키가 없거나 테스트·목 모드면 외부 호출 없이 동작한다

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
- **샘플 데이터**: 지도가 비어 보이지 않게 장소별 한걸음 3~5개, 국적이 섞인 방문 기록을 샘플로 넣는다 (사용자 데이터라 TourAPI와 별개). 샘플임을 구분할 수 있게 한다

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
| `app.tour.service-key` | TourAPI 국문 관광정보 (공공데이터포털 Decoding 키) | 동기화 건너뜀, DB 데이터 유지 |
| `app.station.api-key` | T Data 지하철역 좌표정보 인증키 | 동기화 건너뜀, DB 데이터 유지 |
| `app.station.sync-enabled` / `selectable-radius-meters` | 역 동기화 켜기 / 역 선택 목록 범위 (기본 `true` / `3000`) | - |
| `app.tour.sync-enabled` / `sync-cron` | 장소 동기화 켜기 / 주기 (기본 `true` / 매일 04:00) | - |
| `app.tour.l-dong-regn-cd` / `l-dong-signgu-cd` | 법정동 코드 서울 / 서대문구 (기본 `11` / `410`). 예전 `areaCode`는 쓰지 않는다 | - |
| `app.demo.default-station` | 위치·역이 없을 때 기준 역 (기본 `독립문`, T Data 역 한글명) | 기본값 |

키는 `application.yml`(gitignore) 또는 환경변수(`APP_WEATHER_KMA_SERVICE_KEY`, `APP_TOUR_SERVICE_KEY`, `APP_STATION_API_KEY`)로 넣는다. AI 키는 `spring.ai.anthropic.api-key`(`ANTHROPIC_API_KEY`), `spring.ai.openai.api-key`(`OPENAI_API_KEY`).

## 속도 규칙
- 계획 승인 루프 없이 바로 구현한다
- 변경 후 `./gradlew compileJava` 통과를 확인한다 (.java 수정 시 훅이 자동 실행)
- API 요청/응답 형식이 바뀌면 `docs/api.md`를 같은 변경에서 갱신한다
- 엔드포인트마다 목 모드 성공 테스트 1개 이상 (`LlmControllerTest` 패턴)
- 데모 전에는 `/demo-check`로 전 엔드포인트를 확인한다

## 절대 규칙
1. API 키·`.env`·`src/main/resources/application.yml`을 커밋하지 않는다. `application.yml`은 gitignore 대상이고 팀원끼리 직접 공유한다 (키 패턴은 pre-commit이 차단)
2. `application.yml`의 API 키 값은 읽지 않는다 (`spring.ai.anthropic.api-key`, `spring.ai.openai.api-key`, `app.weather.kma-service-key`, `app.tour.service-key`, `app.station.api-key` 등)
   - 파일 전체를 `cat`·Read로 열지 않는다. 설정을 확인하거나 고칠 땐 필요한 구간만 보고, 키가 있는 줄은 빼고 본다 (예: `grep -v -i 'key' application.yml`)
   - 키 값을 응답·로그·커밋 메시지·문서·테스트에 옮겨 적지 않는다. `.env`와 `ANTHROPIC_API_KEY`·`OPENAI_API_KEY` 같은 환경변수 값도 출력하지 않는다 (`env`, `printenv`, `echo $..._KEY` 금지)
   - 키가 필요한 동작은 목 모드(`LLM_MOCK=true`)로 확인한다. 실제 키 확인이 필요하면 사람에게 요청한다
3. force push 하지 않는다
4. 커밋은 사용자가 요청할 때만 한다
