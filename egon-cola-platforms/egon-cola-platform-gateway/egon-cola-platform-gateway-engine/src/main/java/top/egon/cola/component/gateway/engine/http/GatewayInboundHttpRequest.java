package top.egon.cola.component.gateway.engine.http;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code GatewayInboundHttpRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责网关InboundHttp请求相关的职责与边界。
 * English summary: {@code GatewayInboundHttpRequest} is an immutable data carrier in the current Gateway module; it owns the gateway inbound http request-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param method 参数 方法；parameter method。
 * @param host 参数 host；parameter host。
 * @param uri 参数 uri；parameter uri。
 * @param headers 参数 headers；parameter headers。
 * @param remoteAddress 参数 远程Address；parameter remote address。
 * @param body 参数 body；parameter body。
 */
public record GatewayInboundHttpRequest(
        /**
         * 中文说明：保存 方法 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayInboundHttpRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by method; its type is {@code String}, and {@code GatewayInboundHttpRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayInboundHttpRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayInboundHttpRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        String method,
        /**
         * 中文说明：保存 host 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayInboundHttpRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by host; its type is {@code String}, and {@code GatewayInboundHttpRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayInboundHttpRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayInboundHttpRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        String host,
        /**
         * 中文说明：保存 uri 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayInboundHttpRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by uri; its type is {@code String}, and {@code GatewayInboundHttpRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayInboundHttpRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayInboundHttpRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        String uri,
        /**
         * 中文说明：保存 headers 对应的状态、依赖或配置值；字段类型为 {@code Map<String, List<String>>}，由 {@code GatewayInboundHttpRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by headers; its type is {@code Map<String, List<String>>}, and {@code GatewayInboundHttpRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayInboundHttpRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayInboundHttpRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, List<String>> headers,
        /**
         * 中文说明：保存 远程Address 对应的状态、依赖或配置值；字段类型为 {@code InetSocketAddress}，由 {@code GatewayInboundHttpRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by remote address; its type is {@code InetSocketAddress}, and {@code GatewayInboundHttpRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayInboundHttpRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayInboundHttpRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        InetSocketAddress remoteAddress,
        /**
         * 中文说明：保存 body 对应的状态、依赖或配置值；字段类型为 {@code Flux<DataBuffer>}，由 {@code GatewayInboundHttpRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by body; its type is {@code Flux<DataBuffer>}, and {@code GatewayInboundHttpRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayInboundHttpRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayInboundHttpRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        Flux<DataBuffer> body
) {

    /**
     * 中文说明：创建 {@code GatewayInboundHttpRequest} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayInboundHttpRequest} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param method 参数 方法；parameter method。
     * @param host 参数 host；parameter host。
     * @param uri 参数 uri；parameter uri。
     * @param headers 参数 headers；parameter headers。
     * @param remoteAddress 参数 远程Address；parameter remote address。
     * @param body 参数 body；parameter body。
     */
    public GatewayInboundHttpRequest {
        method = Objects.requireNonNull(method, "method");
        host = Objects.requireNonNull(host, "host");
        uri = Objects.requireNonNull(uri, "uri");
        headers = Map.copyOf(Objects.requireNonNull(headers, "headers"));
        body = Objects.requireNonNull(body, "body");
    }
}
