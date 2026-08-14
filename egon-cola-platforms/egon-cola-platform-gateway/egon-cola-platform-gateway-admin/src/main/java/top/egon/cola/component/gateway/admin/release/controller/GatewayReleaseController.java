package top.egon.cola.component.gateway.admin.release.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.release.domain.dto.GatewayReleaseCreateRequestDTO;
import top.egon.cola.component.gateway.admin.release.domain.dto.GatewayReleaseRollbackRequestDTO;
import top.egon.cola.component.gateway.admin.release.service.GatewayReleaseService;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;
import top.egon.cola.component.gateway.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

import java.util.List;
import java.util.Map;

/**
 * 中文说明：{@code GatewayReleaseController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责网关发布控制器相关的职责与边界。
 * English summary: {@code GatewayReleaseController} is a gateway release controller controller in the current Gateway module; it owns the gateway release controller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping("/api/v1/gateway/admin")
@PreAuthorize("hasAnyAuthority('CAP_gateway:read','CAP_*')")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "gateway-admin",
        entityDomainName = "Gateway Admin 管理实体域",
        code = "gateway-admin-gateway-release-controller",
        name = "GatewayReleaseController 管理接口组")
public class GatewayReleaseController {

    /**
     * 中文说明：保存 服务 对应的状态、依赖或配置值；字段类型为 {@code GatewayReleaseService}，由 {@code GatewayReleaseController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by service; its type is {@code GatewayReleaseService}, and {@code GatewayReleaseController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayReleaseService service;

    /**
     * 中文说明：创建 {@code GatewayReleaseController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayReleaseController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param service 参数 服务；parameter service。
     */
    public GatewayReleaseController(GatewayReleaseService service) {
        this.service = service;
    }

    /**
     * 中文说明：执行 create 操作；该方法是 {@code GatewayReleaseController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create operation; this method is the invocation entry point on {@code GatewayReleaseController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseController.create(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param request 参数 请求；parameter request。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 create 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @PostMapping("/gateway-groups/{gatewayGroupId}/releases")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:releases:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO create(
            @PathVariable String gatewayGroupId,
            @Valid @RequestBody GatewayReleaseCreateRequestDTO request,
            AdminActor actor) {
        return service.create(
                gatewayGroupId,
                new top.egon.cola.component.gateway.admin.release.domain.dto.GatewayReleaseCreateCommandDTO(
                        request.expectedDraftRevision(),
                        request.changeReason()
                ),
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 get 操作；该方法是 {@code GatewayReleaseController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get operation; this method is the invocation entry point on {@code GatewayReleaseController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseController.get(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 get 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @GetMapping("/releases/{releaseId}")
    public top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO get(
            @PathVariable String releaseId) {
        return service.get(releaseId);
    }

    /**
     * 中文说明：执行 diff 操作；该方法是 {@code GatewayReleaseController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the diff operation; this method is the invocation entry point on {@code GatewayReleaseController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseController.diff(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 diff 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @GetMapping("/releases/{releaseId}/diff")
    public Map<String, Object> diff(@PathVariable String releaseId) {
        return service.diff(releaseId);
    }

    /**
     * 中文说明：执行 重试 操作；该方法是 {@code GatewayReleaseController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the retry operation; this method is the invocation entry point on {@code GatewayReleaseController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseController.retry(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 重试 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @PostMapping("/releases/{releaseId}/retry")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:releases:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO retry(
            @PathVariable String releaseId,
            AdminActor actor) {
        return service.retry(
                releaseId,
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 rollback 操作；该方法是 {@code GatewayReleaseController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rollback operation; this method is the invocation entry point on {@code GatewayReleaseController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseController.rollback(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param request 参数 请求；parameter request。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 rollback 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @PostMapping("/gateway-groups/{gatewayGroupId}/rollback")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:releases:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO rollback(
            @PathVariable String gatewayGroupId,
            @Valid @RequestBody GatewayReleaseRollbackRequestDTO request,
            AdminActor actor) {
        return service.rollback(
                gatewayGroupId,
                new top.egon.cola.component.gateway.admin.release.domain.dto.GatewayReleaseRollbackCommandDTO(
                        request.sourceReleaseId(),
                        request.expectedDraftRevision(),
                        request.changeReason()
                ),
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 history 操作；该方法是 {@code GatewayReleaseController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the history operation; this method is the invocation entry point on {@code GatewayReleaseController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseController.history(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 history 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @GetMapping("/gateway-groups/{gatewayGroupId}/releases")
    public List<top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO> history(
            @PathVariable String gatewayGroupId) {
        return service.history(gatewayGroupId);
    }

    /**
     * 中文说明：执行 审计 操作；该方法是 {@code GatewayReleaseController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audit operation; this method is the invocation entry point on {@code GatewayReleaseController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseController.audit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 审计 的处理结果；returns the result of the operation.
     */
    private RequestAuditContext audit() {
        return RequestAuditContext.current();
    }




}
