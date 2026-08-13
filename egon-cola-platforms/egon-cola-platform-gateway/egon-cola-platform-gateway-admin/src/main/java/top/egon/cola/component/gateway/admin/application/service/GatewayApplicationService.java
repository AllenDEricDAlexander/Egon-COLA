package top.egon.cola.component.gateway.admin.application.service;


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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.model.management.DdcManagementScopeBinding;
import top.egon.cola.component.gateway.admin.scope.service.GatewayScopeService;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminRevisionConflictException;
import top.egon.cola.component.gateway.admin.application.domain.po.GatewayApplicationPO;
import top.egon.cola.component.gateway.admin.application.repository.GatewayApplicationRepository;
import top.egon.cola.component.gateway.admin.observability.domain.po.GatewayAuditLogPO;
import top.egon.cola.component.gateway.admin.observability.repository.GatewayAuditLogRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import top.egon.cola.component.gateway.admin.application.domain.dto.GatewayApplicationCreateCommandDTO;
import top.egon.cola.component.gateway.admin.application.domain.dto.GatewayApplicationUpdateCommandDTO;
import top.egon.cola.component.gateway.admin.application.domain.vo.GatewayApplicationVO;
/**
 * 中文说明：{@code GatewayApplicationService} 是服务组件，位于当前 Gateway 模块的相关包中，负责网关Application服务相关的职责与边界。
 * English summary: {@code GatewayApplicationService} is a gateway application service service in the current Gateway module; it owns the gateway application service-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Service
public class GatewayApplicationService {

    /**
     * 中文说明：保存 applications 对应的状态、依赖或配置值；字段类型为 {@code GatewayApplicationRepository}，由 {@code GatewayApplicationService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by applications; its type is {@code GatewayApplicationRepository}, and {@code GatewayApplicationService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayApplicationService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayApplicationRepository applications;

    /**
     * 中文说明：保存 audits 对应的状态、依赖或配置值；字段类型为 {@code GatewayAuditLogRepository}，由 {@code GatewayApplicationService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by audits; its type is {@code GatewayAuditLogRepository}, and {@code GatewayApplicationService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayApplicationService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayAuditLogRepository audits;

    /**
     * 中文说明：保存 scopes 对应的状态、依赖或配置值；字段类型为 {@code GatewayScopeService}，由 {@code GatewayApplicationService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by scopes; its type is {@code GatewayScopeService}, and {@code GatewayApplicationService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayApplicationService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayScopeService scopes;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code GatewayApplicationService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code GatewayApplicationService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayApplicationService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：创建 {@code GatewayApplicationService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayApplicationService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param applications 参数 applications；parameter applications。
     * @param audits 参数 audits；parameter audits。
     * @param scopes 参数 scopes；parameter scopes。
     */
    @Autowired
    public GatewayApplicationService(
            GatewayApplicationRepository applications,
            GatewayAuditLogRepository audits,
            GatewayScopeService scopes) {
        this(applications, audits, scopes, Clock.systemUTC());
    }

    /**
     * 中文说明：创建 {@code GatewayApplicationService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayApplicationService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param applications 参数 applications；parameter applications。
     * @param audits 参数 audits；parameter audits。
     * @param scopes 参数 scopes；parameter scopes。
     * @param clock 参数 clock；parameter clock。
     */
    GatewayApplicationService(
            GatewayApplicationRepository applications,
            GatewayAuditLogRepository audits,
            GatewayScopeService scopes,
            Clock clock) {
        this.applications = applications;
        this.audits = audits;
        this.scopes = scopes;
        this.clock = clock;
    }

    /**
     * 中文说明：执行 create 操作；该方法是 {@code GatewayApplicationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create operation; this method is the invocation entry point on {@code GatewayApplicationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationService.create(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param command 参数 command；parameter command。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 create 的处理结果；returns the result of the operation.
     */
    @Transactional
    public GatewayApplicationVO create(
            GatewayApplicationCreateCommandDTO command,
            AdminActor actor,
            RequestAuditContext request) {
        top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO scope =
                new top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO(
                        required(command.bizCode(), "bizCode"),
                        required(command.namespace(), "namespace"),
                        required(command.env(), "env"),
                        required(command.applicationCode(), "applicationCode")
                );
        DdcManagementScopeBinding binding = scopes.requireEnabled(scope);
        GatewayApplicationPO existing = applications
                .findByBizCodeAndApplicationCodeAndEnvAndDeletedFalse(
                        scope.bizCode(),
                        scope.appCode(),
                        scope.env()
                )
                .orElse(null);
        if (existing != null) {
            throw new GatewayApplicationAlreadyExistsException(
                    existing.getId()
            );
        }
        Instant now = clock.instant();
        GatewayApplicationPO application = new GatewayApplicationPO(
                UuidV7.simpleString(),
                scope.bizCode(),
                scope.appCode(),
                required(command.displayName(), "displayName"),
                scope.env(),
                scope.namespace(),
                command.description(),
                actor.actorId(),
                now
        );
        applications.saveAndFlush(application);
        audit(actor, request, application.getId(), "CREATE", Map.of(
                "bindingId", binding.bindingId(),
                "bizCode", scope.bizCode(),
                "applicationCode", scope.appCode(),
                "env", scope.env(),
                "namespace", scope.namespace()
        ));
        return view(application, scope.namespace(), true);
    }

    /**
     * 中文说明：执行 list 操作；该方法是 {@code GatewayApplicationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the list operation; this method is the invocation entry point on {@code GatewayApplicationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationService.list(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 list 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public List<GatewayApplicationVO> list() {
        return list(new top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO(
                null,
                null,
                null,
                null
        ));
    }

    /**
     * 中文说明：执行 list 操作；该方法是 {@code GatewayApplicationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the list operation; this method is the invocation entry point on {@code GatewayApplicationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationService.list(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param query 参数 query；parameter query。
     * @return 返回 list 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public List<GatewayApplicationVO> list(
            top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO query) {
        List<DdcManagementScopeBinding> bindings = scopes.bindings(query);
        Map<top.egon.cola.component.gateway.admin.scope.domain.GatewayPhysicalApplicationKey,
                List<DdcManagementScopeBinding>> bindingsByApplication =
                bindings.stream().collect(Collectors.groupingBy(
                        GatewayApplicationService::physicalKey,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        return applications.findAllByDeletedFalseOrderByCreatedAtDesc()
                .stream()
                .filter(application -> query.empty()
                        || bindingsByApplication.containsKey(
                        physicalKey(application)))
                .map(application -> scopedView(
                        application,
                        query,
                        bindingsByApplication.getOrDefault(
                                physicalKey(application),
                                List.of()
                        )
                ))
                .toList();
    }

    /**
     * 中文说明：执行 get 操作；该方法是 {@code GatewayApplicationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get operation; this method is the invocation entry point on {@code GatewayApplicationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationService.get(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 get 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public GatewayApplicationVO get(String id) {
        return view(required(id), null, false);
    }

    /**
     * 中文说明：执行 update 操作；该方法是 {@code GatewayApplicationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update operation; this method is the invocation entry point on {@code GatewayApplicationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationService.update(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param command 参数 command；parameter command。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 update 的处理结果；returns the result of the operation.
     */
    @Transactional
    public GatewayApplicationVO update(
            String id,
            GatewayApplicationUpdateCommandDTO command,
            AdminActor actor,
            RequestAuditContext request) {
        GatewayApplicationPO application = required(id);
        if (application.getRevision() != command.expectedRevision()) {
            throw new GatewayAdminRevisionConflictException(
                    application.getRevision()
            );
        }
        application.update(
                required(command.displayName(), "displayName"),
                command.description(),
                actor.actorId(),
                clock.instant()
        );
        applications.flush();
        audit(actor, request, id, "UPDATE", Map.of(
                "displayName", application.getDisplayName(),
                "revision", application.getRevision()
        ));
        return view(application, null, false);
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code GatewayApplicationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code GatewayApplicationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationService.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private GatewayApplicationPO required(String id) {
        return applications.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "gateway application " + id + " was not found"
                ));
    }

    /**
     * 中文说明：执行 审计 操作；该方法是 {@code GatewayApplicationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audit operation; this method is the invocation entry point on {@code GatewayApplicationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationService.audit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @param id 参数 id；parameter id。
     * @param action 参数 action；parameter action。
     * @param after 参数 after；parameter after。
     */
    private void audit(
            AdminActor actor,
            RequestAuditContext request,
            String id,
            String action,
            Map<String, Object> after) {
        audits.save(new GatewayAuditLogPO(
                UuidV7.simpleString(),
                actor.actorId(),
                actor.actorType().name(),
                "MANAGEMENT_API",
                request.requestId(),
                request.traceId(),
                "GATEWAY_APPLICATION",
                id,
                action,
                null,
                after,
                null,
                null,
                true,
                null,
                clock.instant()
        ));
    }

    /**
     * 中文说明：执行 scopedView 操作；该方法是 {@code GatewayApplicationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the scoped view operation; this method is the invocation entry point on {@code GatewayApplicationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationService.scopedView(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param application 参数 application；parameter application。
     * @param query 参数 query；parameter query。
     * @param bindings 参数 bindings；parameter bindings。
     * @return 返回 scopedView 的处理结果；returns the result of the operation.
     */
    private GatewayApplicationVO scopedView(
            GatewayApplicationPO application,
            top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO query,
            List<DdcManagementScopeBinding> bindings) {
        String namespace = application.getNamespace();
        if (!query.empty()) {
            namespace = query.namespace() == null
                    || query.namespace().isBlank()
                    ? matchedNamespace(application, bindings)
                    : query.namespace().trim();
        }
        return view(application, namespace, !bindings.isEmpty());
    }

    /**
     * 中文说明：执行 matched命名空间 操作；该方法是 {@code GatewayApplicationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the matched namespace operation; this method is the invocation entry point on {@code GatewayApplicationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationService.matchedNamespace(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param application 参数 application；parameter application。
     * @param bindings 参数 bindings；parameter bindings。
     * @return 返回 matched命名空间 的处理结果；returns the result of the operation.
     */
    private String matchedNamespace(
            GatewayApplicationPO application,
            List<DdcManagementScopeBinding> bindings) {
        return bindings.stream()
                .map(DdcManagementScopeBinding::namespaceCode)
                .filter(application.getNamespace()::equals)
                .findFirst()
                .orElseGet(() -> bindings.isEmpty()
                        ? application.getNamespace()
                        : bindings.getFirst().namespaceCode());
    }

    /**
     * 中文说明：执行 view 操作；该方法是 {@code GatewayApplicationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the view operation; this method is the invocation entry point on {@code GatewayApplicationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationService.view(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param application 参数 application；parameter application。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param ddcMatched 参数 ddcMatched；parameter ddc matched。
     * @return 返回 view 的处理结果；returns the result of the operation.
     */
    private GatewayApplicationVO view(
            GatewayApplicationPO application,
            String namespace,
            boolean ddcMatched) {
        return new GatewayApplicationVO(
                application.getId(),
                application.getBizCode(),
                application.getApplicationCode(),
                application.getDisplayName(),
                application.getEnv(),
                namespace == null ? application.getNamespace() : namespace,
                application.getDescription(),
                ddcMatched,
                application.getRevision(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }

    /**
     * 中文说明：执行 physical键 操作；该方法是 {@code GatewayApplicationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the physical key operation; this method is the invocation entry point on {@code GatewayApplicationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationService.physicalKey(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param binding 参数 binding；parameter binding。
     * @return 返回 physical键 的处理结果；returns the result of the operation.
     */
    private static top.egon.cola.component.gateway.admin.scope.domain.GatewayPhysicalApplicationKey physicalKey(
            DdcManagementScopeBinding binding) {
        return new top.egon.cola.component.gateway.admin.scope.domain.GatewayPhysicalApplicationKey(
                binding.bizCode(),
                binding.env(),
                binding.appCode()
        );
    }

    /**
     * 中文说明：执行 physical键 操作；该方法是 {@code GatewayApplicationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the physical key operation; this method is the invocation entry point on {@code GatewayApplicationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationService.physicalKey(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param application 参数 application；parameter application。
     * @return 返回 physical键 的处理结果；returns the result of the operation.
     */
    private static top.egon.cola.component.gateway.admin.scope.domain.GatewayPhysicalApplicationKey physicalKey(
            GatewayApplicationPO application) {
        return new top.egon.cola.component.gateway.admin.scope.domain.GatewayPhysicalApplicationKey(
                application.getBizCode(),
                application.getEnv(),
                application.getApplicationCode()
        );
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code GatewayApplicationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code GatewayApplicationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayApplicationService.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }






}
