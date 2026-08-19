package top.egon.cola.component.gateway.engine.mcp.adapter;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.mcp.common.transport.McpSessionStore;
import top.egon.cola.component.gateway.mcp.common.transport.McpSubscriptionEventStore;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class SharedMcpTransportStore
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
