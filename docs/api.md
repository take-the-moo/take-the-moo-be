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
| F1 | POST | `/api/users` | 국적·언어로 익명 사용자 생성 → `userId` (기기에 저장) |
| F1 | GET | `/api/users/{userId}` | 사용자 조회 |
| F2 | GET | `/api/now` | 시간·날씨·위치 추천 + 지난주 인기 (시뮬레이션 파라미터). 추천 이유는 내 나라의 비슷한 사례로 잇는다 |
| F2 | GET | `/api/stations` | 역 선택 목록 (위치 거부·데모용, 서대문 생활권 역) |
| F3 | POST | `/api/scenes` | 맞춤 장면 생성 (국적 바꿔 재생성 가능) |
| F4 | GET | `/api/places/nearby` | 주변 장소 후보 |
| F4 | POST | `/api/explanations` | 선택한 장소 해설 (사용자 언어로 번역) |
| F4·F6·F7 | POST | `/api/photos` | 사진 업로드 (multipart, 10MB 이하 jpeg·png·webp) |
| F4·F6·F7 | GET | `/api/photos/{photoId}` | 사진 파일 |
| F5 | POST | `/api/visits` | 장소 근처에서 방문 도장 (이어 갈 코스·다음 장소 포함) |
| F5 | GET | `/api/users/{userId}/visits` | 날짜별 방문 기록, 코스 진행도 |
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

### POST /api/users

F1 처음 설정. 국적·언어로 익명 사용자를 만든다. 응답의 `userId`를 기기에 저장해 두고 이후 요청에 쓴다.

요청 (`Content-Type: application/json`)

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| nationality | string | O | 국적 코드 (`VN, CN, JP, MN, UZ, US, OTHER`) |
| language | string | O | 이야기·번역 언어 코드 (`KO, EN, VI, ZH, JA, MN, UZ`) |

```json
{ "nationality": "VN", "language": "VI" }
```

응답 201

```json
{
  "resultType": "SUCCESS",
  "httpStatusCode": 201,
  "message": "성공적으로 생성하였습니다.",
  "data": {
    "userId": "3f2b8c1e-7a4d-4e9b-9c2a-1d5e6f7a8b9c",
    "nationality": "VN",
    "language": "VI"
  }
}
```

오류: 400 `COMMON_INVALID_REQUEST`(없는 코드값, JSON 형식 오류), 422 `COMMON_VALIDATION_FAILED`(필드 누락)

---

### GET /api/users/{userId}

저장해 둔 `userId`로 사용자를 조회한다. 앱을 다시 열 때 기기에 저장된 `userId`가 아직 유효한지 확인하는 데 쓴다.

응답 200 — `data` 형식은 `POST /api/users`와 같다.

오류: 404 `USER_NOT_FOUND`(없는 userId — 처음 설정으로 돌아간다)

---

### POST /api/photos

사진 업로드 (F4·F6·F7 공통). 받은 `photoId`를 한걸음·엽서 요청에 넣는다.

요청 `Content-Type: multipart/form-data`

| 파트 | 타입 | 필수 | 설명 |
|---|---|---|---|
| file | file | O | 10MB 이하, `image/jpeg`·`image/png`·`image/webp` (HEIC 불가 — 아이폰 사진은 프론트에서 jpeg로 바꿔 보낸다) |
| userId | string | O | `POST /api/users`가 돌려준 userId |
| placeId | number | - | 찍은 장소 ID |

```bash
curl -F file=@photo.jpg -F userId=3f2b8c1e-7a4d-4e9b-9c2a-1d5e6f7a8b9c -F placeId=7 localhost:8080/api/photos
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

- 장소 × 국적 × 언어별로 서버에 저장한다. 같은 조합의 두 번째 요청부터는 AI를 부르지 않고 바로 온다(`cached: true`). 장소 설명이나 프롬프트가 바뀌면 다시 만든다
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
curl 'localhost:8080/api/places/7/reviews?userId=3f2b8c1e-7a4d-4e9b-9c2a-1d5e6f7a8b9c'
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
| userId | string | O | `POST /api/users`가 돌려준 userId |
| content | string | O | 한 줄 후기, 200자 이하. 앞뒤 공백은 지운다 |
| language | string | - | 실제로 쓴 언어 (`KO, EN, VI, ZH, JA, MN, UZ`). 비우면 서버가 글을 보고 정한다. **사용자 설정 언어를 그대로 넣지 않는다** |
| photoId | string | - | 첨부 사진. `POST /api/photos`로 먼저 올리고 받은 `photoId`. **내가 올린 사진만** 붙일 수 있다 |

```json
{ "userId": "3f2b8c1e-7a4d-4e9b-9c2a-1d5e6f7a8b9c", "content": "아침에 오면 한적해서 사진 찍기 좋아요", "language": "KO",
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
| userId | string | O | `POST /api/users`가 돌려준 userId |
| photoId | string | O | `POST /api/photos`로 올린 **내** 사진 |
| comment | string | O | 엽서에 쓸 한 문장, 100자 이하 |
| template | string | - | 템플릿. 지금은 `BASIC` 하나 (비우면 `BASIC`) |

```json
{ "userId": "3f2b8c1e-7a4d-4e9b-9c2a-1d5e6f7a8b9c", "photoId": "9b1c2d3e-4f5a-6b7c-8d9e-0f1a2b3c4d5e",
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
| userId | string | O | `POST /api/users`가 돌려준 userId |
| placeId | number | O | 도장을 찍을 장소 ID |
| lat | number | O | 현재 위도 (-90~90) |
| lng | number | O | 현재 경도 (-180~180) |
| simulatedWeather | string | - | 시연용 날씨 (`CLEAR, RAINY, SNOWY`). 비우면 실제 날씨 |
| simulatedTime | string | - | 시연용 시각 (`15:20`). 비우면 현재 시각 |

시연에서는 도장을 찍을 **장소의 좌표**를 `lat`, `lng`로 보낸다. 역 좌표를 보내면 먼 장소에서 `VISIT_TOO_FAR`가 된다.

```json
{ "userId": "3f2b8c1e-7a4d-4e9b-9c2a-1d5e6f7a8b9c", "placeId": 34, "lat": 37.5743, "lng": 126.9555,
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
| 404 | `USER_NOT_FOUND` | 없는 userId |
| 404 | `PLACE_NOT_FOUND` | 없거나 노출하지 않는 장소 |
| 403 | `PHOTO_FORBIDDEN` | 다른 사용자의 사진을 한걸음·엽서에 쓰려 함 |
| 404 | `PHOTO_NOT_FOUND` | 없는 photoId |
| 404 | `POSTCARD_NOT_FOUND` | 없는 postcardId |
| 409 | `POSTCARD_NOT_READY` | 완성 전(또는 실패한) 엽서의 이미지를 요청함 |
| 404 | `STORAGE_NOT_FOUND` | 파일 기록은 있는데 저장소에 파일이 없음 |
| 405 | `COMMON_METHOD_NOT_ALLOWED` | 지원하지 않는 HTTP 메서드 |
| 415 | `COMMON_UNSUPPORTED_MEDIA_TYPE` | 지원하지 않는 Content-Type |
| 422 | `COMMON_VALIDATION_FAILED` | 값 검증 실패 (빈 메시지, 길이 초과, 이미지 형식·크기). message에 필드별 사유 |
| 422 | `SCENE_SOURCE_MISSING` | 장소에 이야기를 만들 설명(해설·개요)이 없음 |
| 422 | `REVIEW_INAPPROPRIATE` | 한걸음에 욕설·혐오·광고·연락처가 있음. 저장되지 않는다 |
| 422 | `VISIT_TOO_FAR` | 장소에서 멀어 도장을 받을 수 없음 |
| 500 | `COMMON_INTERNAL_ERROR` | 서버 내부 오류 |
| 500 | `STORAGE_WRITE_FAILED` | 파일을 저장하지 못함 |
| 502 | `LLM_CALL_FAILED` | AI 호출 실패 (인증·요청 한도·서버 오류 등) |
| 502 | `LLM_INVALID_OUTPUT` | AI 응답을 정해진 구조로 해석하지 못함 (재시도 후에도) |
| 504 | `LLM_TIMEOUT` | AI 응답 시간 초과 (연결 5초, 응답 대기 60초) |
