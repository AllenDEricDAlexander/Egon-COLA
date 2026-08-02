package top.egon.cola.component.gateway.mcp.task;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.server.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;

import java.util.Objects;

public final class McpTasksCancelHandler implements McpMethodHandler {

    private final McpTaskService tasks;

    private final McpSecurityGate security;

    public McpTasksCancelHandler(
            McpTaskService tasks,
            McpSecurityGate security) {
        this.tasks = Objects.requireNonNull(tasks, "tasks");
        this.security = Objects.requireNonNull(security, "security");
    }

    @Override
    public String method() {
        return "tasks/cancel";
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
        return Mono.from(tasks.get(taskId, identity.owner()))
                .flatMap(task -> Mono.from(security.authorizeTaskAction(
                                task.serverCode(),
                                task.toolName(),
                                "cancel",
                                identity.security()
                        ))
                        .then(Mono.from(tasks.cancel(
                                taskId,
                                identity.owner()
                        ))))
                .map(task -> McpJsonRpcResponse.success(
                        request.id(),
                        McpTasksGetHandler.describe(task)
                ));
    }
}
