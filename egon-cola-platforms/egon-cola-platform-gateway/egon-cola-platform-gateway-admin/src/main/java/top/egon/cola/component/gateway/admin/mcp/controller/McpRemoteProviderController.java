package top.egon.cola.component.gateway.admin.mcp.controller;


import top.egon.cola.component.gateway.admin.application.controller.*;
import top.egon.cola.component.gateway.admin.application.domain.dto.*;
import top.egon.cola.component.gateway.admin.application.domain.exception.*;
import top.egon.cola.component.gateway.admin.application.domain.po.*;
import top.egon.cola.component.gateway.admin.application.domain.vo.*;
import top.egon.cola.component.gateway.admin.application.repository.*;
import top.egon.cola.component.gateway.admin.application.service.*;
import top.egon.cola.component.gateway.admin.auth.controller.*;
import top.egon.cola.component.gateway.admin.auth.domain.vo.*;
import top.egon.cola.component.gateway.admin.auth.service.*;
import top.egon.cola.component.gateway.admin.bootstrap.*;
import top.egon.cola.component.gateway.admin.catalog.controller.*;
import top.egon.cola.component.gateway.admin.catalog.domain.dto.*;
import top.egon.cola.component.gateway.admin.catalog.domain.enums.*;
import top.egon.cola.component.gateway.admin.catalog.domain.po.*;
import top.egon.cola.component.gateway.admin.catalog.domain.vo.*;
import top.egon.cola.component.gateway.admin.catalog.repository.*;
import top.egon.cola.component.gateway.admin.catalog.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.catalog.service.*;
import top.egon.cola.component.gateway.admin.config.*;
import top.egon.cola.component.gateway.admin.config.properties.*;
import top.egon.cola.component.gateway.admin.credential.controller.*;
import top.egon.cola.component.gateway.admin.credential.domain.dto.*;
import top.egon.cola.component.gateway.admin.credential.domain.po.*;
import top.egon.cola.component.gateway.admin.credential.domain.vo.*;
import top.egon.cola.component.gateway.admin.credential.repository.*;
import top.egon.cola.component.gateway.admin.credential.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.credential.service.*;
import top.egon.cola.component.gateway.admin.group.controller.*;
import top.egon.cola.component.gateway.admin.group.domain.dto.*;
import top.egon.cola.component.gateway.admin.group.domain.po.*;
import top.egon.cola.component.gateway.admin.group.domain.vo.*;
import top.egon.cola.component.gateway.admin.group.repository.*;
import top.egon.cola.component.gateway.admin.group.service.*;
import top.egon.cola.component.gateway.admin.mcp.controller.*;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.*;
import top.egon.cola.component.gateway.admin.mcp.domain.enums.*;
import top.egon.cola.component.gateway.admin.mcp.domain.exception.*;
import top.egon.cola.component.gateway.admin.mcp.domain.po.*;
import top.egon.cola.component.gateway.admin.mcp.domain.vo.*;
import top.egon.cola.component.gateway.admin.mcp.repository.*;
import top.egon.cola.component.gateway.admin.mcp.repository.filesystem.*;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.mcp.service.*;
import top.egon.cola.component.gateway.admin.observability.controller.*;
import top.egon.cola.component.gateway.admin.observability.controller.message.*;
import top.egon.cola.component.gateway.admin.observability.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.observability.domain.dto.*;
import top.egon.cola.component.gateway.admin.observability.domain.enums.*;
import top.egon.cola.component.gateway.admin.observability.domain.po.*;
import top.egon.cola.component.gateway.admin.observability.domain.vo.*;
import top.egon.cola.component.gateway.admin.observability.repository.*;
import top.egon.cola.component.gateway.admin.observability.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.observability.service.*;
import top.egon.cola.component.gateway.admin.release.controller.*;
import top.egon.cola.component.gateway.admin.release.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.release.domain.*;
import top.egon.cola.component.gateway.admin.release.domain.dto.*;
import top.egon.cola.component.gateway.admin.release.domain.enums.*;
import top.egon.cola.component.gateway.admin.release.domain.po.*;
import top.egon.cola.component.gateway.admin.release.domain.vo.*;
import top.egon.cola.component.gateway.admin.release.repository.*;
import top.egon.cola.component.gateway.admin.release.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.release.service.*;
import top.egon.cola.component.gateway.admin.reporting.controller.openapi.*;
import top.egon.cola.component.gateway.admin.reporting.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.reporting.domain.dto.*;
import top.egon.cola.component.gateway.admin.reporting.domain.po.*;
import top.egon.cola.component.gateway.admin.reporting.domain.vo.*;
import top.egon.cola.component.gateway.admin.reporting.repository.*;
import top.egon.cola.component.gateway.admin.reporting.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.reporting.service.*;
import top.egon.cola.component.gateway.admin.routing.controller.*;
import top.egon.cola.component.gateway.admin.routing.domain.*;
import top.egon.cola.component.gateway.admin.routing.domain.dto.*;
import top.egon.cola.component.gateway.admin.routing.domain.po.*;
import top.egon.cola.component.gateway.admin.routing.domain.vo.*;
import top.egon.cola.component.gateway.admin.routing.repository.*;
import top.egon.cola.component.gateway.admin.routing.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.routing.service.*;
import top.egon.cola.component.gateway.admin.rule.domain.dto.*;
import top.egon.cola.component.gateway.admin.rule.domain.vo.*;
import top.egon.cola.component.gateway.admin.rule.service.*;
import top.egon.cola.component.gateway.admin.runtime.controller.*;
import top.egon.cola.component.gateway.admin.runtime.domain.dto.*;
import top.egon.cola.component.gateway.admin.runtime.domain.vo.*;
import top.egon.cola.component.gateway.admin.runtime.service.*;
import top.egon.cola.component.gateway.admin.scope.controller.*;
import top.egon.cola.component.gateway.admin.scope.domain.*;
import top.egon.cola.component.gateway.admin.scope.domain.dto.*;
import top.egon.cola.component.gateway.admin.scope.domain.vo.*;
import top.egon.cola.component.gateway.admin.scope.service.*;
import top.egon.cola.component.gateway.admin.shared.controller.*;
import top.egon.cola.component.gateway.admin.shared.domain.*;
import top.egon.cola.component.gateway.admin.shared.domain.enums.*;
import top.egon.cola.component.gateway.admin.shared.domain.exception.*;
import top.egon.cola.component.gateway.admin.shared.domain.po.*;
import top.egon.cola.component.gateway.admin.shared.domain.vo.*;
import top.egon.cola.component.gateway.admin.shared.repository.*;
import top.egon.cola.component.gateway.admin.shared.repository.jdbc.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
import top.egon.cola.component.gateway.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;
import top.egon.cola.component.gateway.admin.mcp.service.McpControlPlaneService;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.JdbcMcpRemoteProviderRepository;

import java.util.List;
import java.util.Map;


import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteProviderRequestDTO;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteMountRequestDTO;
/**
 * 中文说明：{@code McpRemoteProviderController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责MCP远程提供方控制器相关的职责与边界。
 * English summary: {@code McpRemoteProviderController} is a mcp remote provider controller controller in the current Gateway module; it owns the mcp remote provider controller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping("/api/v1/gateway/admin/mcp/remote")
@PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:read','CAP_*')")
public class McpRemoteProviderController {

    /**
     * 中文说明：保存 服务 对应的状态、依赖或配置值；字段类型为 {@code McpControlPlaneService}，由 {@code McpRemoteProviderController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by service; its type is {@code McpControlPlaneService}, and {@code McpRemoteProviderController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRemoteProviderController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRemoteProviderController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpControlPlaneService service;

    /**
     * 中文说明：创建 {@code McpRemoteProviderController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpRemoteProviderController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param service 参数 服务；parameter service。
     */
    public McpRemoteProviderController(McpControlPlaneService service) {
        this.service = service;
    }

    /**
     * 中文说明：执行 providers 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the providers operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.providers(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 providers 的处理结果；returns the result of the operation.
     */
    @GetMapping("/providers")
    public List<top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteProviderDraftPO> providers(
            @RequestParam String gatewayGroupId) {
        return service.providers(gatewayGroupId);
    }

    /**
     * 中文说明：执行 create提供方 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create provider operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.createProvider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 create提供方 的处理结果；returns the result of the operation.
     */
    @PostMapping("/providers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO createProvider(
            @Valid @RequestBody McpRemoteProviderRequestDTO request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putProvider(
                null,
                request.mutation(),
                idempotencyKey,
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 update提供方 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update provider operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.updateProvider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 update提供方 的处理结果；returns the result of the operation.
     */
    @PutMapping("/providers/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO updateProvider(
            @PathVariable String id,
            @Valid @RequestBody McpRemoteProviderRequestDTO request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putProvider(
                id,
                request.mutation(),
                idempotencyKey,
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 delete提供方 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete provider operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.deleteProvider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 delete提供方 的处理结果；returns the result of the operation.
     */
    @DeleteMapping("/providers/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO deleteProvider(
            @PathVariable String id,
            @Valid @RequestBody top.egon.cola.component.gateway.admin.mcp.domain.dto.McpServerMutationRequestDTO request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.deleteProvider(
                id,
                control(request),
                idempotencyKey,
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 discover 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the discover operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.discover(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 discover 的处理结果；returns the result of the operation.
     */
    @PostMapping("/providers/{id}/discover")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:test','CAP_*')")
    public List<top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO> discover(
            @PathVariable String id) {
        return service.remoteCapabilities(id);
    }

    /**
     * 中文说明：执行 mounts 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mounts operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.mounts(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 mounts 的处理结果；returns the result of the operation.
     */
    @GetMapping("/mounts")
    public List<top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO> mounts(
            @RequestParam String gatewayGroupId) {
        return service.mounts(gatewayGroupId);
    }

    /**
     * 中文说明：执行 createMount 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create mount operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.createMount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 createMount 的处理结果；returns the result of the operation.
     */
    @PostMapping("/mounts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO createMount(
            @Valid @RequestBody McpRemoteMountRequestDTO request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putMount(
                null,
                request.mutation(),
                idempotencyKey,
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 updateMount 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update mount operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.updateMount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 updateMount 的处理结果；returns the result of the operation.
     */
    @PutMapping("/mounts/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO updateMount(
            @PathVariable String id,
            @Valid @RequestBody McpRemoteMountRequestDTO request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.putMount(
                id,
                request.mutation(),
                idempotencyKey,
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 deleteMount 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete mount operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.deleteMount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param request 参数 请求；parameter request。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 deleteMount 的处理结果；returns the result of the operation.
     */
    @DeleteMapping("/mounts/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO deleteMount(
            @PathVariable String id,
            @Valid @RequestBody top.egon.cola.component.gateway.admin.mcp.domain.dto.McpServerMutationRequestDTO request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            AdminActor actor) {
        return service.deleteMount(
                id,
                control(request),
                idempotencyKey,
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 control 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the control operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.control(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 control 的处理结果；returns the result of the operation.
     */
    private top.egon.cola.component.gateway.admin.mcp.domain.dto.McpMutationControlDTO control(
            top.egon.cola.component.gateway.admin.mcp.domain.dto.McpServerMutationRequestDTO request) {
        return new top.egon.cola.component.gateway.admin.mcp.domain.dto.McpMutationControlDTO(
                request.gatewayGroupId(),
                request.expectedRevision(),
                request.expectedDraftRevision(),
                request.changeReason()
        );
    }

    /**
     * 中文说明：执行 审计 操作；该方法是 {@code McpRemoteProviderController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audit operation; this method is the invocation entry point on {@code McpRemoteProviderController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRemoteProviderController.audit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 审计 的处理结果；returns the result of the operation.
     */
    private RequestAuditContext audit() {
        return RequestAuditContext.current();
    }




}
