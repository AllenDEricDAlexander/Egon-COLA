package top.egon.cola.component.gateway.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTaskPolicy;
import top.egon.cola.component.gateway.core.operation.GatewayOperationCall;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvocation;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvoker;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.remote.RemoteMcpToolDriver;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.security.McpSecurityDigests;
import top.egon.cola.component.gateway.mcp.server.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;
import top.egon.cola.component.gateway.mcp.task.McpTask;
import top.egon.cola.component.gateway.mcp.task.McpTaskService;
import top.egon.cola.component.gateway.mcp.telemetry.McpTelemetry;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 中文说明：{@code McpToolsCallHandler} 是处理器，位于当前 Gateway 模块的相关包中，负责MCPTools调用处理器相关的职责与边界。
 * English summary: {@code McpToolsCallHandler} is a mcp tools call handler handler in the current Gateway module; it owns the mcp tools call handler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpToolsCallHandler implements McpMethodHandler {

    /**
     * 中文说明：表示 FORBIDDEN远程ARGUMENTS 这一固定值；它属于 {@code McpToolsCallHandler} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value forbidden remote arguments; it is a state, type, or protocol value of {@code McpToolsCallHandler} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolsCallHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolsCallHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Set<String> FORBIDDEN_REMOTE_ARGUMENTS = Set.of(
            "operationid",
            "providerurl",
            "routeid",
            "servicename",
            "authorization",
            "tlsprofile"
    );

    /**
     * 中文说明：保存 目录 对应的状态、依赖或配置值；字段类型为 {@code McpToolCatalog}，由 {@code McpToolsCallHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by catalog; its type is {@code McpToolCatalog}, and {@code McpToolsCallHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolsCallHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolsCallHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpToolCatalog catalog;

    /**
     * 中文说明：保存 resultBinder 对应的状态、依赖或配置值；字段类型为 {@code McpResultBinder}，由 {@code McpToolsCallHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by result binder; its type is {@code McpResultBinder}, and {@code McpToolsCallHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolsCallHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolsCallHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpResultBinder resultBinder;

    /**
     * 中文说明：保存 invoker 对应的状态、依赖或配置值；字段类型为 {@code GatewayOperationInvoker}，由 {@code McpToolsCallHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by invoker; its type is {@code GatewayOperationInvoker}, and {@code McpToolsCallHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolsCallHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolsCallHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayOperationInvoker invoker;

    /**
     * 中文说明：保存 安全Gate 对应的状态、依赖或配置值；字段类型为 {@code McpSecurityGate}，由 {@code McpToolsCallHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by security gate; its type is {@code McpSecurityGate}, and {@code McpToolsCallHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolsCallHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolsCallHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpSecurityGate securityGate;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code McpToolsCallHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code McpToolsCallHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolsCallHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolsCallHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：保存 任务服务 对应的状态、依赖或配置值；字段类型为 {@code McpTaskService}，由 {@code McpToolsCallHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by task service; its type is {@code McpTaskService}, and {@code McpToolsCallHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolsCallHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolsCallHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpTaskService taskService;

    /**
     * 中文说明：保存 rules 对应的状态、依赖或配置值；字段类型为 {@code Supplier<CompiledMcpRules>}，由 {@code McpToolsCallHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rules; its type is {@code Supplier<CompiledMcpRules>}, and {@code McpToolsCallHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolsCallHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolsCallHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<CompiledMcpRules> rules;

    /**
     * 中文说明：保存 远程 对应的状态、依赖或配置值；字段类型为 {@code RemoteMcpToolDriver}，由 {@code McpToolsCallHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by remote; its type is {@code RemoteMcpToolDriver}, and {@code McpToolsCallHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolsCallHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolsCallHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final RemoteMcpToolDriver remote;

    /**
     * 中文说明：创建 {@code McpToolsCallHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpToolsCallHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param catalog 参数 目录；parameter catalog。
     * @param resultBinder 参数 resultBinder；parameter result binder。
     * @param invoker 参数 invoker；parameter invoker。
     * @param securityGate 参数 安全Gate；parameter security gate。
     */
    public McpToolsCallHandler(
            McpToolCatalog catalog,
            McpResultBinder resultBinder,
            GatewayOperationInvoker invoker,
            McpSecurityGate securityGate) {
        this(
                catalog,
                resultBinder,
                invoker,
                securityGate,
                new ObjectMapper(),
                null,
                () -> null,
                null
        );
    }

    /**
     * 中文说明：创建 {@code McpToolsCallHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpToolsCallHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param catalog 参数 目录；parameter catalog。
     * @param resultBinder 参数 resultBinder；parameter result binder。
     * @param invoker 参数 invoker；parameter invoker。
     * @param securityGate 参数 安全Gate；parameter security gate。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param taskService 参数 任务服务；parameter task service。
     * @param rules 参数 rules；parameter rules。
     */
    public McpToolsCallHandler(
            McpToolCatalog catalog,
            McpResultBinder resultBinder,
            GatewayOperationInvoker invoker,
            McpSecurityGate securityGate,
            ObjectMapper objectMapper,
            McpTaskService taskService,
            Supplier<CompiledMcpRules> rules) {
        this(
                catalog,
                resultBinder,
                invoker,
                securityGate,
                objectMapper,
                taskService,
                rules,
                null
        );
    }

    /**
     * 中文说明：创建 {@code McpToolsCallHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpToolsCallHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param catalog 参数 目录；parameter catalog。
     * @param resultBinder 参数 resultBinder；parameter result binder。
     * @param invoker 参数 invoker；parameter invoker。
     * @param securityGate 参数 安全Gate；parameter security gate。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param taskService 参数 任务服务；parameter task service。
     * @param rules 参数 rules；parameter rules。
     * @param remote 参数 远程；parameter remote。
     */
    public McpToolsCallHandler(
            McpToolCatalog catalog,
            McpResultBinder resultBinder,
            GatewayOperationInvoker invoker,
            McpSecurityGate securityGate,
            ObjectMapper objectMapper,
            McpTaskService taskService,
            Supplier<CompiledMcpRules> rules,
            RemoteMcpToolDriver remote) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.resultBinder = Objects.requireNonNull(
                resultBinder,
                "resultBinder"
        );
        this.invoker = Objects.requireNonNull(invoker, "invoker");
        this.securityGate = Objects.requireNonNull(
                securityGate,
                "securityGate"
        );
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
        this.taskService = taskService;
        this.rules = Objects.requireNonNull(rules, "rules");
        this.remote = remote;
    }

    /**
     * 中文说明：执行 方法 操作；该方法是 {@code McpToolsCallHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the method operation; this method is the invocation entry point on {@code McpToolsCallHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolsCallHandler.method(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 方法 的处理结果；returns the result of the operation.
     */
    @Override
    public String method() {
        return "tools/call";
    }

    /**
     * 中文说明：执行 handle 操作；该方法是 {@code McpToolsCallHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the handle operation; this method is the invocation entry point on {@code McpToolsCallHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolsCallHandler.handle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param context 参数 context；parameter context。
     * @return 返回 handle 的处理结果；returns the result of the operation.
     */
    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        String name = string(request.params().get("name"), "name");
        McpRuntimeTool tool = catalog.tool(
                context.server().serverCode(),
                name
        ).orElseThrow(() -> invalid("MCP tool was not found"));
        Map<String, Object> arguments = arguments(
                request.params().get("arguments")
        );
        McpSecurityGate.IdentityContext identity;
        try {
            identity = McpSecurityGate.IdentityContext.from(
                    context.attributes()
            );
        } catch (IllegalArgumentException exception) {
            throw new McpProtocolException(
                    McpErrorCode.MCP_UNAUTHENTICATED,
                    "MCP identity context is incomplete"
            );
        }
        Map<String, Object> remoteArguments = tool.remoteMountId() == null
                ? Map.of()
                : remoteArguments(arguments);
        return Mono.from(securityGate.authorizeToolCall(
                        tool,
                        identity,
                        arguments,
                        approvalToken(request, context)
                ))
                .then(Mono.defer(() -> {
                    if (tool.remoteMountId() != null) {
                        if (remote == null) {
                            return Mono.error(new McpProtocolException(
                                    McpErrorCode.MCP_REMOTE_UNAVAILABLE,
                                    "remote MCP Tool driver is unavailable"
                            ));
                        }
                        return Mono.from(remote.invoke(
                                        tool,
                                        remoteArguments,
                                        identity,
                                        context.dialect(),
                                        request.meta(),
                                        traceHeaders(context),
                                        McpTelemetry.current(
                                                context.attributes()
                                        )
                                ))
                                .map(result -> McpJsonRpcResponse.success(
                                        request.id(),
                                        result
                                ));
                    }
                    GatewayOperationCall operationCall = operationCall(
                            tool,
                            arguments
                    );
                    GatewayOperationInvocation invocation =
                            new GatewayOperationInvocation(
                                    operationCall,
                                    attribute(
                                            context,
                                            "originalBearerToken"
                                    ),
                                    attribute(context, "callerId"),
                                    attribute(context, "clientIp"),
                                    traceHeaders(context)
                            );
                    McpRuntimeTaskPolicy policy = taskPolicy(tool);
                    if (policy == null) {
                        return Mono.from(McpTelemetry.observeChild(
                                        context.attributes(),
                                        McpTelemetry.ChildKind.OPERATION,
                                        invoker.invoke(invocation)
                                ))
                                .map(result -> McpJsonRpcResponse.success(
                                        request.id(),
                                        resultBinder.bind(result)
                                ));
                    }
                    if (taskService == null) {
                        return Mono.error(new McpProtocolException(
                                McpErrorCode.MCP_INTERNAL_ERROR,
                                "MCP durable task store is unavailable"
                        ));
                    }
                    Publisher<McpTask> create = taskService.create(
                            new McpTaskService.CreateRequest(
                                    tool.serverCode(),
                                    tool.name(),
                                    McpSecurityDigests.arguments(
                                            objectMapper,
                                            arguments
                                    ),
                                    taskInput(operationCall),
                                    seconds(
                                            policy.executionTimeoutSeconds(),
                                            300L
                                    ),
                                    seconds(
                                            policy.resultTtlSeconds(),
                                            86_400L
                                    ),
                                    policy.maxAttempts()
                            ),
                            new McpTaskService.Owner(
                                    identity.subjectId(),
                                    identity.tenantId(),
                                    identity.clientId()
                            )
                    );
                    return Mono.from(McpTelemetry.observeChild(
                                    context.attributes(),
                                    McpTelemetry.ChildKind.TASK,
                                    create
                            ))
                            .map(task -> McpJsonRpcResponse.success(
                                    request.id(),
                                    Map.of("task", Map.of(
                                            "taskId", task.id(),
                                            "status", "working",
                                            "createdAt",
                                            task.createdAt().toString(),
                                            "lastUpdatedAt",
                                            task.updatedAt().toString()
                                    ))
                            ));
                }));
    }

    /**
     * 中文说明：执行 任务策略 操作；该方法是 {@code McpToolsCallHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the task policy operation; this method is the invocation entry point on {@code McpToolsCallHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolsCallHandler.taskPolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param tool 参数 工具；parameter tool。
     * @return 返回 任务策略 的处理结果；returns the result of the operation.
     */
    private McpRuntimeTaskPolicy taskPolicy(McpRuntimeTool tool) {
        CompiledMcpRules active = rules.get();
        if (active == null) {
            return null;
        }
        McpRuntimeTaskPolicy policy = active
                .taskPoliciesByQualifiedTool()
                .get(CompiledMcpRules.qualified(
                        tool.serverCode(),
                        tool.name()
                ));
        return policy != null && policy.enabled() && policy.durable()
                ? policy
                : null;
    }

    /**
     * 中文说明：执行 操作调用 操作；该方法是 {@code McpToolsCallHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the operation call operation; this method is the invocation entry point on {@code McpToolsCallHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolsCallHandler.operationCall(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param tool 参数 工具；parameter tool。
     * @param arguments 参数 arguments；parameter arguments。
     * @return 返回 操作调用 的处理结果；returns the result of the operation.
     */
    private GatewayOperationCall operationCall(
            McpRuntimeTool tool,
            Map<String, Object> arguments) {
        if ("RPC".equals(tool.operationProtocol())) {
            return new GatewayOperationCall(
                    tool.operationId(),
                    Map.of(),
                    Map.of(),
                    arguments
            );
        }
        if (!"HTTP".equals(tool.operationProtocol())) {
            throw invalid("MCP local Tool protocol is invalid");
        }
        Set<String> allowed = Set.of("path", "query", "body");
        if (!allowed.containsAll(arguments.keySet())) {
            throw invalid(
                    "MCP Tool arguments may only contain path, query, and body"
            );
        }
        Map<String, Object> path = locationArguments(arguments, "path");
        Map<String, Object> query = locationArguments(arguments, "query");
        return new GatewayOperationCall(
                tool.operationId(),
                path,
                query,
                arguments.get("body")
        );
    }

    /**
     * 中文说明：执行 locationArguments 操作；该方法是 {@code McpToolsCallHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the location arguments operation; this method is the invocation entry point on {@code McpToolsCallHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolsCallHandler.locationArguments(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param arguments 参数 arguments；parameter arguments。
     * @param location 参数 location；parameter location。
     * @return 返回 locationArguments 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> locationArguments(
            Map<String, Object> arguments,
            String location) {
        Object value = arguments.get(location);
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> source)) {
            throw invalid("MCP Tool " + location + " arguments must be an object");
        }
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, item) -> {
            if (!(key instanceof String name) || name.isBlank()) {
                throw invalid("MCP Tool " + location
                        + " argument names must be non-blank strings");
            }
            result.put(name, item);
        });
        return Map.copyOf(result);
    }

    /**
     * 中文说明：执行 远程Arguments 操作；该方法是 {@code McpToolsCallHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the remote arguments operation; this method is the invocation entry point on {@code McpToolsCallHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolsCallHandler.remoteArguments(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param arguments 参数 arguments；parameter arguments。
     * @return 返回 远程Arguments 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> remoteArguments(
            Map<String, Object> arguments) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        arguments.forEach((name, value) -> {
            String normalized = name.replace("_", "")
                    .replace("-", "")
                    .toLowerCase(Locale.ROOT);
            if (!name.isBlank()
                    && !FORBIDDEN_REMOTE_ARGUMENTS.contains(normalized)) {
                result.put(name, value);
            }
        });
        return Map.copyOf(result);
    }

    /**
     * 中文说明：执行 任务Input 操作；该方法是 {@code McpToolsCallHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the task input operation; this method is the invocation entry point on {@code McpToolsCallHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolsCallHandler.taskInput(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param call 参数 调用；parameter call。
     * @return 返回 任务Input 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> taskInput(GatewayOperationCall call) {
        LinkedHashMap<String, Object> input = new LinkedHashMap<>();
        input.put("operationId", call.operationId());
        input.put("pathArguments", call.pathArguments());
        input.put("queryArguments", call.queryArguments());
        if (call.body() != null) {
            input.put("body", call.body());
        }
        return Map.copyOf(input);
    }

    /**
     * 中文说明：执行 seconds 操作；该方法是 {@code McpToolsCallHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the seconds operation; this method is the invocation entry point on {@code McpToolsCallHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolsCallHandler.seconds(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param configured 参数 configured；parameter configured。
     * @param fallback 参数 fallback；parameter fallback。
     * @return 返回 seconds 的处理结果；returns the result of the operation.
     */
    private Duration seconds(long configured, long fallback) {
        return Duration.ofSeconds(configured == 0L ? fallback : configured);
    }

    /**
     * 中文说明：执行 审批Token 操作；该方法是 {@code McpToolsCallHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the approval token operation; this method is the invocation entry point on {@code McpToolsCallHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolsCallHandler.approvalToken(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param context 参数 context；parameter context。
     * @return 返回 审批Token 的处理结果；returns the result of the operation.
     */
    private String approvalToken(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        Object value = request.meta().get("approvalToken");
        if (value instanceof String token && !token.isBlank()) {
            return token.trim();
        }
        return attribute(context, "mcpApprovalToken");
    }

    /**
     * 中文说明：执行 arguments 操作；该方法是 {@code McpToolsCallHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the arguments operation; this method is the invocation entry point on {@code McpToolsCallHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolsCallHandler.arguments(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 arguments 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> arguments(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> source)) {
            throw invalid("MCP tool arguments must be an object");
        }
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, item) -> {
            if (!(key instanceof String name)) {
                throw invalid("MCP tool argument names must be strings");
            }
            copy.put(name, item);
        });
        return java.util.Collections.unmodifiableMap(copy);
    }

    /**
     * 中文说明：执行 traceHeaders 操作；该方法是 {@code McpToolsCallHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the trace headers operation; this method is the invocation entry point on {@code McpToolsCallHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolsCallHandler.traceHeaders(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @return 返回 traceHeaders 的处理结果；returns the result of the operation.
     */
    private Map<String, String> traceHeaders(McpRequestContext context) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        copyAttribute(context, values, "traceparent");
        copyAttribute(context, values, "tracestate");
        copyAttribute(context, values, "x-egon-request-id");
        return Map.copyOf(values);
    }

    /**
     * 中文说明：执行 copyAttribute 操作；该方法是 {@code McpToolsCallHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the copy attribute operation; this method is the invocation entry point on {@code McpToolsCallHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolsCallHandler.copyAttribute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @param target 参数 target；parameter target。
     * @param name 参数 name；parameter name。
     */
    private void copyAttribute(
            McpRequestContext context,
            Map<String, String> target,
            String name) {
        String value = attribute(context, name);
        if (value != null) {
            target.put(name, value);
        }
    }

    /**
     * 中文说明：执行 attribute 操作；该方法是 {@code McpToolsCallHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the attribute operation; this method is the invocation entry point on {@code McpToolsCallHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolsCallHandler.attribute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @param name 参数 name；parameter name。
     * @return 返回 attribute 的处理结果；returns the result of the operation.
     */
    private String attribute(McpRequestContext context, String name) {
        Object value = context.attributes().get(name);
        return value instanceof String text && !text.isBlank()
                ? text.trim()
                : null;
    }

    /**
     * 中文说明：执行 string 操作；该方法是 {@code McpToolsCallHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the string operation; this method is the invocation entry point on {@code McpToolsCallHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolsCallHandler.string(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 string 的处理结果；returns the result of the operation.
     */
    private String string(Object value, String field) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw invalid("MCP " + field + " is required");
        }
        return text.trim();
    }

    /**
     * 中文说明：执行 invalid 操作；该方法是 {@code McpToolsCallHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invalid operation; this method is the invocation entry point on {@code McpToolsCallHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolsCallHandler.invalid(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param message 参数 消息；parameter message。
     * @return 返回 invalid 的处理结果；returns the result of the operation.
     */
    private McpProtocolException invalid(String message) {
        return new McpProtocolException(
                McpErrorCode.MCP_INVALID_PARAMS,
                message
        );
    }
}
