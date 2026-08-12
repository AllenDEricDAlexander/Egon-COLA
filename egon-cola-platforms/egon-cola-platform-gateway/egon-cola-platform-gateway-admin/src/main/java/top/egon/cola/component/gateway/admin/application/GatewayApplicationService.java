package top.egon.cola.component.gateway.admin.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.model.management.DdcManagementScopeBinding;
import top.egon.cola.component.gateway.admin.application.scope.GatewayScopeService;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.domain.GatewayAdminRevisionConflictException;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayApplicationRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public GatewayApplicationView create(
            CreateGatewayApplication command,
            AdminActor actor,
            RequestAuditContext request) {
        GatewayScopeService.ScopeQuery scope =
                new GatewayScopeService.ScopeQuery(
                        required(command.bizCode(), "bizCode"),
                        required(command.namespace(), "namespace"),
                        required(command.env(), "env"),
                        required(command.applicationCode(), "applicationCode")
                );
        DdcManagementScopeBinding binding = scopes.requireEnabled(scope);
        GatewayApplicationEntity existing = applications
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
        GatewayApplicationEntity application = new GatewayApplicationEntity(
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
    public List<GatewayApplicationView> list() {
        return list(new GatewayScopeService.ScopeQuery(
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
    public List<GatewayApplicationView> list(
            GatewayScopeService.ScopeQuery query) {
        List<DdcManagementScopeBinding> bindings = scopes.bindings(query);
        Map<GatewayScopeService.PhysicalApplicationKey,
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
    public GatewayApplicationView get(String id) {
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
    public GatewayApplicationView update(
            String id,
            UpdateGatewayApplication command,
            AdminActor actor,
            RequestAuditContext request) {
        GatewayApplicationEntity application = required(id);
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
    private GatewayApplicationEntity required(String id) {
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
        audits.save(new GatewayAuditLogEntity(
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
    private GatewayApplicationView scopedView(
            GatewayApplicationEntity application,
            GatewayScopeService.ScopeQuery query,
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
            GatewayApplicationEntity application,
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
    private GatewayApplicationView view(
            GatewayApplicationEntity application,
            String namespace,
            boolean ddcMatched) {
        return new GatewayApplicationView(
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
    private static GatewayScopeService.PhysicalApplicationKey physicalKey(
            DdcManagementScopeBinding binding) {
        return new GatewayScopeService.PhysicalApplicationKey(
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
    private static GatewayScopeService.PhysicalApplicationKey physicalKey(
            GatewayApplicationEntity application) {
        return new GatewayScopeService.PhysicalApplicationKey(
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

    /**
     * 中文说明：{@code CreateGatewayApplication} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Create网关Application相关的职责与边界。
     * English summary: {@code CreateGatewayApplication} is an immutable data carrier in the current Gateway module; it owns the create gateway application-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param bizCode 参数 bizCode；parameter biz code。
     * @param applicationCode 参数 applicationCode；parameter application code。
     * @param displayName 参数 displayName；parameter display name。
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param description 参数 description；parameter description。
     */
    public record CreateGatewayApplication(
            /**
             * 中文说明：保存 bizCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationService.CreateGatewayApplication} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by biz code; its type is {@code String}, and {@code GatewayApplicationService.CreateGatewayApplication} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayApplicationService.CreateGatewayApplication} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService.CreateGatewayApplication}; do not couple callers to its representation when the owning type exposes an API.
             */
            String bizCode,
            /**
             * 中文说明：保存 applicationCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationService.CreateGatewayApplication} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by application code; its type is {@code String}, and {@code GatewayApplicationService.CreateGatewayApplication} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayApplicationService.CreateGatewayApplication} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService.CreateGatewayApplication}; do not couple callers to its representation when the owning type exposes an API.
             */
            String applicationCode,
            /**
             * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationService.CreateGatewayApplication} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code GatewayApplicationService.CreateGatewayApplication} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayApplicationService.CreateGatewayApplication} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService.CreateGatewayApplication}; do not couple callers to its representation when the owning type exposes an API.
             */
            String displayName,
            /**
             * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationService.CreateGatewayApplication} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code GatewayApplicationService.CreateGatewayApplication} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayApplicationService.CreateGatewayApplication} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService.CreateGatewayApplication}; do not couple callers to its representation when the owning type exposes an API.
             */
            String env,
            /**
             * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationService.CreateGatewayApplication} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code GatewayApplicationService.CreateGatewayApplication} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayApplicationService.CreateGatewayApplication} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService.CreateGatewayApplication}; do not couple callers to its representation when the owning type exposes an API.
             */
            String namespace,
            /**
             * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationService.CreateGatewayApplication} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code GatewayApplicationService.CreateGatewayApplication} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayApplicationService.CreateGatewayApplication} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService.CreateGatewayApplication}; do not couple callers to its representation when the owning type exposes an API.
             */
            String description
    ) {
    }

    /**
     * 中文说明：{@code UpdateGatewayApplication} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Update网关Application相关的职责与边界。
     * English summary: {@code UpdateGatewayApplication} is an immutable data carrier in the current Gateway module; it owns the update gateway application-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param displayName 参数 displayName；parameter display name。
     * @param description 参数 description；parameter description。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     */
    public record UpdateGatewayApplication(
            /**
             * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationService.UpdateGatewayApplication} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code GatewayApplicationService.UpdateGatewayApplication} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayApplicationService.UpdateGatewayApplication} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService.UpdateGatewayApplication}; do not couple callers to its representation when the owning type exposes an API.
             */
            String displayName,
            /**
             * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationService.UpdateGatewayApplication} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code GatewayApplicationService.UpdateGatewayApplication} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayApplicationService.UpdateGatewayApplication} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService.UpdateGatewayApplication}; do not couple callers to its representation when the owning type exposes an API.
             */
            String description,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayApplicationService.UpdateGatewayApplication} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code GatewayApplicationService.UpdateGatewayApplication} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayApplicationService.UpdateGatewayApplication} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService.UpdateGatewayApplication}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedRevision
    ) {
    }

    /**
     * 中文说明：{@code GatewayApplicationView} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责网关ApplicationView相关的职责与边界。
     * English summary: {@code GatewayApplicationView} is an immutable data carrier in the current Gateway module; it owns the gateway application view-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param bizCode 参数 bizCode；parameter biz code。
     * @param applicationCode 参数 applicationCode；parameter application code。
     * @param displayName 参数 displayName；parameter display name。
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param description 参数 description；parameter description。
     * @param ddcMatched 参数 ddcMatched；parameter ddc matched。
     * @param revision 参数 revision；parameter revision。
     * @param createdAt 参数 createdAt；parameter created at。
     * @param updatedAt 参数 updatedAt；parameter updated at。
     */
    public record GatewayApplicationView(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationService.GatewayApplicationView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code GatewayApplicationService.GatewayApplicationView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayApplicationService.GatewayApplicationView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService.GatewayApplicationView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 bizCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationService.GatewayApplicationView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by biz code; its type is {@code String}, and {@code GatewayApplicationService.GatewayApplicationView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayApplicationService.GatewayApplicationView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService.GatewayApplicationView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String bizCode,
            /**
             * 中文说明：保存 applicationCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationService.GatewayApplicationView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by application code; its type is {@code String}, and {@code GatewayApplicationService.GatewayApplicationView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayApplicationService.GatewayApplicationView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService.GatewayApplicationView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String applicationCode,
            /**
             * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationService.GatewayApplicationView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code GatewayApplicationService.GatewayApplicationView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayApplicationService.GatewayApplicationView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService.GatewayApplicationView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String displayName,
            /**
             * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationService.GatewayApplicationView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code GatewayApplicationService.GatewayApplicationView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayApplicationService.GatewayApplicationView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService.GatewayApplicationView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String env,
            /**
             * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationService.GatewayApplicationView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code GatewayApplicationService.GatewayApplicationView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayApplicationService.GatewayApplicationView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService.GatewayApplicationView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String namespace,
            /**
             * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayApplicationService.GatewayApplicationView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code GatewayApplicationService.GatewayApplicationView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayApplicationService.GatewayApplicationView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService.GatewayApplicationView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String description,
            /**
             * 中文说明：保存 ddcMatched 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayApplicationService.GatewayApplicationView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by ddc matched; its type is {@code boolean}, and {@code GatewayApplicationService.GatewayApplicationView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayApplicationService.GatewayApplicationView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService.GatewayApplicationView}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean ddcMatched,
            /**
             * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayApplicationService.GatewayApplicationView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code GatewayApplicationService.GatewayApplicationView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayApplicationService.GatewayApplicationView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService.GatewayApplicationView}; do not couple callers to its representation when the owning type exposes an API.
             */
            long revision,
            /**
             * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayApplicationService.GatewayApplicationView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code GatewayApplicationService.GatewayApplicationView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayApplicationService.GatewayApplicationView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService.GatewayApplicationView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant createdAt,
            /**
             * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayApplicationService.GatewayApplicationView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code GatewayApplicationService.GatewayApplicationView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayApplicationService.GatewayApplicationView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayApplicationService.GatewayApplicationView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant updatedAt
    ) {
    }
}
