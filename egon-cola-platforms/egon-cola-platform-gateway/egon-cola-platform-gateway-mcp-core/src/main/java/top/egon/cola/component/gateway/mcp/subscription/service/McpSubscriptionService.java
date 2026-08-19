package top.egon.cola.component.gateway.mcp.subscription.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.mcp.common.security.McpSecurityDigests;
import top.egon.cola.component.gateway.mcp.common.transport.McpSubscriptionEventStore;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Publishes resource changes to deterministic Redis streams for cross-node use.
 * 补充说明 / Supplementary summary: {@code McpSubscriptionService} 是服务组件，位于当前 Gateway 模块的相关包中，负责MCP订阅服务相关的职责与边界。
 * English supplement: {@code McpSubscriptionService} is a mcp subscription service service in the current Gateway module; it owns the mcp subscription service-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpSubscriptionService {

    /**
     * 中文说明：保存 events 对应的状态、依赖或配置值；字段类型为 {@code McpSubscriptionEventStore}，由 {@code McpSubscriptionService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by events; its type is {@code McpSubscriptionEventStore}, and {@code McpSubscriptionService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpSubscriptionService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSubscriptionService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpSubscriptionEventStore events;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code McpSubscriptionService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code McpSubscriptionService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpSubscriptionService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSubscriptionService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code McpSubscriptionService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code McpSubscriptionService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpSubscriptionService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSubscriptionService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：保存 ttl 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpSubscriptionService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by ttl; its type is {@code Duration}, and {@code McpSubscriptionService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpSubscriptionService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSubscriptionService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Duration ttl;

    /**
     * 中文说明：保存 wait 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpSubscriptionService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by wait; its type is {@code Duration}, and {@code McpSubscriptionService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpSubscriptionService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSubscriptionService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Duration wait;

    /**
     * 中文说明：创建 {@code McpSubscriptionService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpSubscriptionService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param events 参数 events；parameter events。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param clock 参数 clock；parameter clock。
     * @param ttl 参数 ttl；parameter ttl。
     * @param wait 参数 wait；parameter wait。
     */
    public McpSubscriptionService(
            McpSubscriptionEventStore events,
            ObjectMapper objectMapper,
            Clock clock,
            Duration ttl,
            Duration wait) {
        this.events = Objects.requireNonNull(events, "events");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
        this.ttl = positive(ttl, "ttl");
        this.wait = positive(wait, "wait");
    }

    /**
     * 中文说明：执行 subscribe 操作；该方法是 {@code McpSubscriptionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the subscribe operation; this method is the invocation entry point on {@code McpSubscriptionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSubscriptionService.subscribe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param sessionId 参数 会话Id；parameter session id。
     * @param uri 参数 uri；parameter uri。
     * @return 返回 subscribe 的处理结果；returns the result of the operation.
     */
    public Publisher<Subscription> subscribe(String sessionId, String uri) {
        Subscription subscription = new Subscription(
                McpSecurityDigests.token(sessionId + '\0' + uri),
                sessionId,
                uri,
                clock.instant()
        );
        return Mono.from(events.append(
                        streamKey("subscription\0" + sessionId),
                        "subscribed",
                        json(Map.of(
                                "subscriptionId", subscription.subscriptionId(),
                                "uri", uri
                        )),
                        ttl
                ))
                .thenReturn(subscription);
    }

    /**
     * 中文说明：执行 publishUpdated 操作；该方法是 {@code McpSubscriptionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the publish updated operation; this method is the invocation entry point on {@code McpSubscriptionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSubscriptionService.publishUpdated(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param uri 参数 uri；parameter uri。
     * @return 返回 publishUpdated 的处理结果；returns the result of the operation.
     */
    public Publisher<ResourceEvent> publishUpdated(String uri) {
        return publish(uri, "UPDATED");
    }

    /**
     * 中文说明：执行 publishListChanged 操作；该方法是 {@code McpSubscriptionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the publish list changed operation; this method is the invocation entry point on {@code McpSubscriptionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSubscriptionService.publishListChanged(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param uri 参数 uri；parameter uri。
     * @return 返回 publishListChanged 的处理结果；returns the result of the operation.
     */
    public Publisher<ResourceEvent> publishListChanged(String uri) {
        return publish(uri, "LIST_CHANGED");
    }

    /**
     * 中文说明：执行 listen 操作；该方法是 {@code McpSubscriptionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the listen operation; this method is the invocation entry point on {@code McpSubscriptionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSubscriptionService.listen(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param uri 参数 uri；parameter uri。
     * @param afterEventId 参数 after事件Id；parameter after event id。
     * @return 返回 listen 的处理结果；returns the result of the operation.
     */
    public Publisher<ResourceEvent> listen(String uri, String afterEventId) {
        return Flux.from(events.listen(
                        streamKey("resource\0" + uri),
                        afterEventId,
                        wait
                ))
                .map(this::decode);
    }

    /**
     * 中文说明：执行 publish 操作；该方法是 {@code McpSubscriptionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the publish operation; this method is the invocation entry point on {@code McpSubscriptionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSubscriptionService.publish(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param uri 参数 uri；parameter uri。
     * @param kind 参数 kind；parameter kind。
     * @return 返回 publish 的处理结果；returns the result of the operation.
     */
    private Mono<ResourceEvent> publish(String uri, String kind) {
        Instant occurredAt = clock.instant();
        String data = json(Map.of(
                "uri", uri,
                "kind", kind,
                "occurredAt", occurredAt.toString()
        ));
        return Mono.from(events.append(
                        streamKey("resource\0" + uri),
                        "resource-event",
                        data,
                        ttl
                ))
                .map(event -> new ResourceEvent(
                        event.eventId(),
                        uri,
                        kind,
                        occurredAt
                ));
    }

    /**
     * 中文说明：执行 decode 操作；该方法是 {@code McpSubscriptionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the decode operation; this method is the invocation entry point on {@code McpSubscriptionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSubscriptionService.decode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param event 参数 事件；parameter event。
     * @return 返回 decode 的处理结果；returns the result of the operation.
     */
    private ResourceEvent decode(McpSubscriptionEventStore.Event event) {
        try {
            JsonNode node = objectMapper.readTree(event.data());
            return new ResourceEvent(
                    event.eventId(),
                    node.path("uri").asText(),
                    node.path("kind").asText(),
                    Instant.parse(node.path("occurredAt").asText())
            );
        } catch (RuntimeException | JsonProcessingException failure) {
            throw new IllegalStateException(
                    "MCP subscription event is invalid",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 stream键 操作；该方法是 {@code McpSubscriptionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the stream key operation; this method is the invocation entry point on {@code McpSubscriptionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSubscriptionService.streamKey(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 stream键 的处理结果；returns the result of the operation.
     */
    private String streamKey(String value) {
        return McpSecurityDigests.token(value);
    }

    /**
     * 中文说明：执行 json 操作；该方法是 {@code McpSubscriptionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the json operation; this method is the invocation entry point on {@code McpSubscriptionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSubscriptionService.json(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 json 的处理结果；returns the result of the operation.
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "MCP subscription serialization failed",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 positive 操作；该方法是 {@code McpSubscriptionService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the positive operation; this method is the invocation entry point on {@code McpSubscriptionService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSubscriptionService.positive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 positive 的处理结果；returns the result of the operation.
     */
    private Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    /**
     * 中文说明：{@code Subscription} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责订阅相关的职责与边界。
     * English summary: {@code Subscription} is an immutable data carrier in the current Gateway module; it owns the subscription-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param subscriptionId 参数 订阅Id；parameter subscription id。
     * @param sessionId 参数 会话Id；parameter session id。
     * @param uri 参数 uri；parameter uri。
     * @param createdAt 参数 createdAt；parameter created at。
     */
    public record Subscription(
            /**
             * 中文说明：保存 订阅Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpSubscriptionService.Subscription} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by subscription id; its type is {@code String}, and {@code McpSubscriptionService.Subscription} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSubscriptionService.Subscription} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSubscriptionService.Subscription}; do not couple callers to its representation when the owning type exposes an API.
             */
            String subscriptionId,
            /**
             * 中文说明：保存 会话Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpSubscriptionService.Subscription} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by session id; its type is {@code String}, and {@code McpSubscriptionService.Subscription} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSubscriptionService.Subscription} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSubscriptionService.Subscription}; do not couple callers to its representation when the owning type exposes an API.
             */
            String sessionId,
            /**
             * 中文说明：保存 uri 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpSubscriptionService.Subscription} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by uri; its type is {@code String}, and {@code McpSubscriptionService.Subscription} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSubscriptionService.Subscription} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSubscriptionService.Subscription}; do not couple callers to its representation when the owning type exposes an API.
             */
            String uri,
            /**
             * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code McpSubscriptionService.Subscription} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code McpSubscriptionService.Subscription} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSubscriptionService.Subscription} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSubscriptionService.Subscription}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant createdAt
    ) {
    }

    /**
     * 中文说明：{@code ResourceEvent} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责资源事件相关的职责与边界。
     * English summary: {@code ResourceEvent} is an immutable data carrier in the current Gateway module; it owns the resource event-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param eventId 参数 事件Id；parameter event id。
     * @param uri 参数 uri；parameter uri。
     * @param kind 参数 kind；parameter kind。
     * @param occurredAt 参数 occurredAt；parameter occurred at。
     */
    public record ResourceEvent(
            /**
             * 中文说明：保存 事件Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpSubscriptionService.ResourceEvent} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by event id; its type is {@code String}, and {@code McpSubscriptionService.ResourceEvent} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSubscriptionService.ResourceEvent} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSubscriptionService.ResourceEvent}; do not couple callers to its representation when the owning type exposes an API.
             */
            String eventId,
            /**
             * 中文说明：保存 uri 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpSubscriptionService.ResourceEvent} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by uri; its type is {@code String}, and {@code McpSubscriptionService.ResourceEvent} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSubscriptionService.ResourceEvent} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSubscriptionService.ResourceEvent}; do not couple callers to its representation when the owning type exposes an API.
             */
            String uri,
            /**
             * 中文说明：保存 kind 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpSubscriptionService.ResourceEvent} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by kind; its type is {@code String}, and {@code McpSubscriptionService.ResourceEvent} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSubscriptionService.ResourceEvent} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSubscriptionService.ResourceEvent}; do not couple callers to its representation when the owning type exposes an API.
             */
            String kind,
            /**
             * 中文说明：保存 occurredAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code McpSubscriptionService.ResourceEvent} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by occurred at; its type is {@code Instant}, and {@code McpSubscriptionService.ResourceEvent} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSubscriptionService.ResourceEvent} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSubscriptionService.ResourceEvent}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant occurredAt
    ) {
    }
}
