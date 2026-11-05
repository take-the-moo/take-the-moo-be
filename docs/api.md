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

| 메서드 | 경로 | 설명 | 응답 data |
|---|---|---|---|
| POST | `/api/chat` | 텍스트 채팅 | ChatResponse |
| GET | `/api/chat/stream` | 텍스트 채팅 (SSE 스트리밍) | SSE 이벤트 |
| POST | `/api/analyze` | 구조화 분석 | AnalysisResult |
| POST | `/api/image` | 이미지 질문 (multipart) | ChatResponse |
| GET | `/api/health` | 서버 상태 | `{status, mock}` |

`promptKey`는 서버 `prompts.yml`에 등록된 키다. 지금 등록된 키: `chat`, `analyze`, `image`.
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
| 405 | `COMMON_METHOD_NOT_ALLOWED` | 지원하지 않는 HTTP 메서드 |
| 415 | `COMMON_UNSUPPORTED_MEDIA_TYPE` | 지원하지 않는 Content-Type |
| 422 | `COMMON_VALIDATION_FAILED` | 값 검증 실패 (빈 메시지, 길이 초과, 이미지 형식·크기). message에 필드별 사유 |
| 500 | `COMMON_INTERNAL_ERROR` | 서버 내부 오류 |
| 502 | `LLM_CALL_FAILED` | AI 호출 실패 (인증·요청 한도·서버 오류 등) |
| 502 | `LLM_INVALID_OUTPUT` | AI 응답을 정해진 구조로 해석하지 못함 (재시도 후에도) |
| 504 | `LLM_TIMEOUT` | AI 응답 시간 초과 (연결 5초, 응답 대기 60초) |
