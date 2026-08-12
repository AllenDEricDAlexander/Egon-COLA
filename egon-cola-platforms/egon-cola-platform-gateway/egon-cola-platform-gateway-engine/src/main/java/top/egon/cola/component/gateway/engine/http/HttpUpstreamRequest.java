package top.egon.cola.component.gateway.engine.http;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 中文说明：{@code HttpUpstreamRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责HttpUpstream请求相关的职责与边界。
 * English summary: {@code HttpUpstreamRequest} is an immutable data carrier in the current Gateway module; it owns the http upstream request-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param provider 参数 提供方；parameter provider。
 * @param method 参数 方法；parameter method。
 * @param pathAndQuery 参数 pathAndQuery；parameter path and query。
 * @param headers 参数 headers；parameter headers。
 * @param body 参数 body；parameter body。
 * @param connectTimeout 参数 connect超时；parameter connect timeout。
 * @param responseHeaderTimeout 参数 响应Header超时；parameter response header timeout。
 * @param streamIdleTimeout 参数 streamIdle超时；parameter stream idle timeout。
 * @param totalTimeout 参数 total超时；parameter total timeout。
 * @param replayable 参数 replayable；parameter replayable。
 */
public record HttpUpstreamRequest(
        /**
         * 中文说明：保存 提供方 对应的状态、依赖或配置值；字段类型为 {@code ProviderInstance}，由 {@code HttpUpstreamRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by provider; its type is {@code ProviderInstance}, and {@code HttpUpstreamRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code HttpUpstreamRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpUpstreamRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        ProviderInstance provider,
        /**
         * 中文说明：保存 方法 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code HttpUpstreamRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by method; its type is {@code String}, and {@code HttpUpstreamRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code HttpUpstreamRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpUpstreamRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        String method,
        /**
         * 中文说明：保存 pathAndQuery 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code HttpUpstreamRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by path and query; its type is {@code String}, and {@code HttpUpstreamRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code HttpUpstreamRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpUpstreamRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        String pathAndQuery,
        /**
         * 中文说明：保存 headers 对应的状态、依赖或配置值；字段类型为 {@code Map<String, List<String>>}，由 {@code HttpUpstreamRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by headers; its type is {@code Map<String, List<String>>}, and {@code HttpUpstreamRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code HttpUpstreamRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpUpstreamRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, List<String>> headers,
        /**
         * 中文说明：保存 body 对应的状态、依赖或配置值；字段类型为 {@code Flux<DataBuffer>}，由 {@code HttpUpstreamRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by body; its type is {@code Flux<DataBuffer>}, and {@code HttpUpstreamRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code HttpUpstreamRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpUpstreamRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        Flux<DataBuffer> body,
        /**
         * 中文说明：保存 connect超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code HttpUpstreamRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by connect timeout; its type is {@code Duration}, and {@code HttpUpstreamRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code HttpUpstreamRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpUpstreamRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration connectTimeout,
        /**
         * 中文说明：保存 响应Header超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code HttpUpstreamRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by response header timeout; its type is {@code Duration}, and {@code HttpUpstreamRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code HttpUpstreamRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpUpstreamRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration responseHeaderTimeout,
        /**
         * 中文说明：保存 streamIdle超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code HttpUpstreamRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by stream idle timeout; its type is {@code Duration}, and {@code HttpUpstreamRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code HttpUpstreamRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpUpstreamRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration streamIdleTimeout,
        /**
         * 中文说明：保存 total超时 对应的状态、依赖或配置值；字段类型为 {@code Optional<Duration>}，由 {@code HttpUpstreamRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by total timeout; its type is {@code Optional<Duration>}, and {@code HttpUpstreamRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code HttpUpstreamRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpUpstreamRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        Optional<Duration> totalTimeout,
        /**
         * 中文说明：保存 replayable 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code HttpUpstreamRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by replayable; its type is {@code boolean}, and {@code HttpUpstreamRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code HttpUpstreamRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpUpstreamRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean replayable
) {

    /**
     * 中文说明：创建 {@code HttpUpstreamRequest} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code HttpUpstreamRequest} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param provider 参数 提供方；parameter provider。
     * @param method 参数 方法；parameter method。
     * @param pathAndQuery 参数 pathAndQuery；parameter path and query。
     * @param headers 参数 headers；parameter headers。
     * @param body 参数 body；parameter body。
     * @param timeout 参数 超时；parameter timeout。
     */
    public HttpUpstreamRequest(
            ProviderInstance provider,
            String method,
            String pathAndQuery,
            Map<String, List<String>> headers,
            Flux<DataBuffer> body,
            Duration timeout) {
        this(
                provider,
                method,
                pathAndQuery,
                headers,
                body,
                timeout,
                timeout,
                timeout,
                Optional.of(timeout),
                false
        );
    }

    /**
     * 中文说明：创建 {@code HttpUpstreamRequest} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code HttpUpstreamRequest} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param provider 参数 提供方；parameter provider。
     * @param method 参数 方法；parameter method。
     * @param pathAndQuery 参数 pathAndQuery；parameter path and query。
     * @param headers 参数 headers；parameter headers。
     * @param body 参数 body；parameter body。
     * @param timeout 参数 超时；parameter timeout。
     * @param replayable 参数 replayable；parameter replayable。
     */
    public HttpUpstreamRequest(
            ProviderInstance provider,
            String method,
            String pathAndQuery,
            Map<String, List<String>> headers,
            Flux<DataBuffer> body,
            Duration timeout,
            boolean replayable) {
        this(
                provider,
                method,
                pathAndQuery,
                headers,
                body,
                timeout,
                timeout,
                timeout,
                Optional.of(timeout),
                replayable
        );
    }

    /**
     * 中文说明：创建 {@code HttpUpstreamRequest} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code HttpUpstreamRequest} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param provider 参数 提供方；parameter provider。
     * @param method 参数 方法；parameter method。
     * @param pathAndQuery 参数 pathAndQuery；parameter path and query。
     * @param headers 参数 headers；parameter headers。
     * @param body 参数 body；parameter body。
     * @param connectTimeout 参数 connect超时；parameter connect timeout。
     * @param responseHeaderTimeout 参数 响应Header超时；parameter response header timeout。
     * @param streamIdleTimeout 参数 streamIdle超时；parameter stream idle timeout。
     * @param totalTimeout 参数 total超时；parameter total timeout。
     * @param replayable 参数 replayable；parameter replayable。
     */
    public HttpUpstreamRequest {
        provider = Objects.requireNonNull(provider, "provider");
        method = Objects.requireNonNull(method, "method");
        pathAndQuery = Objects.requireNonNull(pathAndQuery, "pathAndQuery");
        if (!pathAndQuery.startsWith("/") || pathAndQuery.contains("://")) {
            throw new IllegalArgumentException(
                    "upstream path must be relative to selected provider"
            );
        }
        headers = Map.copyOf(Objects.requireNonNull(headers, "headers"));
        body = Objects.requireNonNull(body, "body");
        connectTimeout = positive(connectTimeout, "connectTimeout");
        responseHeaderTimeout = positive(
                responseHeaderTimeout,
                "responseHeaderTimeout"
        );
        streamIdleTimeout = positive(
                streamIdleTimeout,
                "streamIdleTimeout"
        );
        totalTimeout = Objects.requireNonNull(totalTimeout, "totalTimeout");
        totalTimeout.ifPresent(value -> positive(value, "totalTimeout"));
    }

    /**
     * 中文说明：执行 超时 操作；该方法是 {@code HttpUpstreamRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the timeout operation; this method is the invocation entry point on {@code HttpUpstreamRequest} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpUpstreamRequest.timeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 超时 的处理结果；returns the result of the operation.
     */
    public Duration timeout() {
        return responseHeaderTimeout;
    }

    /**
     * 中文说明：执行 positive 操作；该方法是 {@code HttpUpstreamRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the positive operation; this method is the invocation entry point on {@code HttpUpstreamRequest} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpUpstreamRequest.positive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
}
