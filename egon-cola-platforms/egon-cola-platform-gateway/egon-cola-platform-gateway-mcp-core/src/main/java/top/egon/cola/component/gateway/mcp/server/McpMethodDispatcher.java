package top.egon.cola.component.gateway.mcp.server;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcError;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.telemetry.McpTelemetry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code McpMethodDispatcher} 是分发器，位于当前 Gateway 模块的相关包中，负责MCP方法分发器相关的职责与边界。
 * English summary: {@code McpMethodDispatcher} is a mcp method dispatcher dispatcher in the current Gateway module; it owns the mcp method dispatcher-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpMethodDispatcher {

    /**
     * 中文说明：保存 handlers 对应的状态、依赖或配置值；字段类型为 {@code Map<String, McpMethodHandler>}，由 {@code McpMethodDispatcher} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by handlers; its type is {@code Map<String, McpMethodHandler>}, and {@code McpMethodDispatcher} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpMethodDispatcher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpMethodDispatcher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<String, McpMethodHandler> handlers;

    /**
     * 中文说明：保存 遥测 对应的状态、依赖或配置值；字段类型为 {@code McpTelemetry}，由 {@code McpMethodDispatcher} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by telemetry; its type is {@code McpTelemetry}, and {@code McpMethodDispatcher} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpMethodDispatcher} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpMethodDispatcher}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpTelemetry telemetry;

    /**
     * 中文说明：创建 {@code McpMethodDispatcher} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpMethodDispatcher} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param handlers 参数 handlers；parameter handlers。
     */
    public McpMethodDispatcher(List<McpMethodHandler> handlers) {
        this(handlers, McpTelemetry.noop());
    }

    /**
     * 中文说明：创建 {@code McpMethodDispatcher} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpMethodDispatcher} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param handlers 参数 handlers；parameter handlers。
     * @param telemetry 参数 遥测；parameter telemetry。
     */
    public McpMethodDispatcher(
            List<McpMethodHandler> handlers,
            McpTelemetry telemetry) {
        LinkedHashMap<String, McpMethodHandler> indexed = new LinkedHashMap<>();
        Objects.requireNonNull(handlers, "handlers").stream()
                .sorted(java.util.Comparator.comparing(McpMethodHandler::method))
                .forEach(handler -> {
                    String method = required(handler.method());
                    if (indexed.putIfAbsent(method, handler) != null) {
                        throw new IllegalArgumentException(
                                "duplicate MCP method handler: " + method
                        );
                    }
                });
        this.handlers = Collections.unmodifiableMap(indexed);
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
    }

    /**
     * 中文说明：执行 dispatch 操作；该方法是 {@code McpMethodDispatcher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dispatch operation; this method is the invocation entry point on {@code McpMethodDispatcher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpMethodDispatcher.dispatch(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param context 参数 context；parameter context。
     * @return 返回 dispatch 的处理结果；returns the result of the operation.
     */
    public Publisher<McpJsonRpcResponse> dispatch(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(context, "context");
        McpTelemetry.Scope observation = McpTelemetry.startSafely(
                telemetry,
                telemetryRequest(request, context)
        );
        McpMethodHandler handler = handlers.get(request.method());
        if (handler == null) {
            observation.failure(McpErrorCode.MCP_METHOD_NOT_FOUND.name());
            return request.notification()
                    ? Flux.empty()
                    : Mono.just(McpJsonRpcResponse.methodNotFound(request.id()));
        }

        Flux<McpJsonRpcResponse> responses;
        try {
            LinkedHashMap<String, Object> attributes = new LinkedHashMap<>(
                    context.attributes()
            );
            attributes.put(McpTelemetry.SCOPE_ATTRIBUTE, observation);
            McpRequestContext observedContext = new McpRequestContext(
                    context.server(),
                    context.dialect(),
                    context.sessionId(),
                    Map.copyOf(attributes)
            );
            Publisher<McpJsonRpcResponse> result = Objects.requireNonNull(
                    handler.handle(request, observedContext),
                    "handler result"
            );
            responses = Flux.from(result)
                    .onErrorResume(
                            McpProtocolException.class,
                            error -> Mono.just(error.toResponse(request.id()))
                    )
                    .onErrorResume(
                            error -> Mono.just(internalError(request.id()))
                    );
        } catch (McpProtocolException error) {
            responses = Flux.just(error.toResponse(request.id()));
        } catch (RuntimeException error) {
            responses = Flux.just(internalError(request.id()));
        }
        Flux<McpJsonRpcResponse> observed = responses
                .doOnNext(response -> {
                    if (response.error() == null) {
                        observation.success();
                    } else {
                        observation.failure(
                                response.error().dataCode().name()
                        );
                    }
                })
                .doOnError(ignored -> observation.failure(
                        McpErrorCode.MCP_INTERNAL_ERROR.name()
                ))
                .doOnComplete(observation::success)
                .doOnCancel(() -> observation.failure("CANCELLED"));
        return request.notification()
                ? observed.thenMany(Flux.empty())
                : observed;
    }

    /**
     * 中文说明：执行 遥测请求 操作；该方法是 {@code McpMethodDispatcher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the telemetry request operation; this method is the invocation entry point on {@code McpMethodDispatcher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpMethodDispatcher.telemetryRequest(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param context 参数 context；parameter context。
     * @return 返回 遥测请求 的处理结果；returns the result of the operation.
     */
    private McpTelemetry.Request telemetryRequest(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        Object remote = context.attributes().get("mcp.remote-provider");
        return new McpTelemetry.Request(
                request.method(),
                primitive(request.method()),
                context.server().serverCode(),
                remote instanceof String value && !value.isBlank()
                        ? value
                        : null,
                context.attributes()
        );
    }

    /**
     * 中文说明：执行 primitive 操作；该方法是 {@code McpMethodDispatcher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the primitive operation; this method is the invocation entry point on {@code McpMethodDispatcher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpMethodDispatcher.primitive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param method 参数 方法；parameter method。
     * @return 返回 primitive 的处理结果；returns the result of the operation.
     */
    private String primitive(String method) {
        if (method.startsWith("tools/")) {
            return "TOOL";
        }
        if (method.startsWith("resources/")) {
            return method.contains("subscribe")
                    ? "SUBSCRIPTION"
                    : "RESOURCE";
        }
        if (method.startsWith("subscriptions/")) {
            return "SUBSCRIPTION";
        }
        if (method.startsWith("prompts/")) {
            return "PROMPT";
        }
        if (method.startsWith("completion/")) {
            return "COMPLETION";
        }
        if (method.startsWith("tasks/")) {
            return "TASK";
        }
        if ("initialize".equals(method)
                || "notifications/initialized".equals(method)
                || "server/discover".equals(method)
                || "ping".equals(method)) {
            return "LIFECYCLE";
        }
        return "UNKNOWN";
    }

    /**
     * 中文说明：执行 internalError 操作；该方法是 {@code McpMethodDispatcher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the internal error operation; this method is the invocation entry point on {@code McpMethodDispatcher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpMethodDispatcher.internalError(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 internalError 的处理结果；returns the result of the operation.
     */
    private McpJsonRpcResponse internalError(Object id) {
        return McpJsonRpcResponse.failure(
                id,
                McpJsonRpcError.of(
                        McpErrorCode.MCP_INTERNAL_ERROR,
                        "MCP request processing failed"
                )
        );
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code McpMethodDispatcher} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code McpMethodDispatcher} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpMethodDispatcher.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param method 参数 方法；parameter method。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private String required(String method) {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("MCP handler method is required");
        }
        return method.trim();
    }
}
