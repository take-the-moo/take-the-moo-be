---
description: 목 모드로 서버를 띄워 전 엔드포인트를 curl로 점검하고 결과를 표로 요약한다
---

데모 전 점검. 코드를 고치지 말고 확인만 한다 (문제가 있으면 원인과 수정안만 제시).

1. 8080 포트가 이미 쓰이고 있으면 알리고 멈춘다.
2. `LLM_MOCK=true ./gradlew bootRun`을 백그라운드로 실행하고, `curl -s localhost:8080/api/health`가 응답할 때까지 최대 90초 기다린다. 실패하면 부팅 로그 마지막 부분을 보여준다.
3. 아래를 모두 호출한다 (HTTP 상태와 body를 기록, `-w '%{http_code}'` 사용):
   | # | 요청 | 기대 |
   |---|---|---|
   | 1 | `GET /api/health` | 200, data.mock=true |
   | 2 | `POST /api/chat` `{"message":"안녕"}` | 200, data.answer 존재 |
   | 3 | `GET /api/chat/stream?message=안녕` (`curl -N`) | `event:token` 여러 개 + `event:done` |
   | 4 | `POST /api/analyze` `{"input":"회의록 내용"}` | 200, data에 DTO 필드 |
   | 5 | `POST /api/image` (작은 png를 만들어 `-F file=@x.png -F question=뭐야`) | 200 |
   | 6 | `POST /api/chat` `{"message":""}` | 422 COMMON_VALIDATION_FAILED |
   | 7 | `POST /api/chat` `{"promptKey":"nope","message":"x"}` | 400 LLM_PROMPT_NOT_FOUND |
   | 8 | `POST /api/image` (txt 파일) | 422 COMMON_VALIDATION_FAILED |
   그 밖에 `/ai-feature`로 추가된 엔드포인트가 있으면(`LlmController`와 docs/api.md 확인) 성공 케이스를 1개씩 더한다.
4. 결과를 `| # | 엔드포인트 | 기대 | 실제(상태·code) | 결과(✅/❌) |` 표로 요약한다.
5. ❌ 항목마다 원인(로그·코드 위치 `파일:줄`)과 수정안을 제시한다.
6. 서버를 종료한다 (`lsof -ti tcp:8080 | xargs kill`) — 실패해도 반드시 시도한다.
