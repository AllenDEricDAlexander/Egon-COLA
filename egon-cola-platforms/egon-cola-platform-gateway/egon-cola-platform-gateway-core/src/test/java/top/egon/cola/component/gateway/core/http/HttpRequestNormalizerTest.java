package top.egon.cola.component.gateway.core.http;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpRequestNormalizerTest {

    private final HttpRequestNormalizer normalizer =
            new HttpRequestNormalizer(8, 1024);

    @Test
    void normalizesMethodHostPathAndRemovesHopByHopHeaders() {
        NormalizedHttpRequest request = normalizer.normalize(
                "get",
                "EXAMPLE.COM:80",
                "/orders/%E4%B8%AD%E6%96%87?a=1&a=2",
                Map.of(
                        "Connection", List.of("keep-alive"),
                        "X-Test", List.of("value")
                )
        );

        assertEquals("GET", request.method());
        assertEquals("example.com", request.host());
        assertEquals("/orders/中文", request.normalizedPath());
        assertEquals("a=1&a=2", request.rawQuery());
        assertEquals(Map.of("x-test", List.of("value")), request.headers());
    }

    @Test
    void rejectsTraversalEncodedSeparatorsAndRepeatedEncoding() {
        assertThrows(
                GatewayRequestRejectedException.class,
                () -> normalizer.normalizePath("/a/../b")
        );
        assertThrows(
                GatewayRequestRejectedException.class,
                () -> normalizer.normalizePath("/a%2fb")
        );
        assertThrows(
                GatewayRequestRejectedException.class,
                () -> normalizer.normalizePath("/a%252fb")
        );
    }

    @Test
    void rejectsHeaderBombsAndInjection() {
        assertThrows(
                GatewayRequestRejectedException.class,
                () -> new HttpRequestNormalizer(1, 256)
                        .normalize("GET", "example.com", "/", Map.of(
                                "a", List.of("1"),
                                "b", List.of("2")
                        ))
        );
        assertThrows(
                GatewayRequestRejectedException.class,
                () -> normalizer.normalize(
                        "GET",
                        "example.com",
                        "/",
                        Map.of("x-test", List.of("ok\r\nforged: true"))
                )
        );
    }
}
