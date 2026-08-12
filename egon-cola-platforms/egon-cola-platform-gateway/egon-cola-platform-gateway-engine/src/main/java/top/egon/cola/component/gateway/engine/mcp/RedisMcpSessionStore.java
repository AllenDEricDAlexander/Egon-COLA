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
 * 补充说明 / Supplementary summary: {@code RedisMcpSessionStore} 是存储组件，位于当前 Gateway 模块的相关包中，负责RedisMCP会话存储相关的职责与边界。
 * English supplement: {@code RedisMcpSessionStore} is a redis mcp session store store in the current Gateway module; it owns the redis mcp session store-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class RedisMcpSessionStore
        implements McpSessionStore, McpSubscriptionEventStore {

    /**
     * 中文说明：表示 会话MAPSUFFIX 这一固定值；它属于 {@code RedisMcpSessionStore} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value session map suffix; it is a state, type, or protocol value of {@code RedisMcpSessionStore} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code RedisMcpSessionStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RedisMcpSessionStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final String SESSION_MAP_SUFFIX = "sessions";

    /**
     * 中文说明：保存 redisson 对应的状态、依赖或配置值；字段类型为 {@code RedissonClient}，由 {@code RedisMcpSessionStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by redisson; its type is {@code RedissonClient}, and {@code RedisMcpSessionStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RedisMcpSessionStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RedisMcpSessionStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final RedissonClient redisson;

    /**
     * 中文说明：保存 sessions 对应的状态、依赖或配置值；字段类型为 {@code RMapCache<String, String>}，由 {@code RedisMcpSessionStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by sessions; its type is {@code RMapCache<String, String>}, and {@code RedisMcpSessionStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RedisMcpSessionStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RedisMcpSessionStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final RMapCache<String, String> sessions;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code RedisMcpSessionStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code RedisMcpSessionStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RedisMcpSessionStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RedisMcpSessionStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：保存 键Prefix 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RedisMcpSessionStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by key prefix; its type is {@code String}, and {@code RedisMcpSessionStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RedisMcpSessionStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RedisMcpSessionStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String keyPrefix;

    /**
     * 中文说明：保存 maximumStreamLength 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code RedisMcpSessionStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by maximum stream length; its type is {@code int}, and {@code RedisMcpSessionStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RedisMcpSessionStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RedisMcpSessionStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final int maximumStreamLength;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code RedisMcpSessionStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code RedisMcpSessionStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RedisMcpSessionStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RedisMcpSessionStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：创建 {@code RedisMcpSessionStore} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RedisMcpSessionStore} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param redisson 参数 redisson；parameter redisson。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param keyPrefix 参数 键Prefix；parameter key prefix。
     * @param maximumStreamLength 参数 maximumStreamLength；parameter maximum stream length。
     * @param clock 参数 clock；parameter clock。
     */
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

    /**
     * 中文说明：执行 create 操作；该方法是 {@code RedisMcpSessionStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create operation; this method is the invocation entry point on {@code RedisMcpSessionStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RedisMcpSessionStore.create(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param session 参数 会话；parameter session。
     * @param ttl 参数 ttl；parameter ttl。
     * @return 返回 create 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 find 操作；该方法是 {@code RedisMcpSessionStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code RedisMcpSessionStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RedisMcpSessionStore.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param sessionId 参数 会话Id；parameter session id。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<Session> find(String sessionId) {
        return Mono.fromCompletionStage(sessions.getAsync(requiredSession(
                        sessionId
                )))
                .flatMap(value -> value == null
                        ? Mono.empty()
                        : Mono.just(decode(value)));
    }

    /**
     * 中文说明：执行 touch 操作；该方法是 {@code RedisMcpSessionStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the touch operation; this method is the invocation entry point on {@code RedisMcpSessionStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RedisMcpSessionStore.touch(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param sessionId 参数 会话Id；parameter session id。
     * @param ttl 参数 ttl；parameter ttl。
     * @return 返回 touch 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 delete 操作；该方法是 {@code RedisMcpSessionStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete operation; this method is the invocation entry point on {@code RedisMcpSessionStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RedisMcpSessionStore.delete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param sessionId 参数 会话Id；parameter session id。
     * @return 返回 delete 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 append 操作；该方法是 {@code RedisMcpSessionStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the append operation; this method is the invocation entry point on {@code RedisMcpSessionStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RedisMcpSessionStore.append(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param streamId 参数 streamId；parameter stream id。
     * @param type 参数 type；parameter type。
     * @param data 参数 data；parameter data。
     * @param ttl 参数 ttl；parameter ttl。
     * @return 返回 append 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 listen 操作；该方法是 {@code RedisMcpSessionStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the listen operation; this method is the invocation entry point on {@code RedisMcpSessionStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RedisMcpSessionStore.listen(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param streamId 参数 streamId；parameter stream id。
     * @param afterEventId 参数 after事件Id；parameter after event id。
     * @param wait 参数 wait；parameter wait。
     * @return 返回 listen 的处理结果；returns the result of the operation.
     */
    @Override
    public Flux<Event> listen(
            String streamId,
            String afterEventId,
            Duration wait) {
        StreamMessageId after = afterEventId == null
                || afterEventId.isBlank()
                ? initialReadOffset()
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

    /**
     * 中文说明：执行 initialReadOffset 操作；该方法是 {@code RedisMcpSessionStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the initial read offset operation; this method is the invocation entry point on {@code RedisMcpSessionStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RedisMcpSessionStore.initialReadOffset(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 initialReadOffset 的处理结果；returns the result of the operation.
     */
    static StreamMessageId initialReadOffset() {
        return new StreamMessageId(0L, 0L);
    }

    /**
     * 中文说明：执行 事件 操作；该方法是 {@code RedisMcpSessionStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the event operation; this method is the invocation entry point on {@code RedisMcpSessionStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RedisMcpSessionStore.event(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param values 参数 values；parameter values。
     * @return 返回 事件 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 stream 操作；该方法是 {@code RedisMcpSessionStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the stream operation; this method is the invocation entry point on {@code RedisMcpSessionStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RedisMcpSessionStore.stream(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param streamId 参数 streamId；parameter stream id。
     * @return 返回 stream 的处理结果；returns the result of the operation.
     */
    private RStream<String, String> stream(String streamId) {
        return redisson.getStream(
                keyPrefix + "stream:" + streamId,
                StringCodec.INSTANCE
        );
    }

    /**
     * 中文说明：执行 encode 操作；该方法是 {@code RedisMcpSessionStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the encode operation; this method is the invocation entry point on {@code RedisMcpSessionStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RedisMcpSessionStore.encode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param session 参数 会话；parameter session。
     * @return 返回 encode 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 decode 操作；该方法是 {@code RedisMcpSessionStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the decode operation; this method is the invocation entry point on {@code RedisMcpSessionStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RedisMcpSessionStore.decode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 decode 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 parseId 操作；该方法是 {@code RedisMcpSessionStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the parse id operation; this method is the invocation entry point on {@code RedisMcpSessionStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RedisMcpSessionStore.parseId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 parseId 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 ttlMillis 操作；该方法是 {@code RedisMcpSessionStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the ttl millis operation; this method is the invocation entry point on {@code RedisMcpSessionStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RedisMcpSessionStore.ttlMillis(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param ttl 参数 ttl；parameter ttl。
     * @return 返回 ttlMillis 的处理结果；returns the result of the operation.
     */
    private long ttlMillis(Duration ttl) {
        Objects.requireNonNull(ttl, "ttl");
        long millis = ttl.toMillis();
        if (millis < 1L) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        return millis;
    }

    /**
     * 中文说明：执行 required会话 操作；该方法是 {@code RedisMcpSessionStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required session operation; this method is the invocation entry point on {@code RedisMcpSessionStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RedisMcpSessionStore.requiredSession(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param sessionId 参数 会话Id；parameter session id。
     * @return 返回 required会话 的处理结果；returns the result of the operation.
     */
    private String requiredSession(String sessionId) {
        String normalized = required(sessionId, "sessionId");
        if (!normalized.matches("[A-Za-z0-9-]{16,128}")) {
            throw new IllegalArgumentException("invalid MCP session id");
        }
        return normalized;
    }

    /**
     * 中文说明：执行 normalizedPrefix 操作；该方法是 {@code RedisMcpSessionStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the normalized prefix operation; this method is the invocation entry point on {@code RedisMcpSessionStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RedisMcpSessionStore.normalizedPrefix(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param prefix 参数 prefix；parameter prefix。
     * @return 返回 normalizedPrefix 的处理结果；returns the result of the operation.
     */
    private static String normalizedPrefix(String prefix) {
        String normalized = required(prefix, "keyPrefix");
        if (normalized.length() > 128
                || !normalized.matches("[A-Za-z0-9:_-]+")) {
            throw new IllegalArgumentException("invalid MCP Redis key prefix");
        }
        return normalized.endsWith(":") ? normalized : normalized + ':';
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code RedisMcpSessionStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code RedisMcpSessionStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RedisMcpSessionStore.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
