package top.egon.cola.component.gateway.mcp.remote.service;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteProvider;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteAuthProvider;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteMcpClient;
import top.egon.cola.component.gateway.mcp.common.protocol.McpProtocolException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fingerprint-scoped remote clients with timeout, bulkhead and circuit limits.
 * 补充说明 / Supplementary summary: {@code McpRemoteClientPool} 是类型，位于当前 Gateway 模块的相关包中，负责MCP远程客户端池相关的职责与边界。
 * English supplement: {@code McpRemoteClientPool} is a type in the current Gateway module; it owns the mcp remote client pool-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpRemoteClientPool implements AutoCloseable {

    /**
     * 中文说明：保存 工厂 对应的状态、依赖或配置值；字段类型为 {@code RemoteMcpClient.Factory}，由 {@code McpRemoteClientPool} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by factory; its type is {@code RemoteMcpClient.Factory}, and {@code McpRemoteClientPool} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRemoteClientPool} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteClientPool}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final RemoteMcpClient.Factory factory;

    /**
     * 中文说明：保存 authentication 对应的状态、依赖或配置值；字段类型为 {@code RemoteAuthProvider}，由 {@code McpRemoteClientPool} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by authentication; its type is {@code RemoteAuthProvider}, and {@code McpRemoteClientPool} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRemoteClientPool} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteClientPool}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final RemoteAuthProvider authentication;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code McpRemoteClientPool} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code McpRemoteClientPool} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRemoteClientPool} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteClientPool}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：保存 调用超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpRemoteClientPool} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by call timeout; its type is {@code Duration}, and {@code McpRemoteClientPool} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRemoteClientPool} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteClientPool}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Duration callTimeout;

    /**
     * 中文说明：保存 maxConcurrentCalls 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code McpRemoteClientPool} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by max concurrent calls; its type is {@code int}, and {@code McpRemoteClientPool} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRemoteClientPool} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteClientPool}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final int maxConcurrentCalls;

    /**
     * 中文说明：保存 failureThreshold 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code McpRemoteClientPool} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by failure threshold; its type is {@code int}, and {@code McpRemoteClientPool} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRemoteClientPool} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteClientPool}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final int failureThreshold;

    /**
     * 中文说明：保存 circuitOpenDuration 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpRemoteClientPool} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by circuit open duration; its type is {@code Duration}, and {@code McpRemoteClientPool} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRemoteClientPool} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteClientPool}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Duration circuitOpenDuration;

    /**
     * 中文说明：保存 请求Ids 对应的状态、依赖或配置值；字段类型为 {@code AtomicLong}，由 {@code McpRemoteClientPool} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by request ids; its type is {@code AtomicLong}, and {@code McpRemoteClientPool} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRemoteClientPool} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteClientPool}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicLong requestIds = new AtomicLong();

    /**
     * 中文说明：保存 entries 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Entry>}，由 {@code McpRemoteClientPool} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by entries; its type is {@code Map<String, Entry>}, and {@code McpRemoteClientPool} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRemoteClientPool} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteClientPool}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    /**
     * 中文说明：创建 {@code McpRemoteClientPool} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpRemoteClientPool} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param factory 参数 工厂；parameter factory。
     * @param authentication 参数 authentication；parameter authentication。
     */
    public McpRemoteClientPool(
            RemoteMcpClient.Factory factory,
            RemoteAuthProvider authentication) {
        this(
                factory,
                authentication,
                Clock.systemUTC(),
                Duration.ofSeconds(60),
                32,
                3,
                Duration.ofSeconds(30)
        );
    }

    /**
     * 中文说明：创建 {@code McpRemoteClientPool} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpRemoteClientPool} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param factory 参数 工厂；parameter factory。
     * @param authentication 参数 authentication；parameter authentication。
     * @param clock 参数 clock；parameter clock。
     * @param callTimeout 参数 调用超时；parameter call timeout。
     * @param maxConcurrentCalls 参数 maxConcurrentCalls；parameter max concurrent calls。
     * @param failureThreshold 参数 failureThreshold；parameter failure threshold。
     * @param circuitOpenDuration 参数 circuitOpenDuration；parameter circuit open duration。
     */
    public McpRemoteClientPool(
            RemoteMcpClient.Factory factory,
            RemoteAuthProvider authentication,
            Clock clock,
            Duration callTimeout,
            int maxConcurrentCalls,
            int failureThreshold,
            Duration circuitOpenDuration) {
        this.factory = Objects.requireNonNull(factory, "factory");
        this.authentication = Objects.requireNonNull(
                authentication,
                "authentication"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
        this.callTimeout = positive(callTimeout, "callTimeout");
        if (maxConcurrentCalls < 1 || maxConcurrentCalls > 10_000) {
            throw new IllegalArgumentException(
                    "remote MCP maxConcurrentCalls is invalid"
            );
        }
        this.maxConcurrentCalls = maxConcurrentCalls;
        if (failureThreshold < 1 || failureThreshold > 1_000) {
            throw new IllegalArgumentException(
                    "remote MCP failureThreshold is invalid"
            );
        }
        this.failureThreshold = failureThreshold;
        this.circuitOpenDuration = positive(
                circuitOpenDuration,
                "circuitOpenDuration"
        );
    }

    /**
     * 中文说明：执行 exchange 操作；该方法是 {@code McpRemoteClientPool} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the exchange operation; this method is the invocation entry point on {@code McpRemoteClientPool} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteClientPool.exchange(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @param call 参数 调用；parameter call。
     * @param context 参数 context；parameter context。
     * @return 返回 exchange 的处理结果；returns the result of the operation.
     */
    public Publisher<RemoteMcpClient.ExchangeResponse> exchange(
            McpRuntimeRemoteProvider provider,
            McpDialectTranslator.OutboundCall call,
            RemoteAuthProvider.AuthContext context) {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(call, "call");
        Objects.requireNonNull(context, "context");
        if (!provider.enabled()) {
            return Mono.error(unavailable(
                    "remote MCP Provider is disabled",
                    null
            ));
        }
        return Mono.defer(() -> {
            Entry entry = entry(provider);
            if (!entry.circuit.allow(clock.instant())) {
                return Mono.error(unavailable(
                        "remote MCP circuit is open",
                        null
                ));
            }
            if (!entry.bulkhead.tryAcquire()) {
                return Mono.error(unavailable(
                        "remote MCP bulkhead is full",
                        null
                ));
            }
            return Mono.from(authentication.resolve(
                            new RemoteAuthProvider.AuthRequest(
                                    provider,
                                    context
                            )
                    ))
                    .flatMap(auth -> Mono.from(entry.client.exchange(
                            request(provider, call, auth)
                    )))
                    .timeout(callTimeout)
                    .doOnSuccess(ignored -> entry.circuit.success())
                    .doOnError(ignored -> entry.circuit.failure(
                            clock.instant(),
                            failureThreshold,
                            circuitOpenDuration
                    ))
                    .onErrorMap(failure -> failure instanceof McpProtocolException
                            ? failure
                            : unavailable(
                                    "remote MCP request failed",
                                    failure
                            ))
                    .doFinally(ignored -> entry.bulkhead.release());
        });
    }

    /**
     * 中文说明：执行 健康 操作；该方法是 {@code McpRemoteClientPool} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the health operation; this method is the invocation entry point on {@code McpRemoteClientPool} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteClientPool.health(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @return 返回 健康 的处理结果；returns the result of the operation.
     */
    public Health health(McpRuntimeRemoteProvider provider) {
        Entry entry = entries.get(key(provider));
        if (entry == null) {
            return new Health("NOT_CONNECTED", 0, maxConcurrentCalls);
        }
        return new Health(
                entry.circuit.open(clock.instant()) ? "OPEN" : "AVAILABLE",
                entry.circuit.failures.get(),
                entry.bulkhead.availablePermits()
        );
    }

    /**
     * 中文说明：执行 close 操作；该方法是 {@code McpRemoteClientPool} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the close operation; this method is the invocation entry point on {@code McpRemoteClientPool} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteClientPool.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    public void close() {
        entries.values().forEach(entry -> {
            try {
                entry.client.close();
            } catch (RuntimeException ignored) {
                // Best-effort shutdown; clients own no business state.
            }
        });
        entries.clear();
    }

    /**
     * 中文说明：执行 请求 操作；该方法是 {@code McpRemoteClientPool} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the request operation; this method is the invocation entry point on {@code McpRemoteClientPool} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteClientPool.request(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @param call 参数 调用；parameter call。
     * @param auth 参数 认证；parameter auth。
     * @return 返回 请求 的处理结果；returns the result of the operation.
     */
    private RemoteMcpClient.ExchangeRequest request(
            McpRuntimeRemoteProvider provider,
            McpDialectTranslator.OutboundCall call,
            RemoteAuthProvider.OutboundAuthentication auth) {
        LinkedHashMap<String, String> headers = new LinkedHashMap<>(
                call.headers()
        );
        auth.headers().forEach((name, value) -> {
            if (headers.putIfAbsent(name, value) != null) {
                throw new IllegalArgumentException(
                        "remote authentication cannot replace protocol headers"
                );
            }
        });
        String tlsReference = auth.tlsProfileReference() == null
                ? provider.tlsProfileReference()
                : auth.tlsProfileReference();
        return new RemoteMcpClient.ExchangeRequest(
                provider,
                requestIds.incrementAndGet(),
                call.method(),
                call.params(),
                call.meta(),
                headers,
                tlsReference,
                callTimeout
        );
    }

    /**
     * 中文说明：执行 entry 操作；该方法是 {@code McpRemoteClientPool} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the entry operation; this method is the invocation entry point on {@code McpRemoteClientPool} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteClientPool.entry(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @return 返回 entry 的处理结果；returns the result of the operation.
     */
    private Entry entry(McpRuntimeRemoteProvider provider) {
        String key = key(provider);
        return entries.computeIfAbsent(key, ignored -> new Entry(
                factory.create(provider),
                new Semaphore(maxConcurrentCalls),
                new Circuit()
        ));
    }

    /**
     * 中文说明：执行 键 操作；该方法是 {@code McpRemoteClientPool} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the key operation; this method is the invocation entry point on {@code McpRemoteClientPool} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteClientPool.key(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @return 返回 键 的处理结果；returns the result of the operation.
     */
    private String key(McpRuntimeRemoteProvider provider) {
        return provider.providerId() + "\u0000" + provider.capabilityFingerprint();
    }

    /**
     * 中文说明：执行 positive 操作；该方法是 {@code McpRemoteClientPool} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the positive operation; this method is the invocation entry point on {@code McpRemoteClientPool} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteClientPool.positive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 unavailable 操作；该方法是 {@code McpRemoteClientPool} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the unavailable operation; this method is the invocation entry point on {@code McpRemoteClientPool} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteClientPool.unavailable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param message 参数 消息；parameter message。
     * @param failure 参数 failure；parameter failure。
     * @return 返回 unavailable 的处理结果；returns the result of the operation.
     */
    private McpProtocolException unavailable(
            String message,
            Throwable failure) {
        McpProtocolException result = new McpProtocolException(
                McpErrorCode.MCP_REMOTE_UNAVAILABLE,
                message
        );
        if (failure != null) {
            result.initCause(failure);
        }
        return result;
    }

    /**
     * 中文说明：{@code Health} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责健康相关的职责与边界。
     * English summary: {@code Health} is an immutable data carrier in the current Gateway module; it owns the health-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param state 参数 state；parameter state。
     * @param consecutiveFailures 参数 consecutiveFailures；parameter consecutive failures。
     * @param availablePermits 参数 availablePermits；parameter available permits。
     */
    public record Health(
            /**
             * 中文说明：保存 state 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpRemoteClientPool.Health} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by state; its type is {@code String}, and {@code McpRemoteClientPool.Health} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteClientPool.Health} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteClientPool.Health}; do not couple callers to its representation when the owning type exposes an API.
             */
            String state,
            /**
             * 中文说明：保存 consecutiveFailures 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code McpRemoteClientPool.Health} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by consecutive failures; its type is {@code int}, and {@code McpRemoteClientPool.Health} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteClientPool.Health} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteClientPool.Health}; do not couple callers to its representation when the owning type exposes an API.
             */
            int consecutiveFailures,
            /**
             * 中文说明：保存 availablePermits 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code McpRemoteClientPool.Health} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by available permits; its type is {@code int}, and {@code McpRemoteClientPool.Health} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteClientPool.Health} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteClientPool.Health}; do not couple callers to its representation when the owning type exposes an API.
             */
            int availablePermits
    ) {
    }

    /**
     * 中文说明：{@code Entry} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Entry相关的职责与边界。
     * English summary: {@code Entry} is an immutable data carrier in the current Gateway module; it owns the entry-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param client 参数 客户端；parameter client。
     * @param bulkhead 参数 bulkhead；parameter bulkhead。
     * @param circuit 参数 circuit；parameter circuit。
     */
    private record Entry(
            /**
             * 中文说明：保存 客户端 对应的状态、依赖或配置值；字段类型为 {@code RemoteMcpClient}，由 {@code McpRemoteClientPool.Entry} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by client; its type is {@code RemoteMcpClient}, and {@code McpRemoteClientPool.Entry} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteClientPool.Entry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteClientPool.Entry}; do not couple callers to its representation when the owning type exposes an API.
             */
            RemoteMcpClient client,
            /**
             * 中文说明：保存 bulkhead 对应的状态、依赖或配置值；字段类型为 {@code Semaphore}，由 {@code McpRemoteClientPool.Entry} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by bulkhead; its type is {@code Semaphore}, and {@code McpRemoteClientPool.Entry} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteClientPool.Entry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteClientPool.Entry}; do not couple callers to its representation when the owning type exposes an API.
             */
            Semaphore bulkhead,
            /**
             * 中文说明：保存 circuit 对应的状态、依赖或配置值；字段类型为 {@code Circuit}，由 {@code McpRemoteClientPool.Entry} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by circuit; its type is {@code Circuit}, and {@code McpRemoteClientPool.Entry} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpRemoteClientPool.Entry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteClientPool.Entry}; do not couple callers to its representation when the owning type exposes an API.
             */
            Circuit circuit
    ) {
    }

    /**
     * 中文说明：{@code Circuit} 是类型，位于当前 Gateway 模块的相关包中，负责Circuit相关的职责与边界。
     * English summary: {@code Circuit} is a type in the current Gateway module; it owns the circuit-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class Circuit {

        /**
         * 中文说明：保存 failures 对应的状态、依赖或配置值；字段类型为 {@code AtomicInteger}，由 {@code McpRemoteClientPool.Circuit} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by failures; its type is {@code AtomicInteger}, and {@code McpRemoteClientPool.Circuit} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRemoteClientPool.Circuit} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteClientPool.Circuit}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final AtomicInteger failures = new AtomicInteger();

        /**
         * 中文说明：保存 openUntil 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code McpRemoteClientPool.Circuit} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by open until; its type is {@code Instant}, and {@code McpRemoteClientPool.Circuit} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRemoteClientPool.Circuit} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteClientPool.Circuit}; do not couple callers to its representation when the owning type exposes an API.
         */
        private volatile Instant openUntil;

        /**
         * 中文说明：执行 allow 操作；该方法是 {@code McpRemoteClientPool.Circuit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the allow operation; this method is the invocation entry point on {@code McpRemoteClientPool.Circuit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteClientPool.Circuit.allow(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param now 参数 now；parameter now。
         * @return 返回 allow 的处理结果；returns the result of the operation.
         */
        private boolean allow(Instant now) {
            Instant until = openUntil;
            if (until == null) {
                return true;
            }
            if (now.isBefore(until)) {
                return false;
            }
            openUntil = null;
            failures.set(0);
            return true;
        }

        /**
         * 中文说明：执行 open 操作；该方法是 {@code McpRemoteClientPool.Circuit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the open operation; this method is the invocation entry point on {@code McpRemoteClientPool.Circuit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteClientPool.Circuit.open(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param now 参数 now；parameter now。
         * @return 返回 open 的处理结果；returns the result of the operation.
         */
        private boolean open(Instant now) {
            Instant until = openUntil;
            return until != null && now.isBefore(until);
        }

        /**
         * 中文说明：执行 success 操作；该方法是 {@code McpRemoteClientPool.Circuit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the success operation; this method is the invocation entry point on {@code McpRemoteClientPool.Circuit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteClientPool.Circuit.success(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        private void success() {
            failures.set(0);
            openUntil = null;
        }

        /**
         * 中文说明：执行 failure 操作；该方法是 {@code McpRemoteClientPool.Circuit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the failure operation; this method is the invocation entry point on {@code McpRemoteClientPool.Circuit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteClientPool.Circuit.failure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param now 参数 now；parameter now。
         * @param threshold 参数 threshold；parameter threshold。
         * @param openDuration 参数 openDuration；parameter open duration。
         */
        private void failure(
                Instant now,
                int threshold,
                Duration openDuration) {
            if (failures.incrementAndGet() >= threshold) {
                openUntil = now.plus(openDuration);
            }
        }
    }
}
