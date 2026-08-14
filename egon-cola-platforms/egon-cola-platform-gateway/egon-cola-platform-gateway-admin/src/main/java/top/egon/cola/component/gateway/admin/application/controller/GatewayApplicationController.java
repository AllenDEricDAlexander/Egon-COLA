package top.egon.cola.component.gateway.admin.application.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.application.domain.dto.GatewayApplicationCreateRequestDTO;
import top.egon.cola.component.gateway.admin.application.domain.dto.GatewayApplicationUpdateRequestDTO;
import top.egon.cola.component.gateway.admin.application.service.GatewayApplicationService;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;
import top.egon.cola.component.gateway.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

import java.util.List;

/**
 * 中文说明：{@code GatewayApplicationController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责网关Application控制器相关的职责与边界。
 * English summary: {@code GatewayApplicationController} is a gateway application controller controller in the current Gateway module; it owns the gateway application controller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping("/api/v1/gateway/admin/applications")
@PreAuthorize("hasAnyAuthority('CAP_gateway:read','CAP_*')")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "gateway-admin",
        entityDomainName = "Gateway Admin 管理实体域",
        code = "gateway-admin-gateway-application-controller",
        name = "GatewayApplicationController 管理接口组")
public class GatewayApplicationController {

    /**
     * 中文说明：保存 服务 对应的状态、依赖或配置值；字段类型为 {@code GatewayApplicationService}，由 {@code GatewayApplicationController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by service; its type is {@code GatewayApplicationService}, and {@code GatewayApplicationController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayApplicationController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayApplicationService service;

    /**
     * 中文说明：创建 {@code GatewayApplicationController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayApplicationController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param service 参数 服务；parameter service。
     */
    public GatewayApplicationController(GatewayApplicationService service) {
        this.service = service;
    }

    /**
     * 中文说明：执行 list 操作；该方法是 {@code GatewayApplicationController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the list operation; this method is the invocation entry point on {@code GatewayApplicationController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationController.list(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param bizCode 参数 bizCode；parameter biz code。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param env 参数 env；parameter env。
     * @param appCode 参数 appCode；parameter app code。
     * @return 返回 list 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @GetMapping
    public List<top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO> list(
            @RequestParam(required = false) String bizCode,
            @RequestParam(required = false) String namespace,
            @RequestParam(required = false) String env,
            @RequestParam(required = false) String appCode) {
        return service.list(new top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO(
                bizCode,
                namespace,
                env,
                appCode
        ));
    }

    /**
     * 中文说明：执行 create 操作；该方法是 {@code GatewayApplicationController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create operation; this method is the invocation entry point on {@code GatewayApplicationController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationController.create(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 create 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:applications:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO create(
            @Valid @RequestBody GatewayApplicationCreateRequestDTO request,
            AdminActor actor) {
        return service.create(
                new top.egon.cola.component.gateway.admin.application.domain.dto.GatewayApplicationCreateCommandDTO(
                        request.bizCode(),
                        request.applicationCode(),
                        request.displayName(),
                        request.env(),
                        request.namespace(),
                        request.description()
                ),
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 get 操作；该方法是 {@code GatewayApplicationController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get operation; this method is the invocation entry point on {@code GatewayApplicationController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationController.get(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 get 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @GetMapping("/{id}")
    public top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO get(
            @PathVariable String id) {
        return service.get(id);
    }

    /**
     * 中文说明：执行 update 操作；该方法是 {@code GatewayApplicationController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update operation; this method is the invocation entry point on {@code GatewayApplicationController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationController.update(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param request 参数 请求；parameter request。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 update 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:applications:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO update(
            @PathVariable String id,
            @Valid @RequestBody GatewayApplicationUpdateRequestDTO request,
            AdminActor actor) {
        return service.update(
                id,
                new top.egon.cola.component.gateway.admin.application.domain.dto.GatewayApplicationUpdateCommandDTO(
                        request.displayName(),
                        request.description(),
                        request.expectedRevision()
                ),
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 审计 操作；该方法是 {@code GatewayApplicationController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audit operation; this method is the invocation entry point on {@code GatewayApplicationController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationController.audit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 审计 的处理结果；returns the result of the operation.
     */
    private RequestAuditContext audit() {
        return RequestAuditContext.current();
    }




}
