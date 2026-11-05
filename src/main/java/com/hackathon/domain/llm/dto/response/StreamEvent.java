package com.hackathon.domain.llm.dto.response;

/**
 * /api/chat/stream SSE 이벤트의 data. 이벤트 이름: token / done / error.
 * 토큰도 JSON으로 감싸 보낸다 — 줄바꿈이 들어간 토큰이 SSE 형식을 깨지 않게.
 */
public sealed interface StreamEvent {

    String name();

    record Token(String text) implements StreamEvent {
        @Override
        public String name() {
            return "token";
        }
    }

    record Done(String model, UsageDto usage, long latencyMs) implements StreamEvent {
        @Override
        public String name() {
            return "done";
        }
    }

    record Error(String code, String message) implements StreamEvent {
        @Override
        public String name() {
            return "error";
        }
    }
}
