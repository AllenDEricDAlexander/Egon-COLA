package top.egon.cola.component.gateway.admin.group.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupCreateCommandDTO;
import top.egon.cola.component.gateway.admin.group.domain.dto.GatewayGroupUpdateCommandDTO;
import top.egon.cola.component.gateway.admin.group.domain.po.GatewayGroupPO;
import top.egon.cola.component.gateway.admin.group.domain.vo.GatewayGroupVO;
import top.egon.cola.component.gateway.admin.group.repository.GatewayGroupRepository;
import top.egon.cola.component.gateway.admin.observability.domain.po.GatewayAuditLogPO;
import top.egon.cola.component.gateway.admin.observability.repository.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.routing.domain.po.GatewayDraftPO;
import top.egon.cola.component.gateway.admin.routing.repository.GatewayDraftJpaRepository;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;
import top.egon.cola.component.gateway.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminRevisionConflictException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 中文说明：{@code GatewayGroupService} 是服务组件，位于当前 Gateway 模块的相关包中，负责网关Group服务相关的职责与边界。
 * English summary: {@code GatewayGroupService} is a gateway group service service in the current Gateway module; it owns the gateway group service-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Service
public class GatewayGroupService {

    /**
     * 中文说明：保存 groups 对应的状态、依赖或配置值；字段类型为 {@code GatewayGroupRepository}，由 {@code GatewayGroupService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by groups; its type is {@code GatewayGroupRepository}, and {@code GatewayGroupService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayGroupService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayGroupRepository groups;

    /**
     * 中文说明：保存 drafts 对应的状态、依赖或配置值；字段类型为 {@code GatewayDraftRepository}，由 {@code GatewayGroupService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by drafts; its type is {@code GatewayDraftRepository}, and {@code GatewayGroupService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayGroupService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayDraftJpaRepository drafts;

    /**
     * 中文说明：保存 audits 对应的状态、依赖或配置值；字段类型为 {@code GatewayAuditLogRepository}，由 {@code GatewayGroupService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by audits; its type is {@code GatewayAuditLogRepository}, and {@code GatewayGroupService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayGroupService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayAuditLogRepository audits;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code GatewayGroupService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code GatewayGroupService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayGroupService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayGroupService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：创建 {@code GatewayGroupService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayGroupService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param groups 参数 groups；parameter groups。
     * @param drafts 参数 drafts；parameter drafts。
     * @param audits 参数 audits；parameter audits。
     */
    @Autowired
    public GatewayGroupService(
            GatewayGroupRepository groups,
            GatewayDraftJpaRepository drafts,
            GatewayAuditLogRepository audits) {
        this(groups, drafts, audits, Clock.systemUTC());
    }

    /**
     * 中文说明：创建 {@code GatewayGroupService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayGroupService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param groups 参数 groups；parameter groups。
     * @param drafts 参数 drafts；parameter drafts。
     * @param audits 参数 audits；parameter audits。
     * @param clock 参数 clock；parameter clock。
     */
    GatewayGroupService(
            GatewayGroupRepository groups,
            GatewayDraftJpaRepository drafts,
            GatewayAuditLogRepository audits,
            Clock clock) {
        this.groups = groups;
        this.drafts = drafts;
        this.audits = audits;
        this.clock = clock;
    }

    /**
     * 中文说明：执行 create 操作；该方法是 {@code GatewayGroupService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create operation; this method is the invocation entry point on {@code GatewayGroupService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupService.create(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param command 参数 command；parameter command。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 create 的处理结果；returns the result of the operation.
     */
    @Transactional
    public GatewayGroupVO create(
            GatewayGroupCreateCommandDTO command,
            AdminActor actor,
            RequestAuditContext request) {
        Instant now = clock.instant();
        String id = UuidV7.simpleString();
        GatewayGroupPO group = new GatewayGroupPO(
                id,
                command.gatewayGroupCode(),
                command.displayName(),
                command.env(),
                command.namespace(),
                command.description(),
                actor.actorId(),
                now
        );
        groups.save(group);
        drafts.save(new GatewayDraftPO(id, actor.actorId(), now));
        audit(
                actor,
                request,
                "GATEWAY_GROUP",
                id,
                "CREATE",
                null,
                Map.of(
                        "gatewayGroupCode",
                        command.gatewayGroupCode(),
                        "env",
                        command.env(),
                        "namespace",
                        command.namespace()
                )
        );
        return view(group);
    }

    /**
     * 中文说明：执行 list 操作；该方法是 {@code GatewayGroupService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the list operation; this method is the invocation entry point on {@code GatewayGroupService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupService.list(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 list 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public List<GatewayGroupVO> list() {
        return groups.findAllByDeletedFalseOrderByCreatedAtDesc()
                .stream()
                .map(this::view)
                .toList();
    }

    /**
     * 中文说明：执行 get 操作；该方法是 {@code GatewayGroupService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get operation; this method is the invocation entry point on {@code GatewayGroupService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupService.get(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 get 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public GatewayGroupVO get(String id) {
        return view(required(id));
    }

    /**
     * 中文说明：执行 update 操作；该方法是 {@code GatewayGroupService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update operation; this method is the invocation entry point on {@code GatewayGroupService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupService.update(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param command 参数 command；parameter command。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 update 的处理结果；returns the result of the operation.
     */
    @Transactional
    public GatewayGroupVO update(
            String id,
            GatewayGroupUpdateCommandDTO command,
            AdminActor actor,
            RequestAuditContext request) {
        GatewayGroupPO group = required(id);
        assertRevision(command.expectedRevision(), group.getRevision());
        Map<String, Object> before = Map.of(
                "displayName",
                group.getDisplayName(),
                "enabled",
                group.isEnabled()
        );
        group.update(
                command.displayName(),
                command.description(),
                actor.actorId(),
                clock.instant()
        );
        groups.flush();
        audit(
                actor,
                request,
                "GATEWAY_GROUP",
                id,
                "UPDATE",
                before,
                Map.of(
                        "displayName",
                        group.getDisplayName(),
                        "enabled",
                        group.isEnabled()
                )
        );
        return view(group);
    }

    /**
     * 中文说明：执行 setEnabled 操作；该方法是 {@code GatewayGroupService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set enabled operation; this method is the invocation entry point on {@code GatewayGroupService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupService.setEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param enabled 参数 enabled；parameter enabled。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 setEnabled 的处理结果；returns the result of the operation.
     */
    @Transactional
    public GatewayGroupVO setEnabled(
            String id,
            boolean enabled,
            AdminActor actor,
            RequestAuditContext request) {
        GatewayGroupPO group = required(id);
        group.setEnabled(enabled, actor.actorId(), clock.instant());
        groups.flush();
        audit(
                actor,
                request,
                "GATEWAY_GROUP",
                id,
                enabled ? "ENABLE" : "DISABLE",
                Map.of("enabled", !enabled),
                Map.of("enabled", enabled)
        );
        return view(group);
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code GatewayGroupService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code GatewayGroupService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupService.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private GatewayGroupPO required(String id) {
        return groups.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "gateway group " + id + " was not found"
                ));
    }

    /**
     * 中文说明：执行 assertRevision 操作；该方法是 {@code GatewayGroupService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the assert revision operation; this method is the invocation entry point on {@code GatewayGroupService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupService.assertRevision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param expected 参数 expected；parameter expected。
     * @param current 参数 current；parameter current。
     */
    private void assertRevision(long expected, long current) {
        if (expected != current) {
            throw new GatewayAdminRevisionConflictException(current);
        }
    }

    /**
     * 中文说明：执行 审计 操作；该方法是 {@code GatewayGroupService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audit operation; this method is the invocation entry point on {@code GatewayGroupService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupService.audit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @param resourceType 参数 资源Type；parameter resource type。
     * @param resourceId 参数 资源Id；parameter resource id。
     * @param action 参数 action；parameter action。
     * @param before 参数 before；parameter before。
     * @param after 参数 after；parameter after。
     */
    private void audit(
            AdminActor actor,
            RequestAuditContext request,
            String resourceType,
            String resourceId,
            String action,
            Map<String, Object> before,
            Map<String, Object> after) {
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
                before,
                after,
                null,
                null,
                true,
                null,
                clock.instant()
        ));
    }

    /**
     * 中文说明：执行 view 操作；该方法是 {@code GatewayGroupService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the view operation; this method is the invocation entry point on {@code GatewayGroupService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayGroupService.view(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param group 参数 group；parameter group。
     * @return 返回 view 的处理结果；returns the result of the operation.
     */
    private GatewayGroupVO view(GatewayGroupPO group) {
        return new GatewayGroupVO(
                group.getId(),
                group.getGatewayGroupCode(),
                group.getDisplayName(),
                group.getEnv(),
                group.getNamespace(),
                group.getDescription(),
                group.isEnabled(),
                group.getRevision(),
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }






}
