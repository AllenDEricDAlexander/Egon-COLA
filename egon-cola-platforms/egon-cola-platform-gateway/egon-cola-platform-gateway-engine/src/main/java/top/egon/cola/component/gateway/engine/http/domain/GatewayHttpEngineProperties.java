package top.egon.cola.component.gateway.engine.http.domain;

import top.egon.cola.component.gateway.engine.common.security.domain.GatewayTransportSecurity;

import java.time.Duration;
import java.util.Objects;

/**
 * 中文说明：{@code GatewayHttpEngineProperties} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责网关Http引擎Properties相关的职责与边界。
 * English summary: {@code GatewayHttpEngineProperties} is an immutable data carrier in the current Gateway module; it owns the gateway http engine properties-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param publicListener 参数 public监听器；parameter public listener。
 * @param internalListener 参数 internal监听器；parameter internal listener。
 * @param maxHeaderCount 参数 maxHeaderCount；parameter max header count。
 * @param maxHeaderBytes 参数 maxHeaderBytes；parameter max header bytes。
 * @param defaultMaxBodyBytes 参数 defaultMaxBodyBytes；parameter default max body bytes。
 * @param connectionIdleTimeout 参数 connectionIdle超时；parameter connection idle timeout。
 * @param drainTimeout 参数 drain超时；parameter drain timeout。
 * @param upstreamMaxConnections 参数 upstreamMaxConnections；parameter upstream max connections。
 * @param upstreamPendingAcquireMaxCount 参数 upstreamPendingAcquireMaxCount；parameter upstream pending acquire max count。
 * @param absoluteMaxRequestBodyBytes 参数 absoluteMax请求BodyBytes；parameter absolute max request body bytes。
 * @param bodyLogSampleBytes 参数 bodyLogSampleBytes；parameter body log sample bytes。
 * @param absoluteMaxBodyLogSampleBytes 参数 absoluteMaxBodyLogSampleBytes；parameter absolute max body log sample bytes。
 * @param maxConnectTimeout 参数 maxConnect超时；parameter max connect timeout。
 * @param maxResponseHeaderTimeout 参数 max响应Header超时；parameter max response header timeout。
 * @param maxStreamIdleTimeout 参数 maxStreamIdle超时；parameter max stream idle timeout。
 * @param maxTotalTimeout 参数 maxTotal超时；parameter max total timeout。
 * @param maxWebsocketIdleTimeout 参数 maxWebSocketIdle超时；parameter max websocket idle timeout。
 * @param maxWebsocketFrameBytes 参数 maxWebSocketFrameBytes；parameter max websocket frame bytes。
 */
public record GatewayHttpEngineProperties(
        /**
         * 中文说明：保存 public监听器 对应的状态、依赖或配置值；字段类型为 {@code Listener}，由 {@code GatewayHttpEngineProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by public listener; its type is {@code Listener}, and {@code GatewayHttpEngineProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        Listener publicListener,
        /**
         * 中文说明：保存 internal监听器 对应的状态、依赖或配置值；字段类型为 {@code Listener}，由 {@code GatewayHttpEngineProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by internal listener; its type is {@code Listener}, and {@code GatewayHttpEngineProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        Listener internalListener,
        /**
         * 中文说明：保存 maxHeaderCount 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayHttpEngineProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max header count; its type is {@code int}, and {@code GatewayHttpEngineProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        int maxHeaderCount,
        /**
         * 中文说明：保存 maxHeaderBytes 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayHttpEngineProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max header bytes; its type is {@code int}, and {@code GatewayHttpEngineProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        int maxHeaderBytes,
        /**
         * 中文说明：保存 defaultMaxBodyBytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayHttpEngineProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by default max body bytes; its type is {@code long}, and {@code GatewayHttpEngineProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        long defaultMaxBodyBytes,
        /**
         * 中文说明：保存 connectionIdle超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayHttpEngineProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by connection idle timeout; its type is {@code Duration}, and {@code GatewayHttpEngineProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration connectionIdleTimeout,
        /**
         * 中文说明：保存 drain超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayHttpEngineProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by drain timeout; its type is {@code Duration}, and {@code GatewayHttpEngineProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration drainTimeout,
        /**
         * 中文说明：保存 upstreamMaxConnections 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayHttpEngineProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by upstream max connections; its type is {@code int}, and {@code GatewayHttpEngineProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        int upstreamMaxConnections,
        /**
         * 中文说明：保存 upstreamPendingAcquireMaxCount 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayHttpEngineProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by upstream pending acquire max count; its type is {@code int}, and {@code GatewayHttpEngineProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        int upstreamPendingAcquireMaxCount,
        /**
         * 中文说明：保存 absoluteMax请求BodyBytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayHttpEngineProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by absolute max request body bytes; its type is {@code long}, and {@code GatewayHttpEngineProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        long absoluteMaxRequestBodyBytes,
        /**
         * 中文说明：保存 bodyLogSampleBytes 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayHttpEngineProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by body log sample bytes; its type is {@code int}, and {@code GatewayHttpEngineProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        int bodyLogSampleBytes,
        /**
         * 中文说明：保存 absoluteMaxBodyLogSampleBytes 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayHttpEngineProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by absolute max body log sample bytes; its type is {@code int}, and {@code GatewayHttpEngineProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        int absoluteMaxBodyLogSampleBytes,
        /**
         * 中文说明：保存 maxConnect超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayHttpEngineProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max connect timeout; its type is {@code Duration}, and {@code GatewayHttpEngineProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration maxConnectTimeout,
        /**
         * 中文说明：保存 max响应Header超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayHttpEngineProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max response header timeout; its type is {@code Duration}, and {@code GatewayHttpEngineProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration maxResponseHeaderTimeout,
        /**
         * 中文说明：保存 maxStreamIdle超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayHttpEngineProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max stream idle timeout; its type is {@code Duration}, and {@code GatewayHttpEngineProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration maxStreamIdleTimeout,
        /**
         * 中文说明：保存 maxTotal超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayHttpEngineProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max total timeout; its type is {@code Duration}, and {@code GatewayHttpEngineProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration maxTotalTimeout,
        /**
         * 中文说明：保存 maxWebSocketIdle超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayHttpEngineProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max websocket idle timeout; its type is {@code Duration}, and {@code GatewayHttpEngineProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration maxWebsocketIdleTimeout,
        /**
         * 中文说明：保存 maxWebSocketFrameBytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayHttpEngineProperties} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max websocket frame bytes; its type is {@code long}, and {@code GatewayHttpEngineProperties} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties}; do not couple callers to its representation when the owning type exposes an API.
         */
        long maxWebsocketFrameBytes
) {

    /**
     * 中文说明：表示 MIB 这一固定值；它属于 {@code GatewayHttpEngineProperties} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value mib; it is a state, type, or protocol value of {@code GatewayHttpEngineProperties} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final long MIB = 1024L * 1024L;

    /**
     * 中文说明：表示 LEGACYAGGREGATEDMAXBODYBYTES 这一固定值；它属于 {@code GatewayHttpEngineProperties} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value legacy aggregated max body bytes; it is a state, type, or protocol value of {@code GatewayHttpEngineProperties} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final long LEGACY_AGGREGATED_MAX_BODY_BYTES = 64L * MIB;

    /**
     * 中文说明：创建 {@code GatewayHttpEngineProperties} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayHttpEngineProperties} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param publicListener 参数 public监听器；parameter public listener。
     * @param internalListener 参数 internal监听器；parameter internal listener。
     * @param maxHeaderCount 参数 maxHeaderCount；parameter max header count。
     * @param maxHeaderBytes 参数 maxHeaderBytes；parameter max header bytes。
     * @param defaultMaxBodyBytes 参数 defaultMaxBodyBytes；parameter default max body bytes。
     * @param connectionIdleTimeout 参数 connectionIdle超时；parameter connection idle timeout。
     * @param drainTimeout 参数 drain超时；parameter drain timeout。
     * @param upstreamMaxConnections 参数 upstreamMaxConnections；parameter upstream max connections。
     * @param upstreamPendingAcquireMaxCount 参数 upstreamPendingAcquireMaxCount；parameter upstream pending acquire max count。
     */
    public GatewayHttpEngineProperties(
            Listener publicListener,
            Listener internalListener,
            int maxHeaderCount,
            int maxHeaderBytes,
            long defaultMaxBodyBytes,
            Duration connectionIdleTimeout,
            Duration drainTimeout,
            int upstreamMaxConnections,
            int upstreamPendingAcquireMaxCount) {
        this(
                publicListener,
                internalListener,
                maxHeaderCount,
                maxHeaderBytes,
                defaultMaxBodyBytes,
                connectionIdleTimeout,
                drainTimeout,
                upstreamMaxConnections,
                upstreamPendingAcquireMaxCount,
                1024L * MIB,
                8 * 1024,
                64 * 1024,
                Duration.ofSeconds(60),
                Duration.ofMinutes(10),
                Duration.ofMinutes(30),
                Duration.ofHours(2),
                Duration.ofHours(2),
                64L * MIB
        );
    }

    /**
     * 中文说明：创建 {@code GatewayHttpEngineProperties} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayHttpEngineProperties} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param publicListener 参数 public监听器；parameter public listener。
     * @param internalListener 参数 internal监听器；parameter internal listener。
     * @param maxHeaderCount 参数 maxHeaderCount；parameter max header count。
     * @param maxHeaderBytes 参数 maxHeaderBytes；parameter max header bytes。
     * @param defaultMaxBodyBytes 参数 defaultMaxBodyBytes；parameter default max body bytes。
     * @param connectionIdleTimeout 参数 connectionIdle超时；parameter connection idle timeout。
     * @param drainTimeout 参数 drain超时；parameter drain timeout。
     * @param upstreamMaxConnections 参数 upstreamMaxConnections；parameter upstream max connections。
     * @param upstreamPendingAcquireMaxCount 参数 upstreamPendingAcquireMaxCount；parameter upstream pending acquire max count。
     * @param absoluteMaxRequestBodyBytes 参数 absoluteMax请求BodyBytes；parameter absolute max request body bytes。
     * @param bodyLogSampleBytes 参数 bodyLogSampleBytes；parameter body log sample bytes。
     * @param absoluteMaxBodyLogSampleBytes 参数 absoluteMaxBodyLogSampleBytes；parameter absolute max body log sample bytes。
     * @param maxConnectTimeout 参数 maxConnect超时；parameter max connect timeout。
     * @param maxResponseHeaderTimeout 参数 max响应Header超时；parameter max response header timeout。
     * @param maxStreamIdleTimeout 参数 maxStreamIdle超时；parameter max stream idle timeout。
     * @param maxTotalTimeout 参数 maxTotal超时；parameter max total timeout。
     * @param maxWebsocketIdleTimeout 参数 maxWebSocketIdle超时；parameter max websocket idle timeout。
     * @param maxWebsocketFrameBytes 参数 maxWebSocketFrameBytes；parameter max websocket frame bytes。
     */
    public GatewayHttpEngineProperties {
        publicListener = Objects.requireNonNull(publicListener, "publicListener");
        internalListener = Objects.requireNonNull(
                internalListener,
                "internalListener"
        );
        if (publicListener.enabled()
                && internalListener.enabled()
                && publicListener.port() == internalListener.port()
                && publicListener.port() != 0) {
            throw new IllegalArgumentException(
                    "PUBLIC and INTERNAL HTTP ports must be different"
            );
        }
        if (!publicListener.enabled() && !internalListener.enabled()) {
            throw new IllegalArgumentException(
                    "at least one HTTP listener must be enabled"
            );
        }
        if (internalListener.enabled()
                && internalListener.transportSecurity().enabled()
                && !internalListener.transportSecurity()
                .clientCertificateRequired()) {
            throw new IllegalArgumentException(
                    "INTERNAL HTTP TLS must require a client certificate"
            );
        }
        if (maxHeaderCount < 1 || maxHeaderBytes < 256) {
            throw new IllegalArgumentException("invalid HTTP header limits");
        }
        if (absoluteMaxRequestBodyBytes < 1
                || absoluteMaxRequestBodyBytes > 1024L * MIB) {
            throw new IllegalArgumentException(
                    "absoluteMaxRequestBodyBytes must be between 1 byte and "
                            + "1 GiB"
            );
        }
        long aggregatedMaximum = Math.min(
                LEGACY_AGGREGATED_MAX_BODY_BYTES,
                absoluteMaxRequestBodyBytes
        );
        if (defaultMaxBodyBytes < 1
                || defaultMaxBodyBytes > aggregatedMaximum) {
            throw new IllegalArgumentException(
                    "defaultMaxBodyBytes must be between 1 byte and "
                            + "the lower of 64 MiB and "
                            + "absoluteMaxRequestBodyBytes"
            );
        }
        if (absoluteMaxBodyLogSampleBytes < 1
                || absoluteMaxBodyLogSampleBytes > 64 * 1024
                || bodyLogSampleBytes < 1
                || bodyLogSampleBytes > absoluteMaxBodyLogSampleBytes) {
            throw new IllegalArgumentException(
                    "invalid HTTP body log sample limits"
            );
        }
        connectionIdleTimeout = positive(
                connectionIdleTimeout,
                "connectionIdleTimeout"
        );
        drainTimeout = positive(drainTimeout, "drainTimeout");
        maxConnectTimeout = range(
                maxConnectTimeout,
                Duration.ofMillis(100),
                Duration.ofSeconds(60),
                "maxConnectTimeout"
        );
        maxResponseHeaderTimeout = range(
                maxResponseHeaderTimeout,
                Duration.ofSeconds(1),
                Duration.ofMinutes(10),
                "maxResponseHeaderTimeout"
        );
        maxStreamIdleTimeout = range(
                maxStreamIdleTimeout,
                Duration.ofSeconds(1),
                Duration.ofMinutes(30),
                "maxStreamIdleTimeout"
        );
        maxTotalTimeout = range(
                maxTotalTimeout,
                Duration.ofSeconds(1),
                Duration.ofHours(2),
                "maxTotalTimeout"
        );
        maxWebsocketIdleTimeout = range(
                maxWebsocketIdleTimeout,
                Duration.ofSeconds(1),
                Duration.ofHours(2),
                "maxWebsocketIdleTimeout"
        );
        if (maxWebsocketFrameBytes < 1024L
                || maxWebsocketFrameBytes > 64L * MIB) {
            throw new IllegalArgumentException(
                    "maxWebsocketFrameBytes must be between 1 KiB and 64 MiB"
            );
        }
        if (upstreamMaxConnections < 1
                || upstreamPendingAcquireMaxCount < 0) {
            throw new IllegalArgumentException(
                    "invalid upstream connection pool limits"
            );
        }
    }

    /**
     * 中文说明：{@code Listener} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责监听器相关的职责与边界。
     * English summary: {@code Listener} is an immutable data carrier in the current Gateway module; it owns the listener-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param enabled 参数 enabled；parameter enabled。
     * @param host 参数 host；parameter host。
     * @param port 参数 port；parameter port。
     * @param transportSecurity 参数 传输安全；parameter transport security。
     */
    public record Listener(
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayHttpEngineProperties.Listener} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code GatewayHttpEngineProperties.Listener} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties.Listener} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties.Listener}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 host 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayHttpEngineProperties.Listener} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by host; its type is {@code String}, and {@code GatewayHttpEngineProperties.Listener} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties.Listener} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties.Listener}; do not couple callers to its representation when the owning type exposes an API.
             */
            String host,
            /**
             * 中文说明：保存 port 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayHttpEngineProperties.Listener} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by port; its type is {@code int}, and {@code GatewayHttpEngineProperties.Listener} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties.Listener} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties.Listener}; do not couple callers to its representation when the owning type exposes an API.
             */
            int port,
            /**
             * 中文说明：保存 传输安全 对应的状态、依赖或配置值；字段类型为 {@code GatewayTransportSecurity}，由 {@code GatewayHttpEngineProperties.Listener} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by transport security; its type is {@code GatewayTransportSecurity}, and {@code GatewayHttpEngineProperties.Listener} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayHttpEngineProperties.Listener} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpEngineProperties.Listener}; do not couple callers to its representation when the owning type exposes an API.
             */
            GatewayTransportSecurity transportSecurity
    ) {

        /**
         * 中文说明：创建 {@code GatewayHttpEngineProperties.Listener} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayHttpEngineProperties.Listener} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param enabled 参数 enabled；parameter enabled。
         * @param host 参数 host；parameter host。
         * @param port 参数 port；parameter port。
         */
        public Listener(boolean enabled, String host, int port) {
            this(
                    enabled,
                    host,
                    port,
                    GatewayTransportSecurity.developmentPlaintextConfig()
            );
        }

        /**
         * 中文说明：创建 {@code GatewayHttpEngineProperties.Listener} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayHttpEngineProperties.Listener} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param enabled 参数 enabled；parameter enabled。
         * @param host 参数 host；parameter host。
         * @param port 参数 port；parameter port。
         * @param transportSecurity 参数 传输安全；parameter transport security。
         */
        public Listener {
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("listener host is required");
            }
            if (port < 0 || port > 65535) {
                throw new IllegalArgumentException(
                        "listener port must be between 0 and 65535"
                );
            }
            transportSecurity = Objects.requireNonNull(
                    transportSecurity,
                    "transportSecurity"
            );
        }
    }

    /**
     * 中文说明：执行 positive 操作；该方法是 {@code GatewayHttpEngineProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the positive operation; this method is the invocation entry point on {@code GatewayHttpEngineProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpEngineProperties.positive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 positive 的处理结果；returns the result of the operation.
     */
    private static Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    /**
     * 中文说明：执行 range 操作；该方法是 {@code GatewayHttpEngineProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the range operation; this method is the invocation entry point on {@code GatewayHttpEngineProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpEngineProperties.range(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param minimum 参数 minimum；parameter minimum。
     * @param maximum 参数 maximum；parameter maximum。
     * @param field 参数 field；parameter field。
     * @return 返回 range 的处理结果；returns the result of the operation.
     */
    private static Duration range(
            Duration value,
            Duration minimum,
            Duration maximum,
            String field) {
        Objects.requireNonNull(value, field);
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(
                    field + " must be between " + minimum + " and " + maximum
            );
        }
        return value;
    }
}
