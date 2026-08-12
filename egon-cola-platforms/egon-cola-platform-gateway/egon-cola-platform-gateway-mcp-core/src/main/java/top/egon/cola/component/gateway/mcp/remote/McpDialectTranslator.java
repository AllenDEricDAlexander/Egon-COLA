package top.egon.cola.component.gateway.mcp.remote;

import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteMcpClient;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Adapter between the caller dialect and the fixed remote Provider dialect.
 * 补充说明 / Supplementary summary: {@code McpDialectTranslator} 是类型，位于当前 Gateway 模块的相关包中，负责MCPDialectTranslator相关的职责与边界。
 * English supplement: {@code McpDialectTranslator} is a type in the current Gateway module; it owns the mcp dialect translator-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpDialectTranslator {

    /**
     * 中文说明：表示 TRACEHEADERS 这一固定值；它属于 {@code McpDialectTranslator} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value trace headers; it is a state, type, or protocol value of {@code McpDialectTranslator} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpDialectTranslator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpDialectTranslator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Set<String> TRACE_HEADERS = Set.of(
            "traceparent",
            "tracestate",
            "x-egon-request-id"
    );

    /**
     * 中文说明：执行 outbound 操作；该方法是 {@code McpDialectTranslator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the outbound operation; this method is the invocation entry point on {@code McpDialectTranslator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpDialectTranslator.outbound(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param inboundDialect 参数 inboundDialect；parameter inbound dialect。
     * @param remoteDialect 参数 远程Dialect；parameter remote dialect。
     * @param method 参数 方法；parameter method。
     * @param params 参数 params；parameter params。
     * @param meta 参数 meta；parameter meta。
     * @param traceHeaders 参数 traceHeaders；parameter trace headers。
     * @return 返回 outbound 的处理结果；returns the result of the operation.
     */
    public OutboundCall outbound(
            McpProtocolDialect inboundDialect,
            McpProtocolDialect remoteDialect,
            String method,
            Map<String, Object> params,
            Map<String, Object> meta,
            Map<String, String> traceHeaders) {
        if (inboundDialect == null || remoteDialect == null) {
            throw new IllegalArgumentException("MCP dialect is required");
        }
        String translatedMethod = translateMethod(
                inboundDialect,
                remoteDialect,
                required(method, "method")
        );
        LinkedHashMap<String, Object> translatedParams = new LinkedHashMap<>();
        if (params != null) {
            params.forEach((name, value) -> {
                if (!sensitive(name)) {
                    translatedParams.put(name, value);
                }
            });
        }
        LinkedHashMap<String, Object> translatedMeta = new LinkedHashMap<>();
        if (meta != null) {
            meta.forEach((name, value) -> {
                if (!sensitive(name)) {
                    translatedMeta.put(name, value);
                }
            });
        }
        translatedMeta.put("protocolVersion", remoteDialect.protocolVersion());
        translatedMeta.put("gatewayDialect", inboundDialect.protocolVersion());
        copyTraceMetadata(traceHeaders, translatedMeta);

        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        headers.put("mcp-protocol-version", remoteDialect.protocolVersion());
        copyTraceHeaders(traceHeaders, headers);
        if (remoteDialect == McpProtocolDialect.RC_2026_07_28) {
            headers.put("mcp-method", translatedMethod);
            Object name = translatedParams.get("name");
            if (name != null) {
                headers.put("mcp-name", String.valueOf(name));
            }
        }
        if (remoteDialect == McpProtocolDialect.LEGACY_2024_SSE) {
            headers.remove("mcp-protocol-version");
        }
        return new OutboundCall(
                translatedMethod,
                Map.copyOf(translatedParams),
                Map.copyOf(translatedMeta),
                Map.copyOf(headers)
        );
    }

    /**
     * 中文说明：执行 result 操作；该方法是 {@code McpDialectTranslator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the result operation; this method is the invocation entry point on {@code McpDialectTranslator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpDialectTranslator.result(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param response 参数 响应；parameter response。
     * @return 返回 result 的处理结果；returns the result of the operation.
     */
    public Map<String, Object> result(
            RemoteMcpClient.ExchangeResponse response) {
        if (response == null) {
            throw unavailable("remote MCP response was empty", null);
        }
        if (response.error() == null) {
            return response.result();
        }
        RemoteMcpClient.RemoteError error = response.error();
        McpErrorCode code = switch (error.code()) {
            case -32600 -> McpErrorCode.MCP_INVALID_REQUEST;
            case -32601 -> McpErrorCode.MCP_METHOD_NOT_FOUND;
            case -32602 -> McpErrorCode.MCP_INVALID_PARAMS;
            case -32023 -> McpErrorCode.MCP_UNAUTHENTICATED;
            case -32024 -> McpErrorCode.MCP_FORBIDDEN;
            case -32025 -> McpErrorCode.MCP_APPROVAL_REQUIRED;
            case -32028 -> McpErrorCode.MCP_TASK_NOT_FOUND;
            case -32029 -> McpErrorCode.MCP_RESOURCE_REJECTED;
            default -> McpErrorCode.MCP_REMOTE_UNAVAILABLE;
        };
        throw new McpProtocolException(code, safeRemoteMessage(error.message()));
    }

    /**
     * 中文说明：执行 translate方法 操作；该方法是 {@code McpDialectTranslator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the translate method operation; this method is the invocation entry point on {@code McpDialectTranslator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpDialectTranslator.translateMethod(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param inbound 参数 inbound；parameter inbound。
     * @param outbound 参数 outbound；parameter outbound。
     * @param method 参数 方法；parameter method。
     * @return 返回 translate方法 的处理结果；returns the result of the operation.
     */
    private String translateMethod(
            McpProtocolDialect inbound,
            McpProtocolDialect outbound,
            String method) {
        if (inbound == outbound) {
            return method;
        }
        if (outbound == McpProtocolDialect.LEGACY_2024_SSE
                && "server/discover".equals(method)) {
            return "initialize";
        }
        if (inbound == McpProtocolDialect.LEGACY_2024_SSE
                && "initialize".equals(method)
                && outbound == McpProtocolDialect.RC_2026_07_28) {
            return "server/discover";
        }
        return method;
    }

    /**
     * 中文说明：执行 copyTrace元数据 操作；该方法是 {@code McpDialectTranslator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the copy trace metadata operation; this method is the invocation entry point on {@code McpDialectTranslator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpDialectTranslator.copyTraceMetadata(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param target 参数 target；parameter target。
     */
    private void copyTraceMetadata(
            Map<String, String> source,
            Map<String, Object> target) {
        if (source == null) {
            return;
        }
        source.forEach((name, value) -> {
            String normalized = name.toLowerCase(Locale.ROOT);
            if (TRACE_HEADERS.contains(normalized)
                    && value != null && !value.isBlank()) {
                target.put(normalized, value.trim());
            }
        });
    }

    /**
     * 中文说明：执行 copyTraceHeaders 操作；该方法是 {@code McpDialectTranslator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the copy trace headers operation; this method is the invocation entry point on {@code McpDialectTranslator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpDialectTranslator.copyTraceHeaders(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param target 参数 target；parameter target。
     */
    private void copyTraceHeaders(
            Map<String, String> source,
            Map<String, String> target) {
        if (source == null) {
            return;
        }
        source.forEach((name, value) -> {
            String normalized = name.toLowerCase(Locale.ROOT);
            if (TRACE_HEADERS.contains(normalized)
                    && value != null && !value.isBlank()) {
                target.put(normalized, value.trim());
            }
        });
    }

    /**
     * 中文说明：执行 sensitive 操作；该方法是 {@code McpDialectTranslator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the sensitive operation; this method is the invocation entry point on {@code McpDialectTranslator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpDialectTranslator.sensitive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param name 参数 name；parameter name。
     * @return 返回 sensitive 的处理结果；returns the result of the operation.
     */
    private boolean sensitive(String name) {
        String normalized = name.toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "");
        return normalized.contains("authorization")
                || normalized.contains("bearer")
                || normalized.contains("password")
                || normalized.contains("secret")
                || normalized.equals("token")
                || normalized.equals("approvaltoken");
    }

    /**
     * 中文说明：执行 safe远程消息 操作；该方法是 {@code McpDialectTranslator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the safe remote message operation; this method is the invocation entry point on {@code McpDialectTranslator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpDialectTranslator.safeRemoteMessage(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param message 参数 消息；parameter message。
     * @return 返回 safe远程消息 的处理结果；returns the result of the operation.
     */
    private String safeRemoteMessage(String message) {
        if (message == null || message.isBlank()) {
            return "remote MCP request failed";
        }
        String result = message.replaceAll(
                "(?i)bearer\\s+[a-z0-9._~+/=-]+",
                "Bearer [redacted]"
        );
        return result.length() > 512 ? result.substring(0, 512) : result;
    }

    /**
     * 中文说明：执行 unavailable 操作；该方法是 {@code McpDialectTranslator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the unavailable operation; this method is the invocation entry point on {@code McpDialectTranslator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpDialectTranslator.unavailable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param message 参数 消息；parameter message。
     * @param cause 参数 cause；parameter cause。
     * @return 返回 unavailable 的处理结果；returns the result of the operation.
     */
    private McpProtocolException unavailable(
            String message,
            Throwable cause) {
        McpProtocolException exception = new McpProtocolException(
                McpErrorCode.MCP_REMOTE_UNAVAILABLE,
                message
        );
        if (cause != null) {
            exception.initCause(cause);
        }
        return exception;
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code McpDialectTranslator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code McpDialectTranslator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpDialectTranslator.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MCP " + field + " is required");
        }
        return value.trim();
    }

    /**
     * 中文说明：{@code OutboundCall} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Outbound调用相关的职责与边界。
     * English summary: {@code OutboundCall} is an immutable data carrier in the current Gateway module; it owns the outbound call-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param method 参数 方法；parameter method。
     * @param params 参数 params；parameter params。
     * @param meta 参数 meta；parameter meta。
     * @param headers 参数 headers；parameter headers。
     */
    public record OutboundCall(
            /**
             * 中文说明：保存 方法 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpDialectTranslator.OutboundCall} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by method; its type is {@code String}, and {@code McpDialectTranslator.OutboundCall} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpDialectTranslator.OutboundCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpDialectTranslator.OutboundCall}; do not couple callers to its representation when the owning type exposes an API.
             */
            String method,
            /**
             * 中文说明：保存 params 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpDialectTranslator.OutboundCall} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by params; its type is {@code Map<String, Object>}, and {@code McpDialectTranslator.OutboundCall} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpDialectTranslator.OutboundCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpDialectTranslator.OutboundCall}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> params,
            /**
             * 中文说明：保存 meta 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpDialectTranslator.OutboundCall} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by meta; its type is {@code Map<String, Object>}, and {@code McpDialectTranslator.OutboundCall} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpDialectTranslator.OutboundCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpDialectTranslator.OutboundCall}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> meta,
            /**
             * 中文说明：保存 headers 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code McpDialectTranslator.OutboundCall} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by headers; its type is {@code Map<String, String>}, and {@code McpDialectTranslator.OutboundCall} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpDialectTranslator.OutboundCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpDialectTranslator.OutboundCall}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, String> headers
    ) {

        /**
         * 中文说明：创建 {@code McpDialectTranslator.OutboundCall} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpDialectTranslator.OutboundCall} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param method 参数 方法；parameter method。
         * @param params 参数 params；parameter params。
         * @param meta 参数 meta；parameter meta。
         * @param headers 参数 headers；parameter headers。
         */
        public OutboundCall {
            method = requiredValue(method);
            params = params == null ? Map.of() : Map.copyOf(params);
            meta = meta == null ? Map.of() : Map.copyOf(meta);
            headers = headers == null ? Map.of() : Map.copyOf(headers);
        }

        /**
         * 中文说明：执行 required值 操作；该方法是 {@code McpDialectTranslator.OutboundCall} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the required value operation; this method is the invocation entry point on {@code McpDialectTranslator.OutboundCall} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpDialectTranslator.OutboundCall.requiredValue(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param value 参数 值；parameter value。
         * @return 返回 required值 的处理结果；returns the result of the operation.
         */
        private static String requiredValue(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                        "MCP outbound method is required"
                );
            }
            return value.trim();
        }
    }
}
