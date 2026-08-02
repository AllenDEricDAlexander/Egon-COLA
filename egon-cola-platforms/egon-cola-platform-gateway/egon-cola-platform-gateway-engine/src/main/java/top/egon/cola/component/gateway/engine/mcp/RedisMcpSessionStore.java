package top.egon.cola.component.gateway.engine.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RMapCache;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamReadArgs;
import org.redisson.client.codec.StringCodec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.mcp.transport.McpSessionStore;
import top.egon.cola.component.gateway.mcp.transport.McpSubscriptionEventStore;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Redis-backed MCP sessions and bounded event streams shared by engine nodes.
 */
public final class RedisMcpSessionStore
        implements McpSessionStore, McpSubscriptionEventStore {

    private static final String SESSION_MAP_SUFFIX = "sessions";

    private final RedissonClient redisson;

    private final RMapCache<String, String> sessions;

    private final ObjectMapper objectMapper;

    private final String keyPrefix;

    private final int maximumStreamLength;

    private final Clock clock;

    public RedisMcpSessionStore(
            RedissonClient redisson,
            ObjectMapper objectMapper,
            String keyPrefix,
            int maximumStreamLength,
            Clock clock) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
        this.keyPrefix = normalizedPrefix(keyPrefix);
        if (maximumStreamLength < 1 || maximumStreamLength > 10_000) {
            throw new IllegalArgumentException(
                    "maximumStreamLength must be between 1 and 10000"
            );
        }
        this.maximumStreamLength = maximumStreamLength;
        this.clock = Objects.requireNonNull(clock, "clock");
        this.sessions = redisson.getMapCache(
                this.keyPrefix + SESSION_MAP_SUFFIX,
                StringCodec.INSTANCE
        );
    }

    @Override
    public Mono<Void> create(Session session, Duration ttl) {
        Objects.requireNonNull(session, "session");
        long ttlMillis = ttlMillis(ttl);
        return Mono.fromCompletionStage(sessions.fastPutIfAbsentAsync(
                        session.sessionId(),
                        encode(session),
                        ttlMillis,
                        TimeUnit.MILLISECONDS,
                        0L,
                        TimeUnit.MILLISECONDS
                ))
                .flatMap(created -> created
                        ? Mono.<Void>empty()
                        : Mono.error(new IllegalStateException(
                                "MCP session already exists"
                        )));
    }

    @Override
    public Mono<Session> find(String sessionId) {
        return Mono.fromCompletionStage(sessions.getAsync(requiredSession(
                        sessionId
                )))
                .flatMap(value -> value == null
                        ? Mono.empty()
                        : Mono.just(decode(value)));
    }

    @Override
    public Mono<Void> touch(String sessionId, Duration ttl) {
        return Mono.fromCompletionStage(sessions.updateEntryExpirationAsync(
                        requiredSession(sessionId),
                        ttlMillis(ttl),
                        TimeUnit.MILLISECONDS,
                        0L,
                        TimeUnit.MILLISECONDS
                ))
                .flatMap(updated -> updated
                        ? Mono.<Void>empty()
                        : Mono.error(new IllegalStateException(
                                "MCP session does not exist"
                        )));
    }

    @Override
    public Mono<Boolean> delete(String sessionId) {
        String normalized = requiredSession(sessionId);
        Mono<Boolean> sessionDeleted = Mono.fromCompletionStage(
                sessions.fastRemoveAsync(normalized)
        ).map(count -> count > 0L);
        Mono<Boolean> streamDeleted = Mono.fromCompletionStage(
                stream(normalized).deleteAsync()
        );
        return Mono.zip(sessionDeleted, streamDeleted)
                .map(result -> result.getT1() || result.getT2());
    }

    @Override
    public Mono<Event> append(
            String streamId,
            String type,
            String data,
            Duration ttl) {
        String normalized = requiredSession(streamId);
        Instant createdAt = clock.instant();
        RStream<String, String> stream = stream(normalized);
        StreamAddArgs<String, String> args = StreamAddArgs.entries(Map.of(
                "type", required(type, "type"),
                "data", Objects.requireNonNull(data, "data"),
                "createdAt", createdAt.toString()
        )).trimNonStrict().maxLen(maximumStreamLength).noLimit();
        return Mono.fromCompletionStage(stream.addAsync(args))
                .flatMap(id -> Mono.fromCompletionStage(
                                stream.expireAsync(ttl)
                        )
                        .thenReturn(new Event(
                                id.toString(),
                                type,
                                data,
                                createdAt
                        )));
    }

    @Override
    public Flux<Event> listen(
            String streamId,
            String afterEventId,
            Duration wait) {
        StreamMessageId after = afterEventId == null
                || afterEventId.isBlank()
                ? StreamMessageId.MIN
                : parseId(afterEventId);
        return Mono.fromCompletionStage(stream(requiredSession(streamId))
                        .readAsync(StreamReadArgs.greaterThan(after)
                                .count(maximumStreamLength)
                                .timeout(wait)))
                .flatMapMany(entries -> Flux.fromIterable(
                        entries.entrySet().stream()
                                .sorted(Map.Entry.comparingByKey(
                                        Comparator.comparingLong(
                                                StreamMessageId::getId0
                                        ).thenComparingLong(
                                                StreamMessageId::getId1
                                        )
                                ))
                                .toList()
                ))
                .map(entry -> event(entry.getKey(), entry.getValue()));
    }

    private Event event(
            StreamMessageId id,
            Map<String, String> values) {
        return new Event(
                id.toString(),
                required(values.get("type"), "event type"),
                Objects.requireNonNull(values.get("data"), "event data"),
                Instant.parse(required(
                        values.get("createdAt"),
                        "event createdAt"
                ))
        );
    }

    private RStream<String, String> stream(String streamId) {
        return redisson.getStream(
                keyPrefix + "stream:" + streamId,
                StringCodec.INSTANCE
        );
    }

    private String encode(Session session) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("sessionId", session.sessionId());
        value.put("serverCode", session.serverCode());
        value.put("subjectId", session.subjectId());
        value.put("tenantId", session.tenantId());
        value.put("clientId", session.clientId());
        value.put("createdAt", session.createdAt().toString());
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "MCP session serialization failed",
                    failure
            );
        }
    }

    private Session decode(String value) {
        try {
            JsonNode node = objectMapper.readTree(value);
            return new Session(
                    node.path("sessionId").asText(),
                    node.path("serverCode").asText(),
                    node.path("subjectId").asText(),
                    node.path("tenantId").asText(),
                    node.path("clientId").asText(),
                    Instant.parse(node.path("createdAt").asText())
            );
        } catch (RuntimeException | JsonProcessingException failure) {
            throw new IllegalStateException(
                    "MCP session payload is invalid",
                    failure
            );
        }
    }

    private StreamMessageId parseId(String value) {
        String[] parts = value.trim().split("-", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("invalid MCP event id");
        }
        try {
            return new StreamMessageId(
                    Long.parseLong(parts[0]),
                    Long.parseLong(parts[1])
            );
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    "invalid MCP event id",
                    failure
            );
        }
    }

    private long ttlMillis(Duration ttl) {
        Objects.requireNonNull(ttl, "ttl");
        long millis = ttl.toMillis();
        if (millis < 1L) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        return millis;
    }

    private String requiredSession(String sessionId) {
        String normalized = required(sessionId, "sessionId");
        if (!normalized.matches("[A-Za-z0-9-]{16,128}")) {
            throw new IllegalArgumentException("invalid MCP session id");
        }
        return normalized;
    }

    private static String normalizedPrefix(String prefix) {
        String normalized = required(prefix, "keyPrefix");
        if (normalized.length() > 128
                || !normalized.matches("[A-Za-z0-9:_-]+")) {
            throw new IllegalArgumentException("invalid MCP Redis key prefix");
        }
        return normalized.endsWith(":") ? normalized : normalized + ':';
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
