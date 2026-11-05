package com.hackathon.global.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ExternalHttpImageTest {

    private static final int MAX_BYTES = 10;

    private final ExternalHttp externalHttp = new ExternalHttp(JsonMapper.builder().build());
    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        respond("/image", 200, "image/jpeg", new byte[MAX_BYTES]);
        respond("/large", 200, "image/jpeg", new byte[MAX_BYTES + 1]);
        respond("/html", 200, "text/html", new byte[1]);
        respond("/missing", 404, "image/jpeg", new byte[1]);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private void respond(String path, int status, String contentType, byte[] body) {
        server.createContext(path, exchange -> {
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
    }

    private ExternalImage get(String path) {
        return externalHttp.getImage("테스트", "http://127.0.0.1:" + server.getAddress().getPort() + path, MAX_BYTES);
    }

    @Test
    void 이미지를_받는다() {
        ExternalImage image = get("/image");

        assertThat(image.content()).hasSize(MAX_BYTES);
        assertThat(image.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void 크기를_넘거나_이미지가_아니면_형식_오류다() {
        assertThatThrownBy(() -> get("/large")).isInstanceOfSatisfying(ExternalApiException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ExternalApiErrorType.INVALID_RESPONSE));
        assertThatThrownBy(() -> get("/html")).isInstanceOfSatisfying(ExternalApiException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ExternalApiErrorType.INVALID_RESPONSE));
    }

    @Test
    void 받지_못하면_호출_오류다() {
        assertThatThrownBy(() -> get("/missing")).isInstanceOfSatisfying(ExternalApiException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ExternalApiErrorType.CALL_FAILED));
        server.stop(0);
        assertThatThrownBy(() -> get("/image")).isInstanceOfSatisfying(ExternalApiException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ExternalApiErrorType.CALL_FAILED));
    }
}
