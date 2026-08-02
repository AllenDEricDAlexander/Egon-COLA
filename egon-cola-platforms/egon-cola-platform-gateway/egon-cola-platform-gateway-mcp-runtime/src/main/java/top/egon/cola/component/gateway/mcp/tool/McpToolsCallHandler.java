package top.egon.cola.component.gateway.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTaskPolicy;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvocation;
import top.egon.cola.component.gateway.core.operation.GatewayOperationInvoker;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.remote.RemoteMcpToolDriver;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.security.McpSecurityDigests;
import top.egon.cola.component.gateway.mcp.server.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;
import top.egon.cola.component.gateway.mcp.task.McpTaskService;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class McpToolsCallHandler implements McpMethodHandler {

    private final McpToolCatalog catalog;

    private final McpArgumentBinder argumentBinder;

    private final McpResultBinder resultBinder;

    private final GatewayOperationInvoker invoker;

    private final McpSecurityGate securityGate;

    private final ObjectMapper objectMapper;

    private final McpTaskService taskService;

    private final Supplier<CompiledMcpRules> rules;

    private final RemoteMcpToolDriver remote;

    public McpToolsCallHandler(
            McpToolCatalog catalog,
            McpArgumentBinder argumentBinder,
            McpResultBinder resultBinder,
            GatewayOperationInvoker invoker,
            McpSecurityGate securityGate) {
        this(
                catalog,
                argumentBinder,
                resultBinder,
                invoker,
                securityGate,
                new ObjectMapper(),
                null,
                () -> null,
                null
        );
    }

    public McpToolsCallHandler(
            McpToolCatalog catalog,
            McpArgumentBinder argumentBinder,
            McpResultBinder resultBinder,
            GatewayOperationInvoker invoker,
            McpSecurityGate securityGate,
            ObjectMapper objectMapper,
            McpTaskService taskService,
            Supplier<CompiledMcpRules> rules) {
        this(
                catalog,
                argumentBinder,
                resultBinder,
                invoker,
                securityGate,
                objectMapper,
                taskService,
                rules,
                null
        );
    }

    public McpToolsCallHandler(
            McpToolCatalog catalog,
            McpArgumentBinder argumentBinder,
            McpResultBinder resultBinder,
            GatewayOperationInvoker invoker,
            McpSecurityGate securityGate,
            ObjectMapper objectMapper,
            McpTaskService taskService,
            Supplier<CompiledMcpRules> rules,
            RemoteMcpToolDriver remote) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.argumentBinder = Objects.requireNonNull(
                argumentBinder,
                "argumentBinder"
        );
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

    @Override
    public String method() {
        return "tools/call";
    }

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
        Map<String, Object> boundArguments = tool.remoteMountId() == null
                ? argumentBinder.bind(tool, arguments)
                : argumentBinder.bindRemote(arguments);
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
                                        boundArguments,
                                        identity,
                                        context.dialect(),
                                        request.meta(),
                                        traceHeaders(context)
                                ))
                                .map(result -> McpJsonRpcResponse.success(
                                        request.id(),
                                        result
                                ));
                    }
                    GatewayOperationInvocation invocation =
                            new GatewayOperationInvocation(
                                    tool.operationId(),
                                    boundArguments,
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
                        return Mono.from(invoker.invoke(invocation))
                                .map(result -> McpJsonRpcResponse.success(
                                        request.id(),
                                        resultBinder.bind(tool, result)
                                ));
                    }
                    if (taskService == null) {
                        return Mono.error(new McpProtocolException(
                                McpErrorCode.MCP_INTERNAL_ERROR,
                                "MCP durable task store is unavailable"
                        ));
                    }
                    return Mono.from(taskService.create(
                                    new McpTaskService.CreateRequest(
                                            tool.serverCode(),
                                            tool.name(),
                                            McpSecurityDigests.arguments(
                                                    objectMapper,
                                                    arguments
                                            ),
                                            Map.of(
                                                    "operationId",
                                                    tool.operationId(),
                                                    "arguments",
                                                    boundArguments
                                            ),
                                            seconds(
                                                    policy
                                                            .executionTimeoutSeconds(),
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

    private Duration seconds(long configured, long fallback) {
        return Duration.ofSeconds(configured == 0L ? fallback : configured);
    }

    private String approvalToken(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        Object value = request.meta().get("approvalToken");
        if (value instanceof String token && !token.isBlank()) {
            return token.trim();
        }
        return attribute(context, "mcpApprovalToken");
    }

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

    private Map<String, String> traceHeaders(McpRequestContext context) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        copyAttribute(context, values, "traceparent");
        copyAttribute(context, values, "tracestate");
        copyAttribute(context, values, "x-egon-request-id");
        return Map.copyOf(values);
    }

    private void copyAttribute(
            McpRequestContext context,
            Map<String, String> target,
            String name) {
        String value = attribute(context, name);
        if (value != null) {
            target.put(name, value);
        }
    }

    private String attribute(McpRequestContext context, String name) {
        Object value = context.attributes().get(name);
        return value instanceof String text && !text.isBlank()
                ? text.trim()
                : null;
    }

    private String string(Object value, String field) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw invalid("MCP " + field + " is required");
        }
        return text.trim();
    }

    private McpProtocolException invalid(String message) {
        return new McpProtocolException(
                McpErrorCode.MCP_INVALID_PARAMS,
                message
        );
    }
}
