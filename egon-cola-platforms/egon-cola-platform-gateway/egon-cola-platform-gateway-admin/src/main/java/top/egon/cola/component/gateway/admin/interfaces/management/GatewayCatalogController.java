package top.egon.cola.component.gateway.admin.interfaces.management;

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
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogService;
import top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogStore;
import top.egon.cola.component.gateway.admin.domain.AdminActor;

import java.util.List;
import java.util.Map;

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
    public GatewayCatalogStore.CatalogTree catalog(
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
    public ResourceCreated createInterfaceGroup(
            @PathVariable String applicationId,
            @Valid @RequestBody ManualInterfaceGroupRequest request,
            AdminActor actor) {
        String id = service.createManualInterfaceGroup(
                applicationId,
                new GatewayCatalogStore.ManualHierarchy(
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
        return new ResourceCreated(id);
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
    public GatewayCatalogService.OperationDetail createOperation(
            @PathVariable String interfaceGroupId,
            @Valid @RequestBody ManualOperationRequest request,
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
    public GatewayCatalogService.OperationDetail operation(
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
    public GatewayCatalogService.OperationDetail updateMetadata(
            @PathVariable String operationId,
            @Valid @RequestBody ManualMetadataRequest request,
            AdminActor actor) {
        return service.updateMetadata(
                operationId,
                new GatewayCatalogService.ManualMetadata(
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
    public GatewayCatalogService.OperationDetail updateDefinition(
            @PathVariable String operationId,
            @Valid @RequestBody ManualDefinitionRequest request,
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
    public GatewayCatalogService.OperationDetail deprecate(
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

    /**
     * 中文说明：{@code ResourceCreated} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责资源Created相关的职责与边界。
     * English summary: {@code ResourceCreated} is an immutable data carrier in the current Gateway module; it owns the resource created-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     */
    public record ResourceCreated(
    /**
     * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogController.ResourceCreated} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code GatewayCatalogController.ResourceCreated} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ResourceCreated} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ResourceCreated}; do not couple callers to its representation when the owning type exposes an API.
     */
    String id) {
    }

    /**
     * 中文说明：{@code ManualInterfaceGroupRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Manual接口Group请求相关的职责与边界。
     * English summary: {@code ManualInterfaceGroupRequest} is an immutable data carrier in the current Gateway module; it owns the manual interface group request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param businessCode 参数 businessCode；parameter business code。
     * @param businessName 参数 businessName；parameter business name。
     * @param entityCode 参数 entityCode；parameter entity code。
     * @param entityName 参数 entityName；parameter entity name。
     * @param interfaceGroupCode 参数 接口GroupCode；parameter interface group code。
     * @param interfaceGroupName 参数 接口GroupName；parameter interface group name。
     * @param className 参数 className；parameter class name。
     * @param description 参数 description；parameter description。
     */
    public record ManualInterfaceGroupRequest(
            /**
             * 中文说明：保存 businessCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogController.ManualInterfaceGroupRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by business code; its type is {@code String}, and {@code GatewayCatalogController.ManualInterfaceGroupRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualInterfaceGroupRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualInterfaceGroupRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String businessCode,
            /**
             * 中文说明：保存 businessName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogController.ManualInterfaceGroupRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by business name; its type is {@code String}, and {@code GatewayCatalogController.ManualInterfaceGroupRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualInterfaceGroupRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualInterfaceGroupRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String businessName,
            /**
             * 中文说明：保存 entityCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogController.ManualInterfaceGroupRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by entity code; its type is {@code String}, and {@code GatewayCatalogController.ManualInterfaceGroupRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualInterfaceGroupRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualInterfaceGroupRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String entityCode,
            /**
             * 中文说明：保存 entityName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogController.ManualInterfaceGroupRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by entity name; its type is {@code String}, and {@code GatewayCatalogController.ManualInterfaceGroupRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualInterfaceGroupRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualInterfaceGroupRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String entityName,
            /**
             * 中文说明：保存 接口GroupCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogController.ManualInterfaceGroupRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by interface group code; its type is {@code String}, and {@code GatewayCatalogController.ManualInterfaceGroupRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualInterfaceGroupRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualInterfaceGroupRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String interfaceGroupCode,
            /**
             * 中文说明：保存 接口GroupName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogController.ManualInterfaceGroupRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by interface group name; its type is {@code String}, and {@code GatewayCatalogController.ManualInterfaceGroupRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualInterfaceGroupRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualInterfaceGroupRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String interfaceGroupName,
            /**
             * 中文说明：保存 className 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogController.ManualInterfaceGroupRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by class name; its type is {@code String}, and {@code GatewayCatalogController.ManualInterfaceGroupRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualInterfaceGroupRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualInterfaceGroupRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String className,
            /**
             * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogController.ManualInterfaceGroupRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code GatewayCatalogController.ManualInterfaceGroupRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualInterfaceGroupRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualInterfaceGroupRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String description
    ) {
    }

    /**
     * 中文说明：{@code ManualOperationRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Manual操作请求相关的职责与边界。
     * English summary: {@code ManualOperationRequest} is an immutable data carrier in the current Gateway module; it owns the manual operation request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param protocol 参数 protocol；parameter protocol。
     * @param httpMethod 参数 http方法；parameter http method。
     * @param path 参数 path；parameter path。
     * @param serviceName 参数 服务Name；parameter service name。
     * @param fullMethodName 参数 full方法Name；parameter full method name。
     * @param providerServiceName 参数 提供方服务Name；parameter provider service name。
     * @param group 参数 group；parameter group。
     * @param version 参数 version；parameter version。
     * @param transport 参数 传输；parameter transport。
     * @param externalAccessible 参数 externalAccessible；parameter external accessible。
     * @param definition 参数 定义；parameter definition。
     */
    public record ManualOperationRequest(
            /**
             * 中文说明：保存 protocol 对应的状态、依赖或配置值；字段类型为 {@code GatewayCatalogService.Protocol}，由 {@code GatewayCatalogController.ManualOperationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by protocol; its type is {@code GatewayCatalogService.Protocol}, and {@code GatewayCatalogController.ManualOperationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualOperationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualOperationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotNull GatewayCatalogService.Protocol protocol,
            /**
             * 中文说明：保存 http方法 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogController.ManualOperationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by http method; its type is {@code String}, and {@code GatewayCatalogController.ManualOperationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualOperationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualOperationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String httpMethod,
            /**
             * 中文说明：保存 path 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogController.ManualOperationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by path; its type is {@code String}, and {@code GatewayCatalogController.ManualOperationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualOperationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualOperationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String path,
            /**
             * 中文说明：保存 服务Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogController.ManualOperationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by service name; its type is {@code String}, and {@code GatewayCatalogController.ManualOperationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualOperationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualOperationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serviceName,
            /**
             * 中文说明：保存 full方法Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogController.ManualOperationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by full method name; its type is {@code String}, and {@code GatewayCatalogController.ManualOperationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualOperationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualOperationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String fullMethodName,
            /**
             * 中文说明：保存 提供方服务Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogController.ManualOperationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by provider service name; its type is {@code String}, and {@code GatewayCatalogController.ManualOperationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualOperationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualOperationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String providerServiceName,
            /**
             * 中文说明：保存 group 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogController.ManualOperationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by group; its type is {@code String}, and {@code GatewayCatalogController.ManualOperationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualOperationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualOperationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String group,
            /**
             * 中文说明：保存 version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogController.ManualOperationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by version; its type is {@code String}, and {@code GatewayCatalogController.ManualOperationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualOperationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualOperationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String version,
            /**
             * 中文说明：保存 传输 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogController.ManualOperationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by transport; its type is {@code String}, and {@code GatewayCatalogController.ManualOperationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualOperationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualOperationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String transport,
            /**
             * 中文说明：保存 externalAccessible 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayCatalogController.ManualOperationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by external accessible; its type is {@code boolean}, and {@code GatewayCatalogController.ManualOperationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualOperationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualOperationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean externalAccessible,
            /**
             * 中文说明：保存 定义 对应的状态、依赖或配置值；字段类型为 {@code ManualDefinitionRequest}，由 {@code GatewayCatalogController.ManualOperationRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by definition; its type is {@code ManualDefinitionRequest}, and {@code GatewayCatalogController.ManualOperationRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualOperationRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualOperationRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotNull ManualDefinitionRequest definition
    ) {

        /**
         * 中文说明：执行 command 操作；该方法是 {@code GatewayCatalogController.ManualOperationRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the command operation; this method is the invocation entry point on {@code GatewayCatalogController.ManualOperationRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogController.ManualOperationRequest.command(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 command 的处理结果；returns the result of the operation.
         */
        private GatewayCatalogService.ManualOperation command() {
            return new GatewayCatalogService.ManualOperation(
                    protocol,
                    httpMethod,
                    path,
                    serviceName,
                    fullMethodName,
                    providerServiceName,
                    group,
                    version,
                    transport,
                    externalAccessible,
                    definition.definition()
            );
        }
    }

    /**
     * 中文说明：{@code ManualDefinitionRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Manual定义请求相关的职责与边界。
     * English summary: {@code ManualDefinitionRequest} is an immutable data carrier in the current Gateway module; it owns the manual definition request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param summary 参数 summary；parameter summary。
     * @param tags 参数 tags；parameter tags。
     * @param requestSchema 参数 请求模式；parameter request schema。
     * @param responseSchema 参数 响应模式；parameter response schema。
     * @param errorSchema 参数 error模式；parameter error schema。
     * @param descriptorSnapshot 参数 descriptorSnapshot；parameter descriptor snapshot。
     * @param attributes 参数 attributes；parameter attributes。
     * @param externalAccessible 参数 externalAccessible；parameter external accessible。
     */
    public record ManualDefinitionRequest(
            /**
             * 中文说明：保存 summary 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogController.ManualDefinitionRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by summary; its type is {@code String}, and {@code GatewayCatalogController.ManualDefinitionRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualDefinitionRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualDefinitionRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String summary,
            /**
             * 中文说明：保存 tags 对应的状态、依赖或配置值；字段类型为 {@code List<String>}，由 {@code GatewayCatalogController.ManualDefinitionRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tags; its type is {@code List<String>}, and {@code GatewayCatalogController.ManualDefinitionRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualDefinitionRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualDefinitionRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotNull List<String> tags,
            /**
             * 中文说明：保存 请求模式 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayCatalogController.ManualDefinitionRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by request schema; its type is {@code Map<String, Object>}, and {@code GatewayCatalogController.ManualDefinitionRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualDefinitionRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualDefinitionRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotNull Map<String, Object> requestSchema,
            /**
             * 中文说明：保存 响应模式 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayCatalogController.ManualDefinitionRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by response schema; its type is {@code Map<String, Object>}, and {@code GatewayCatalogController.ManualDefinitionRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualDefinitionRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualDefinitionRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotNull Map<String, Object> responseSchema,
            /**
             * 中文说明：保存 error模式 对应的状态、依赖或配置值；字段类型为 {@code List<Map<String, Object>>}，由 {@code GatewayCatalogController.ManualDefinitionRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by error schema; its type is {@code List<Map<String, Object>>}, and {@code GatewayCatalogController.ManualDefinitionRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualDefinitionRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualDefinitionRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotNull List<Map<String, Object>> errorSchema,
            /**
             * 中文说明：保存 descriptorSnapshot 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayCatalogController.ManualDefinitionRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by descriptor snapshot; its type is {@code Map<String, Object>}, and {@code GatewayCatalogController.ManualDefinitionRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualDefinitionRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualDefinitionRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> descriptorSnapshot,
            /**
             * 中文说明：保存 attributes 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayCatalogController.ManualDefinitionRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by attributes; its type is {@code Map<String, Object>}, and {@code GatewayCatalogController.ManualDefinitionRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualDefinitionRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualDefinitionRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotNull Map<String, Object> attributes,
            /**
             * 中文说明：保存 externalAccessible 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayCatalogController.ManualDefinitionRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by external accessible; its type is {@code boolean}, and {@code GatewayCatalogController.ManualDefinitionRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualDefinitionRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualDefinitionRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean externalAccessible
    ) {

        /**
         * 中文说明：执行 定义 操作；该方法是 {@code GatewayCatalogController.ManualDefinitionRequest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the definition operation; this method is the invocation entry point on {@code GatewayCatalogController.ManualDefinitionRequest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogController.ManualDefinitionRequest.definition(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 定义 的处理结果；returns the result of the operation.
         */
        private GatewayCatalogService.ManualDefinition definition() {
            return new GatewayCatalogService.ManualDefinition(
                    summary,
                    tags,
                    requestSchema,
                    responseSchema,
                    errorSchema,
                    descriptorSnapshot,
                    attributes,
                    externalAccessible
            );
        }
    }

    /**
     * 中文说明：{@code ManualMetadataRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Manual元数据请求相关的职责与边界。
     * English summary: {@code ManualMetadataRequest} is an immutable data carrier in the current Gateway module; it owns the manual metadata request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param summary 参数 summary；parameter summary。
     * @param tags 参数 tags；parameter tags。
     * @param owner 参数 owner；parameter owner。
     */
    public record ManualMetadataRequest(
            /**
             * 中文说明：保存 summary 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogController.ManualMetadataRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by summary; its type is {@code String}, and {@code GatewayCatalogController.ManualMetadataRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualMetadataRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualMetadataRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String summary,
            /**
             * 中文说明：保存 tags 对应的状态、依赖或配置值；字段类型为 {@code List<String>}，由 {@code GatewayCatalogController.ManualMetadataRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tags; its type is {@code List<String>}, and {@code GatewayCatalogController.ManualMetadataRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualMetadataRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualMetadataRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotNull List<String> tags,
            /**
             * 中文说明：保存 owner 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogController.ManualMetadataRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by owner; its type is {@code String}, and {@code GatewayCatalogController.ManualMetadataRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogController.ManualMetadataRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogController.ManualMetadataRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String owner
    ) {
    }
}
