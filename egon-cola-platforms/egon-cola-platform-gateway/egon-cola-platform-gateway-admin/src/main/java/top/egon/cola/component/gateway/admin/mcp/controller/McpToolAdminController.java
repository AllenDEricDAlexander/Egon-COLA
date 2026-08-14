package top.egon.cola.component.gateway.admin.mcp.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideRequestDTO;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolRequestDTO;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpToolMutationRequestDTO;
import top.egon.cola.component.gateway.admin.mcp.service.McpToolAdminService;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;
import top.egon.cola.component.gateway.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

import java.util.List;

/**
 * 中文说明：{@code McpToolAdminController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责MCP工具管理端控制器相关的职责与边界。
 * English summary: {@code McpToolAdminController} is a mcp tool admin controller controller in the current Gateway module; it owns the mcp tool admin controller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping("/api/v1/gateway/admin/mcp")
@PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:read','CAP_*')")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "gateway-admin",
        entityDomainName = "Gateway Admin 管理实体域",
        code = "gateway-admin-mcp-tool-admin-controller",
        name = "McpToolAdminController 管理接口组")
public class McpToolAdminController {

    /**
     * 中文说明：保存 服务 对应的状态、依赖或配置值；字段类型为 {@code McpToolAdminService}，由 {@code McpToolAdminController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by service; its type is {@code McpToolAdminService}, and {@code McpToolAdminController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolAdminController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpToolAdminService service;

    /**
     * 中文说明：创建 {@code McpToolAdminController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpToolAdminController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param service 参数 服务；parameter service。
     */
    public McpToolAdminController(McpToolAdminService service) {
        this.service = service;
    }

    /**
     * 中文说明：执行 managedTools 操作；该方法是 {@code McpToolAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the managed tools operation; this method is the invocation entry point on {@code McpToolAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminController.managedTools(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param groupId 参数 groupId；parameter group id。
     * @param serverId 参数 服务器Id；parameter server id。
     * @return 返回 managedTools 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @GetMapping("/groups/{groupId}/managed-tools")
    public List<top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO> managedTools(
            @PathVariable String groupId,
            @RequestParam(required = false) String serverId) {
        return service.managedTools(groupId, serverId);
    }

    /**
     * 中文说明：执行 putOverride 操作；该方法是 {@code McpToolAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the put override operation; this method is the invocation entry point on {@code McpToolAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminController.putOverride(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param toolId 参数 工具Id；parameter tool id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 putOverride 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @PutMapping("/managed-tools/{toolId}/override")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO putOverride(
            @PathVariable String toolId,
            @Valid @RequestBody McpManagedToolOverrideRequestDTO request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putOverride(
                toolId,
                request.mutation(),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        );
    }

    /**
     * 中文说明：执行 deleteOverride 操作；该方法是 {@code McpToolAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete override operation; this method is the invocation entry point on {@code McpToolAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminController.deleteOverride(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param toolId 参数 工具Id；parameter tool id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 deleteOverride 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @DeleteMapping("/managed-tools/{toolId}/override")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO deleteOverride(
            @PathVariable String toolId,
            @Valid @RequestBody McpToolMutationRequestDTO request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.deleteOverride(
                toolId,
                request.control(),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        );
    }

    /**
     * 中文说明：执行 远程Tools 操作；该方法是 {@code McpToolAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the remote tools operation; this method is the invocation entry point on {@code McpToolAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminController.remoteTools(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverId 参数 服务器Id；parameter server id。
     * @return 返回 远程Tools 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @GetMapping("/remote-tools")
    public List<top.egon.cola.component.gateway.admin.mcp.domain.vo.McpRemoteToolVO> remoteTools(
            @RequestParam String gatewayGroupId,
            @RequestParam(required = false) String serverId) {
        return service.remoteTools(gatewayGroupId, serverId);
    }

    /**
     * 中文说明：执行 create远程工具 操作；该方法是 {@code McpToolAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create remote tool operation; this method is the invocation entry point on {@code McpToolAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminController.createRemoteTool(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 create远程工具 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @PostMapping("/remote-tools")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO createRemoteTool(
            @Valid @RequestBody McpRemoteToolRequestDTO request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putRemoteTool(
                null,
                request.mutation(),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        );
    }

    /**
     * 中文说明：执行 update远程工具 操作；该方法是 {@code McpToolAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update remote tool operation; this method is the invocation entry point on {@code McpToolAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminController.updateRemoteTool(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param toolId 参数 工具Id；parameter tool id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 update远程工具 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @PutMapping("/remote-tools/{toolId}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO updateRemoteTool(
            @PathVariable String toolId,
            @Valid @RequestBody McpRemoteToolRequestDTO request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putRemoteTool(
                toolId,
                request.mutation(),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        );
    }

    /**
     * 中文说明：执行 delete远程工具 操作；该方法是 {@code McpToolAdminController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete remote tool operation; this method is the invocation entry point on {@code McpToolAdminController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminController.deleteRemoteTool(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param toolId 参数 工具Id；parameter tool id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 delete远程工具 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @DeleteMapping("/remote-tools/{toolId}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO deleteRemoteTool(
            @PathVariable String toolId,
            @Valid @RequestBody McpToolMutationRequestDTO request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.deleteRemoteTool(
                toolId,
                request.control(),
                idempotencyKey,
                actor,
                RequestAuditContext.current()
        );
    }






}
