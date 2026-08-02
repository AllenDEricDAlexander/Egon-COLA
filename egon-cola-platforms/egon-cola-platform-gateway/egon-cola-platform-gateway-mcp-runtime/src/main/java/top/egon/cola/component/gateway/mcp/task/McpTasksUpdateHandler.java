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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class McpTasksUpdateHandler implements McpMethodHandler {

    private final McpTaskService tasks;

    private final McpSecurityGate security;

    public McpTasksUpdateHandler(
            McpTaskService tasks,
            McpSecurityGate security) {
        this.tasks = Objects.requireNonNull(tasks, "tasks");
        this.security = Objects.requireNonNull(security, "security");
    }

    @Override
    public String method() {
        return "tasks/update";
    }

    @Override
    public Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context) {
        McpTasksGetHandler.Identity identity =
                McpTasksGetHandler.identity(context);
        String taskId = McpTasksGetHandler.string(
                request.params().get("taskId"),
                "taskId"
        );
        String key = McpTasksGetHandler.string(
                request.params().get("inputRequestKey"),
                "inputRequestKey"
        );
        Map<String, Object> input = input(request.params().get("input"));
        return Mono.from(tasks.get(taskId, identity.owner()))
                .flatMap(task -> Mono.from(security.authorizeTaskAction(
                                task.serverCode(),
                                task.toolName(),
                                "update",
                                identity.security()
                        ))
                        .then(Mono.from(tasks.provideInput(
                                taskId,
                                key,
                                input,
                                identity.owner()
                        ))))
                .map(task -> McpJsonRpcResponse.success(
                        request.id(),
                        McpTasksGetHandler.describe(task)
                ));
    }

    private Map<String, Object> input(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            throw new McpProtocolException(
                    McpErrorCode.MCP_INVALID_PARAMS,
                    "MCP task input must be an object"
            );
        }
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, item) -> {
            if (!(key instanceof String name)) {
                throw new McpProtocolException(
                        McpErrorCode.MCP_INVALID_PARAMS,
                        "MCP task input names must be strings"
                );
            }
            result.put(name, item);
        });
        return Map.copyOf(result);
    }
}
