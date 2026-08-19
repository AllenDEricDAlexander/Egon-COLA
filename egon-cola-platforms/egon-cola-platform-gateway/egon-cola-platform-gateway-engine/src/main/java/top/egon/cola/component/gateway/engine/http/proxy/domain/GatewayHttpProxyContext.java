package top.egon.cola.component.gateway.engine.http.proxy.domain;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.transport.EffectiveGatewayTransportPolicy;
import top.egon.cola.component.gateway.engine.http.adapter.HttpUpstreamAdapter;
import top.egon.cola.component.gateway.engine.http.common.logging.GatewayBodyLogDirection;
import top.egon.cola.component.gateway.engine.http.common.logging.GatewayBodyLogEvent;
import top.egon.cola.component.gateway.engine.http.common.logging.GatewayBodyLogTap;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 中文说明：{@code GatewayHttpProxyContext} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责网关Http代理Context相关的职责与边界。
 * English summary: {@code GatewayHttpProxyContext} is an immutable data carrier in the current Gateway module; it owns the gateway http proxy context-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param adapter 参数 adapter；parameter adapter。
 * @param provider 参数 提供方；parameter provider。
 * @param method 参数 方法；parameter method。
 * @param pathAndQuery 参数 pathAndQuery；parameter path and query。
 * @param headers 参数 headers；parameter headers。
 * @param body 参数 body；parameter body。
 * @param policy 参数 策略；parameter policy。
 * @param bodyLogSampleBytes 参数 bodyLogSampleBytes；parameter body log sample bytes。
 * @param bodyLogObserver 参数 bodyLogObserver；parameter body log observer。
 */
public record GatewayHttpProxyContext(
        /**
         * 中文说明：保存 adapter 对应的状态、依赖或配置值；字段类型为 {@code HttpUpstreamAdapter}，由 {@code GatewayHttpProxyContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by adapter; its type is {@code HttpUpstreamAdapter}, and {@code GatewayHttpProxyContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpProxyContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpProxyContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        HttpUpstreamAdapter adapter,
        /**
         * 中文说明：保存 提供方 对应的状态、依赖或配置值；字段类型为 {@code ProviderInstance}，由 {@code GatewayHttpProxyContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by provider; its type is {@code ProviderInstance}, and {@code GatewayHttpProxyContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpProxyContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpProxyContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        ProviderInstance provider,
        /**
         * 中文说明：保存 方法 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayHttpProxyContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by method; its type is {@code String}, and {@code GatewayHttpProxyContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpProxyContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpProxyContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        String method,
        /**
         * 中文说明：保存 pathAndQuery 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayHttpProxyContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by path and query; its type is {@code String}, and {@code GatewayHttpProxyContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpProxyContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpProxyContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        String pathAndQuery,
        /**
         * 中文说明：保存 headers 对应的状态、依赖或配置值；字段类型为 {@code Map<String, List<String>>}，由 {@code GatewayHttpProxyContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by headers; its type is {@code Map<String, List<String>>}, and {@code GatewayHttpProxyContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpProxyContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpProxyContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, List<String>> headers,
        /**
         * 中文说明：保存 body 对应的状态、依赖或配置值；字段类型为 {@code Flux<DataBuffer>}，由 {@code GatewayHttpProxyContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by body; its type is {@code Flux<DataBuffer>}, and {@code GatewayHttpProxyContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpProxyContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpProxyContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        Flux<DataBuffer> body,
        /**
         * 中文说明：保存 策略 对应的状态、依赖或配置值；字段类型为 {@code EffectiveGatewayTransportPolicy}，由 {@code GatewayHttpProxyContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by policy; its type is {@code EffectiveGatewayTransportPolicy}, and {@code GatewayHttpProxyContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpProxyContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpProxyContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        EffectiveGatewayTransportPolicy policy,
        /**
         * 中文说明：保存 bodyLogSampleBytes 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayHttpProxyContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by body log sample bytes; its type is {@code int}, and {@code GatewayHttpProxyContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpProxyContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpProxyContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        int bodyLogSampleBytes,
        /**
         * 中文说明：保存 bodyLogObserver 对应的状态、依赖或配置值；字段类型为 {@code Consumer<GatewayBodyLogEvent>}，由 {@code GatewayHttpProxyContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by body log observer; its type is {@code Consumer<GatewayBodyLogEvent>}, and {@code GatewayHttpProxyContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayHttpProxyContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHttpProxyContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        Consumer<GatewayBodyLogEvent> bodyLogObserver
) {

    /**
     * 中文说明：创建 {@code GatewayHttpProxyContext} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayHttpProxyContext} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param adapter 参数 adapter；parameter adapter。
     * @param provider 参数 提供方；parameter provider。
     * @param method 参数 方法；parameter method。
     * @param pathAndQuery 参数 pathAndQuery；parameter path and query。
     * @param headers 参数 headers；parameter headers。
     * @param body 参数 body；parameter body。
     * @param policy 参数 策略；parameter policy。
     */
    public GatewayHttpProxyContext(
            HttpUpstreamAdapter adapter,
            ProviderInstance provider,
            String method,
            String pathAndQuery,
            Map<String, List<String>> headers,
            Flux<DataBuffer> body,
            EffectiveGatewayTransportPolicy policy) {
        this(
                adapter,
                provider,
                method,
                pathAndQuery,
                headers,
                body,
                policy,
                GatewayBodyLogTap.DEFAULT_SAMPLE_BYTES,
                ignored -> {
                }
        );
    }

    /**
     * 中文说明：创建 {@code GatewayHttpProxyContext} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayHttpProxyContext} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param adapter 参数 adapter；parameter adapter。
     * @param provider 参数 提供方；parameter provider。
     * @param method 参数 方法；parameter method。
     * @param pathAndQuery 参数 pathAndQuery；parameter path and query。
     * @param headers 参数 headers；parameter headers。
     * @param body 参数 body；parameter body。
     * @param policy 参数 策略；parameter policy。
     * @param bodyLogSampleBytes 参数 bodyLogSampleBytes；parameter body log sample bytes。
     * @param bodyLogObserver 参数 bodyLogObserver；parameter body log observer。
     */
    public GatewayHttpProxyContext {
        adapter = Objects.requireNonNull(adapter, "adapter");
        provider = Objects.requireNonNull(provider, "provider");
        method = Objects.requireNonNull(method, "method");
        pathAndQuery = Objects.requireNonNull(pathAndQuery, "pathAndQuery");
        headers = Map.copyOf(Objects.requireNonNull(headers, "headers"));
        body = Objects.requireNonNull(body, "body");
        policy = Objects.requireNonNull(policy, "policy");
        if (bodyLogSampleBytes < 1) {
            throw new IllegalArgumentException(
                    "bodyLogSampleBytes must be positive"
            );
        }
        bodyLogObserver = Objects.requireNonNull(
                bodyLogObserver,
                "bodyLogObserver"
        );
    }

    /**
     * 中文说明：执行 observeBody 操作；该方法是 {@code GatewayHttpProxyContext} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the observe body operation; this method is the invocation entry point on {@code GatewayHttpProxyContext} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpProxyContext.observeBody(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param direction 参数 direction；parameter direction。
     * @param bodyHeaders 参数 bodyHeaders；parameter body headers。
     * @return 返回 observeBody 的处理结果；returns the result of the operation.
     */
    public Flux<DataBuffer> observeBody(
            Flux<DataBuffer> source,
            GatewayBodyLogDirection direction,
            Map<String, List<String>> bodyHeaders) {
        return GatewayBodyLogTap.tap(
                source,
                policy.bodyLogEnabled(),
                firstHeader(bodyHeaders, "content-type"),
                direction,
                bodyLogSampleBytes,
                bodyLogObserver
        );
    }

    /**
     * 中文说明：执行 firstHeader 操作；该方法是 {@code GatewayHttpProxyContext} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the first header operation; this method is the invocation entry point on {@code GatewayHttpProxyContext} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHttpProxyContext.firstHeader(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param headers 参数 headers；parameter headers。
     * @param expectedName 参数 expectedName；parameter expected name。
     * @return 返回 firstHeader 的处理结果；returns the result of the operation.
     */
    private static String firstHeader(
            Map<String, List<String>> headers,
            String expectedName) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(expectedName)
                    && entry.getValue() != null
                    && !entry.getValue().isEmpty()) {
                return entry.getValue().getFirst();
            }
        }
        return null;
    }
}
