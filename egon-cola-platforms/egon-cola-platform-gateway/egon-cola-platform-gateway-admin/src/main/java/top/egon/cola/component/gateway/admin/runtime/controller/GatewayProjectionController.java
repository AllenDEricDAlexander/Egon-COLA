package top.egon.cola.component.gateway.admin.runtime.controller;


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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionService;

/**
 * 中文说明：{@code GatewayProjectionController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责网关投影控制器相关的职责与边界。
 * English summary: {@code GatewayProjectionController} is a gateway projection controller controller in the current Gateway module; it owns the gateway projection controller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping("/api/v1/gateway/admin")
@PreAuthorize("hasAnyAuthority('CAP_gateway:read','CAP_*')")
public class GatewayProjectionController {

    /**
     * 中文说明：保存 服务 对应的状态、依赖或配置值；字段类型为 {@code GatewayProjectionService}，由 {@code GatewayProjectionController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by service; its type is {@code GatewayProjectionService}, and {@code GatewayProjectionController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayProjectionController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayProjectionController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayProjectionService service;

    /**
     * 中文说明：创建 {@code GatewayProjectionController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayProjectionController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param service 参数 服务；parameter service。
     */
    public GatewayProjectionController(GatewayProjectionService service) {
        this.service = service;
    }

    /**
     * 中文说明：执行 引擎Nodes 操作；该方法是 {@code GatewayProjectionController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the engine nodes operation; this method is the invocation entry point on {@code GatewayProjectionController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionController.engineNodes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 引擎Nodes 的处理结果；returns the result of the operation.
     */
    @GetMapping("/gateway-groups/{gatewayGroupId}/engine-nodes")
    public top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO<?> engineNodes(
            @PathVariable String gatewayGroupId) {
        return service.engineNodes(gatewayGroupId);
    }

    /**
     * 中文说明：执行 services 操作；该方法是 {@code GatewayProjectionController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the services operation; this method is the invocation entry point on {@code GatewayProjectionController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionController.services(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param bizCode 参数 bizCode；parameter biz code。
     * @param appCode 参数 appCode；parameter app code。
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param serviceKind 参数 服务Kind；parameter service kind。
     * @param protocol 参数 protocol；parameter protocol。
     * @param serviceName 参数 服务Name；parameter service name。
     * @param group 参数 group；parameter group。
     * @param version 参数 version；parameter version。
     * @return 返回 services 的处理结果；returns the result of the operation.
     */
    @GetMapping("/providers/services")
    public top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO<?> services(
            @RequestParam String bizCode,
            @RequestParam String appCode,
            @RequestParam String env,
            @RequestParam String namespace,
            @RequestParam(required = false) String serviceKind,
            @RequestParam(required = false) String protocol,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String version) {
        return service.services(query(
                bizCode,
                appCode,
                env,
                namespace,
                serviceKind,
                protocol,
                serviceName,
                group,
                version
        ));
    }

    /**
     * 中文说明：执行 instances 操作；该方法是 {@code GatewayProjectionController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the instances operation; this method is the invocation entry point on {@code GatewayProjectionController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionController.instances(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param bizCode 参数 bizCode；parameter biz code。
     * @param appCode 参数 appCode；parameter app code。
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param protocol 参数 protocol；parameter protocol。
     * @param serviceName 参数 服务Name；parameter service name。
     * @param serviceKind 参数 服务Kind；parameter service kind。
     * @param group 参数 group；parameter group。
     * @param version 参数 version；parameter version。
     * @return 返回 instances 的处理结果；returns the result of the operation.
     */
    @GetMapping("/providers/instances")
    public top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO<?> instances(
            @RequestParam String bizCode,
            @RequestParam String appCode,
            @RequestParam String env,
            @RequestParam String namespace,
            @RequestParam(required = false) String protocol,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String serviceKind,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String version) {
        if (protocol == null && serviceName == null) {
            return service.instances(bizCode, appCode, env, namespace);
        }
        if (protocol == null || serviceName == null) {
            throw new IllegalArgumentException(
                    "protocol and serviceName must be supplied together"
            );
        }
        return service.instances(query(
                bizCode,
                appCode,
                env,
                namespace,
                serviceKind,
                protocol,
                serviceName,
                group,
                version
        ));
    }

    /**
     * 中文说明：执行 consistency 操作；该方法是 {@code GatewayProjectionController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the consistency operation; this method is the invocation entry point on {@code GatewayProjectionController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionController.consistency(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 consistency 的处理结果；returns the result of the operation.
     */
    @GetMapping("/gateway-groups/{gatewayGroupId}/runtime-consistency")
    public top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayRuntimeConsistencyVO consistency(
            @PathVariable String gatewayGroupId) {
        return service.runtimeConsistency(gatewayGroupId);
    }

    /**
     * 中文说明：执行 query 操作；该方法是 {@code GatewayProjectionController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the query operation; this method is the invocation entry point on {@code GatewayProjectionController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayProjectionController.query(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param bizCode 参数 bizCode；parameter biz code。
     * @param appCode 参数 appCode；parameter app code。
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param serviceKind 参数 服务Kind；parameter service kind。
     * @param protocol 参数 protocol；parameter protocol。
     * @param serviceName 参数 服务Name；parameter service name。
     * @param group 参数 group；parameter group。
     * @param version 参数 version；parameter version。
     * @return 返回 query 的处理结果；returns the result of the operation.
     */
    private top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO query(
            String bizCode,
            String appCode,
            String env,
            String namespace,
            String serviceKind,
            String protocol,
            String serviceName,
            String group,
            String version) {
        return new top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO(
                bizCode,
                appCode,
                env,
                namespace,
                serviceKind,
                protocol,
                serviceName,
                group,
                version
        );
    }
}
