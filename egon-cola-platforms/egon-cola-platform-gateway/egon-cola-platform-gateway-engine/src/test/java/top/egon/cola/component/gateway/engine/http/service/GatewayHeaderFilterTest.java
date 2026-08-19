package top.egon.cola.component.gateway.engine.http.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayHeaderFilterTest {

    private final GatewayHeaderFilter filter = new GatewayHeaderFilter();

    @Test
    void removesFixedAndConnectionDeclaredHopByHopHeaders() {
        Map<String, List<String>> filtered = filter.requestHeaders(Map.ofEntries(
                Map.entry("Connection", List.of("keep-alive, Foo", "Bar")),
                Map.entry("Foo", List.of("drop")),
                Map.entry("bar", List.of("drop")),
                Map.entry("Proxy-Connection", List.of("drop")),
                Map.entry("TE", List.of("trailers")),
                Map.entry("Content-Type", List.of("multipart/form-data; boundary=x")),
                Map.entry("Authorization", List.of("Bearer secret")),
                Map.entry("OpenAI-Organization", List.of("org")),
                Map.entry("OpenAI-Project", List.of("project")),
                Map.entry("Idempotency-Key", List.of("key")),
                Map.entry("Traceparent", List.of("00-a-b-01"))
        ));

        assertFalse(filtered.containsKey("connection"));
        assertFalse(filtered.containsKey("foo"));
        assertFalse(filtered.containsKey("bar"));
        assertFalse(filtered.containsKey("proxy-connection"));
        assertFalse(filtered.containsKey("te"));
        assertEquals(List.of("Bearer secret"), filtered.get("authorization"));
        assertEquals(List.of("org"), filtered.get("openai-organization"));
        assertEquals(List.of("project"), filtered.get("openai-project"));
        assertEquals(List.of("key"), filtered.get("idempotency-key"));
        assertTrue(filtered.containsKey("content-type"));
        assertTrue(filtered.containsKey("traceparent"));
    }

    @Test
    void preservesBinaryResponseMetadataWhileRemovingHopByHopHeaders() {
        Map<String, List<String>> filtered = filter.responseHeaders(Map.of(
                "Connection", List.of("X-Private"),
                "X-Private", List.of("drop"),
                "Content-Disposition", List.of("attachment; filename=a.wav"),
                "Content-Encoding", List.of("gzip"),
                "Content-Type", List.of("audio/wav")
        ));

        assertFalse(filtered.containsKey("connection"));
        assertFalse(filtered.containsKey("x-private"));
        assertEquals(
                List.of("attachment; filename=a.wav"),
                filtered.get("content-disposition")
        );
        assertEquals(List.of("gzip"), filtered.get("content-encoding"));
        assertEquals(List.of("audio/wav"), filtered.get("content-type"));
    }
}
