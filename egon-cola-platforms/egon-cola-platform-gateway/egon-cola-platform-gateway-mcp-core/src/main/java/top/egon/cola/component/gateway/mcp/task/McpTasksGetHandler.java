package top.egon.cola.component.gateway.mcp.task;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.server.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;
import top.egon.cola.component.gateway.mcp.telemetry.McpTelemetry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class McpTasksGetHandler implements McpMethodHandler {

    private final McpTaskService tasks;

    private final McpSecurityGate security;

    public McpTasksGetHandler(
            McpTaskService tasks,
            McpSecurityGate security) {
        this.tasks = Objects.requireNonNull(tasks, "tasks");
        this.security = Objects.requireNonNull(security, "security");
    }

    @Override
    public String method() {
        return "tasks/get";
    }

    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        Identity identity = identity(context);
        Publisher<McpTask> get = tasks.get(
                string(request.params().get("taskId"), "taskId"),
                identity.owner()
        );
        return Mono.from(McpTelemetry.observeChild(
                        context.attributes(),
                        McpTelemetry.ChildKind.TASK,
                        get
                ))
                .flatMap(task -> Mono.from(security.authorizeTaskAction(
                                task.serverCode(),
                                task.toolName(),
                                "get",
                                identity.security()
                        ))
                        .thenReturn(task))
                .map(task -> McpJsonRpcResponse.success(
                        request.id(),
                        describe(task)
                ));
    }

    public static Map<String, Object> describe(McpTask task) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("taskId", task.id());
        value.put("status", task.state().name().toLowerCase(
                java.util.Locale.ROOT
        ));
        value.put("createdAt", task.createdAt().toString());
        value.put("lastUpdatedAt", task.updatedAt().toString());
        if (task.resultPayload() != null) {
            value.put("result", task.resultPayload());
        }
        if (task.errorPayload() != null) {
            value.put("error", task.errorPayload());
        }
        if (task.state() == McpTask.State.INPUT_REQUIRED) {
            value.put("inputRequestKey", task.inputPayload().get(
                    "inputRequestKey"
            ));
            value.put("inputRequest", task.inputPayload().getOrDefault(
                    "inputRequest",
                    Map.of()
            ));
        }
        return Map.copyOf(value);
    }

    static Identity identity(McpRequestContext context) {
        try {
            McpSecurityGate.IdentityContext security =
                    McpSecurityGate.IdentityContext.from(
                            context.attributes()
                    );
            return new Identity(
                    security,
                    new McpTaskService.Owner(
                            security.subjectId(),
                            security.tenantId(),
                            security.clientId()
                    )
            );
        } catch (IllegalArgumentException failure) {
            throw new McpProtocolException(
                    McpErrorCode.MCP_UNAUTHENTICATED,
                    "MCP identity context is incomplete"
            );
        }
    }

    static String string(Object value, String field) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw new McpProtocolException(
                    McpErrorCode.MCP_INVALID_PARAMS,
                    "MCP task " + field + " is required"
            );
        }
        return text.trim();
    }

    record Identity(
            McpSecurityGate.IdentityContext security,
            McpTaskService.Owner owner
    ) {
    }
}
