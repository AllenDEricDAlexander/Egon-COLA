package top.egon.cola.component.gateway.admin.catalog.controller;


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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.gateway.admin.catalog.service.GatewayCatalogService;
import top.egon.cola.component.gateway.admin.catalog.repository.GatewayCatalogRepository;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;

import java.util.List;
import java.util.Map;


import top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCatalogResourceCreatedVO;
import top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO;
import top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationRequestDTO;
import top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionRequestDTO;
import top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualMetadataRequestDTO;
/**
 * 中文说明：{@code GatewayCatalogController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责网关目录控制器相关的职责与边界。
 * English summary: {@code GatewayCatalogController} is a gateway catalog controller controller in the current Gateway module; it owns the gateway catalog controller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping("/api/v1/gateway/admin")
@PreAuthorize("hasAnyAuthority('CAP_gateway:read','CAP_*')")
public class GatewayCatalogController {

    /**
     * 中文说明：保存 服务 对应的状态、依赖或配置值；字段类型为 {@code GatewayCatalogService}，由 {@code GatewayCatalogController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by service; its type is {@code GatewayCatalogService}, and {@code GatewayCatalogController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCatalogController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCatalogService service;

    /**
     * 中文说明：创建 {@code GatewayCatalogController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayCatalogController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param service 参数 服务；parameter service。
     */
    public GatewayCatalogController(GatewayCatalogService service) {
        this.service = service;
    }

    /**
     * 中文说明：执行 目录 操作；该方法是 {@code GatewayCatalogController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the catalog operation; this method is the invocation entry point on {@code GatewayCatalogController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogController.catalog(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @return 返回 目录 的处理结果；returns the result of the operation.
     */
    @GetMapping("/applications/{applicationId}/catalog")
    public top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCatalogTreeVO catalog(
            @PathVariable String applicationId) {
        return service.catalog(applicationId);
    }

    /**
     * 中文说明：执行 create接口Group 操作；该方法是 {@code GatewayCatalogController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create interface group operation; this method is the invocation entry point on {@code GatewayCatalogController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogController.createInterfaceGroup(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param request 参数 请求；parameter request。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 create接口Group 的处理结果；returns the result of the operation.
     */
    @PostMapping("/applications/{applicationId}/manual-interface-groups")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:catalog:write','CAP_*')")
    public GatewayCatalogResourceCreatedVO createInterfaceGroup(
            @PathVariable String applicationId,
            @Valid @RequestBody GatewayManualInterfaceGroupRequestDTO request,
            AdminActor actor) {
        String id = service.createManualInterfaceGroup(
                applicationId,
                new top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualHierarchyDTO(
                        request.businessCode(),
                        request.businessName(),
                        request.entityCode(),
                        request.entityName(),
                        request.interfaceGroupCode(),
                        request.interfaceGroupName(),
                        request.className(),
                        request.description()
                ),
                actor,
                audit()
        );
        return new GatewayCatalogResourceCreatedVO(id);
    }

    /**
     * 中文说明：执行 create操作 操作；该方法是 {@code GatewayCatalogController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create operation operation; this method is the invocation entry point on {@code GatewayCatalogController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogController.createOperation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param interfaceGroupId 参数 接口GroupId；parameter interface group id。
     * @param request 参数 请求；parameter request。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 create操作 的处理结果；returns the result of the operation.
     */
    @PostMapping("/interface-groups/{interfaceGroupId}/manual-operations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CAP_gateway:catalog:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayOperationDetailVO createOperation(
            @PathVariable String interfaceGroupId,
            @Valid @RequestBody GatewayManualOperationRequestDTO request,
            AdminActor actor) {
        return service.createManualOperation(
                interfaceGroupId,
                request.command(),
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 操作 操作；该方法是 {@code GatewayCatalogController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the operation operation; this method is the invocation entry point on {@code GatewayCatalogController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogController.operation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @return 返回 操作 的处理结果；returns the result of the operation.
     */
    @GetMapping("/operations/{operationId}")
    public top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayOperationDetailVO operation(
            @PathVariable String operationId) {
        return service.detail(operationId);
    }

    /**
     * 中文说明：执行 update元数据 操作；该方法是 {@code GatewayCatalogController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update metadata operation; this method is the invocation entry point on {@code GatewayCatalogController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogController.updateMetadata(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @param request 参数 请求；parameter request。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 update元数据 的处理结果；returns the result of the operation.
     */
    @PutMapping("/operations/{operationId}/metadata")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:catalog:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayOperationDetailVO updateMetadata(
            @PathVariable String operationId,
            @Valid @RequestBody GatewayManualMetadataRequestDTO request,
            AdminActor actor) {
        return service.updateMetadata(
                operationId,
                new top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualMetadataDTO(
                        request.summary(),
                        request.tags(),
                        request.owner()
                ),
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 update定义 操作；该方法是 {@code GatewayCatalogController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update definition operation; this method is the invocation entry point on {@code GatewayCatalogController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogController.updateDefinition(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @param request 参数 请求；parameter request。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 update定义 的处理结果；returns the result of the operation.
     */
    @PutMapping("/operations/{operationId}/manual-definition")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:catalog:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayOperationDetailVO updateDefinition(
            @PathVariable String operationId,
            @Valid @RequestBody GatewayManualDefinitionRequestDTO request,
            AdminActor actor) {
        return service.updateManualDefinition(
                operationId,
                request.definition(),
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 deprecate 操作；该方法是 {@code GatewayCatalogController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the deprecate operation; this method is the invocation entry point on {@code GatewayCatalogController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogController.deprecate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 deprecate 的处理结果；returns the result of the operation.
     */
    @PostMapping("/operations/{operationId}/deprecate")
    @PreAuthorize("hasAnyAuthority('CAP_gateway:catalog:write','CAP_*')")
    public top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayOperationDetailVO deprecate(
            @PathVariable String operationId,
            AdminActor actor) {
        return service.deprecate(
                operationId,
                actor,
                audit()
        );
    }

    /**
     * 中文说明：执行 审计 操作；该方法是 {@code GatewayCatalogController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audit operation; this method is the invocation entry point on {@code GatewayCatalogController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogController.audit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 审计 的处理结果；returns the result of the operation.
     */
    private RequestAuditContext audit() {
        return RequestAuditContext.current();
    }










}
