# API 명세 (프론트용)

- Base URL: `http://localhost:8080` · Swagger UI: `/swagger-ui.html`
- CORS: 개발용으로 모든 origin 허용
- 목 모드(`LLM_MOCK=true`)에서는 실제 AI 호출 없이 `[MOCK]`으로 시작하는 고정 응답이 온다. 형식은 실제와 같다.

## 공통 응답 형식

모든 JSON 응답은 `ApiResult`로 감싼다.

```json
// 성공
{
  "resultType": "SUCCESS",
  "httpStatusCode": 200,
  "message": "요청에 성공하였습니다.",
  "data": { ... }
}

// 실패 — code로 오류를 구분한다 (data 없음)
{
  "resultType": "FAIL",
  "httpStatusCode": 504,
  "code": "LLM_TIMEOUT",
  "message": "AI 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요."
}
```

`message`는 사용자에게 그대로 보여 줘도 되는 문구다. 분기는 `code`로 한다.

## 인증 (로그인)

앱 내 아이디·비밀번호 로그인이다. 로그인 흐름·자동 로그인·오류 처리는 [`docs/frontend-auth.md`](frontend-auth.md)에 정리했다.

- 가입·로그인 응답의 `accessToken`(JWT)을 기기에 저장하고 **모든 요청에** `Authorization: Bearer {accessToken}` 헤더를 붙인다
- `userId`를 쓰는 API(도장·한걸음 작성·사진·엽서·나의 기록, `userId`를 보낸 추천·한걸음 목록)는 토큰의 사용자와 `userId`가 같아야 한다
  - 토큰 없음 401 `USER_LOGIN_REQUIRED` · 잘못됐거나 만료된 토큰 401 `USER_INVALID_TOKEN` · 다른 사람 `userId` 403 `USER_FORBIDDEN`
- `userId`가 필요 없는 API(역 목록·주변 장소·해설·장면·흔적 지도 등)는 토큰 없이도 된다
- `userId` = 로그인 아이디다

## 엔드포인트 요약

### 범용 샘플 API

| 메서드 | 경로 | 설명 | 응답 data |
|---|---|---|---|
| POST | `/api/chat` | 텍스트 채팅 | ChatResponse |
| GET | `/api/chat/stream` | 텍스트 채팅 (SSE 스트리밍) | SSE 이벤트 |
| POST | `/api/analyze` | 구조화 분석 | AnalysisResult |
| POST | `/api/image` | 이미지 질문 (multipart) | ChatResponse |
| GET | `/api/health` | 서버 상태 | `{status, mock}` |

### 서비스 API (작업 중)

아래는 기획 P0 목록이다. 엔드포인트를 구현할 때 요청/응답 섹션을 이 문서에 추가한다. 섹션이 없는 엔드포인트는 아직 형식이 정해지지 않은 것이다.

| 기능 | 메서드 | 경로 | 설명 |
|---|---|---|---|
| F1 | GET | `/api/auth/check-id` | 아이디 중복 확인 |
| F1 | POST | `/api/auth/signup` | 회원가입 (아이디·비밀번호·이름·국적·언어) → 토큰 |
| F1 | POST | `/api/auth/login` | 로그인 → 토큰 |
| F1 | GET | `/api/users/me` | 내 정보 (자동 로그인 확인) |
| F1 | PATCH | `/api/users/me` | 내 이름·국적·언어 수정 |
| F1 | GET | `/api/users/{userId}` | 사용자 조회 (본인만) |
| F2 | GET | `/api/now` | 시간·날씨·위치 추천 + 지난주 인기 (시뮬레이션 파라미터). 추천 이유는 내 나라의 비슷한 사례로 잇는다 |
| F2 | GET | `/api/stations` | 역 선택 목록 (위치 거부·데모용, 서대문 생활권 역) |
| F3 | POST | `/api/scenes` | 맞춤 장면 생성 (국적 바꿔 재생성 가능) |
| F4 | GET | `/api/places/nearby` | 주변 장소 후보 |
| F4 | POST | `/api/explanations` | 선택한 장소 해설 (사용자 언어로 번역) |
| F4·F6·F7 | POST | `/api/photos` | 사진 업로드 (multipart, 10MB 이하 jpeg·png·webp) |
| F4·F6·F7 | GET | `/api/photos/{photoId}` | 사진 파일 |
| F5 | POST | `/api/visits` | 장소 근처에서 방문 도장 (이어 갈 코스·다음 장소 포함) |
| F5 | GET | `/api/users/{userId}/visits` | 나의 기록: 도장, 안 간 코스 장소, 코스 진행도, 날짜별 기록 |
| F6 | GET | `/api/map/pins` | 장소별 방문 수·국적 분포 |
| F6 | GET | `/api/places/{placeId}/reviews` | 한걸음 목록 (보는 사람 언어로 번역) |
| F6 | POST | `/api/places/{placeId}/reviews` | 한걸음 작성 (부적절하면 422 `REVIEW_INAPPROPRIATE`) |
| F7 | POST | `/api/postcards` | 엽서 생성 시작 (비동기). 사진 1장 + 한 문장 + 템플릿 |
| F7 | GET | `/api/postcards/{postcardId}` | 생성 상태 (`PENDING`→`PROCESSING`→`COMPLETED`/`FAILED`), 완료면 이미지 URL |
| F7 | GET | `/api/postcards/{postcardId}/image` | 엽서 이미지(JPEG). `?download=true`면 파일로 내려받기 |

공통 코드값 — 국적 `nationality`: `VN, CN, JP, MN, UZ, US, OTHER` / 언어 `language`: `KO, EN, VI, ZH, JA, MN, UZ`. 대문자 상수 그대로 주고받는다.

`promptKey`는 서버 `prompts.yml`에 등록된 키다. 지금 등록된 키: `chat`, `analyze`, `image`, `scene`(F3 전용, `/api/scenes`가 쓴다).
`params`는 시스템 프롬프트의 `{변수}`를 채운다 (예: `chat`은 `target`, `analyze`는 `domain`).

---

### POST /api/chat

요청 (`Content-Type: application/json`)

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| promptKey | string | - | 기본 `chat`, 50자 이하 |
| message | string | O | 사용자 메시지, 10000자 이하 |
| params | object(string→string) | - | 프롬프트 변수, 20개 이하 |

```json
{
  "promptKey": "chat",
  "message": "해커톤에서 밤샘할 때 집중력 유지하는 팁 알려줘",
  "params": { "target": "해커톤 참가자" }
}
```

응답 200

```json
{
  "resultType": "SUCCESS",
  "httpStatusCode": 200,
  "message": "요청에 성공하였습니다.",
  "data": {
    "answer": "짧은 휴식을 자주 넣고, 물을 충분히 마시세요.",
    "model": "claude-haiku-5-5",
    "usage": { "inputTokens": 152, "outputTokens": 348 },
    "latencyMs": 1834
  }
}
```

오류: 400 `COMMON_INVALID_REQUEST`, 400 `LLM_PROMPT_NOT_FOUND`, 422 `COMMON_VALIDATION_FAILED`, 502 `LLM_CALL_FAILED`, 504 `LLM_TIMEOUT`

---

### GET /api/chat/stream

쿼리: `promptKey`(기본 `chat`), `message`(필수, 10000자 이하)

응답 `Content-Type: text/event-stream`. 이벤트 3종류:

| event | data | 설명 |
|---|---|---|
| `token` | `{"text": "..."}` | 받은 순서대로 이어 붙인다 |
| `done` | `{"model", "usage": {"inputTokens", "outputTokens"}, "latencyMs"}` | 정상 종료. 마지막 이벤트 |
| `error` | `{"code", "message"}` | 스트림 중 오류. 이 이벤트 뒤 스트림이 닫힌다 |

```
event:token
data:{"text":"안녕"}

event:token
data:{"text":"하세요"}

event:done
data:{"model":"claude-haiku-5-5","usage":{"inputTokens":21,"outputTokens":5},"latencyMs":812}
```

스트림이 **시작되기 전** 오류(검증 실패, 없는 promptKey)는 SSE가 아니라 일반 ApiResult JSON(400/422)으로 온다.
응답의 `Content-Type`이 `application/json`이면 오류로 처리한다.

```js
const es = new EventSource(`/api/chat/stream?message=${encodeURIComponent(text)}`);
es.addEventListener("token", (e) => append(JSON.parse(e.data).text));
es.addEventListener("done", () => es.close());
es.addEventListener("error", (e) => {
  if (e.data) showError(JSON.parse(e.data).message); // 서버가 보낸 error 이벤트
  es.close();                                       // 연결 오류(시작 전 400/422 포함)
});
```

`EventSource`는 시작 전 오류 응답의 본문을 읽을 수 없다. 오류 메시지가 필요하면 `fetch` + `ReadableStream`으로 읽거나, 입력 검증을 먼저 한다.

---

### POST /api/analyze

요청 (`Content-Type: application/json`)

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| promptKey | string | - | 기본 `analyze` |
| input | string | O | 분석할 텍스트, 20000자 이하 |
| params | object(string→string) | - | 프롬프트 변수 |

```json
{
  "input": "오늘 회의: 프론트는 로그인 화면 완료, 백엔드는 LLM 연동 중. 내일 오전까지 데모 시나리오 확정 필요.",
  "params": { "domain": "프로젝트 관리" }
}
```

응답 200 — `data`는 정해진 구조(AnalysisResult)

```json
{
  "resultType": "SUCCESS",
  "httpStatusCode": 200,
  "message": "요청에 성공하였습니다.",
  "data": {
    "summary": "프론트는 로그인 화면을 마쳤고 백엔드는 LLM 연동을 진행 중이다.",
    "keyPoints": ["로그인 화면 완료", "LLM 연동 진행 중"],
    "actionItems": ["내일 오전까지 데모 시나리오 확정"]
  }
}
```

오류: 400 `COMMON_INVALID_REQUEST`, 400 `LLM_PROMPT_NOT_FOUND`, 422 `COMMON_VALIDATION_FAILED`, 502 `LLM_CALL_FAILED`, 502 `LLM_INVALID_OUTPUT`, 504 `LLM_TIMEOUT`

---

### POST /api/image

요청 `Content-Type: multipart/form-data`

| 파트 | 타입 | 필수 | 설명 |
|---|---|---|---|
| file | file | O | 5MB 이하, `image/jpeg`·`image/png`·`image/webp` |
| question | string | O | 질문, 2000자 이하 |
| promptKey | string | - | 기본 `image` |

```bash
curl -F file=@photo.png -F question="이 사진에 뭐가 보여?" localhost:8080/api/image
```

응답 200 — `data` 형식은 `/api/chat`과 같다.

오류: 400 `COMMON_INVALID_REQUEST`(file 파트 없음), 400 `LLM_PROMPT_NOT_FOUND`, 422 `COMMON_VALIDATION_FAILED`(형식·크기), 502 `LLM_CALL_FAILED`, 504 `LLM_TIMEOUT`

---

### GET /api/auth/check-id

F1 아이디 중복 확인. 가입 화면에서 아이디를 입력할 때 부른다. 토큰 없이 부른다.

쿼리: `loginId` (필수, 영문 소문자·숫자·밑줄 `_` 4~20자)

```bash
curl 'localhost:8080/api/auth/check-id?loginId=linh_2026'
```

응답 200

```json
{
  "resultType": "SUCCESS",
  "httpStatusCode": 200,
  "message": "요청에 성공하였습니다.",
  "data": { "loginId": "linh_2026", "available": true }
}
```

- `available: false`면 이미 쓰는 아이디다
- 확인한 뒤 가입하기 전에 다른 사람이 먼저 가입할 수 있다. 그때 가입은 409 `USER_LOGIN_ID_TAKEN`이다

오류: 422 `COMMON_VALIDATION_FAILED`(형식이 틀림 — `message`에 규칙이 온다), 400 `COMMON_INVALID_REQUEST`(`loginId` 없음)

---

### POST /api/auth/signup

F1 회원가입. 가입하면 바로 로그인되어 토큰을 준다. 이메일 인증 같은 절차는 없다.

요청 (`Content-Type: application/json`)

| 필드 | 필수 | 설명 |
|---|---|---|
| loginId | O | 아이디. 영문 소문자·숫자·밑줄 `_` 4~20자. 가입 뒤 바꿀 수 없다. 이 값이 `userId`가 된다 |
| password | O | 비밀번호 8~64자 |
| name | O | 화면에 보이는 이름 1~20자 (앞뒤 공백은 지운다) |
| nationality | O | 국적 (`VN, CN, JP, MN, UZ, US, OTHER`) |
| language | O | 이야기·번역 언어 (`KO, EN, VI, ZH, JA, MN, UZ`) |

```json
{ "loginId": "linh_2026", "password": "seodaemun1908", "name": "린", "nationality": "VN", "language": "VI" }
```

응답 201

```json
{
  "resultType": "SUCCESS",
  "httpStatusCode": 201,
  "message": "성공적으로 생성하였습니다.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJsaW5oXzIwMjYi...",
    "expiresAt": "2026-12-07T06:00:00Z",
    "user": { "userId": "linh_2026", "name": "린", "nationality": "VN", "language": "VI" }
  }
}
```

- `accessToken`을 기기에 저장하고 이후 모든 요청에 `Authorization: Bearer {accessToken}`으로 보낸다
- `expiresAt`(UTC)은 30일 뒤다. 지나면 다시 로그인한다
- 비밀번호는 서버에 해시로만 저장한다

오류: 409 `USER_LOGIN_ID_TAKEN`(이미 있는 아이디), 422 `COMMON_VALIDATION_FAILED`(필드 누락, 아이디 형식, 비밀번호 길이, 이름 길이), 400 `COMMON_INVALID_REQUEST`(없는 국적·언어 코드, JSON 형식 오류)

---

### POST /api/auth/login

F1 로그인. 토큰 없이 부른다.

```json
{ "loginId": "linh_2026", "password": "seodaemun1908" }
```

응답 200 — `data` 형식은 `POST /api/auth/signup`과 같다 (`accessToken`, `expiresAt`, `user`).

오류: 401 `USER_INVALID_CREDENTIALS`(아이디가 없거나 비밀번호가 틀림 — 둘을 구분하지 않는다), 422 `COMMON_VALIDATION_FAILED`(빈 값)

---

### GET /api/users/me

F1 내 정보. **토큰 필요**. 앱을 켤 때 저장된 토큰으로 불러 자동 로그인에 쓴다.

```bash
curl localhost:8080/api/users/me -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...'
```

응답 200

```json
{
  "resultType": "SUCCESS",
  "httpStatusCode": 200,
  "message": "요청에 성공하였습니다.",
  "data": { "userId": "linh_2026", "name": "린", "nationality": "VN", "language": "VI" }
}
```

오류: 401 `USER_LOGIN_REQUIRED`(토큰 없음), 401 `USER_INVALID_TOKEN`(잘못됐거나 만료된 토큰 — 로그인 화면으로), 404 `USER_NOT_FOUND`(토큰의 사용자가 없어짐)

---

### PATCH /api/users/me

F1 내 정보 수정. **토큰 필요**. 이름·국적·언어 중 **보낸 것만** 바꾼다. 아이디는 바꿀 수 없다.

```json
{ "language": "EN" }
```

| 필드 | 설명 |
|---|---|
| name | 1~20자 (앞뒤 공백은 지운다). 빈 문자열은 422 |
| nationality | 국적 코드 |
| language | 언어 코드. 바꾸면 이후 추천 이유·장면·번역이 이 언어로 온다 |

응답 200 — 바뀐 뒤의 내 정보 (`GET /api/users/me`와 같은 형식)

오류: 401 `USER_LOGIN_REQUIRED`·`USER_INVALID_TOKEN`, 422 `COMMON_VALIDATION_FAILED`(빈 이름, 20자 초과), 400 `COMMON_INVALID_REQUEST`(없는 코드값)

---

### GET /api/users/{userId}

사용자 조회. **토큰 필요, 본인만**. 보통은 `GET /api/users/me`를 쓴다.

응답 200 — `GET /api/users/me`와 같은 형식

오류: 401 `USER_LOGIN_REQUIRED`·`USER_INVALID_TOKEN`, 403 `USER_FORBIDDEN`(다른 사람), 404 `USER_NOT_FOUND`

---

### GET /api/now

F2 지금 추천. 앱을 열자마자 질문 없이 부른다. 지금 시간·날씨·위치에 맞는 장소 1곳과 그 이유를 준다. 이유는 사용자 나라의 비슷한 사례로 잇는 한 문장이고, 자세한 연결은 `POST /api/scenes`가 맡는다.

쿼리 (모두 선택. 발표장이 실내라 위치·날씨·시간을 지정할 수 있다)

| 이름 | 설명 |
|---|---|
| userId | 사용자. 이 사용자의 언어·국적으로 이유를 쓴다. 없으면 국적 `OTHER`, 언어 `EN` |
| lat, lng | 현재 위치 (위도·경도). 둘을 함께 보낸다 |
| stationId | `GET /api/stations`에서 고른 역. `lat`·`lng`가 있으면 무시한다 |
| weather | 날씨 시뮬레이션 `CLEAR, RAINY, SNOWY`. 없으면 기상청 실제 날씨 |
| time | 시각 시뮬레이션 `HH:mm` (서울, 예: `15:00`). 없으면 지금 |

기준 위치 = `lat`·`lng` → 없으면 `stationId` → 둘 다 없으면 기본 역(독립문).

```bash
curl 'localhost:8080/api/now?userId=linh_2026&stationId=12&weather=RAINY&time=15:00'
```

응답 200

```json
{
  "resultType": "SUCCESS",
  "httpStatusCode": 200,
  "message": "요청에 성공하였습니다.",
  "data": {
    "place": {
      "placeId": 34,
      "name": "서대문형무소역사관",
      "imageUrl": "https://tong.visitkorea.or.kr/cms/resource/example/prison.jpg",
      "distanceMeters": 197,
      "walkMinutes": 3,
      "indoor": true
    },
    "reason": "Nơi này giống nhà tù Hỏa Lò ở Hà Nội.",
    "reasonKo": "베트남의 호아로 수용소와 비슷한 곳이에요.",
    "context": {
      "time": "15:00",
      "timeSlot": "AFTERNOON",
      "weather": "RAINY",
      "temperature": null,
      "weatherSource": "SIMULATED",
      "baseStation": "독립문"
    },
    "alternatives": [
      { "placeId": 4, "name": "독립문", "distanceMeters": 287 }
    ],
    "weeklyPopular": [
      { "placeId": 4, "name": "독립문", "visitorCount": 12 },
      { "placeId": 51, "name": "독립문영천시장", "visitorCount": 9 }
    ]
  }
}
```

- 추천 방법: 서버가 규칙으로 후보를 거른 뒤(노출 중·해설 있음 → 지금 시간대 → 비·눈이면 실내 → 가까운 순 6곳) AI가 그중 1곳을 고른다. 거리 제한은 없다
  - 밤(21–06시)처럼 맞는 시간대가 없으면 `context.timeSlot`이 `null`이고 시간대 조건 없이 고른다. 실내 후보가 하나도 없으면 실내 조건도 뺀다
- `reason`은 사용자 언어, `reasonKo`는 쉬운 한국어 한 문장이다
  - 내 나라 연결("베트남의 호아로 수용소와 비슷한 곳이에요")은 팀이 검토한 데이터(`data/place-connections.json`)에 있는 사례만 쓴다. F3 장면의 `connectionLine`과 같은 사례라 두 화면의 말이 맞는다. 데이터에 없는 장소·국적 조합이면 사례 이름 없이 쓴다
- `walkMinutes`는 서버가 계산한다 (직선거리 ÷ 분당 67m, 올림)
- `alternatives`는 추천에서 빠진 다른 후보 최대 3곳 (가까운 순)
- `weeklyPopular`는 지난주 인기 장소다. 오늘을 포함한 최근 7일(서울 날짜) 동안 방문 도장을 찍은 사람이 많은 순으로 최대 3곳. 위치·추천과 무관하게 모두에게 같다
  - `visitorCount`는 사람 수다 (한 사람이 여러 날 와도 1명). 노출하지 않는 장소는 빠지고, 방문 기록이 없으면 빈 배열이다
- `context.temperature`는 기상청 날씨일 때만 온다. `weather`를 지정했거나 기본값이면 `null`
- AI가 후보 밖 장소를 고르거나 응답에 실패하면 가장 가까운 후보와 기본 문구("지금 가까이에서 가 볼 만한 곳이에요.")를 준다. 그래서 AI 오류로 실패하지 않는다
- 추천 결과와 위치는 저장하지 않는다. 실제 AI 호출은 몇 초(대부분 2~5초, Haiku effort low) 걸린다
- 목 모드에서는 가장 가까운 후보와 `[MOCK EN] …` 형태의 이유가 온다

오류: 404 `USER_NOT_FOUND`(없는 userId), 404 `RECOMMEND_NOT_FOUND`(노출 중인 장소가 하나도 없음), 422 `STATION_INVALID_LOCATION`, 404 `STATION_NOT_FOUND`, 400 `COMMON_INVALID_REQUEST`(없는 날씨 값, `HH:mm`이 아닌 시각, 숫자가 아닌 좌표)

---

### GET /api/stations

F2 역 선택 목록. 위치 권한을 거부했거나 데모에서 기준 역을 고를 때 보여 준다. 고른 역의 `stationId`를 `GET /api/now`·`GET /api/places/nearby`에 보낸다.

요청 파라미터 없음.

```bash
curl 'localhost:8080/api/stations'
```

응답 200

```json
{
  "resultType": "SUCCESS",
  "httpStatusCode": 200,
  "message": "요청에 성공하였습니다.",
  "data": [
    { "stationId": 12, "name": "독립문", "line": "3호선", "isDefault": true },
    { "stationId": 13, "name": "무악재", "line": "3호선", "isDefault": false },
    { "stationId": 40, "name": "신촌", "line": "2호선", "isDefault": false },
    { "stationId": 41, "name": "신촌", "line": "경의중앙선", "isDefault": false }
  ]
}
```

- 서대문 생활권 역을 정해진 순서로 준다: 독립문 · 무악재 · 홍제 · 서대문 · 충정로(경기대입구) · 아현 · 이대 · 신촌 · 가좌. 거리로 거르지 않는다
- 같은 이름의 역은 호선마다 따로 온다. 위치가 다르므로 `stationId`로 구분한다 (예: 신촌 2호선 ≠ 신촌 경의중앙선)
- `isDefault: true`는 아무 역도 고르지 않았을 때 서버가 기준으로 쓰는 역이다 (독립문). 처음에 이 역을 선택된 상태로 보여 주면 된다
- 서버에 역 데이터가 아직 없으면 빈 배열이 온다. 이때도 `GET /api/now`는 기본 역 기준으로 동작한다

---

### GET /api/places/nearby

F4 주변 장소. 현장 해설을 볼 수 있는 장소를 가까운 순으로 준다. 고른 장소의 `placeId`를 `POST /api/explanations`에 보낸다.

쿼리 (모두 선택)

| 이름 | 설명 |
|---|---|
| lat, lng | 현재 위치 (위도·경도). 둘을 함께 보낸다 |
| stationId | `GET /api/stations`에서 고른 역. `lat`·`lng`가 있으면 무시한다 |
| limit | 개수. 기본 5, 최대 20 (범위를 벗어나면 1~20으로 맞춘다) |

기준 위치 = `lat`·`lng` → 없으면 `stationId` → 둘 다 없으면 기본 역(독립문).

```bash
curl 'localhost:8080/api/places/nearby?lat=37.5745&lng=126.9577&limit=3'
curl 'localhost:8080/api/places/nearby?stationId=12'
```

응답 200

```json
{
  "resultType": "SUCCESS",
  "httpStatusCode": 200,
  "message": "요청에 성공하였습니다.",
  "data": [
    { "placeId": 34, "name": "서대문형무소역사관", "imageUrl": "https://tong.visitkorea.or.kr/cms/resource/example/prison.jpg", "distanceMeters": 197 },
    { "placeId": 4, "name": "독립문", "imageUrl": "https://tong.visitkorea.or.kr/cms/resource/example/gate.jpg", "distanceMeters": 287 },
    { "placeId": 51, "name": "독립문영천시장", "imageUrl": null, "distanceMeters": 412 }
  ]
}
```

- 노출 중이고 해설 원문(팀이 쓴 해설 또는 TourAPI 개요)이 있는 장소만 온다. 거리 제한은 없다
- `distanceMeters`는 직선거리(미터)다. `imageUrl`은 없으면 `null`
- 위치(`lat`·`lng`)는 계산에만 쓰고 서버에 저장하거나 로그에 남기지 않는다

오류: 422 `STATION_INVALID_LOCATION`(`lat`·`lng` 중 하나만 옴, 범위 밖), 404 `STATION_NOT_FOUND`(없는 stationId), 400 `COMMON_INVALID_REQUEST`(숫자가 아닌 값)

---

### POST /api/explanations

F4 현장 해설. 장소에 저장된 한국어 해설을 요청 언어로 번역해 준다. AI는 번역만 하고 내용을 더하거나 빼지 않는다.

요청

```json
{ "placeId": 34, "language": "VI" }
```

| 필드 | 필수 | 설명 |
|---|---|---|
| placeId | O | `GET /api/places/nearby`의 `placeId` |
| language | O | 읽을 언어 (`KO, EN, VI, ZH, JA, MN, UZ`). 보통 사용자 언어 |

```bash
curl -X POST localhost:8080/api/explanations -H 'Content-Type: application/json' \
  -d '{"placeId": 34, "language": "VI"}'
```

응답 200

```json
{
  "resultType": "SUCCESS",
  "httpStatusCode": 200,
  "message": "요청에 성공하였습니다.",
  "data": {
    "placeId": 34,
    "name": "서대문형무소역사관",
    "language": "VI",
    "text": "Nhà tù Seodaemun do Nhật Bản xây dựng vào năm 1908…",
    "original": "1908년 일제가 세운 감옥이다…",
    "cached": false
  }
}
```

- `text`는 요청 언어로 옮긴 해설, `original`은 한국어 원문이다. 화면에서 원문 보기를 함께 둘 수 있다
- 원문은 팀이 쓴 해설이 있으면 그것, 없으면 한국관광공사 TourAPI 개요다
- `language`가 `KO`면 AI를 부르지 않고 원문을 그대로 준다 (`text` = `original`, `cached: false`)
- 번역은 장소 × 언어별로 서버에 저장해 두므로 두 번째부터는 바로 온다(`cached: true`). 처음 번역할 때만 AI 호출로 몇 초 걸린다. 원문이 바뀌면 다시 번역한다
- 목 모드에서는 `text`가 `[번역] 원문` 형태로 온다

오류: 404 `PLACE_NOT_FOUND`(없거나 노출하지 않는 장소), 422 `PLACE_EXPLANATION_MISSING`(해설 원문이 없는 장소 — 주변 장소 목록에는 나오지 않는다), 422 `COMMON_VALIDATION_FAILED`(placeId·language 누락), 400 `COMMON_INVALID_REQUEST`(없는 언어 코드), 502·504 `LLM_*`(번역 실패)

---

### POST /api/photos

사진 업로드 (F4·F6·F7 공통). 받은 `photoId`를 한걸음·엽서 요청에 넣는다.

요청 `Content-Type: multipart/form-data`

| 파트 | 타입 | 필수 | 설명 |
|---|---|---|---|
| file | file | O | 10MB 이하, `image/jpeg`·`image/png`·`image/webp` (HEIC 불가 — 아이폰 사진은 프론트에서 jpeg로 바꿔 보낸다) |
| userId | string | O | 로그인 아이디 (토큰의 사용자와 같아야 한다) |
| placeId | number | - | 찍은 장소 ID |

```bash
curl -F file=@photo.jpg -F userId=linh_2026 -F placeId=7 localhost:8080/api/photos
```

응답 201

```json
{
  "resultType": "SUCCESS",
  "httpStatusCode": 201,
  "message": "성공적으로 생성하였습니다.",
  "data": {
    "photoId": "9b1c2d3e-4f5a-6b7c-8d9e-0f1a2b3c4d5e",
    "url": "/api/photos/9b1c2d3e-4f5a-6b7c-8d9e-0f1a2b3c4d5e",
    "placeId": 7
  }
}
```

- `url`은 서버 기준 경로다. 서버 주소를 앞에 붙여 `<img src>`에 쓴다
- 올린 시각과 작성자는 내보내지 않는다

오류: 422 `COMMON_VALIDATION_FAILED`(형식·크기, userId 누락), 400 `COMMON_INVALID_REQUEST`(file 파트 없음), 404 `USER_NOT_FOUND`, 404 `PLACE_NOT_FOUND`

---

### GET /api/photos/{photoId}

사진 파일. JSON이 아니라 이미지 바이트를 그대로 준다 (`Content-Type`은 올린 형식). 한 번 올린 사진은 바뀌지 않아 7일 캐시한다.

오류(JSON): 404 `PHOTO_NOT_FOUND`(없는 photoId), 404 `STORAGE_NOT_FOUND`(기록은 있는데 파일이 없음 — 서버 저장소가 지워진 경우)

---

### POST /api/scenes

F3 맞춤 장면. 장소 이야기를 요청 언어와 쉬운 한국어로, 관점 국적의 역사와 잇는 한 줄과 함께 돌려준다.
**같은 장소에 `nationality`만 바꿔 다시 부르면 다른 이야기가 온다** (발표의 "같은 장소, 다른 이야기").

요청 (`Content-Type: application/json`)

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| placeId | number | O | 장소 ID (노출 중인 장소만) |
| nationality | string | O | 관점 국적. 보통 사용자 국적(`GET /api/users/{userId}`), 국적 전환 시연에서는 다른 값 |
| language | string | O | 이야기 언어. `storyNative`·`connectionLine`이 이 언어로 온다 |

```json
{ "placeId": 1, "nationality": "VN", "language": "VI" }
```

응답 200

```json
{
  "resultType": "SUCCESS",
  "httpStatusCode": 200,
  "message": "요청에 성공하였습니다.",
  "data": {
    "placeId": 1,
    "placeName": "서대문형무소역사관",
    "nationality": "VN",
    "language": "VI",
    "storyNative": "Nơi đây từng là nhà tù do Nhật Bản xây dựng vào năm 1908…",
    "storyEasyKorean": "이곳은 1908년에 일제가 세운 감옥이 있던 곳이에요…",
    "connectionLine": "Giống như nhà tù Hỏa Lò ở Hà Nội, nơi đây cũng là nơi giam giữ những người mơ về độc lập.",
    "cached": false
  }
}
```

- 장소 × 국적 × 언어별로 서버에 저장한다. 같은 조합의 두 번째 요청부터는 AI를 부르지 않고 바로 온다(`cached: true`). 장소 설명·내 나라 연결 데이터·프롬프트가 바뀌면 다시 만든다
- `connectionLine`은 팀이 검토한 내 나라 연결 데이터(`data/place-connections.json`)의 사례를 풀어 쓴다 (F2 추천 이유와 같은 사례). 데이터에 없는 조합이면 특정 외국 사례 이름 없이 공통된 경험으로 잇는다
- 처음 만들 때는 AI 호출로 몇 초 걸린다. 시연할 조합은 미리 한 번씩 불러 두면 발표 때 바로 나온다
- 목 모드에서는 `[MOCK]`으로 시작하는 고정 이야기가 온다 (장소 이름·국적은 반영된다)

오류: 404 `PLACE_NOT_FOUND`(없거나 노출하지 않는 장소), 422 `SCENE_SOURCE_MISSING`(이야기를 만들 장소 설명이 없음), 422 `COMMON_VALIDATION_FAILED`(필드 누락), 400 `COMMON_INVALID_REQUEST`(없는 코드값), 502 `LLM_CALL_FAILED`, 502 `LLM_INVALID_OUTPUT`, 504 `LLM_TIMEOUT`

---

### GET /api/places/{placeId}/reviews

F6 한걸음 목록. 장소의 한걸음을 최신순으로 주고, 원문 언어가 보는 사람 언어와 다르면 번역을 함께 준다.

쿼리 (모두 선택)

| 이름 | 설명 |
|---|---|
| language | 보는 사람 언어 (`KO, EN, VI, ZH, JA, MN, UZ`). userId보다 우선한다 |
| userId | 보는 사람. `language`가 없으면 이 사용자의 언어로 번역한다 |
| limit | 개수. 기본 20, 최대 50 |

보는 사람 언어 = `language` → 없으면 `userId`의 언어 → 둘 다 없으면 `KO`.

```bash
curl 'localhost:8080/api/places/7/reviews?userId=linh_2026'
```

응답 200

```json
{
  "resultType": "SUCCESS",
  "httpStatusCode": 200,
  "message": "요청에 성공하였습니다.",
  "data": {
    "placeId": 7,
    "viewerLanguage": "VI",
    "reviews": [
      {
        "reviewId": 12,
        "content": "아침에 오면 한적해서 사진 찍기 좋아요",
        "language": "KO",
        "translatedContent": "Buổi sáng vắng người nên chụp ảnh rất đẹp",
        "nationality": "JP",
        "writtenDate": "2026-11-07",
        "photoUrl": "/api/photos/9b1c2d3e-4f5a-6b7c-8d9e-0f1a2b3c4d5e",
        "sample": false
      },
      {
        "reviewId": 9,
        "content": "Đẹp lắm",
        "language": "VI",
        "translatedContent": null,
        "nationality": "VN",
        "writtenDate": "2026-11-06",
        "photoUrl": null,
        "sample": true
      }
    ]
  }
}
```

- `translatedContent`가 `null`이면 원문(`content`)을 보여 준다. 원문 언어가 보는 사람 언어와 같거나, 번역에 실패한 경우다 (실패해도 목록은 200으로 온다)
- 번역은 서버에 저장해 두므로 같은 한걸음 × 언어는 두 번째부터 바로 온다. 처음 번역할 때만 AI 호출로 1~2초 걸린다
- 작성자 ID와 작성 시각은 내보내지 않는다. 날짜(`writtenDate`)와 국적까지만
- `sample`이 `true`면 서버가 시연용으로 미리 넣은 샘플 한걸음이다 (실제 사용자가 쓴 글이 아니다). 샘플은 서버를 켤 때 장소별로 들어가고, `writtenDate`는 며칠 전 날짜로 보인다. 화면에 따로 표시할지는 프론트가 정한다
- 목 모드에서는 번역이 `[MOCK VI] 원문` 형태로 온다
- `photoUrl`은 첨부 사진 주소(서버 기준 경로)다. 사진이 없으면 `null`

오류: 404 `PLACE_NOT_FOUND`, 404 `USER_NOT_FOUND`(없는 userId), 400 `COMMON_INVALID_REQUEST`(없는 언어 코드, 숫자가 아닌 limit)

---

### POST /api/places/{placeId}/reviews

F6 한걸음 작성. 장소에 한 줄 후기를 남긴다. 저장 전에 두 번 거르고, 걸리면 저장하지 않는다.

1. 링크·이메일·전화번호(숫자 9자리 이상)가 있으면 바로 거절한다 (AI 없이)
2. AI가 욕설·혐오(국가·민족 비하 포함)·광고·스팸을 검사하고, `language`가 없으면 작성 언어를 정한다

요청 (`Content-Type: application/json`)

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| userId | string | O | 로그인 아이디 (토큰의 사용자와 같아야 한다) |
| content | string | O | 한 줄 후기, 200자 이하. 앞뒤 공백은 지운다 |
| language | string | - | 실제로 쓴 언어 (`KO, EN, VI, ZH, JA, MN, UZ`). 비우면 서버가 글을 보고 정한다. **사용자 설정 언어를 그대로 넣지 않는다** |
| photoId | string | - | 첨부 사진. `POST /api/photos`로 먼저 올리고 받은 `photoId`. **내가 올린 사진만** 붙일 수 있다 |

```json
{ "userId": "linh_2026", "content": "아침에 오면 한적해서 사진 찍기 좋아요", "language": "KO",
  "photoId": "9b1c2d3e-4f5a-6b7c-8d9e-0f1a2b3c4d5e" }
```

응답 201

```json
{
  "resultType": "SUCCESS",
  "httpStatusCode": 201,
  "message": "성공적으로 생성하였습니다.",
  "data": {
    "reviewId": 12,
    "placeId": 7,
    "content": "아침에 오면 한적해서 사진 찍기 좋아요",
    "language": "KO",
    "nationality": "VN",
    "writtenDate": "2026-11-07",
    "photoUrl": "/api/photos/9b1c2d3e-4f5a-6b7c-8d9e-0f1a2b3c4d5e"
  }
}
```

- 작성자 ID와 작성 시각은 내보내지 않는다. 날짜(`writtenDate`)와 국적까지만
- 사진은 AI 검사 전에 확인한다. 사진 없이 쓰면 `photoUrl`은 `null`
- 목 모드에서는 AI 검사를 통과로 보고 링크·이메일·전화번호 규칙만 동작한다. `language`를 비우면 `KO`로 저장된다

오류: 422 `REVIEW_INAPPROPRIATE`(부적절한 내용 — 다시 쓰게 한다), 404 `USER_NOT_FOUND`, 404 `PLACE_NOT_FOUND`, 403 `PHOTO_FORBIDDEN`(남의 사진), 404 `PHOTO_NOT_FOUND`, 422 `COMMON_VALIDATION_FAILED`(빈 글, 200자 초과), 400 `COMMON_INVALID_REQUEST`(없는 언어 코드), 502 `LLM_CALL_FAILED`, 502 `LLM_INVALID_OUTPUT`, 504 `LLM_TIMEOUT`

---

### POST /api/postcards

F7 회고 엽서 생성 시작. 사진 1장 + 한 문장을 템플릿에 넣은 JPEG 한 장을 만든다. **비동기**라 바로 `PENDING`으로 돌려주고, 프론트는 `GET /api/postcards/{postcardId}`로 1~2초마다 상태를 확인한다.

- 사진은 원본 그대로 쓴다 (휴대폰 사진의 회전 정보는 반영한다). 사진 칸 비율에 맞춰 가운데를 자른다
- 제목(사용자 언어로 "날짜, 장소")은 AI가 쓰고, 제목과 한 문장은 서버가 템플릿 글꼴로 쓴다. 목 모드·AI 실패면 기본 제목(언어별 날짜 · 한국어 장소)
- 보통 2~3초 안에 끝난다
- 제목의 장소는 사진을 올릴 때 준 `placeId`. 없으면 "서대문"

요청 (`Content-Type: application/json`)

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| userId | string | O | 로그인 아이디 (토큰의 사용자와 같아야 한다) |
| photoId | string | O | `POST /api/photos`로 올린 **내** 사진 |
| comment | string | O | 엽서에 쓸 한 문장, 100자 이하 |
| template | string | - | 템플릿. 지금은 `BASIC` 하나 (비우면 `BASIC`) |

```json
{ "userId": "linh_2026", "photoId": "9b1c2d3e-4f5a-6b7c-8d9e-0f1a2b3c4d5e",
  "comment": "독립문 앞에서 처음으로 한국의 가을을 느꼈다" }
```

응답 201

```json
{
  "resultType": "SUCCESS",
  "httpStatusCode": 201,
  "message": "성공적으로 생성하였습니다.",
  "data": {
    "postcardId": "c7d8e9f0-1a2b-3c4d-5e6f-7a8b9c0d1e2f",
    "status": "PENDING",
    "template": "BASIC",
    "imageUrl": null,
    "downloadUrl": null,
    "errorCode": null
  }
}
```

오류: 404 `USER_NOT_FOUND`, 403 `PHOTO_FORBIDDEN`(남의 사진), 404 `PHOTO_NOT_FOUND`, 422 `COMMON_VALIDATION_FAILED`(빈 한 문장, 100자 초과, photoId 누락), 400 `COMMON_INVALID_REQUEST`(없는 템플릿)

---

### GET /api/postcards/{postcardId}

엽서 생성 상태. `data` 형식은 `POST /api/postcards`와 같다.

| status | 뜻 | 프론트 |
|---|---|---|
| `PENDING` | 대기 중 | 계속 확인 |
| `PROCESSING` | 만드는 중 | 계속 확인 |
| `COMPLETED` | 완성. `imageUrl`(보기)·`downloadUrl`(내려받기)이 온다 | 이미지 표시 |
| `FAILED` | 실패. `errorCode`에 사유 | 다시 만들기 안내 |

`errorCode`: `RENDER_FAILED`(사진을 읽지 못함 — 깨진 파일 등), `PHOTO_FILE_MISSING`(사진 파일이 없음), `INTERRUPTED`(만드는 중 서버가 다시 켜짐)

```json
{
  "resultType": "SUCCESS",
  "httpStatusCode": 200,
  "message": "요청에 성공하였습니다.",
  "data": {
    "postcardId": "c7d8e9f0-1a2b-3c4d-5e6f-7a8b9c0d1e2f",
    "status": "COMPLETED",
    "template": "BASIC",
    "imageUrl": "/api/postcards/c7d8e9f0-1a2b-3c4d-5e6f-7a8b9c0d1e2f/image",
    "downloadUrl": "/api/postcards/c7d8e9f0-1a2b-3c4d-5e6f-7a8b9c0d1e2f/image?download=true",
    "errorCode": null
  }
}
```

오류: 404 `POSTCARD_NOT_FOUND`

---

### GET /api/postcards/{postcardId}/image

완성된 엽서 이미지. JSON이 아니라 JPEG 바이트를 그대로 준다 (1200×1800, 템플릿에 따라 다름).

| 쿼리 | 설명 |
|---|---|
| download | `true`면 `Content-Disposition: attachment` — 브라우저가 파일(`seodaemun-postcard.jpg`)로 저장한다. 없으면 `inline`(화면에 표시) |

- 내려받기 버튼은 `<a href="{서버}/api/postcards/{id}/image?download=true">`면 된다. 아이폰에서 사진 앱에 넣으려면 이미지를 길게 눌러 저장하거나 `navigator.share`로 공유한다

오류(JSON): 409 `POSTCARD_NOT_READY`(아직 만드는 중이거나 실패), 404 `POSTCARD_NOT_FOUND`

---

### POST /api/visits

F5 방문 도장. 장소 근처(기본 200m)에 있으면 도장을 찍고, 이어 가기 좋은 코스와 다음 장소를 함께 돌려준다. 좌표는 거리 계산에만 쓰고 저장하지 않는다.

위치 권한을 거부했으면 이 API를 부르지 말고 안내를 보여 준다 (좌표가 없으면 422).

요청 (`Content-Type: application/json`)

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| userId | string | O | 로그인 아이디 (토큰의 사용자와 같아야 한다) |
| placeId | number | O | 도장을 찍을 장소 ID |
| lat | number | O | 현재 위도 (-90~90) |
| lng | number | O | 현재 경도 (-180~180) |
| simulatedWeather | string | - | 시연용 날씨 (`CLEAR, RAINY, SNOWY`). 비우면 실제 날씨 |
| simulatedTime | string | - | 시연용 시각 (`15:20`). 비우면 현재 시각 |

시연에서는 도장을 찍을 **장소의 좌표**를 `lat`, `lng`로 보낸다. 역 좌표를 보내면 먼 장소에서 `VISIT_TOO_FAR`가 된다.

```json
{ "userId": "linh_2026", "placeId": 34, "lat": 37.5743, "lng": 126.9555,
  "simulatedWeather": "RAINY", "simulatedTime": "15:20" }
```

응답 201 (새 도장) / 200 (오늘 이미 찍은 도장, `alreadyStamped: true`. 거리는 보지 않는다)

```json
{
  "resultType": "SUCCESS",
  "httpStatusCode": 201,
  "message": "성공적으로 생성하였습니다.",
  "data": {
    "visitId": 1,
    "placeId": 34,
    "placeName": "서대문형무소역사관",
    "visitedDate": "2026-11-08",
    "alreadyStamped": false,
    "firstVisit": true,
    "recommendedCourse": {
      "courseId": "independence",
      "name": "Hành trình Độc lập",
      "visitedCount": 1,
      "totalCount": 3,
      "completed": false,
      "stops": [
        { "placeId": 34, "name": "서대문형무소역사관", "visited": true },
        { "placeId": 4, "name": "독립문", "visited": false },
        { "placeId": 53, "name": "독립문영천시장", "visited": false }
      ],
      "next": { "placeId": 4, "name": "독립문", "walkingMinutes": 7 }
    }
  }
}
```

- `firstVisit`: 이 장소에 처음 왔으면 `true`. 처음이면 "도장 획득!", 아니면 "다시 왔어요"처럼 구분할 수 있다
- `recommendedCourse`: 이 장소가 든 코스 중 하나. 이번 도장으로 완주한 코스 → 미완주 중 많이 진행한 코스 → 남은 거리가 짧은 코스 순서로 고른다. 코스에 없는 장소이거나 이어 갈 코스가 없으면 `null`
- `name`: 사용자 언어로 된 코스 이름. 없으면 영어, 그다음 한국어. 장소 이름은 한국어다
- 진행도는 날짜와 순서에 상관없이 누적된다. 완주하면 `completed: true`, `next: null`
- `next`: 남은 장소 중 지금 시간대에 맞고 비·눈이면 실내인 곳에서 가장 가까운 곳. 그런 곳이 없으면 남은 곳 중 가장 가까운 곳
- `walkingMinutes`: 직선거리로 계산한 추정치라 실제보다 짧을 수 있다
- 방문 시각은 내보내지 않는다. 날짜(`visitedDate`)까지만

오류: 422 `VISIT_TOO_FAR`(장소에서 멀다), 404 `USER_NOT_FOUND`, 404 `PLACE_NOT_FOUND`, 422 `COMMON_VALIDATION_FAILED`(좌표 없음, 범위 밖), 400 `COMMON_INVALID_REQUEST`(없는 날씨 코드, 시각 형식 오류)

---

### GET /api/users/{userId}/visits

F5 나의 기록. 화면에 필요한 도장, 안 간 코스 장소, 코스 진행도, 날짜별 기록을 한 번에 돌려준다.

응답 200

```json
{
  "resultType": "SUCCESS",
  "httpStatusCode": 200,
  "message": "요청에 성공하였습니다.",
  "data": {
    "stampCount": 3,
    "completedCourseCount": 1,
    "stamps": [
      { "placeId": 53, "name": "독립문영천시장", "firstVisitedDate": "2026-11-08" }
    ],
    "undiscovered": [
      { "placeId": 21, "name": "서대문자연사박물관" }
    ],
    "courses": [
      {
        "courseId": "independence",
        "name": "독립 코스",
        "visitedCount": 3,
        "totalCount": 3,
        "completed": true,
        "stops": [
          { "placeId": 34, "name": "서대문형무소역사관", "visited": true }
        ]
      }
    ],
    "days": [
      { "date": "2026-11-08", "places": [{ "placeId": 34, "name": "서대문형무소역사관" }] }
    ]
  }
}
```

- `stampCount`, `stamps`: 도장은 장소당 하나다. 처음 방문한 날이 최근인 순서이고, 같은 날이면 나중에 찍은 도장이 먼저다
- `undiscovered`: 코스 장소 중 아직 안 간 곳 전부. 진행 중인 코스의 남은 장소가 먼저 오므로, 칸이 적으면 앞에서부터 쓴다
- `courses`: 코스 전체. 완주 → 많이 진행한 순이고 같으면 파일 순서다. **시작 전 코스(`visitedCount: 0`)도 들어 있으므로**, 완주 표시는 `completed`로 거른다
- `days`: 최신 날짜 순. 같은 날은 도장을 찍은 순서다
- 사라진 장소(비활성)의 도장과 기록은 남는다. 이미 다녀온 장소가 비활성이 되어도 진행도와 완주는 유지하고, **아직 안 간 장소가 비활성이면 그 코스를 뺀다**
- 코스 이름은 사용자 언어 → 영어 → 한국어 순서로 고른다. 장소 이름은 한국어다
- 방문 시각과 위치는 내보내지 않는다

오류: 404 `USER_NOT_FOUND`

---

### GET /api/map/pins

F6 흔적 지도 — "먼저 온 유학생의 발자취". 노출 중인 장소마다 다녀간 사람 수, 국적 분포, 한걸음 수를 준다. 핀을 누르면 `GET /api/places/{placeId}/reviews`로 한걸음을 보여 준다.

요청 파라미터 없음.

```bash
curl 'localhost:8080/api/map/pins'
```

응답 200

```json
{
  "resultType": "SUCCESS",
  "httpStatusCode": 200,
  "message": "요청에 성공하였습니다.",
  "data": [
    {
      "placeId": 4,
      "name": "독립문",
      "latitude": 37.5723,
      "longitude": 126.9598,
      "imageUrl": "https://tong.visitkorea.or.kr/cms/resource/example/gate.jpg",
      "visitorCount": 7,
      "reviewCount": 4,
      "nationalities": [
        { "nationality": "VN", "count": 3 },
        { "nationality": "CN", "count": 2 },
        { "nationality": "JP", "count": 1 },
        { "nationality": "MN", "count": 1 }
      ]
    },
    {
      "placeId": 51,
      "name": "독립문영천시장",
      "latitude": 37.5719,
      "longitude": 126.9585,
      "imageUrl": null,
      "visitorCount": 0,
      "reviewCount": 0,
      "nationalities": []
    }
  ]
}
```

- 노출 중인 장소가 모두 온다 (방문이 없는 장소도 `visitorCount: 0`으로 온다). 순서는 방문자 많은 순 → 한걸음 많은 순
- `visitorCount`는 방문 도장을 찍은 사람 수다. 한 사람이 여러 날 와도 1명이다. 기간 제한 없이 지금까지 전부 센다
- `nationalities`는 다녀간 사람의 국적별 인원이다. 많은 순, 같으면 국적 코드 순서(`VN, CN, JP, MN, UZ, US, OTHER`). 사용자 정보를 찾을 수 없는 방문자는 국적 분포에서만 빠지므로, 합이 `visitorCount`보다 작을 수 있다 (보통은 같다)
- `latitude`·`longitude`는 **장소의** 좌표다. 개인의 위치·방문 날짜·시각과 사용자 ID는 내보내지 않는다
- 시연용 샘플 사용자의 방문·한걸음도 센다

---

### GET /api/health

```json
{
  "resultType": "SUCCESS",
  "httpStatusCode": 200,
  "message": "요청에 성공하였습니다.",
  "data": { "status": "ok", "mock": true }
}
```

---

## 에러 코드

| HTTP | code | 언제 |
|---|---|---|
| 400 | `COMMON_INVALID_REQUEST` | JSON 형식 오류, 필수 파라미터·파트 누락, 타입 불일치 |
| 400 | `LLM_PROMPT_NOT_FOUND` | 등록되지 않은 promptKey |
| 404 | `COMMON_NOT_FOUND` | 없는 경로 |
| 401 | `USER_LOGIN_REQUIRED` | 토큰이 필요한 요청에 `Authorization` 헤더가 없음 |
| 401 | `USER_INVALID_TOKEN` | 토큰이 잘못됐거나 만료됨 → 다시 로그인 |
| 401 | `USER_INVALID_CREDENTIALS` | 로그인 아이디가 없거나 비밀번호가 틀림 |
| 403 | `USER_FORBIDDEN` | 다른 사람의 userId로 요청함 |
| 404 | `USER_NOT_FOUND` | 없는 userId |
| 409 | `USER_LOGIN_ID_TAKEN` | 이미 사용 중인 아이디로 가입함 |
| 404 | `PLACE_NOT_FOUND` | 없거나 노출하지 않는 장소 |
| 404 | `STATION_NOT_FOUND` | 없는 stationId |
| 404 | `RECOMMEND_NOT_FOUND` | 지금 추천할 장소가 없음 (노출 중이고 해설이 있는 장소가 하나도 없다) |
| 403 | `PHOTO_FORBIDDEN` | 다른 사용자의 사진을 한걸음·엽서에 쓰려 함 |
| 404 | `PHOTO_NOT_FOUND` | 없는 photoId |
| 404 | `POSTCARD_NOT_FOUND` | 없는 postcardId |
| 409 | `POSTCARD_NOT_READY` | 완성 전(또는 실패한) 엽서의 이미지를 요청함 |
| 404 | `STORAGE_NOT_FOUND` | 파일 기록은 있는데 저장소에 파일이 없음 |
| 405 | `COMMON_METHOD_NOT_ALLOWED` | 지원하지 않는 HTTP 메서드 |
| 415 | `COMMON_UNSUPPORTED_MEDIA_TYPE` | 지원하지 않는 Content-Type |
| 422 | `COMMON_VALIDATION_FAILED` | 값 검증 실패 (빈 메시지, 길이 초과, 이미지 형식·크기). message에 필드별 사유 |
| 422 | `SCENE_SOURCE_MISSING` | 장소에 이야기를 만들 설명(해설·개요)이 없음 |
| 422 | `PLACE_EXPLANATION_MISSING` | 장소에 현장 해설 원문(해설·개요)이 없음 |
| 422 | `STATION_INVALID_LOCATION` | `lat`·`lng` 중 하나만 오거나 범위(위도 ±90, 경도 ±180)를 벗어남 |
| 422 | `REVIEW_INAPPROPRIATE` | 한걸음에 욕설·혐오·광고·연락처가 있음. 저장되지 않는다 |
| 422 | `VISIT_TOO_FAR` | 장소에서 멀어 도장을 받을 수 없음 |
| 500 | `COMMON_INTERNAL_ERROR` | 서버 내부 오류 |
| 500 | `STORAGE_WRITE_FAILED` | 파일을 저장하지 못함 |
| 502 | `LLM_CALL_FAILED` | AI 호출 실패 (인증·요청 한도·서버 오류 등) |
| 502 | `LLM_INVALID_OUTPUT` | AI 응답을 정해진 구조로 해석하지 못함 (재시도 후에도) |
| 504 | `LLM_TIMEOUT` | AI 응답 시간 초과 (연결 5초, 응답 대기 60초) |
