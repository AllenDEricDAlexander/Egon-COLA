package top.egon.cola.component.gateway.mcp.task.service;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcRequest;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpJsonRpcResponse;
import top.egon.cola.component.gateway.mcp.common.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.common.security.McpSecurityGate;
import top.egon.cola.component.gateway.mcp.server.service.McpMethodHandler;
import top.egon.cola.component.gateway.mcp.server.domain.McpRequestContext;
import top.egon.cola.component.gateway.mcp.task.domain.McpTask;
import top.egon.cola.component.gateway.mcp.common.telemetry.McpTelemetry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code McpTasksUpdateHandler} 是处理器，位于当前 Gateway 模块的相关包中，负责MCPTasksUpdate处理器相关的职责与边界。
 * English summary: {@code McpTasksUpdateHandler} is a mcp tasks update handler handler in the current Gateway module; it owns the mcp tasks update handler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpTasksUpdateHandler implements McpMethodHandler {

    /**
     * 中文说明：保存 tasks 对应的状态、依赖或配置值；字段类型为 {@code McpTaskService}，由 {@code McpTasksUpdateHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by tasks; its type is {@code McpTaskService}, and {@code McpTasksUpdateHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTasksUpdateHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTasksUpdateHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpTaskService tasks;

    /**
     * 中文说明：保存 安全 对应的状态、依赖或配置值；字段类型为 {@code McpSecurityGate}，由 {@code McpTasksUpdateHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by security; its type is {@code McpSecurityGate}, and {@code McpTasksUpdateHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTasksUpdateHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTasksUpdateHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpSecurityGate security;

    /**
     * 中文说明：创建 {@code McpTasksUpdateHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpTasksUpdateHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param tasks 参数 tasks；parameter tasks。
     * @param security 参数 安全；parameter security。
     */
    public McpTasksUpdateHandler(
            McpTaskService tasks,
            McpSecurityGate security) {
        this.tasks = Objects.requireNonNull(tasks, "tasks");
        this.security = Objects.requireNonNull(security, "security");
    }

    /**
     * 中文说明：执行 方法 操作；该方法是 {@code McpTasksUpdateHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the method operation; this method is the invocation entry point on {@code McpTasksUpdateHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTasksUpdateHandler.method(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 方法 的处理结果；returns the result of the operation.
     */
    @Override
    public String method() {
        return "tasks/update";
    }

    /**
     * 中文说明：执行 handle 操作；该方法是 {@code McpTasksUpdateHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the handle operation; this method is the invocation entry point on {@code McpTasksUpdateHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTasksUpdateHandler.handle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
        String key = McpTasksGetHandler.string(
                request.params().get("inputRequestKey"),
                "inputRequestKey"
        );
        Map<String, Object> input = input(request.params().get("input"));
        Publisher<McpTask> update = Mono.from(tasks.get(
                        taskId,
                        identity.owner()
                ))
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
                        ))));
        return Mono.from(McpTelemetry.observeChild(
                        context.attributes(),
                        McpTelemetry.ChildKind.TASK,
                        update
                ))
                .map(task -> McpJsonRpcResponse.success(
                        request.id(),
                        McpTasksGetHandler.describe(task)
                ));
    }

    /**
     * 中文说明：执行 input 操作；该方法是 {@code McpTasksUpdateHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the input operation; this method is the invocation entry point on {@code McpTasksUpdateHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTasksUpdateHandler.input(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 input 的处理结果；returns the result of the operation.
     */
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
