package top.egon.cola.component.gateway.admin.mcp.interfaces;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.mcp.application.McpControlPlaneService;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpTaskStore;

import java.util.List;

/**
 * 中文说明：{@code McpTaskAdminController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责MCP任务管理端控制器相关的职责与边界。
 * English summary: {@code McpTaskAdminController} is a mcp task admin controller controller in the current Gateway module; it owns the mcp task admin controller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping("/api/v1/gateway/admin/mcp/tasks")
@PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:runtime:read','CAP_*')")
public class McpTaskAdminController {

    /**
     * 中文说明：保存 服务 对应的状态、依赖或配置值；字段类型为 {@code McpControlPlaneService}，由 {@code McpTaskAdminController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by service; its type is {@code McpControlPlaneService}, and {@code McpTaskAdminController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskAdminController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskAdminController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpControlPlaneService service;

    /**
     * 中文说明：创建 {@code McpTaskAdminController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpTaskAdminController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param service 参数 服务；parameter service。
     */
    public McpTaskAdminController(McpControlPlaneService service) {
        this.service = service;
    }

    /**
     * 中文说明：执行 list 操作；该方法是 {@code McpTaskAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the list operation; this method is the invocation entry point on {@code McpTaskAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskAdminController.list(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param tenantId 参数 tenantId；parameter tenant id。
     * @param clientId 参数 客户端Id；parameter client id。
     * @return 返回 list 的处理结果；returns the result of the operation.
     */
    @GetMapping
    public List<JdbcMcpTaskStore.TaskRecord> list(
            @RequestParam String tenantId,
            @RequestParam(required = false) String clientId) {
        return service.tasks(tenantId, clientId);
    }

    /**
     * 中文说明：执行 get 操作；该方法是 {@code McpTaskAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get operation; this method is the invocation entry point on {@code McpTaskAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskAdminController.get(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 get 的处理结果；returns the result of the operation.
     */
    @GetMapping("/{id}")
    public JdbcMcpTaskStore.TaskRecord get(@PathVariable String id) {
        return service.task(id);
    }

    /**
     * 中文说明：执行 cancel 操作；该方法是 {@code McpTaskAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the cancel operation; this method is the invocation entry point on {@code McpTaskAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskAdminController.cancel(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 cancel 的处理结果；returns the result of the operation.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public CancelResult cancel(
            @PathVariable String id,
            @Valid @RequestBody CancelRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return new CancelResult(service.cancelTask(
                id,
                request.expectedRevision(),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        ));
    }

    /**
     * 中文说明：{@code CancelRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Cancel请求相关的职责与边界。
     * English summary: {@code CancelRequest} is an immutable data carrier in the current Gateway module; it owns the cancel request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     */
    public record CancelRequest(
    /**
     * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpTaskAdminController.CancelRequest} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code McpTaskAdminController.CancelRequest} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskAdminController.CancelRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskAdminController.CancelRequest}; do not couple callers to its representation when the owning type exposes an API.
     */
    @PositiveOrZero long expectedRevision) {
    }

    /**
     * 中文说明：{@code CancelResult} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责CancelResult相关的职责与边界。
     * English summary: {@code CancelResult} is an immutable data carrier in the current Gateway module; it owns the cancel result-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param cancelled 参数 cancelled；parameter cancelled。
     */
    public record CancelResult(
    /**
     * 中文说明：保存 cancelled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpTaskAdminController.CancelResult} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by cancelled; its type is {@code boolean}, and {@code McpTaskAdminController.CancelResult} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskAdminController.CancelResult} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskAdminController.CancelResult}; do not couple callers to its representation when the owning type exposes an API.
     */
    boolean cancelled) {
    }
}
