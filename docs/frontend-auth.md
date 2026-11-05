# 로그인 연동 가이드 (프론트용)

앱 내 아이디·비밀번호 로그인이다. 첫 화면에서 로그인이 필수이고, 한 번 로그인하면 30일 동안 자동 로그인된다.
엔드포인트별 요청·응답 전체는 [`api.md`](api.md)를 본다. 이 문서는 화면 흐름과 처리 규칙만 정리한다.

## 바뀐 점 (익명 → 로그인)

| 전 | 후 |
|---|---|
| `POST /api/users`로 익명 사용자 생성, `userId`(UUID)를 기기에 저장 | **삭제.** `POST /api/auth/signup` / `login`으로 토큰을 받는다 |
| 요청에 `userId`만 보냄 | `userId`(= 로그인 아이디)는 그대로 보내고, **모든 요청에 `Authorization` 헤더**를 붙인다 |
| `userId`만 알면 누구나 그 사람으로 요청 가능 | 토큰의 사용자와 `userId`가 다르면 403 |

기기에 저장할 것: `accessToken`, `expiresAt`, `user`(`userId`·`name`·`nationality`·`language`).

## 화면 흐름

```
앱 실행
 ├─ 저장된 accessToken 없음 ─────────────→ 로그인 화면
 └─ 있음 → GET /api/users/me (Bearer 토큰)
            ├─ 200 → user 정보 갱신 → 메인(지금 추천)      ← 자동 로그인
            └─ 401 → 토큰 지우기 → 로그인 화면

로그인 화면
 ├─ [로그인]   POST /api/auth/login  → 200: 토큰·user 저장 → 메인
 │                                    → 401 USER_INVALID_CREDENTIALS: "아이디 또는 비밀번호가 맞지 않습니다."
 └─ [회원가입] → 가입 화면

가입 화면 (아이디·비밀번호·이름·국적·언어)
 ├─ 아이디 입력 후 [중복 확인] GET /api/auth/check-id?loginId=  → available true/false
 └─ [가입]   POST /api/auth/signup → 201: 토큰·user 저장 → 메인 (따로 로그인하지 않는다)
                                   → 409 USER_LOGIN_ID_TAKEN: 그 사이 누가 가져감 → 아이디 다시 입력

설정 화면
 ├─ 이름·국적·언어 수정  PATCH /api/users/me (바꾼 항목만) → 200: 저장된 user 갱신
 └─ 로그아웃            기기의 토큰·user를 지우고 로그인 화면으로 (서버 호출 없음)
```

## 요청에 토큰 붙이기

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

- 토큰이 있으면 **모든 API 요청에** 붙인다 (공통 HTTP 클라이언트에서 한 번에 처리하면 된다)
- `userId`를 보내는 API에는 로그인한 사람의 `userId`를 넣는다: 도장 `POST /api/visits`, 한걸음 작성, 사진 올리기, 엽서 만들기, 나의 기록, 추천(`/api/now?userId=`), 한걸음 목록(`?userId=`)
- 로그인·가입·중복 확인은 토큰 없이 부른다

## 입력 규칙 (가입 화면 안내 문구로 쓴다)

| 항목 | 규칙 |
|---|---|
| 아이디 | 영문 소문자·숫자·밑줄 `_` 4~20자. 가입 뒤 바꿀 수 없다 |
| 비밀번호 | 8~64자 |
| 이름 | 1~20자 |
| 국적 | `VN, CN, JP, MN, UZ, US, OTHER` |
| 언어 | `KO, EN, VI, ZH, JA, MN, UZ` |

규칙에 안 맞으면 422 `COMMON_VALIDATION_FAILED`이고 `message`에 이유가 온다 (그대로 보여 줘도 된다).

## 오류 처리

| code | 언제 | 프론트 처리 |
|---|---|---|
| `USER_LOGIN_REQUIRED` (401) | 토큰 없이 로그인이 필요한 API를 부름 | 로그인 화면으로 |
| `USER_INVALID_TOKEN` (401) | 토큰이 만료(30일)됐거나 잘못됨 | 토큰 지우고 로그인 화면으로 |
| `USER_INVALID_CREDENTIALS` (401) | 로그인 실패 | 로그인 화면에 메시지 |
| `USER_FORBIDDEN` (403) | 다른 사람 `userId`로 요청 | 저장된 `userId`가 토큰과 어긋남 → 로그아웃 처리 |
| `USER_LOGIN_ID_TAKEN` (409) | 가입하려는 아이디가 이미 있음 | 아이디 다시 입력 |

**401은 code와 상관없이 "로그인 화면으로"로 처리하면 된다** (`USER_INVALID_CREDENTIALS`만 로그인 화면 안에서 메시지로 보여 준다).

## 알아 둘 점

- 토큰은 30일 뒤 만료된다. 리프레시 토큰은 없다 — 만료되면 다시 로그인한다
- 서버를 재배포해도 서명 키가 같으면 로그인이 유지된다 (서버 설정 `app.auth.jwt-secret`)
- 아이디·비밀번호 찾기는 없다
- 추천·역 목록·주변 장소·해설·장면·흔적 지도는 `userId` 없이도 동작한다 (로그인 화면 뒤에 있으므로 보통은 토큰과 함께 부른다)
- Swagger(`/swagger-ui.html`)에서 시험할 때는 로그인 응답의 `accessToken`을 오른쪽 위 **Authorize**에 넣는다
