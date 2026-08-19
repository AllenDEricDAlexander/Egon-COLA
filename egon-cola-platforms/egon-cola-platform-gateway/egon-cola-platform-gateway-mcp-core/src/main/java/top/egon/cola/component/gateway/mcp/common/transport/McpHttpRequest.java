package top.egon.cola.component.gateway.mcp.common.transport;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Transport-neutral MCP HTTP request passed from a concrete HTTP listener.
 * 补充说明 / Supplementary summary: {@code McpHttpRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责MCPHttp请求相关的职责与边界。
 * English supplement: {@code McpHttpRequest} is an immutable data carrier in the current Gateway module; it owns the mcp http request-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param method 参数 方法；parameter method。
 * @param path 参数 path；parameter path。
 * @param headers 参数 headers；parameter headers。
 * @param body 参数 body；parameter body。
 * @param attributes 参数 attributes；parameter attributes。
 */
public record McpHttpRequest(
        /**
         * 中文说明：保存 方法 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpHttpRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by method; its type is {@code String}, and {@code McpHttpRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpHttpRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpHttpRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        String method,
        /**
         * 中文说明：保存 path 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpHttpRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by path; its type is {@code String}, and {@code McpHttpRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpHttpRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpHttpRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        String path,
        /**
         * 中文说明：保存 headers 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code McpHttpRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by headers; its type is {@code Map<String, String>}, and {@code McpHttpRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpHttpRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpHttpRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, String> headers,
        /**
         * 中文说明：保存 body 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpHttpRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by body; its type is {@code String}, and {@code McpHttpRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpHttpRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpHttpRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        String body,
        /**
         * 中文说明：保存 attributes 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpHttpRequest} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by attributes; its type is {@code Map<String, Object>}, and {@code McpHttpRequest} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpHttpRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpHttpRequest}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> attributes
) {

    /**
     * 中文说明：创建 {@code McpHttpRequest} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpHttpRequest} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param method 参数 方法；parameter method。
     * @param path 参数 path；parameter path。
     * @param headers 参数 headers；parameter headers。
     * @param body 参数 body；parameter body。
     * @param attributes 参数 attributes；parameter attributes。
     */
    public McpHttpRequest {
        method = required(method, "method").toUpperCase(Locale.ROOT);
        path = required(path, "path");
        headers = normalized(headers);
        body = body == null ? "" : body;
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    /**
     * 中文说明：执行 header 操作；该方法是 {@code McpHttpRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the header operation; this method is the invocation entry point on {@code McpHttpRequest} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpHttpRequest.header(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param name 参数 name；parameter name。
     * @return 返回 header 的处理结果；returns the result of the operation.
     */
    public String header(String name) {
        return name == null ? null : headers.get(
                name.toLowerCase(Locale.ROOT)
        );
    }

    /**
     * 中文说明：执行 normalized 操作；该方法是 {@code McpHttpRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the normalized operation; this method is the invocation entry point on {@code McpHttpRequest} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpHttpRequest.normalized(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @return 返回 normalized 的处理结果；returns the result of the operation.
     */
    private static Map<String, String> normalized(
            Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        source.forEach((name, value) -> result.put(
                required(name, "header name").toLowerCase(Locale.ROOT),
                required(value, "header value")
        ));
        return Collections.unmodifiableMap(result);
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code McpHttpRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code McpHttpRequest} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpHttpRequest.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
