package top.egon.cola.component.gateway.admin.routing.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.catalog.repository.GatewayCatalogRepository;
import top.egon.cola.component.gateway.admin.observability.domain.po.GatewayAuditLogPO;
import top.egon.cola.component.gateway.admin.observability.repository.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftMutationControlDTO;
import top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayPolicyMutationDTO;
import top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayRouteMutationDTO;
import top.egon.cola.component.gateway.admin.routing.domain.po.GatewayDraftPO;
import top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftDiffVO;
import top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftMutationResultVO;
import top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftVO;
import top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationIssueVO;
import top.egon.cola.component.gateway.admin.routing.domain.vo.GatewayDraftValidationReportVO;
import top.egon.cola.component.gateway.admin.routing.repository.GatewayDraftJpaRepository;
import top.egon.cola.component.gateway.admin.routing.repository.GatewayDraftRepository;
import top.egon.cola.component.gateway.admin.rule.service.GatewayRuleCanonicalizer;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;
import top.egon.cola.component.gateway.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminIdempotencyConflictException;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.shared.repository.IdempotencyRepository;
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
    private final GatewayDraftJpaRepository drafts;

    /**
     * 中文说明：保存 存储 对应的状态、依赖或配置值；字段类型为 {@code GatewayDraftRepository}，由 {@code GatewayDraftService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by store; its type is {@code GatewayDraftRepository}, and {@code GatewayDraftService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayDraftRepository store;

    /**
     * 中文说明：保存 目录 对应的状态、依赖或配置值；字段类型为 {@code GatewayCatalogRepository}，由 {@code GatewayDraftService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by catalog; its type is {@code GatewayCatalogRepository}, and {@code GatewayDraftService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCatalogRepository catalog;

    /**
     * 中文说明：保存 idempotency 对应的状态、依赖或配置值；字段类型为 {@code IdempotencyRepository}，由 {@code GatewayDraftService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by idempotency; its type is {@code IdempotencyRepository}, and {@code GatewayDraftService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDraftService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final IdempotencyRepository idempotency;

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
            GatewayDraftJpaRepository drafts,
            GatewayDraftRepository store,
            GatewayCatalogRepository catalog,
            IdempotencyRepository idempotency,
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
            GatewayDraftJpaRepository drafts,
            GatewayDraftRepository store,
            GatewayCatalogRepository catalog,
            IdempotencyRepository idempotency,
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
    public GatewayDraftVO get(String gatewayGroupId) {
        GatewayDraftPO draft = required(gatewayGroupId);
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
    public GatewayDraftMutationResultVO putRoute(
            String gatewayGroupId,
            String routeId,
            GatewayRouteMutationDTO command,
            AdminActor actor,
            RequestAuditContext request) {
        Map<String, Object> canonicalContent = routeMapper.canonicalize(
                command.content()
        );
        GatewayRouteMutationDTO canonicalCommand = new GatewayRouteMutationDTO(
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
        GatewayDraftMutationResultVO replay = replay(
                gatewayGroupId,
                command.idempotencyKey(),
                canonicalDigest,
                legacyDigest
        );
        if (replay != null) {
            return replay;
        }
        top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO operation =
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
        GatewayDraftPO draft = editable(
                gatewayGroupId,
                command.expectedRevision()
        );
        Instant now = clock.instant();
        store.upsertRoute(new top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO(
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
    public GatewayDraftMutationResultVO deleteRoute(
            String gatewayGroupId,
            String routeId,
            GatewayDraftMutationControlDTO control,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest(Map.of(
                "action", "DELETE_ROUTE",
                "routeId", routeId,
                "expectedRevision", control.expectedRevision()
        ));
        GatewayDraftMutationResultVO replay = replay(
                gatewayGroupId,
                control.idempotencyKey(),
                digest
        );
        if (replay != null) {
            return replay;
        }
        GatewayDraftPO draft = editable(
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
    public GatewayDraftMutationResultVO putPolicy(
            String gatewayGroupId,
            String policyId,
            GatewayPolicyMutationDTO command,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest(Map.of(
                "action", "PUT_POLICY",
                "policyId", policyId,
                "command", command
        ));
        GatewayDraftMutationResultVO replay = replay(
                gatewayGroupId,
                command.idempotencyKey(),
                digest
        );
        if (replay != null) {
            return replay;
        }
        GatewayDraftPO draft = editable(
                gatewayGroupId,
                command.expectedRevision()
        );
        Instant now = clock.instant();
        store.upsertPolicy(new top.egon.cola.component.gateway.admin.routing.domain.po.GatewayPolicyDraftPO(
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
    public GatewayDraftMutationResultVO deletePolicy(
            String gatewayGroupId,
            String policyId,
            GatewayDraftMutationControlDTO control,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest(Map.of(
                "action", "DELETE_POLICY",
                "policyId", policyId,
                "expectedRevision", control.expectedRevision()
        ));
        GatewayDraftMutationResultVO replay = replay(
                gatewayGroupId,
                control.idempotencyKey(),
                digest
        );
        if (replay != null) {
            return replay;
        }
        GatewayDraftPO draft = editable(
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
    public GatewayDraftValidationReportVO validate(String gatewayGroupId) {
        GatewayDraftVO draft = get(gatewayGroupId);
        List<GatewayDraftValidationIssueVO> errors = new ArrayList<>();
        List<GatewayDraftValidationIssueVO> warnings = new ArrayList<>();
        for (top.egon.cola.component.gateway.admin.routing.domain.po.GatewayRouteDraftPO route : draft.routes()) {
            Map<String, Object> canonicalContent = routeMapper.canonicalize(
                    route.content()
            );
            top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO operation =
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
            ).forEach(issue -> errors.add(new GatewayDraftValidationIssueVO(
                    "routes." + route.routeId() + "." + issue.path(),
                    issue.code(),
                    issue.message()
            )));
            if (operation == null) {
                errors.add(new GatewayDraftValidationIssueVO(
                        "routes." + route.routeId() + ".operationId",
                        "OPERATION_NOT_FOUND",
                        "referenced operation does not exist"
                ));
                continue;
            }
            if ("OFFLINE".equals(operation.lifecycleStatus())) {
                errors.add(new GatewayDraftValidationIssueVO(
                        "routes." + route.routeId() + ".operationId",
                        "OPERATION_OFFLINE",
                        "offline operation cannot be published"
                ));
            } else if ("DISCOVERED".equals(
                    operation.lifecycleStatus())) {
                errors.add(new GatewayDraftValidationIssueVO(
                        "routes." + route.routeId() + ".operationId",
                        "OPERATION_NOT_ACTIVE",
                        "operation is not active on any provider"
                ));
            } else if ("DEPRECATED".equals(operation.lifecycleStatus())) {
                warnings.add(new GatewayDraftValidationIssueVO(
                        "routes." + route.routeId() + ".operationId",
                        "OPERATION_DEPRECATED",
                        "deprecated operation remains routable"
                ));
            }
            if (isPublic(canonicalContent)
                    && !operation.externalAccessible()) {
                errors.add(new GatewayDraftValidationIssueVO(
                        "routes." + route.routeId() + ".accessZones",
                        "EXTERNAL_ACCESS_DENIED",
                        "operation is not externally accessible"
                ));
            }
        }
        return new GatewayDraftValidationReportVO(
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
    public GatewayDraftDiffVO diff(String gatewayGroupId) {
        GatewayDraftVO draft = get(gatewayGroupId);
        return new GatewayDraftDiffVO(
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
    private GatewayDraftMutationResultVO finish(
            GatewayDraftPO draft,
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
        GatewayDraftMutationResultVO result = new GatewayDraftMutationResultVO(
                draft.getRevision(),
                resourceId,
                false
        );
        idempotency.save(new top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO(
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
        audits.save(new GatewayAuditLogPO(
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
    private GatewayDraftMutationResultVO replay(
            String gatewayGroupId,
            String key,
            String digest,
            String... compatibleDigests) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "idempotencyKey is required"
            );
        }
        top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO existing = idempotency.find(
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
        return new GatewayDraftMutationResultVO(value, existing.resourceId(), true);
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
    private GatewayDraftPO editable(String id, long expectedRevision) {
        GatewayDraftPO draft = required(id);
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
    private GatewayDraftPO required(String id) {
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
    private GatewayDraftVO view(GatewayDraftPO draft) {
        return new GatewayDraftVO(
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
















}
