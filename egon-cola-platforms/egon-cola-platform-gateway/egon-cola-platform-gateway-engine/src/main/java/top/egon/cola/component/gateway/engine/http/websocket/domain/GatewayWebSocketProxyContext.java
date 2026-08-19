package top.egon.cola.component.gateway.engine.http.websocket.domain;

import top.egon.cola.component.gateway.engine.http.websocket.service.GatewayWebSocketObserver;

import top.egon.cola.component.gateway.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.transport.EffectiveGatewayTransportPolicy;
import top.egon.cola.component.gateway.engine.common.transport.service.GatewayCommitGuard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 中文说明：{@code GatewayWebSocketProxyContext} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责网关WebSocket代理Context相关的职责与边界。
 * English summary: {@code GatewayWebSocketProxyContext} is an immutable data carrier in the current Gateway module; it owns the gateway web socket proxy context-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param provider 参数 提供方；parameter provider。
 * @param pathAndQuery 参数 pathAndQuery；parameter path and query。
 * @param headers 参数 headers；parameter headers。
 * @param subprotocolCandidates 参数 subprotocolCandidates；parameter subprotocol candidates。
 * @param policy 参数 策略；parameter policy。
 * @param commitGuard 参数 commitGuard；parameter commit guard。
 * @param observer 参数 observer；parameter observer。
 */
public record GatewayWebSocketProxyContext(
        /**
         * 中文说明：保存 提供方 对应的状态、依赖或配置值；字段类型为 {@code ProviderInstance}，由 {@code GatewayWebSocketProxyContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by provider; its type is {@code ProviderInstance}, and {@code GatewayWebSocketProxyContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayWebSocketProxyContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketProxyContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        ProviderInstance provider,
        /**
         * 中文说明：保存 pathAndQuery 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayWebSocketProxyContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by path and query; its type is {@code String}, and {@code GatewayWebSocketProxyContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayWebSocketProxyContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketProxyContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        String pathAndQuery,
        /**
         * 中文说明：保存 headers 对应的状态、依赖或配置值；字段类型为 {@code Map<String, List<String>>}，由 {@code GatewayWebSocketProxyContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by headers; its type is {@code Map<String, List<String>>}, and {@code GatewayWebSocketProxyContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayWebSocketProxyContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketProxyContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, List<String>> headers,
        /**
         * 中文说明：保存 subprotocolCandidates 对应的状态、依赖或配置值；字段类型为 {@code List<String>}，由 {@code GatewayWebSocketProxyContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by subprotocol candidates; its type is {@code List<String>}, and {@code GatewayWebSocketProxyContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayWebSocketProxyContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketProxyContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        List<String> subprotocolCandidates,
        /**
         * 中文说明：保存 策略 对应的状态、依赖或配置值；字段类型为 {@code EffectiveGatewayTransportPolicy}，由 {@code GatewayWebSocketProxyContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by policy; its type is {@code EffectiveGatewayTransportPolicy}, and {@code GatewayWebSocketProxyContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayWebSocketProxyContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketProxyContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        EffectiveGatewayTransportPolicy policy,
        /**
         * 中文说明：保存 commitGuard 对应的状态、依赖或配置值；字段类型为 {@code GatewayCommitGuard}，由 {@code GatewayWebSocketProxyContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by commit guard; its type is {@code GatewayCommitGuard}, and {@code GatewayWebSocketProxyContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayWebSocketProxyContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketProxyContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        GatewayCommitGuard commitGuard,
        /**
         * 中文说明：保存 observer 对应的状态、依赖或配置值；字段类型为 {@code GatewayWebSocketObserver}，由 {@code GatewayWebSocketProxyContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by observer; its type is {@code GatewayWebSocketObserver}, and {@code GatewayWebSocketProxyContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayWebSocketProxyContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketProxyContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        GatewayWebSocketObserver observer
) {

    /**
     * 中文说明：表示 TOKEN 这一固定值；它属于 {@code GatewayWebSocketProxyContext} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value token; it is a state, type, or protocol value of {@code GatewayWebSocketProxyContext} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayWebSocketProxyContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketProxyContext}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Pattern TOKEN = Pattern.compile(
            "[!#$%&'*+\\-.^_`|~0-9A-Za-z]+"
    );

    /**
     * 中文说明：创建 {@code GatewayWebSocketProxyContext} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayWebSocketProxyContext} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param provider 参数 提供方；parameter provider。
     * @param pathAndQuery 参数 pathAndQuery；parameter path and query。
     * @param headers 参数 headers；parameter headers。
     * @param subprotocolCandidates 参数 subprotocolCandidates；parameter subprotocol candidates。
     * @param policy 参数 策略；parameter policy。
     * @param commitGuard 参数 commitGuard；parameter commit guard。
     * @param observer 参数 observer；parameter observer。
     */
    public GatewayWebSocketProxyContext {
        provider = Objects.requireNonNull(provider, "provider");
        if (pathAndQuery == null
                || !pathAndQuery.startsWith("/")
                || pathAndQuery.contains("://")
                || pathAndQuery.indexOf('#') >= 0
                || pathAndQuery.indexOf('\r') >= 0
                || pathAndQuery.indexOf('\n') >= 0) {
            throw new IllegalArgumentException(
                    "pathAndQuery must be an origin-form target"
            );
        }
        headers = immutableHeaders(headers);
        subprotocolCandidates = List.copyOf(Objects.requireNonNull(
                subprotocolCandidates,
                "subprotocolCandidates"
        ));
        if (subprotocolCandidates.stream().anyMatch(candidate ->
                candidate == null || !TOKEN.matcher(candidate).matches())) {
            throw new IllegalArgumentException(
                    "invalid WebSocket subprotocol candidate"
            );
        }
        policy = Objects.requireNonNull(policy, "policy");
        if (policy.transportProtocol()
                != GatewayTransportProtocol.WEBSOCKET) {
            throw new IllegalArgumentException(
                    "WebSocket context requires WEBSOCKET policy"
            );
        }
        if (policy.websocketIdleTimeout().isEmpty()
                || policy.websocketMaxFrameBytes().isEmpty()) {
            throw new IllegalArgumentException(
                    "WebSocket policy requires idle and frame limits"
            );
        }
        commitGuard = Objects.requireNonNull(
                commitGuard,
                "commitGuard"
        );
        observer = Objects.requireNonNull(observer, "observer");
    }

    /**
     * 中文说明：执行 acceptsSubprotocol 操作；该方法是 {@code GatewayWebSocketProxyContext} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the accepts subprotocol operation; this method is the invocation entry point on {@code GatewayWebSocketProxyContext} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketProxyContext.acceptsSubprotocol(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param selected 参数 selected；parameter selected。
     * @return 返回 acceptsSubprotocol 的处理结果；returns the result of the operation.
     */
    public boolean acceptsSubprotocol(String selected) {
        return selected == null || selected.isBlank()
                || subprotocolCandidates.contains(selected);
    }

    /**
     * 中文说明：执行 immutableHeaders 操作；该方法是 {@code GatewayWebSocketProxyContext} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the immutable headers operation; this method is the invocation entry point on {@code GatewayWebSocketProxyContext} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketProxyContext.immutableHeaders(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @return 返回 immutableHeaders 的处理结果；returns the result of the operation.
     */
    private static Map<String, List<String>> immutableHeaders(
            Map<String, List<String>> source) {
        Objects.requireNonNull(source, "headers");
        Map<String, List<String>> copy = new LinkedHashMap<>();
        source.forEach((name, values) -> {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "WebSocket header name is required"
                );
            }
            List<String> safeValues = List.copyOf(
                    Objects.requireNonNull(values, "header values")
            );
            if (safeValues.stream().anyMatch(value -> value == null
                    || value.indexOf('\r') >= 0
                    || value.indexOf('\n') >= 0)) {
                throw new IllegalArgumentException(
                        "invalid WebSocket header value"
                );
            }
            copy.put(name, safeValues);
        });
        return Map.copyOf(copy);
    }
}
