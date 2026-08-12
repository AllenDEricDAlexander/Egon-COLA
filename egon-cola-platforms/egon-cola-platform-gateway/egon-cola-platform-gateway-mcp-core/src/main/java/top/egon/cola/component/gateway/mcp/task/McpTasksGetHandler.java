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

/**
 * 中文说明：{@code McpTasksGetHandler} 是处理器，位于当前 Gateway 模块的相关包中，负责MCPTasksGet处理器相关的职责与边界。
 * English summary: {@code McpTasksGetHandler} is a mcp tasks get handler handler in the current Gateway module; it owns the mcp tasks get handler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpTasksGetHandler implements McpMethodHandler {

    /**
     * 中文说明：保存 tasks 对应的状态、依赖或配置值；字段类型为 {@code McpTaskService}，由 {@code McpTasksGetHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by tasks; its type is {@code McpTaskService}, and {@code McpTasksGetHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTasksGetHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTasksGetHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpTaskService tasks;

    /**
     * 中文说明：保存 安全 对应的状态、依赖或配置值；字段类型为 {@code McpSecurityGate}，由 {@code McpTasksGetHandler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by security; its type is {@code McpSecurityGate}, and {@code McpTasksGetHandler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTasksGetHandler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTasksGetHandler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpSecurityGate security;

    /**
     * 中文说明：创建 {@code McpTasksGetHandler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpTasksGetHandler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param tasks 参数 tasks；parameter tasks。
     * @param security 参数 安全；parameter security。
     */
    public McpTasksGetHandler(
            McpTaskService tasks,
            McpSecurityGate security) {
        this.tasks = Objects.requireNonNull(tasks, "tasks");
        this.security = Objects.requireNonNull(security, "security");
    }

    /**
     * 中文说明：执行 方法 操作；该方法是 {@code McpTasksGetHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the method operation; this method is the invocation entry point on {@code McpTasksGetHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTasksGetHandler.method(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 方法 的处理结果；returns the result of the operation.
     */
    @Override
    public String method() {
        return "tasks/get";
    }

    /**
     * 中文说明：执行 handle 操作；该方法是 {@code McpTasksGetHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the handle operation; this method is the invocation entry point on {@code McpTasksGetHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTasksGetHandler.handle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param context 参数 context；parameter context。
     * @return 返回 handle 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 describe 操作；该方法是 {@code McpTasksGetHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the describe operation; this method is the invocation entry point on {@code McpTasksGetHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTasksGetHandler.describe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param task 参数 任务；parameter task。
     * @return 返回 describe 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 身份 操作；该方法是 {@code McpTasksGetHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the identity operation; this method is the invocation entry point on {@code McpTasksGetHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTasksGetHandler.identity(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @return 返回 身份 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 string 操作；该方法是 {@code McpTasksGetHandler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the string operation; this method is the invocation entry point on {@code McpTasksGetHandler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTasksGetHandler.string(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 string 的处理结果；returns the result of the operation.
     */
    static String string(Object value, String field) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw new McpProtocolException(
                    McpErrorCode.MCP_INVALID_PARAMS,
                    "MCP task " + field + " is required"
            );
        }
        return text.trim();
    }

    /**
     * 中文说明：{@code Identity} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责身份相关的职责与边界。
     * English summary: {@code Identity} is an immutable data carrier in the current Gateway module; it owns the identity-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param security 参数 安全；parameter security。
     * @param owner 参数 owner；parameter owner。
     */
    record Identity(
            /**
             * 中文说明：保存 安全 对应的状态、依赖或配置值；字段类型为 {@code McpSecurityGate.IdentityContext}，由 {@code McpTasksGetHandler.Identity} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by security; its type is {@code McpSecurityGate.IdentityContext}, and {@code McpTasksGetHandler.Identity} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTasksGetHandler.Identity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTasksGetHandler.Identity}; do not couple callers to its representation when the owning type exposes an API.
             */
            McpSecurityGate.IdentityContext security,
            /**
             * 中文说明：保存 owner 对应的状态、依赖或配置值；字段类型为 {@code McpTaskService.Owner}，由 {@code McpTasksGetHandler.Identity} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by owner; its type is {@code McpTaskService.Owner}, and {@code McpTasksGetHandler.Identity} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTasksGetHandler.Identity} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTasksGetHandler.Identity}; do not couple callers to its representation when the owning type exposes an API.
             */
            McpTaskService.Owner owner
    ) {
    }
}
