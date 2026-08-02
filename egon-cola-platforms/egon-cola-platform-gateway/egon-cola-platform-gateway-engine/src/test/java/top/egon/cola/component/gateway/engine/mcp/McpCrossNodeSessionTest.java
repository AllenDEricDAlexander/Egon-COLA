package top.egon.cola.component.gateway.engine.mcp;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.mcp.transport.McpHttpRequest;
import top.egon.cola.component.gateway.mcp.transport.McpHttpResponse;
import top.egon.cola.component.gateway.mcp.transport.McpSessionStore;
import top.egon.cola.component.gateway.mcp.transport.McpSubscriptionEventStore;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpCrossNodeSessionTest {

    @Test
    void stablePostOnNodeBDeliversEventToGetStreamOnNodeA() {
        SharedMcpTransportStore shared = new SharedMcpTransportStore();
        McpEngineHttpHandler nodeA = McpTransportIntegrationTest.handler(
                shared
        );
        McpEngineHttpHandler nodeB = McpTransportIntegrationTest.handler(
                shared
        );

        McpHttpResponse initialized = nodeA.handle(request(
                "POST",
                Map.of("content-type", "application/json"),
                """
                {"jsonrpc":"2.0","id":1,"method":"initialize",\
                "params":{"protocolVersion":"2025-11-25"}}
                """
        )).block();
        String sessionId = initialized.header("Mcp-Session-Id");
        assertNotNull(sessionId);

        McpHttpResponse posted = nodeB.handle(request(
                "POST",
                Map.of(
                        "content-type", "application/json",
                        "Mcp-Protocol-Version", "2025-11-25",
                        "Mcp-Session-Id", sessionId
                ),
                """
                {"jsonrpc":"2.0","id":2,"method":"ping","params":{}}
                """
        )).block();
        assertEquals(200, posted.status());

        McpHttpResponse stream = nodeA.handle(request(
                "GET",
                Map.of(
                        "accept", "text/event-stream",
                        "Mcp-Session-Id", sessionId
                ),
                ""
        )).block();
        String event = Flux.from(stream.body())
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .next()
                .block(Duration.ofSeconds(1));
        assertTrue(event.contains("event:message"));
        assertTrue(event.contains("\"id\":2"));
        assertTrue(event.contains("\"result\":{}"));
    }

    @Test
    void sessionBindingRejectsAnotherSubject() {
        SharedMcpTransportStore shared = new SharedMcpTransportStore();
        McpEngineHttpHandler node = McpTransportIntegrationTest.handler(
                shared
        );
        String sessionId = node.handle(request(
                "POST",
                Map.of("content-type", "application/json"),
                """
                {"jsonrpc":"2.0","id":1,"method":"initialize",\
                "params":{"protocolVersion":"2025-11-25"}}
                """
        )).block().header("Mcp-Session-Id");

        McpHttpRequest foreign = new McpHttpRequest(
                "POST",
                "/mcp/orders",
                Map.of(
                        "content-type", "application/json",
                        "Mcp-Protocol-Version", "2025-11-25",
                        "Mcp-Session-Id", sessionId
                ),
                """
                {"jsonrpc":"2.0","id":2,"method":"ping","params":{}}
                """,
                Map.of("testSubject", "user-2")
        );
        McpEngineHttpHandler foreignNode = McpTransportIntegrationTest.handler(
                shared,
                Map.of(
                        "callerId", "user-2",
                        "tenantId", "tenant-1",
                        "idp.client-id", "client-1"
                )
        );

        McpHttpResponse denied = foreignNode.handle(foreign).block();
        assertEquals(403, denied.status());
        assertTrue(body(denied).contains("MCP_SESSION_BINDING_MISMATCH"));
    }

    private McpHttpRequest request(
            String method,
            Map<String, String> headers,
            String body) {
        return new McpHttpRequest(
                method,
                "/mcp/orders",
                headers,
                body,
                Map.of()
        );
    }

    private String body(McpHttpResponse response) {
        return Flux.from(response.body())
                .reduce(new StringBuilder(), (result, bytes) -> result.append(
                        new String(bytes, StandardCharsets.UTF_8)
                ))
                .map(StringBuilder::toString)
                .block();
    }
}

final class SharedMcpTransportStore
        implements McpSessionStore, McpSubscriptionEventStore {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    private final Map<String, List<Event>> events = new ConcurrentHashMap<>();

    private final AtomicLong sequence = new AtomicLong();

    @Override
    public Mono<Void> create(Session session, Duration ttl) {
        sessions.put(session.sessionId(), session);
        return Mono.empty();
    }

    @Override
    public Mono<Session> find(String sessionId) {
        return Mono.justOrEmpty(sessions.get(sessionId));
    }

    @Override
    public Mono<Void> touch(String sessionId, Duration ttl) {
        return sessions.containsKey(sessionId)
                ? Mono.empty()
                : Mono.error(new IllegalStateException("missing session"));
    }

    @Override
    public Mono<Boolean> delete(String sessionId) {
        events.remove(sessionId);
        return Mono.just(sessions.remove(sessionId) != null);
    }

    @Override
    public Mono<Event> append(
            String sessionId,
            String type,
            String data,
            Duration ttl) {
        long value = sequence.incrementAndGet();
        Event event = new Event(
                value + "-0",
                type,
                data,
                Instant.parse("2026-08-02T00:00:00Z")
        );
        events.computeIfAbsent(
                sessionId,
                ignored -> java.util.Collections.synchronizedList(
                        new ArrayList<>()
                )
        ).add(event);
        return Mono.just(event);
    }

    @Override
    public Flux<Event> listen(
            String sessionId,
            String afterEventId,
            Duration wait) {
        long after = afterEventId == null || afterEventId.isBlank()
                ? 0L
                : Long.parseLong(afterEventId.split("-")[0]);
        return Flux.fromIterable(List.copyOf(events.getOrDefault(
                sessionId,
                List.of()
        ))).filter(event -> Long.parseLong(
                event.eventId().split("-")[0]
        ) > after);
    }
}
