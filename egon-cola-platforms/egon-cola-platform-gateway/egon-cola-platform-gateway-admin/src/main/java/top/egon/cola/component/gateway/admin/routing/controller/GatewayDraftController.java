package top.egon.cola.component.gateway.admin.routing.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationRequestDTO;
import top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO;
import top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftRouteRequestDTO;
import top.egon.cola.component.gateway.admin.routing.service.GatewayDraftService;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;
import top.egon.cola.component.gateway.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

/**
 * 中文说明：{@code GatewayDraftController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责网关草稿控制器相关的职责与边界。
 * English summary: {@code GatewayDraftController} is a gateway draft controller controller in the current Gateway module; it owns the gateway draft controller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping("/api/v1/gateway/admin/gateway-groups/{gatewayGroupId}/draft")
@PreAuthorize("hasAnyAuthority('CAP_gateway:read','CAP_*')")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "gateway-admin",
        entityDomainName = "Gateway Admin 管理实体域",
        code = "gateway-admin-gateway-draft-controller",
        name = "GatewayDraftController 管理接口组")
public class GatewayDraftController {

    /**
     * 中文说明：保存 服务 对应的状态、依赖或配置值；字段类型为 {@code GatewayDraftService}，由 {@code GatewayDraftController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by service; its type is {@code GatewayDraftService}, and {@code GatewayDraftController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayDraftService service;

    /**
     * 中文说明：创建 {@code GatewayDraftController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayDraftController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param service 参数 服务；parameter service。
     */
    public GatewayDraftController(GatewayDraftService service) {
        this.service = service;
    }

    /**
     * 中文说明：执行 get 操作；该方法是 {@code GatewayDraftController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get operation; this method is the invocation entry point on {@code GatewayDraftController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftController.get(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 get 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @GetMapping
    public top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO get(
            @PathVariable String gatewayGroupId) {
        return service.get(gatewayGroupId);
    }

    /**
     * 中文说明：执行 put路由 操作；该方法是 {@code GatewayDraftController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the put route operation; this method is the invocation entry point on {@code GatewayDraftController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftController.putRoute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param routeId 参数 路由Id；parameter route id。
     * @param request 参数 请求；parameter request。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 put路由 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @PutMapping("/routes/{routeId}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:drafts:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftMutationResultVO putRoute(
            @PathVariable String gatewayGroupId,
            @PathVariable String routeId,
            @Valid @RequestBody GatewayDraftRouteRequestDTO request,
            AdminActor actor) {
        return service.putRoute(
                gatewayGroupId,
                routeId,
                new top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayRouteMutationDTO(
                        request.operationId(),
                        request.content(),
                        request.enabled(),
                        request.expectedRevision(),
                        request.idempotencyKey(),
                        request.changeReason()
                ),
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 delete路由 操作；该方法是 {@code GatewayDraftController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete route operation; this method is the invocation entry point on {@code GatewayDraftController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftController.deleteRoute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param routeId 参数 路由Id；parameter route id。
     * @param request 参数 请求；parameter request。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 delete路由 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @DeleteMapping("/routes/{routeId}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:drafts:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftMutationResultVO deleteRoute(
            @PathVariable String gatewayGroupId,
            @PathVariable String routeId,
            @Valid @RequestBody GatewayDraftMutationRequestDTO request,
            AdminActor actor) {
        return service.deleteRoute(
                gatewayGroupId,
                routeId,
                request.control(),
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 put策略 操作；该方法是 {@code GatewayDraftController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the put policy operation; this method is the invocation entry point on {@code GatewayDraftController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftController.putPolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param policyId 参数 策略Id；parameter policy id。
     * @param request 参数 请求；parameter request。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 put策略 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @PutMapping("/policies/{policyId}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:drafts:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftMutationResultVO putPolicy(
            @PathVariable String gatewayGroupId,
            @PathVariable String policyId,
            @Valid @RequestBody GatewayDraftPolicyRequestDTO request,
            AdminActor actor) {
        return service.putPolicy(
                gatewayGroupId,
                policyId,
                new top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayPolicyMutationDTO(
                        request.policyType(),
                        request.policyScope(),
                        request.content(),
                        request.enabled(),
                        request.expectedRevision(),
                        request.idempotencyKey(),
                        request.changeReason()
                ),
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 delete策略 操作；该方法是 {@code GatewayDraftController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete policy operation; this method is the invocation entry point on {@code GatewayDraftController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftController.deletePolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param policyId 参数 策略Id；parameter policy id。
     * @param request 参数 请求；parameter request。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 delete策略 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @DeleteMapping("/policies/{policyId}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:drafts:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftMutationResultVO deletePolicy(
            @PathVariable String gatewayGroupId,
            @PathVariable String policyId,
            @Valid @RequestBody GatewayDraftMutationRequestDTO request,
            AdminActor actor) {
        return service.deletePolicy(
                gatewayGroupId,
                policyId,
                request.control(),
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 validate 操作；该方法是 {@code GatewayDraftController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate operation; this method is the invocation entry point on {@code GatewayDraftController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftController.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 validate 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @PostMapping("/validate")
    public top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO validate(
            @PathVariable String gatewayGroupId) {
        return service.validate(gatewayGroupId);
    }

    /**
     * 中文说明：执行 diff 操作；该方法是 {@code GatewayDraftController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the diff operation; this method is the invocation entry point on {@code GatewayDraftController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftController.diff(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 diff 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @GetMapping("/diff")
    public top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO diff(
            @PathVariable String gatewayGroupId) {
        return service.diff(gatewayGroupId);
    }

    /**
     * 中文说明：执行 审计 操作；该方法是 {@code GatewayDraftController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audit operation; this method is the invocation entry point on {@code GatewayDraftController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftController.audit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 审计 的处理结果；returns the result of the operation.
     */
    private RequestAuditContext audit() {
        return RequestAuditContext.current();
    }






}
