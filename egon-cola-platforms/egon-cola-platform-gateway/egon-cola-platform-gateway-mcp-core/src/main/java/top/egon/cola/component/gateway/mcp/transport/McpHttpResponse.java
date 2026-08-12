package top.egon.cola.component.gateway.mcp.transport;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Transport-neutral MCP response supporting direct JSON and streaming SSE.
 * 补充说明 / Supplementary summary: {@code McpHttpResponse} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责MCPHttp响应相关的职责与边界。
 * English supplement: {@code McpHttpResponse} is an immutable data carrier in the current Gateway module; it owns the mcp http response-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param status 参数 status；parameter status。
 * @param headers 参数 headers；parameter headers。
 * @param body 参数 body；parameter body。
 * @param flushPerEvent 参数 flushPer事件；parameter flush per event。
 */
public record McpHttpResponse(
        /**
         * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code McpHttpResponse} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code int}, and {@code McpHttpResponse} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpHttpResponse} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpHttpResponse}; do not couple callers to its representation when the owning type exposes an API.
         */
        int status,
        /**
         * 中文说明：保存 headers 对应的状态、依赖或配置值；字段类型为 {@code Map<String, List<String>>}，由 {@code McpHttpResponse} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by headers; its type is {@code Map<String, List<String>>}, and {@code McpHttpResponse} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpHttpResponse} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpHttpResponse}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, List<String>> headers,
        /**
         * 中文说明：保存 body 对应的状态、依赖或配置值；字段类型为 {@code Publisher<byte[]>}，由 {@code McpHttpResponse} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by body; its type is {@code Publisher<byte[]>}, and {@code McpHttpResponse} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpHttpResponse} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpHttpResponse}; do not couple callers to its representation when the owning type exposes an API.
         */
        Publisher<byte[]> body,
        /**
         * 中文说明：保存 flushPer事件 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpHttpResponse} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by flush per event; its type is {@code boolean}, and {@code McpHttpResponse} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpHttpResponse} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpHttpResponse}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean flushPerEvent
) {

    /**
     * 中文说明：创建 {@code McpHttpResponse} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpHttpResponse} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param status 参数 status；parameter status。
     * @param headers 参数 headers；parameter headers。
     * @param body 参数 body；parameter body。
     * @param flushPerEvent 参数 flushPer事件；parameter flush per event。
     */
    public McpHttpResponse {
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("invalid HTTP status");
        }
        headers = normalized(headers);
        body = Objects.requireNonNull(body, "body");
    }

    /**
     * 中文说明：执行 header 操作；该方法是 {@code McpHttpResponse} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the header operation; this method is the invocation entry point on {@code McpHttpResponse} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpHttpResponse.header(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param name 参数 name；parameter name。
     * @return 返回 header 的处理结果；returns the result of the operation.
     */
    public String header(String name) {
        if (name == null) {
            return null;
        }
        List<String> values = headers.get(name.toLowerCase(Locale.ROOT));
        return values == null || values.isEmpty() ? null : values.getFirst();
    }

    /**
     * 中文说明：执行 json 操作；该方法是 {@code McpHttpResponse} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the json operation; this method is the invocation entry point on {@code McpHttpResponse} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpHttpResponse.json(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @param body 参数 body；parameter body。
     * @param extraHeaders 参数 extraHeaders；parameter extra headers。
     * @return 返回 json 的处理结果；returns the result of the operation.
     */
    public static McpHttpResponse json(
            int status,
            String body,
            Map<String, List<String>> extraHeaders) {
        LinkedHashMap<String, List<String>> headers = new LinkedHashMap<>();
        headers.put(
                "content-type",
                List.of("application/json; charset=UTF-8")
        );
        if (extraHeaders != null) {
            headers.putAll(extraHeaders);
        }
        return new McpHttpResponse(
                status,
                headers,
                Flux.just(body.getBytes(StandardCharsets.UTF_8)),
                false
        );
    }

    /**
     * 中文说明：执行 empty 操作；该方法是 {@code McpHttpResponse} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the empty operation; this method is the invocation entry point on {@code McpHttpResponse} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpHttpResponse.empty(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @return 返回 empty 的处理结果；returns the result of the operation.
     */
    public static McpHttpResponse empty(int status) {
        return new McpHttpResponse(status, Map.of(), Flux.empty(), false);
    }

    /**
     * 中文说明：执行 normalized 操作；该方法是 {@code McpHttpResponse} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the normalized operation; this method is the invocation entry point on {@code McpHttpResponse} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpHttpResponse.normalized(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @return 返回 normalized 的处理结果；returns the result of the operation.
     */
    private static Map<String, List<String>> normalized(
            Map<String, List<String>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, List<String>> result = new LinkedHashMap<>();
        source.forEach((name, values) -> result.put(
                Objects.requireNonNull(name, "header name")
                        .toLowerCase(Locale.ROOT),
                List.copyOf(Objects.requireNonNull(values, "header values"))
        ));
        return Collections.unmodifiableMap(result);
    }
}
