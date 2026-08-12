package top.egon.cola.component.gateway.mcp.task;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.mcp.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.server.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.McpRequestContext;
import top.egon.cola.component.gateway.mcp.telemetry.McpTelemetry;

import java.util.Objects;

/**
 * 中文说明：{@code McpTasksCancelHandler} 是处理器，位于当前 Gateway 模块的相关包中，负责MCPTasksCancel处理器相关的职责与边界。
 * English summary: {@code McpTasksCancelHandler} is a mcp tasks cancel handler handler in the current Gateway module; it owns the mcp tasks cancel handler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpTasksCancelHandler implements McpMethodHandler {

    /**
     * 中文说明：保存 tasks 对应的状态、依赖或配置值；字段类型为 {@code McpTaskService}，由 {@code McpTasksCancelHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by tasks; its type is {@code McpTaskService}, and {@code McpTasksCancelHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTasksCancelHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTasksCancelHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpTaskService tasks;

    /**
     * 中文说明：保存 安全 对应的状态、依赖或配置值；字段类型为 {@code McpSecurityGate}，由 {@code McpTasksCancelHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by security; its type is {@code McpSecurityGate}, and {@code McpTasksCancelHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTasksCancelHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTasksCancelHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpSecurityGate security;

    /**
     * 中文说明：创建 {@code McpTasksCancelHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpTasksCancelHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param tasks 参数 tasks；parameter tasks。
     * @param security 参数 安全；parameter security。
     */
    public McpTasksCancelHandler(
            McpTaskService tasks,
            McpSecurityGate security) {
        this.tasks = Objects.requireNonNull(tasks, "tasks");
        this.security = Objects.requireNonNull(security, "security");
    }

    /**
     * 中文说明：执行 方法 操作；该方法是 {@code McpTasksCancelHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the method operation; this method is the invocation entry point on {@code McpTasksCancelHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTasksCancelHandler.method(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 方法 的处理结果；returns the result of the operation.
     */
    @Override
    public String method() {
        return "tasks/cancel";
    }

    /**
     * 中文说明：执行 handle 操作；该方法是 {@code McpTasksCancelHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the handle operation; this method is the invocation entry point on {@code McpTasksCancelHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTasksCancelHandler.handle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param context 参数 context；parameter context。
     * @return 返回 handle 的处理结果；returns the result of the operation.
     */
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
        Publisher<McpTask> cancel = Mono.from(tasks.get(
                        taskId,
                        identity.owner()
                ))
                .flatMap(task -> Mono.from(security.authorizeTaskAction(
                                task.serverCode(),
                                task.toolName(),
                                "cancel",
                                identity.security()
                        ))
                        .then(Mono.from(tasks.cancel(
                                taskId,
                                identity.owner()
                        ))));
        return Mono.from(McpTelemetry.observeChild(
                        context.attributes(),
                        McpTelemetry.ChildKind.TASK,
                        cancel
                ))
                .map(task -> McpJsonRpcResponse.success(
                        request.id(),
                        McpTasksGetHandler.describe(task)
                ));
    }
}
