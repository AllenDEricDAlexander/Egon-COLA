package top.egon.cola.component.gateway.admin.application.routing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.GatewayAdminIdempotencyConflictException;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.IdempotencyStore;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogStore;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftRepository;
import top.egon.cola.component.gateway.admin.rule.GatewayRouteDraftMapper;
import top.egon.cola.component.gateway.admin.rule.GatewayRouteTransportPolicyValidator;
import top.egon.cola.component.gateway.admin.rule.GatewayRuleCanonicalizer;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 中文说明：{@code GatewayDraftService} 是服务组件，位于当前 Gateway 模块的相关包中，负责网关草稿服务相关的职责与边界。
 * English summary: {@code GatewayDraftService} is a gateway draft service service in the current Gateway module; it owns the gateway draft service-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Service
public class GatewayDraftService {

    /**
     * 中文说明：表示 SCOPE 这一固定值；它属于 {@code GatewayDraftService} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value scope; it is a state, type, or protocol value of {@code GatewayDraftService} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final String SCOPE = "GATEWAY_DRAFT";

    /**
     * 中文说明：保存 drafts 对应的状态、依赖或配置值；字段类型为 {@code GatewayDraftRepository}，由 {@code GatewayDraftService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by drafts; its type is {@code GatewayDraftRepository}, and {@code GatewayDraftService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayDraftRepository drafts;

    /**
     * 中文说明：保存 存储 对应的状态、依赖或配置值；字段类型为 {@code GatewayDraftStore}，由 {@code GatewayDraftService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by store; its type is {@code GatewayDraftStore}, and {@code GatewayDraftService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayDraftStore store;

    /**
     * 中文说明：保存 目录 对应的状态、依赖或配置值；字段类型为 {@code GatewayCatalogStore}，由 {@code GatewayDraftService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by catalog; its type is {@code GatewayCatalogStore}, and {@code GatewayDraftService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCatalogStore catalog;

    /**
     * 中文说明：保存 idempotency 对应的状态、依赖或配置值；字段类型为 {@code IdempotencyStore}，由 {@code GatewayDraftService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by idempotency; its type is {@code IdempotencyStore}, and {@code GatewayDraftService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final IdempotencyStore idempotency;

    /**
     * 中文说明：保存 audits 对应的状态、依赖或配置值；字段类型为 {@code GatewayAuditLogRepository}，由 {@code GatewayDraftService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by audits; its type is {@code GatewayAuditLogRepository}, and {@code GatewayDraftService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayAuditLogRepository audits;

    /**
     * 中文说明：保存 canonicalizer 对应的状态、依赖或配置值；字段类型为 {@code GatewayRuleCanonicalizer}，由 {@code GatewayDraftService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by canonicalizer; its type is {@code GatewayRuleCanonicalizer}, and {@code GatewayDraftService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRuleCanonicalizer canonicalizer =
            new GatewayRuleCanonicalizer();

    /**
     * 中文说明：保存 路由映射器 对应的状态、依赖或配置值；字段类型为 {@code GatewayRouteDraftMapper}，由 {@code GatewayDraftService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by route mapper; its type is {@code GatewayRouteDraftMapper}, and {@code GatewayDraftService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRouteDraftMapper routeMapper =
            new GatewayRouteDraftMapper();

    /**
     * 中文说明：保存 传输校验器 对应的状态、依赖或配置值；字段类型为 {@code GatewayRouteTransportPolicyValidator}，由 {@code GatewayDraftService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by transport validator; its type is {@code GatewayRouteTransportPolicyValidator}, and {@code GatewayDraftService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRouteTransportPolicyValidator transportValidator =
            new GatewayRouteTransportPolicyValidator();

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code GatewayDraftService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code GatewayDraftService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：创建 {@code GatewayDraftService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayDraftService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param drafts 参数 drafts；parameter drafts。
     * @param store 参数 存储；parameter store。
     * @param catalog 参数 目录；parameter catalog。
     * @param idempotency 参数 idempotency；parameter idempotency。
     * @param audits 参数 audits；parameter audits。
     */
    @Autowired
    public GatewayDraftService(
            GatewayDraftRepository drafts,
            GatewayDraftStore store,
            GatewayCatalogStore catalog,
            IdempotencyStore idempotency,
            GatewayAuditLogRepository audits) {
        this(
                drafts,
                store,
                catalog,
                idempotency,
                audits,
                Clock.systemUTC()
        );
    }

    /**
     * 中文说明：创建 {@code GatewayDraftService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayDraftService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param drafts 参数 drafts；parameter drafts。
     * @param store 参数 存储；parameter store。
     * @param catalog 参数 目录；parameter catalog。
     * @param idempotency 参数 idempotency；parameter idempotency。
     * @param audits 参数 audits；parameter audits。
     * @param clock 参数 clock；parameter clock。
     */
    GatewayDraftService(
            GatewayDraftRepository drafts,
            GatewayDraftStore store,
            GatewayCatalogStore catalog,
            IdempotencyStore idempotency,
            GatewayAuditLogRepository audits,
            Clock clock) {
        this.drafts = drafts;
        this.store = store;
        this.catalog = catalog;
        this.idempotency = idempotency;
        this.audits = audits;
        this.clock = clock;
    }

    /**
     * 中文说明：执行 get 操作；该方法是 {@code GatewayDraftService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get operation; this method is the invocation entry point on {@code GatewayDraftService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftService.get(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 get 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public DraftView get(String gatewayGroupId) {
        GatewayDraftEntity draft = required(gatewayGroupId);
        return view(draft);
    }

    /**
     * 中文说明：执行 put路由 操作；该方法是 {@code GatewayDraftService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the put route operation; this method is the invocation entry point on {@code GatewayDraftService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftService.putRoute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param routeId 参数 路由Id；parameter route id。
     * @param command 参数 command；parameter command。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 put路由 的处理结果；returns the result of the operation.
     */
    @Transactional
    public MutationResult putRoute(
            String gatewayGroupId,
            String routeId,
            RouteMutation command,
            AdminActor actor,
            RequestAuditContext request) {
        Map<String, Object> canonicalContent = routeMapper.canonicalize(
                command.content()
        );
        RouteMutation canonicalCommand = new RouteMutation(
                command.operationId(),
                canonicalContent,
                command.enabled(),
                command.expectedRevision(),
                command.idempotencyKey(),
                command.changeReason()
        );
        String legacyDigest = digest(Map.of(
                "action", "PUT_ROUTE",
                "routeId", routeId,
                "command", command
        ));
        String canonicalDigest = digest(Map.of(
                "action", "PUT_ROUTE",
                "routeId", routeId,
                "command", canonicalCommand
        ));
        MutationResult replay = replay(
                gatewayGroupId,
                command.idempotencyKey(),
                canonicalDigest,
                legacyDigest
        );
        if (replay != null) {
            return replay;
        }
        GatewayCatalogStore.OperationRecord operation =
                catalog.findOperation(command.operationId())
                        .orElseThrow(() -> new GatewayAdminNotFoundException(
                                "gateway operation "
                                        + command.operationId()
                                        + " was not found"
                        ));
        if (isPublic(canonicalContent)
                && !operation.externalAccessible()) {
            throw new IllegalArgumentException(
                    "PUBLIC route references an internal-only operation"
            );
        }
        GatewayDraftEntity draft = editable(
                gatewayGroupId,
                command.expectedRevision()
        );
        Instant now = clock.instant();
        store.upsertRoute(new GatewayDraftStore.RouteDraft(
                gatewayGroupId,
                required(routeId, "routeId"),
                command.operationId(),
                canonicalContent,
                command.enabled(),
                now,
                actor.actorId()
        ));
        return finish(
                draft,
                "ROUTE",
                routeId,
                "UPSERT",
                command.changeReason(),
                command.idempotencyKey(),
                canonicalDigest,
                actor,
                request,
                now
        );
    }

    /**
     * 中文说明：执行 delete路由 操作；该方法是 {@code GatewayDraftService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete route operation; this method is the invocation entry point on {@code GatewayDraftService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftService.deleteRoute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param routeId 参数 路由Id；parameter route id。
     * @param control 参数 control；parameter control。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 delete路由 的处理结果；returns the result of the operation.
     */
    @Transactional
    public MutationResult deleteRoute(
            String gatewayGroupId,
            String routeId,
            MutationControl control,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest(Map.of(
                "action", "DELETE_ROUTE",
                "routeId", routeId,
                "expectedRevision", control.expectedRevision()
        ));
        MutationResult replay = replay(
                gatewayGroupId,
                control.idempotencyKey(),
                digest
        );
        if (replay != null) {
            return replay;
        }
        GatewayDraftEntity draft = editable(
                gatewayGroupId,
                control.expectedRevision()
        );
        store.deleteRoute(gatewayGroupId, routeId);
        return finish(
                draft,
                "ROUTE",
                routeId,
                "DELETE",
                control.changeReason(),
                control.idempotencyKey(),
                digest,
                actor,
                request,
                clock.instant()
        );
    }

    /**
     * 中文说明：执行 put策略 操作；该方法是 {@code GatewayDraftService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the put policy operation; this method is the invocation entry point on {@code GatewayDraftService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftService.putPolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param policyId 参数 策略Id；parameter policy id。
     * @param command 参数 command；parameter command。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 put策略 的处理结果；returns the result of the operation.
     */
    @Transactional
    public MutationResult putPolicy(
            String gatewayGroupId,
            String policyId,
            PolicyMutation command,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest(Map.of(
                "action", "PUT_POLICY",
                "policyId", policyId,
                "command", command
        ));
        MutationResult replay = replay(
                gatewayGroupId,
                command.idempotencyKey(),
                digest
        );
        if (replay != null) {
            return replay;
        }
        GatewayDraftEntity draft = editable(
                gatewayGroupId,
                command.expectedRevision()
        );
        Instant now = clock.instant();
        store.upsertPolicy(new GatewayDraftStore.PolicyDraft(
                gatewayGroupId,
                required(policyId, "policyId"),
                required(command.policyType(), "policyType")
                        .toUpperCase(),
                required(command.policyScope(), "policyScope")
                        .toUpperCase(),
                Map.copyOf(command.content()),
                command.enabled(),
                now,
                actor.actorId()
        ));
        return finish(
                draft,
                "POLICY",
                policyId,
                "UPSERT",
                command.changeReason(),
                command.idempotencyKey(),
                digest,
                actor,
                request,
                now
        );
    }

    /**
     * 中文说明：执行 delete策略 操作；该方法是 {@code GatewayDraftService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete policy operation; this method is the invocation entry point on {@code GatewayDraftService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftService.deletePolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param policyId 参数 策略Id；parameter policy id。
     * @param control 参数 control；parameter control。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 delete策略 的处理结果；returns the result of the operation.
     */
    @Transactional
    public MutationResult deletePolicy(
            String gatewayGroupId,
            String policyId,
            MutationControl control,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest(Map.of(
                "action", "DELETE_POLICY",
                "policyId", policyId,
                "expectedRevision", control.expectedRevision()
        ));
        MutationResult replay = replay(
                gatewayGroupId,
                control.idempotencyKey(),
                digest
        );
        if (replay != null) {
            return replay;
        }
        GatewayDraftEntity draft = editable(
                gatewayGroupId,
                control.expectedRevision()
        );
        store.deletePolicy(gatewayGroupId, policyId);
        return finish(
                draft,
                "POLICY",
                policyId,
                "DELETE",
                control.changeReason(),
                control.idempotencyKey(),
                digest,
                actor,
                request,
                clock.instant()
        );
    }

    /**
     * 中文说明：执行 validate 操作；该方法是 {@code GatewayDraftService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate operation; this method is the invocation entry point on {@code GatewayDraftService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftService.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 validate 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public ValidationReport validate(String gatewayGroupId) {
        DraftView draft = get(gatewayGroupId);
        List<ValidationIssue> errors = new ArrayList<>();
        List<ValidationIssue> warnings = new ArrayList<>();
        for (GatewayDraftStore.RouteDraft route : draft.routes()) {
            Map<String, Object> canonicalContent = routeMapper.canonicalize(
                    route.content()
            );
            GatewayCatalogStore.OperationRecord operation =
                    catalog.findOperation(route.operationId()).orElse(null);
            GatewayProtocol protocol = operation == null
                    ? null
                    : GatewayProtocol.valueOf(operation.protocol());
            GatewayResponseMode responseMode = operation == null
                    ? null
                    : operationResponseMode(operation.id());
            transportValidator.validate(
                    canonicalContent,
                    protocol,
                    responseMode
            ).forEach(issue -> errors.add(new ValidationIssue(
                    "routes." + route.routeId() + "." + issue.path(),
                    issue.code(),
                    issue.message()
            )));
            if (operation == null) {
                errors.add(new ValidationIssue(
                        "routes." + route.routeId() + ".operationId",
                        "OPERATION_NOT_FOUND",
                        "referenced operation does not exist"
                ));
                continue;
            }
            if ("OFFLINE".equals(operation.lifecycleStatus())) {
                errors.add(new ValidationIssue(
                        "routes." + route.routeId() + ".operationId",
                        "OPERATION_OFFLINE",
                        "offline operation cannot be published"
                ));
            } else if ("DISCOVERED".equals(
                    operation.lifecycleStatus())) {
                errors.add(new ValidationIssue(
                        "routes." + route.routeId() + ".operationId",
                        "OPERATION_NOT_ACTIVE",
                        "operation is not active on any provider"
                ));
            } else if ("DEPRECATED".equals(operation.lifecycleStatus())) {
                warnings.add(new ValidationIssue(
                        "routes." + route.routeId() + ".operationId",
                        "OPERATION_DEPRECATED",
                        "deprecated operation remains routable"
                ));
            }
            if (isPublic(canonicalContent)
                    && !operation.externalAccessible()) {
                errors.add(new ValidationIssue(
                        "routes." + route.routeId() + ".accessZones",
                        "EXTERNAL_ACCESS_DENIED",
                        "operation is not externally accessible"
                ));
            }
        }
        return new ValidationReport(
                errors.isEmpty(),
                draft.revision(),
                List.copyOf(errors),
                List.copyOf(warnings),
                digest(Map.of(
                        "routes", draft.routes(),
                        "policies", draft.policies()
                ))
        );
    }

    /**
     * 中文说明：执行 diff 操作；该方法是 {@code GatewayDraftService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the diff operation; this method is the invocation entry point on {@code GatewayDraftService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftService.diff(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 diff 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public DraftDiff diff(String gatewayGroupId) {
        DraftView draft = get(gatewayGroupId);
        return new DraftDiff(
                draft.basedOnReleaseId(),
                draft.revision(),
                draft.routes().size(),
                draft.policies().size(),
                digest(Map.of(
                        "routes", draft.routes(),
                        "policies", draft.policies()
                ))
        );
    }

    /**
     * 中文说明：执行 finish 操作；该方法是 {@code GatewayDraftService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the finish operation; this method is the invocation entry point on {@code GatewayDraftService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftService.finish(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param draft 参数 草稿；parameter draft。
     * @param resourceType 参数 资源Type；parameter resource type。
     * @param resourceId 参数 资源Id；parameter resource id。
     * @param action 参数 action；parameter action。
     * @param reason 参数 reason；parameter reason。
     * @param key 参数 键；parameter key。
     * @param payloadSha 参数 payloadSha；parameter payload sha。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @param now 参数 now；parameter now。
     * @return 返回 finish 的处理结果；returns the result of the operation.
     */
    private MutationResult finish(
            GatewayDraftEntity draft,
            String resourceType,
            String resourceId,
            String action,
            String reason,
            String key,
            String payloadSha,
            AdminActor actor,
            RequestAuditContext request,
            Instant now) {
        draft.touch(required(reason, "changeReason"), actor.actorId(), now);
        drafts.flush();
        MutationResult result = new MutationResult(
                draft.getRevision(),
                resourceId,
                false
        );
        idempotency.save(new IdempotencyStore.Record(
                SCOPE,
                draft.getGatewayGroupId(),
                required(key, "idempotencyKey"),
                payloadSha,
                resourceId,
                Map.of(
                        "revision", result.revision(),
                        "resourceId", resourceId
                ),
                now,
                now.plus(Duration.ofDays(7))
        ));
        audits.save(new GatewayAuditLogEntity(
                UuidV7.simpleString(),
                actor.actorId(),
                actor.actorType().name(),
                "MANAGEMENT_API",
                request.requestId(),
                request.traceId(),
                resourceType,
                resourceId,
                action,
                null,
                Map.of("changeReason", reason),
                draft.getRevision(),
                null,
                true,
                null,
                now
        ));
        return result;
    }

    /**
     * 中文说明：执行 replay 操作；该方法是 {@code GatewayDraftService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the replay operation; this method is the invocation entry point on {@code GatewayDraftService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftService.replay(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param key 参数 键；parameter key。
     * @param digest 参数 digest；parameter digest。
     * @param compatibleDigests 参数 compatibleDigests；parameter compatible digests。
     * @return 返回 replay 的处理结果；returns the result of the operation.
     */
    private MutationResult replay(
            String gatewayGroupId,
            String key,
            String digest,
            String... compatibleDigests) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "idempotencyKey is required"
            );
        }
        IdempotencyStore.Record existing = idempotency.find(
                SCOPE,
                gatewayGroupId,
                key
        ).orElse(null);
        if (existing == null) {
            return null;
        }
        boolean compatible = java.util.Arrays.stream(compatibleDigests)
                .anyMatch(existing.payloadSha256()::equals);
        if (!existing.payloadSha256().equals(digest) && !compatible) {
            throw new GatewayAdminIdempotencyConflictException();
        }
        Object revision = existing.response().get("revision");
        long value = revision instanceof Number number
                ? number.longValue()
                : Long.parseLong(revision.toString());
        return new MutationResult(value, existing.resourceId(), true);
    }

    /**
     * 中文说明：执行 editable 操作；该方法是 {@code GatewayDraftService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the editable operation; this method is the invocation entry point on {@code GatewayDraftService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftService.editable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @return 返回 editable 的处理结果；returns the result of the operation.
     */
    private GatewayDraftEntity editable(String id, long expectedRevision) {
        GatewayDraftEntity draft = required(id);
        draft.assertEditable(expectedRevision);
        return draft;
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code GatewayDraftService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code GatewayDraftService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftService.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private GatewayDraftEntity required(String id) {
        return drafts.findById(id)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "gateway draft " + id + " was not found"
                ));
    }

    /**
     * 中文说明：执行 view 操作；该方法是 {@code GatewayDraftService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the view operation; this method is the invocation entry point on {@code GatewayDraftService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftService.view(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param draft 参数 草稿；parameter draft。
     * @return 返回 view 的处理结果；returns the result of the operation.
     */
    private DraftView view(GatewayDraftEntity draft) {
        return new DraftView(
                draft.getGatewayGroupId(),
                draft.getRevision(),
                draft.getBasedOnReleaseId(),
                draft.getStatus(),
                draft.getChangeSummary(),
                store.routes(draft.getGatewayGroupId()),
                store.policies(draft.getGatewayGroupId()),
                draft.getUpdatedAt()
        );
    }

    /**
     * 中文说明：执行 isPublic 操作；该方法是 {@code GatewayDraftService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the is public operation; this method is the invocation entry point on {@code GatewayDraftService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftService.isPublic(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @return 返回 isPublic 的处理结果；returns the result of the operation.
     */
    private boolean isPublic(Map<String, Object> content) {
        Object zones = content.get("accessZones");
        return zones instanceof Iterable<?> values
                && java.util.stream.StreamSupport.stream(
                values.spliterator(),
                false
        ).anyMatch(value -> "PUBLIC".equalsIgnoreCase(value.toString()));
    }

    /**
     * 中文说明：执行 操作响应Mode 操作；该方法是 {@code GatewayDraftService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the operation response mode operation; this method is the invocation entry point on {@code GatewayDraftService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftService.operationResponseMode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @return 返回 操作响应Mode 的处理结果；returns the result of the operation.
     */
    private GatewayResponseMode operationResponseMode(String operationId) {
        return catalog.loadDefinitions(operationId)
                .stream()
                .findFirst()
                .map(definition -> definition.attributes().getOrDefault(
                        "responseMode",
                        "TRANSPARENT"
                ).toString())
                .map(GatewayResponseMode::valueOf)
                .orElse(GatewayResponseMode.TRANSPARENT);
    }

    /**
     * 中文说明：执行 digest 操作；该方法是 {@code GatewayDraftService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the digest operation; this method is the invocation entry point on {@code GatewayDraftService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftService.digest(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 digest 的处理结果；returns the result of the operation.
     */
    private String digest(Object value) {
        return GatewayRuleCanonicalizer.sha256(
                canonicalizer.canonicalBytes(value)
        );
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code GatewayDraftService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code GatewayDraftService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftService.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：{@code DraftView} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责草稿View相关的职责与边界。
     * English summary: {@code DraftView} is an immutable data carrier in the current Gateway module; it owns the draft view-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param revision 参数 revision；parameter revision。
     * @param basedOnReleaseId 参数 basedOn发布Id；parameter based on release id。
     * @param status 参数 status；parameter status。
     * @param changeSummary 参数 changeSummary；parameter change summary。
     * @param routes 参数 routes；parameter routes。
     * @param policies 参数 policies；parameter policies。
     * @param updatedAt 参数 updatedAt；parameter updated at。
     */
    public record DraftView(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftService.DraftView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code GatewayDraftService.DraftView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.DraftView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.DraftView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayDraftService.DraftView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code GatewayDraftService.DraftView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.DraftView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.DraftView}; do not couple callers to its representation when the owning type exposes an API.
             */
            long revision,
            /**
             * 中文说明：保存 basedOn发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftService.DraftView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by based on release id; its type is {@code String}, and {@code GatewayDraftService.DraftView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.DraftView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.DraftView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String basedOnReleaseId,
            /**
             * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftService.DraftView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code String}, and {@code GatewayDraftService.DraftView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.DraftView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.DraftView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String status,
            /**
             * 中文说明：保存 changeSummary 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftService.DraftView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change summary; its type is {@code String}, and {@code GatewayDraftService.DraftView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.DraftView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.DraftView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeSummary,
            /**
             * 中文说明：保存 routes 对应的状态、依赖或配置值；字段类型为 {@code List<GatewayDraftStore.RouteDraft>}，由 {@code GatewayDraftService.DraftView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by routes; its type is {@code List<GatewayDraftStore.RouteDraft>}, and {@code GatewayDraftService.DraftView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.DraftView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.DraftView}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<GatewayDraftStore.RouteDraft> routes,
            /**
             * 中文说明：保存 policies 对应的状态、依赖或配置值；字段类型为 {@code List<GatewayDraftStore.PolicyDraft>}，由 {@code GatewayDraftService.DraftView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by policies; its type is {@code List<GatewayDraftStore.PolicyDraft>}, and {@code GatewayDraftService.DraftView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.DraftView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.DraftView}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<GatewayDraftStore.PolicyDraft> policies,
            /**
             * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayDraftService.DraftView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code GatewayDraftService.DraftView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.DraftView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.DraftView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant updatedAt
    ) {
    }

    /**
     * 中文说明：{@code MutationControl} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责MutationControl相关的职责与边界。
     * English summary: {@code MutationControl} is an immutable data carrier in the current Gateway module; it owns the mutation control-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record MutationControl(
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayDraftService.MutationControl} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code GatewayDraftService.MutationControl} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.MutationControl} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.MutationControl}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedRevision,
            /**
             * 中文说明：保存 idempotency键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftService.MutationControl} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by idempotency key; its type is {@code String}, and {@code GatewayDraftService.MutationControl} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.MutationControl} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.MutationControl}; do not couple callers to its representation when the owning type exposes an API.
             */
            String idempotencyKey,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftService.MutationControl} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code GatewayDraftService.MutationControl} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.MutationControl} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.MutationControl}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeReason
    ) {
    }

    /**
     * 中文说明：{@code RouteMutation} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责路由Mutation相关的职责与边界。
     * English summary: {@code RouteMutation} is an immutable data carrier in the current Gateway module; it owns the route mutation-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param operationId 参数 操作Id；parameter operation id。
     * @param content 参数 content；parameter content。
     * @param enabled 参数 enabled；parameter enabled。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record RouteMutation(
            /**
             * 中文说明：保存 操作Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftService.RouteMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by operation id; its type is {@code String}, and {@code GatewayDraftService.RouteMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.RouteMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.RouteMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String operationId,
            /**
             * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayDraftService.RouteMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code Map<String, Object>}, and {@code GatewayDraftService.RouteMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.RouteMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.RouteMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> content,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayDraftService.RouteMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code GatewayDraftService.RouteMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.RouteMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.RouteMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayDraftService.RouteMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code GatewayDraftService.RouteMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.RouteMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.RouteMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedRevision,
            /**
             * 中文说明：保存 idempotency键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftService.RouteMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by idempotency key; its type is {@code String}, and {@code GatewayDraftService.RouteMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.RouteMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.RouteMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String idempotencyKey,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftService.RouteMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code GatewayDraftService.RouteMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.RouteMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.RouteMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeReason
    ) {
    }

    /**
     * 中文说明：{@code PolicyMutation} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责策略Mutation相关的职责与边界。
     * English summary: {@code PolicyMutation} is an immutable data carrier in the current Gateway module; it owns the policy mutation-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param policyType 参数 策略Type；parameter policy type。
     * @param policyScope 参数 策略Scope；parameter policy scope。
     * @param content 参数 content；parameter content。
     * @param enabled 参数 enabled；parameter enabled。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record PolicyMutation(
            /**
             * 中文说明：保存 策略Type 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftService.PolicyMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by policy type; its type is {@code String}, and {@code GatewayDraftService.PolicyMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.PolicyMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.PolicyMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String policyType,
            /**
             * 中文说明：保存 策略Scope 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftService.PolicyMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by policy scope; its type is {@code String}, and {@code GatewayDraftService.PolicyMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.PolicyMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.PolicyMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String policyScope,
            /**
             * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayDraftService.PolicyMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code Map<String, Object>}, and {@code GatewayDraftService.PolicyMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.PolicyMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.PolicyMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> content,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayDraftService.PolicyMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code GatewayDraftService.PolicyMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.PolicyMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.PolicyMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayDraftService.PolicyMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code GatewayDraftService.PolicyMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.PolicyMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.PolicyMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedRevision,
            /**
             * 中文说明：保存 idempotency键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftService.PolicyMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by idempotency key; its type is {@code String}, and {@code GatewayDraftService.PolicyMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.PolicyMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.PolicyMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String idempotencyKey,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftService.PolicyMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code GatewayDraftService.PolicyMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.PolicyMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.PolicyMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeReason
    ) {
    }

    /**
     * 中文说明：{@code MutationResult} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责MutationResult相关的职责与边界。
     * English summary: {@code MutationResult} is an immutable data carrier in the current Gateway module; it owns the mutation result-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param revision 参数 revision；parameter revision。
     * @param resourceId 参数 资源Id；parameter resource id。
     * @param replayed 参数 replayed；parameter replayed。
     */
    public record MutationResult(
            /**
             * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayDraftService.MutationResult} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code GatewayDraftService.MutationResult} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.MutationResult} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.MutationResult}; do not couple callers to its representation when the owning type exposes an API.
             */
            long revision,
            /**
             * 中文说明：保存 资源Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftService.MutationResult} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by resource id; its type is {@code String}, and {@code GatewayDraftService.MutationResult} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.MutationResult} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.MutationResult}; do not couple callers to its representation when the owning type exposes an API.
             */
            String resourceId,
            /**
             * 中文说明：保存 replayed 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayDraftService.MutationResult} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by replayed; its type is {@code boolean}, and {@code GatewayDraftService.MutationResult} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.MutationResult} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.MutationResult}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean replayed
    ) {
    }

    /**
     * 中文说明：{@code ValidationIssue} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责ValidationIssue相关的职责与边界。
     * English summary: {@code ValidationIssue} is an immutable data carrier in the current Gateway module; it owns the validation issue-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param path 参数 path；parameter path。
     * @param code 参数 code；parameter code。
     * @param message 参数 消息；parameter message。
     */
    public record ValidationIssue(
            /**
             * 中文说明：保存 path 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftService.ValidationIssue} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by path; its type is {@code String}, and {@code GatewayDraftService.ValidationIssue} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.ValidationIssue} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.ValidationIssue}; do not couple callers to its representation when the owning type exposes an API.
             */
            String path,
            /**
             * 中文说明：保存 code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftService.ValidationIssue} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by code; its type is {@code String}, and {@code GatewayDraftService.ValidationIssue} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.ValidationIssue} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.ValidationIssue}; do not couple callers to its representation when the owning type exposes an API.
             */
            String code,
            /**
             * 中文说明：保存 消息 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftService.ValidationIssue} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by message; its type is {@code String}, and {@code GatewayDraftService.ValidationIssue} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.ValidationIssue} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.ValidationIssue}; do not couple callers to its representation when the owning type exposes an API.
             */
            String message
    ) {
    }

    /**
     * 中文说明：{@code ValidationReport} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Validation报告相关的职责与边界。
     * English summary: {@code ValidationReport} is an immutable data carrier in the current Gateway module; it owns the validation report-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param valid 参数 valid；parameter valid。
     * @param revision 参数 revision；parameter revision。
     * @param errors 参数 errors；parameter errors。
     * @param warnings 参数 warnings；parameter warnings。
     * @param draftSha256 参数 草稿Sha256；parameter draft sha256。
     */
    public record ValidationReport(
            /**
             * 中文说明：保存 valid 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayDraftService.ValidationReport} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by valid; its type is {@code boolean}, and {@code GatewayDraftService.ValidationReport} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.ValidationReport} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.ValidationReport}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean valid,
            /**
             * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayDraftService.ValidationReport} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code GatewayDraftService.ValidationReport} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.ValidationReport} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.ValidationReport}; do not couple callers to its representation when the owning type exposes an API.
             */
            long revision,
            /**
             * 中文说明：保存 errors 对应的状态、依赖或配置值；字段类型为 {@code List<ValidationIssue>}，由 {@code GatewayDraftService.ValidationReport} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by errors; its type is {@code List<ValidationIssue>}, and {@code GatewayDraftService.ValidationReport} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.ValidationReport} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.ValidationReport}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<ValidationIssue> errors,
            /**
             * 中文说明：保存 warnings 对应的状态、依赖或配置值；字段类型为 {@code List<ValidationIssue>}，由 {@code GatewayDraftService.ValidationReport} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by warnings; its type is {@code List<ValidationIssue>}, and {@code GatewayDraftService.ValidationReport} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.ValidationReport} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.ValidationReport}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<ValidationIssue> warnings,
            /**
             * 中文说明：保存 草稿Sha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftService.ValidationReport} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by draft sha256; its type is {@code String}, and {@code GatewayDraftService.ValidationReport} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.ValidationReport} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.ValidationReport}; do not couple callers to its representation when the owning type exposes an API.
             */
            String draftSha256
    ) {
    }

    /**
     * 中文说明：{@code DraftDiff} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责草稿Diff相关的职责与边界。
     * English summary: {@code DraftDiff} is an immutable data carrier in the current Gateway module; it owns the draft diff-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param basedOnReleaseId 参数 basedOn发布Id；parameter based on release id。
     * @param revision 参数 revision；parameter revision。
     * @param routeCount 参数 路由Count；parameter route count。
     * @param policyCount 参数 策略Count；parameter policy count。
     * @param draftSha256 参数 草稿Sha256；parameter draft sha256。
     */
    public record DraftDiff(
            /**
             * 中文说明：保存 basedOn发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftService.DraftDiff} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by based on release id; its type is {@code String}, and {@code GatewayDraftService.DraftDiff} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.DraftDiff} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.DraftDiff}; do not couple callers to its representation when the owning type exposes an API.
             */
            String basedOnReleaseId,
            /**
             * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayDraftService.DraftDiff} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code GatewayDraftService.DraftDiff} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.DraftDiff} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.DraftDiff}; do not couple callers to its representation when the owning type exposes an API.
             */
            long revision,
            /**
             * 中文说明：保存 路由Count 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayDraftService.DraftDiff} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by route count; its type is {@code int}, and {@code GatewayDraftService.DraftDiff} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.DraftDiff} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.DraftDiff}; do not couple callers to its representation when the owning type exposes an API.
             */
            int routeCount,
            /**
             * 中文说明：保存 策略Count 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayDraftService.DraftDiff} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by policy count; its type is {@code int}, and {@code GatewayDraftService.DraftDiff} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.DraftDiff} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.DraftDiff}; do not couple callers to its representation when the owning type exposes an API.
             */
            int policyCount,
            /**
             * 中文说明：保存 草稿Sha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftService.DraftDiff} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by draft sha256; its type is {@code String}, and {@code GatewayDraftService.DraftDiff} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftService.DraftDiff} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService.DraftDiff}; do not couple callers to its representation when the owning type exposes an API.
             */
            String draftSha256
    ) {
    }
}
