package top.egon.cola.component.gateway.admin.mcp.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.GatewayAdminIdempotencyConflictException;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.IdempotencyStore;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftRepository;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpManagedToolOverrideStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpRemoteProviderStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpRemoteToolDraftStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.McpServerEntity;
import top.egon.cola.component.gateway.admin.mcp.persistence.McpServerRepository;
import top.egon.cola.component.gateway.admin.rule.GatewayRuleCanonicalizer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 中文说明：{@code McpToolAdminService} 是服务组件，位于当前 Gateway 模块的相关包中，负责MCP工具管理端服务相关的职责与边界。
 * English summary: {@code McpToolAdminService} is a mcp tool admin service service in the current Gateway module; it owns the mcp tool admin service-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Service
public class McpToolAdminService {

    /**
     * 中文说明：表示 IDEMPOTENCYSCOPE 这一固定值；它属于 {@code McpToolAdminService} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value idempotency scope; it is a state, type, or protocol value of {@code McpToolAdminService} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolAdminService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final String IDEMPOTENCY_SCOPE = "GATEWAY_MCP";

    /**
     * 中文说明：表示 RISKLEVELS 这一固定值；它属于 {@code McpToolAdminService} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value risk levels; it is a state, type, or protocol value of {@code McpToolAdminService} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolAdminService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final List<String> RISK_LEVELS = List.of(
            "LOW", "MEDIUM", "HIGH", "CRITICAL"
    );

    /**
     * 中文说明：保存 content工厂 对应的状态、依赖或配置值；字段类型为 {@code McpReleaseContentFactory}，由 {@code McpToolAdminService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by content factory; its type is {@code McpReleaseContentFactory}, and {@code McpToolAdminService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolAdminService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpReleaseContentFactory contentFactory;

    /**
     * 中文说明：保存 validation 对应的状态、依赖或配置值；字段类型为 {@code McpValidationService}，由 {@code McpToolAdminService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by validation; its type is {@code McpValidationService}, and {@code McpToolAdminService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolAdminService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpValidationService validation;

    /**
     * 中文说明：保存 managedOverrides 对应的状态、依赖或配置值；字段类型为 {@code JdbcMcpManagedToolOverrideStore}，由 {@code McpToolAdminService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by managed overrides; its type is {@code JdbcMcpManagedToolOverrideStore}, and {@code McpToolAdminService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolAdminService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcMcpManagedToolOverrideStore managedOverrides;

    /**
     * 中文说明：保存 远程Tools 对应的状态、依赖或配置值；字段类型为 {@code JdbcMcpRemoteToolDraftStore}，由 {@code McpToolAdminService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by remote tools; its type is {@code JdbcMcpRemoteToolDraftStore}, and {@code McpToolAdminService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolAdminService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcMcpRemoteToolDraftStore remoteTools;

    /**
     * 中文说明：保存 远程 对应的状态、依赖或配置值；字段类型为 {@code JdbcMcpRemoteProviderStore}，由 {@code McpToolAdminService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by remote; its type is {@code JdbcMcpRemoteProviderStore}, and {@code McpToolAdminService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolAdminService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcMcpRemoteProviderStore remote;

    /**
     * 中文说明：保存 servers 对应的状态、依赖或配置值；字段类型为 {@code McpServerRepository}，由 {@code McpToolAdminService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by servers; its type is {@code McpServerRepository}, and {@code McpToolAdminService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolAdminService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpServerRepository servers;

    /**
     * 中文说明：保存 drafts 对应的状态、依赖或配置值；字段类型为 {@code GatewayDraftRepository}，由 {@code McpToolAdminService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by drafts; its type is {@code GatewayDraftRepository}, and {@code McpToolAdminService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolAdminService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayDraftRepository drafts;

    /**
     * 中文说明：保存 idempotency 对应的状态、依赖或配置值；字段类型为 {@code IdempotencyStore}，由 {@code McpToolAdminService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by idempotency; its type is {@code IdempotencyStore}, and {@code McpToolAdminService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolAdminService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final IdempotencyStore idempotency;

    /**
     * 中文说明：保存 audits 对应的状态、依赖或配置值；字段类型为 {@code GatewayAuditLogRepository}，由 {@code McpToolAdminService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by audits; its type is {@code GatewayAuditLogRepository}, and {@code McpToolAdminService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolAdminService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayAuditLogRepository audits;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code McpToolAdminService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code McpToolAdminService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolAdminService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：保存 canonicalizer 对应的状态、依赖或配置值；字段类型为 {@code GatewayRuleCanonicalizer}，由 {@code McpToolAdminService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by canonicalizer; its type is {@code GatewayRuleCanonicalizer}, and {@code McpToolAdminService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolAdminService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRuleCanonicalizer canonicalizer =
            new GatewayRuleCanonicalizer();

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code McpToolAdminService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code McpToolAdminService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpToolAdminService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：创建 {@code McpToolAdminService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpToolAdminService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param contentFactory 参数 content工厂；parameter content factory。
     * @param validation 参数 validation；parameter validation。
     * @param managedOverrides 参数 managedOverrides；parameter managed overrides。
     * @param remoteTools 参数 远程Tools；parameter remote tools。
     * @param remote 参数 远程；parameter remote。
     * @param servers 参数 servers；parameter servers。
     * @param drafts 参数 drafts；parameter drafts。
     * @param idempotency 参数 idempotency；parameter idempotency。
     * @param audits 参数 audits；parameter audits。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    @Autowired
    public McpToolAdminService(
            McpReleaseContentFactory contentFactory,
            McpValidationService validation,
            JdbcMcpManagedToolOverrideStore managedOverrides,
            JdbcMcpRemoteToolDraftStore remoteTools,
            JdbcMcpRemoteProviderStore remote,
            McpServerRepository servers,
            GatewayDraftRepository drafts,
            IdempotencyStore idempotency,
            GatewayAuditLogRepository audits,
            ObjectMapper objectMapper) {
        this(
                contentFactory,
                validation,
                managedOverrides,
                remoteTools,
                remote,
                servers,
                drafts,
                idempotency,
                audits,
                objectMapper,
                Clock.systemUTC()
        );
    }

    /**
     * 中文说明：创建 {@code McpToolAdminService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpToolAdminService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param contentFactory 参数 content工厂；parameter content factory。
     * @param validation 参数 validation；parameter validation。
     * @param managedOverrides 参数 managedOverrides；parameter managed overrides。
     * @param remoteTools 参数 远程Tools；parameter remote tools。
     * @param remote 参数 远程；parameter remote。
     * @param servers 参数 servers；parameter servers。
     * @param drafts 参数 drafts；parameter drafts。
     * @param idempotency 参数 idempotency；parameter idempotency。
     * @param audits 参数 audits；parameter audits。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param clock 参数 clock；parameter clock。
     */
    McpToolAdminService(
            McpReleaseContentFactory contentFactory,
            McpValidationService validation,
            JdbcMcpManagedToolOverrideStore managedOverrides,
            JdbcMcpRemoteToolDraftStore remoteTools,
            JdbcMcpRemoteProviderStore remote,
            McpServerRepository servers,
            GatewayDraftRepository drafts,
            IdempotencyStore idempotency,
            GatewayAuditLogRepository audits,
            ObjectMapper objectMapper,
            Clock clock) {
        this.contentFactory = contentFactory;
        this.validation = validation;
        this.managedOverrides = managedOverrides;
        this.remoteTools = remoteTools;
        this.remote = remote;
        this.servers = servers;
        this.drafts = drafts;
        this.idempotency = idempotency;
        this.audits = audits;
        this.objectMapper = objectMapper.copy();
        this.clock = clock;
    }

    /**
     * 中文说明：执行 managedTools 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the managed tools operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.managedTools(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverId 参数 服务器Id；parameter server id。
     * @return 返回 managedTools 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public List<ManagedToolView> managedTools(
            String gatewayGroupId,
            String serverId) {
        String groupId = required(gatewayGroupId, "gatewayGroupId");
        if (serverId != null) {
            requiredServerInGroup(serverId, groupId);
        }
        return contentFactory.managedTools(groupId).stream()
                .filter(item -> serverId == null
                        || serverId.equals(item.serverId()))
                .map(this::managedView)
                .toList();
    }

    /**
     * 中文说明：执行 putOverride 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the put override operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.putOverride(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param toolId 参数 工具Id；parameter tool id。
     * @param command 参数 command；parameter command。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 putOverride 的处理结果；returns the result of the operation.
     */
    @Transactional
    public McpControlPlaneService.MutationResult putOverride(
            String toolId,
            ManagedToolOverrideMutation command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("PUT_MANAGED_TOOL_OVERRIDE", Map.of(
                "toolId", toolId,
                "command", command
        ));
        McpControlPlaneService.MutationResult replay = replay(
                command.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        McpReleaseContentFactory.ManagedToolProjection managed =
                requiredManaged(command.gatewayGroupId(), toolId);
        if (Boolean.TRUE.equals(command.enabled())) {
            throw new IllegalArgumentException(
                    "managed Tool override can only disable a Tool"
            );
        }
        String serverId = optional(command.serverId());
        if (serverId != null) {
            requiredServerInGroup(serverId, command.gatewayGroupId());
        }
        Set<String> additions = clean(command.additionalPermissions());
        if (!additions.containsAll(managed.additionalPermissions())) {
            throw new IllegalArgumentException(
                    "additionalPermissions cannot remove existing permissions"
            );
        }
        String minimumRisk = optional(command.minimumRiskLevel());
        if (minimumRisk != null
                && risk(minimumRisk) < risk(managed.codeRiskLevel())) {
            throw new IllegalArgumentException(
                    "minimumRiskLevel cannot lower annotation riskLevel"
            );
        }
        String currentMinimumRisk = managed.minimumRiskLevel();
        if (currentMinimumRisk != null
                && (minimumRisk == null
                || risk(minimumRisk) < risk(currentMinimumRisk))) {
            throw new IllegalArgumentException(
                    "minimumRiskLevel cannot lower existing minimum risk"
            );
        }
        if (!managed.tool().enabled()
                && !Boolean.FALSE.equals(command.enabled())) {
            throw new IllegalArgumentException(
                    "disabled managed Tool can only be restored by deleting "
                            + "its override"
            );
        }
        GatewayDraftEntity draft = editable(
                command.gatewayGroupId(),
                command.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = managedOverrides.save(
                new JdbcMcpManagedToolOverrideStore.ManagedToolOverride(
                        toolId,
                        command.gatewayGroupId(),
                        managed.tool().operationId(),
                        serverId,
                        additions,
                        minimumRisk,
                        command.enabled(),
                        command.expectedRevision()
                ),
                command.expectedRevision(),
                actor,
                now
        );
        validation.requireValid(contentFactory.preview(
                command.gatewayGroupId()
        ));
        return finish(
                draft,
                toolId,
                mutation.revision(),
                "MCP_MANAGED_TOOL_OVERRIDE",
                "UPDATE",
                command.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of("operationId", managed.tool().operationId()),
                now
        );
    }

    /**
     * 中文说明：执行 deleteOverride 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete override operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.deleteOverride(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param toolId 参数 工具Id；parameter tool id。
     * @param control 参数 control；parameter control。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 deleteOverride 的处理结果；returns the result of the operation.
     */
    @Transactional
    public McpControlPlaneService.MutationResult deleteOverride(
            String toolId,
            MutationControl control,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("DELETE_MANAGED_TOOL_OVERRIDE", Map.of(
                "toolId", toolId,
                "control", control
        ));
        McpControlPlaneService.MutationResult replay = replay(
                control.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        McpReleaseContentFactory.ManagedToolProjection managed =
                requiredManaged(control.gatewayGroupId(), toolId);
        GatewayDraftEntity draft = editable(
                control.gatewayGroupId(),
                control.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = managedOverrides.delete(
                toolId,
                control.gatewayGroupId(),
                managed.tool().operationId(),
                control.expectedRevision()
        );
        validation.requireValid(contentFactory.preview(
                control.gatewayGroupId()
        ));
        return finish(
                draft,
                toolId,
                mutation.revision(),
                "MCP_MANAGED_TOOL_OVERRIDE",
                "DELETE",
                control.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of("operationId", managed.tool().operationId()),
                now
        );
    }

    /**
     * 中文说明：执行 远程Tools 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the remote tools operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.remoteTools(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverId 参数 服务器Id；parameter server id。
     * @return 返回 远程Tools 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public List<RemoteToolView> remoteTools(
            String gatewayGroupId,
            String serverId) {
        String groupId = required(gatewayGroupId, "gatewayGroupId");
        Map<String, McpServerEntity> serverById = servers
                .findAllByGatewayGroupIdAndDeletedFalseOrderByServerCode(
                        groupId
                ).stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                        McpServerEntity::getId,
                        java.util.function.Function.identity()
                ));
        if (serverId != null && !serverById.containsKey(serverId)) {
            throw notFound("MCP Server", serverId);
        }
        return remoteTools.load(groupId).stream()
                .filter(item -> serverId == null
                        || serverId.equals(item.serverId()))
                .map(item -> remoteView(item, serverById.get(item.serverId())))
                .toList();
    }

    /**
     * 中文说明：执行 put远程工具 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the put remote tool operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.putRemoteTool(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param command 参数 command；parameter command。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 put远程工具 的处理结果；returns the result of the operation.
     */
    @Transactional
    public McpControlPlaneService.MutationResult putRemoteTool(
            String id,
            RemoteToolMutation command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = id == null
                ? digest("CREATE_REMOTE_TOOL", command)
                : digest("UPDATE_REMOTE_TOOL", Map.of(
                        "id", id,
                        "command", command
                ));
        McpControlPlaneService.MutationResult replay = replay(
                command.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        String resourceId = id == null ? UuidV7.simpleString() : id;
        requiredServerInGroup(command.serverId(), command.gatewayGroupId());
        requireRemoteMount(
                command.remoteMountId(),
                command.gatewayGroupId(),
                command.serverId()
        );
        if (id != null) {
            var existing = requiredRemoteTool(
                    command.gatewayGroupId(),
                    id
            );
            if (!existing.serverId().equals(command.serverId())) {
                throw new IllegalArgumentException(
                        "remote MCP Tool cannot move between Servers"
                );
            }
        }
        risk(defaulted(command.riskLevel(), "LOW"));
        GatewayDraftEntity draft = editable(
                command.gatewayGroupId(),
                command.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = remoteTools.save(
                new JdbcMcpRemoteToolDraftStore.RemoteToolDraft(
                        resourceId,
                        command.gatewayGroupId(),
                        command.serverId(),
                        command.name(),
                        command.remoteMountId(),
                        remoteContent(command),
                        command.enabled(),
                        command.expectedRevision()
                ),
                command.expectedRevision(),
                actor,
                now
        );
        validation.requireValid(contentFactory.preview(
                command.gatewayGroupId()
        ));
        return finish(
                draft,
                resourceId,
                mutation.revision(),
                "MCP_REMOTE_TOOL",
                id == null ? "CREATE" : "UPDATE",
                command.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of("name", command.name()),
                now
        );
    }

    /**
     * 中文说明：执行 delete远程工具 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete remote tool operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.deleteRemoteTool(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param control 参数 control；parameter control。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 delete远程工具 的处理结果；returns the result of the operation.
     */
    @Transactional
    public McpControlPlaneService.MutationResult deleteRemoteTool(
            String id,
            MutationControl control,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("DELETE_REMOTE_TOOL", Map.of(
                "id", id,
                "control", control
        ));
        McpControlPlaneService.MutationResult replay = replay(
                control.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        var existing = requiredRemoteTool(control.gatewayGroupId(), id);
        GatewayDraftEntity draft = editable(
                control.gatewayGroupId(),
                control.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = remoteTools.softDelete(
                id,
                control.expectedRevision(),
                actor,
                now
        );
        validation.requireValid(contentFactory.preview(
                control.gatewayGroupId()
        ));
        return finish(
                draft,
                id,
                mutation.revision(),
                "MCP_REMOTE_TOOL",
                "DELETE",
                control.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of("name", existing.name()),
                now
        );
    }

    /**
     * 中文说明：执行 managedView 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the managed view operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.managedView(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 managedView 的处理结果；returns the result of the operation.
     */
    private ManagedToolView managedView(
            McpReleaseContentFactory.ManagedToolProjection value) {
        var tool = value.tool();
        return new ManagedToolView(
                tool.toolId(),
                value.gatewayGroupId(),
                tool.operationId(),
                value.operationKey(),
                tool.name(),
                tool.description(),
                tool.operationProtocol(),
                schema(tool.inputSchema()),
                schema(tool.outputSchema()),
                value.codeServerId(),
                value.codeServerCode(),
                value.serverId(),
                tool.serverCode(),
                value.codePermissions(),
                value.additionalPermissions(),
                tool.requiredPermissions(),
                value.codeRiskLevel(),
                value.minimumRiskLevel(),
                tool.riskLevel(),
                tool.idempotent(),
                tool.enabled(),
                value.overrideRevision()
        );
    }

    /**
     * 中文说明：执行 远程View 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the remote view operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.remoteView(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param server 参数 服务器；parameter server。
     * @return 返回 远程View 的处理结果；returns the result of the operation.
     */
    private RemoteToolView remoteView(
            JdbcMcpRemoteToolDraftStore.RemoteToolDraft value,
            McpServerEntity server) {
        if (server == null) {
            throw notFound("MCP Server", value.serverId());
        }
        Map<String, Object> content = value.content();
        return new RemoteToolView(
                value.id(),
                value.gatewayGroupId(),
                value.serverId(),
                server.getServerCode(),
                value.name(),
                optionalText(content.get("description")),
                value.remoteMountId(),
                content.get("inputSchema"),
                content.get("outputSchema"),
                stringMap(content.get("annotations")),
                clean(content.get("requiredPermissions")),
                defaulted(optionalText(content.get("riskLevel")), "LOW"),
                flag(content.get("idempotent")),
                value.enabled(),
                value.revision()
        );
    }

    /**
     * 中文说明：执行 远程Content 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the remote content operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.remoteContent(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param command 参数 command；parameter command。
     * @return 返回 远程Content 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> remoteContent(RemoteToolMutation command) {
        Map<String, Object> content = new LinkedHashMap<>();
        put(content, "description", command.description());
        put(content, "inputSchema", command.inputSchema());
        put(content, "outputSchema", command.outputSchema());
        if (command.annotations() != null && !command.annotations().isEmpty()) {
            content.put("annotations", Map.copyOf(command.annotations()));
        }
        Set<String> permissions = clean(command.requiredPermissions());
        if (!permissions.isEmpty()) {
            content.put("requiredPermissions", permissions);
        }
        content.put("riskLevel", defaulted(command.riskLevel(), "LOW"));
        content.put("idempotent", command.idempotent());
        return Map.copyOf(content);
    }

    /**
     * 中文说明：执行 require远程Mount 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require remote mount operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.requireRemoteMount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param mountId 参数 mountId；parameter mount id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverId 参数 服务器Id；parameter server id。
     */
    private void requireRemoteMount(
            String mountId,
            String gatewayGroupId,
            String serverId) {
        var mount = remote.mounts(gatewayGroupId).stream()
                .filter(item -> item.id().equals(mountId))
                .findFirst()
                .orElseThrow(() -> notFound("MCP Remote Mount", mountId));
        if (!mount.serverId().equals(serverId)) {
            throw new IllegalArgumentException(
                    "remote MCP Tool mount belongs to another Server"
            );
        }
    }

    /**
     * 中文说明：执行 requiredManaged 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required managed operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.requiredManaged(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param toolId 参数 工具Id；parameter tool id。
     * @return 返回 requiredManaged 的处理结果；returns the result of the operation.
     */
    private McpReleaseContentFactory.ManagedToolProjection requiredManaged(
            String gatewayGroupId,
            String toolId) {
        return contentFactory.managedTools(gatewayGroupId).stream()
                .filter(item -> item.tool().toolId().equals(toolId))
                .findFirst()
                .orElseThrow(() -> notFound("MCP Managed Tool", toolId));
    }

    /**
     * 中文说明：执行 required远程工具 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required remote tool operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.requiredRemoteTool(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param id 参数 id；parameter id。
     * @return 返回 required远程工具 的处理结果；returns the result of the operation.
     */
    private JdbcMcpRemoteToolDraftStore.RemoteToolDraft requiredRemoteTool(
            String gatewayGroupId,
            String id) {
        return remoteTools.load(gatewayGroupId).stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("MCP Remote Tool", id));
    }

    /**
     * 中文说明：执行 required服务器InGroup 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required server in group operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.requiredServerInGroup(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 required服务器InGroup 的处理结果；returns the result of the operation.
     */
    private McpServerEntity requiredServerInGroup(
            String id,
            String gatewayGroupId) {
        McpServerEntity server = servers.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> notFound("MCP Server", id));
        if (!server.getGatewayGroupId().equals(gatewayGroupId)) {
            throw new IllegalArgumentException(
                    "MCP resource belongs to another Gateway Group"
            );
        }
        return server;
    }

    /**
     * 中文说明：执行 editable 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the editable operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.editable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @return 返回 editable 的处理结果；returns the result of the operation.
     */
    private GatewayDraftEntity editable(
            String gatewayGroupId,
            long expectedRevision) {
        GatewayDraftEntity draft = drafts.findById(gatewayGroupId)
                .orElseThrow(() -> notFound("Gateway Draft", gatewayGroupId));
        draft.assertEditable(expectedRevision);
        return draft;
    }

    /**
     * 中文说明：执行 finish 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the finish operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.finish(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param draft 参数 草稿；parameter draft。
     * @param resourceId 参数 资源Id；parameter resource id。
     * @param resourceRevision 参数 资源Revision；parameter resource revision。
     * @param resourceType 参数 资源Type；parameter resource type。
     * @param action 参数 action；parameter action。
     * @param changeReason 参数 changeReason；parameter change reason。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param payloadDigest 参数 payloadDigest；parameter payload digest。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @param summary 参数 summary；parameter summary。
     * @param now 参数 now；parameter now。
     * @return 返回 finish 的处理结果；returns the result of the operation.
     */
    private McpControlPlaneService.MutationResult finish(
            GatewayDraftEntity draft,
            String resourceId,
            long resourceRevision,
            String resourceType,
            String action,
            String changeReason,
            String idempotencyKey,
            String payloadDigest,
            AdminActor actor,
            RequestAuditContext request,
            Map<String, Object> summary,
            Instant now) {
        draft.touch(required(changeReason, "changeReason"), actor.actorId(), now);
        drafts.flush();
        var result = new McpControlPlaneService.MutationResult(
                draft.getRevision(),
                resourceId,
                resourceRevision,
                false
        );
        idempotency.save(new IdempotencyStore.Record(
                IDEMPOTENCY_SCOPE,
                draft.getGatewayGroupId(),
                required(idempotencyKey, "idempotencyKey"),
                payloadDigest,
                resourceId,
                Map.of(
                        "draftRevision", result.draftRevision(),
                        "resourceRevision", result.resourceRevision()
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
                summary,
                null,
                null,
                true,
                null,
                now
        ));
        return result;
    }

    /**
     * 中文说明：执行 replay 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the replay operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.replay(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param payloadDigest 参数 payloadDigest；parameter payload digest。
     * @return 返回 replay 的处理结果；returns the result of the operation.
     */
    private McpControlPlaneService.MutationResult replay(
            String gatewayGroupId,
            String idempotencyKey,
            String payloadDigest) {
        IdempotencyStore.Record existing = idempotency.find(
                IDEMPOTENCY_SCOPE,
                gatewayGroupId,
                required(idempotencyKey, "idempotencyKey")
        ).orElse(null);
        if (existing == null) {
            return null;
        }
        if (!existing.payloadSha256().equals(payloadDigest)) {
            throw new GatewayAdminIdempotencyConflictException();
        }
        return new McpControlPlaneService.MutationResult(
                number(existing.response(), "draftRevision"),
                existing.resourceId(),
                number(existing.response(), "resourceRevision"),
                true
        );
    }

    /**
     * 中文说明：执行 digest 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the digest operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.digest(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param action 参数 action；parameter action。
     * @param command 参数 command；parameter command。
     * @return 返回 digest 的处理结果；returns the result of the operation.
     */
    private String digest(String action, Object command) {
        return GatewayRuleCanonicalizer.sha256(canonicalizer.canonicalBytes(
                Map.of("action", action, "command", command)
        ));
    }

    /**
     * 中文说明：执行 risk 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the risk operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.risk(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 risk 的处理结果；returns the result of the operation.
     */
    private int risk(String value) {
        int level = RISK_LEVELS.indexOf(value);
        if (level < 0) {
            throw new IllegalArgumentException(
                    "unsupported MCP Tool risk level " + value
            );
        }
        return level;
    }

    /**
     * 中文说明：执行 模式 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the schema operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.schema(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 模式 的处理结果；returns the result of the operation.
     */
    private Object schema(String value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, Object.class);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("stored MCP schema is invalid", failure);
        }
    }

    /**
     * 中文说明：执行 stringMap 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the string map operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.stringMap(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 stringMap 的处理结果；returns the result of the operation.
     */
    private Map<String, String> stringMap(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(
                key.toString(),
                Objects.toString(item, "")
        ));
        return Map.copyOf(result);
    }

    /**
     * 中文说明：执行 clean 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the clean operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.clean(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 clean 的处理结果；returns the result of the operation.
     */
    private Set<String> clean(Object value) {
        if (value == null) {
            return Set.of();
        }
        Iterable<?> values = value instanceof Iterable<?> iterable
                ? iterable
                : List.of(value);
        Set<String> result = new LinkedHashSet<>();
        values.forEach(item -> {
            String text = optionalText(item);
            if (text != null) {
                result.add(text);
            }
        });
        return Set.copyOf(result);
    }

    /**
     * 中文说明：执行 put 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the put operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.put(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param target 参数 target；parameter target。
     * @param key 参数 键；parameter key。
     * @param value 参数 值；parameter value。
     */
    private void put(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    /**
     * 中文说明：执行 flag 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the flag operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.flag(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 flag 的处理结果；returns the result of the operation.
     */
    private boolean flag(Object value) {
        return value instanceof Boolean bool
                ? bool
                : Boolean.parseBoolean(Objects.toString(value, "false"));
    }

    /**
     * 中文说明：执行 number 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the number operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.number(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param key 参数 键；parameter key。
     * @return 返回 number 的处理结果；returns the result of the operation.
     */
    private long number(Map<String, Object> value, String key) {
        Object item = value.get(key);
        return item instanceof Number number
                ? number.longValue()
                : Long.parseLong(item.toString());
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private String required(String value, String field) {
        String result = optional(value);
        if (result == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return result;
    }

    /**
     * 中文说明：执行 optional 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the optional operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.optional(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 optional 的处理结果；returns the result of the operation.
     */
    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 中文说明：执行 optionalText 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the optional text operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.optionalText(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 optionalText 的处理结果；returns the result of the operation.
     */
    private String optionalText(Object value) {
        return value == null ? null : optional(value.toString());
    }

    /**
     * 中文说明：执行 defaulted 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the defaulted operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.defaulted(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param defaultValue 参数 default值；parameter default value。
     * @return 返回 defaulted 的处理结果；returns the result of the operation.
     */
    private String defaulted(String value, String defaultValue) {
        String result = optional(value);
        return result == null ? defaultValue : result;
    }

    /**
     * 中文说明：执行 notFound 操作；该方法是 {@code McpToolAdminService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the not found operation; this method is the invocation entry point on {@code McpToolAdminService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpToolAdminService.notFound(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param resource 参数 资源；parameter resource。
     * @param id 参数 id；parameter id。
     * @return 返回 notFound 的处理结果；returns the result of the operation.
     */
    private GatewayAdminNotFoundException notFound(
            String resource,
            String id) {
        return new GatewayAdminNotFoundException(
                resource + " " + id + " was not found"
        );
    }

    /**
     * 中文说明：{@code ManagedToolOverrideMutation} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Managed工具OverrideMutation相关的职责与边界。
     * English summary: {@code ManagedToolOverrideMutation} is an immutable data carrier in the current Gateway module; it owns the managed tool override mutation-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param enabled 参数 enabled；parameter enabled。
     * @param serverId 参数 服务器Id；parameter server id。
     * @param additionalPermissions 参数 additionalPermissions；parameter additional permissions。
     * @param minimumRiskLevel 参数 minimumRiskLevel；parameter minimum risk level。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record ManagedToolOverrideMutation(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.ManagedToolOverrideMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpToolAdminService.ManagedToolOverrideMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolOverrideMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolOverrideMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code Boolean}，由 {@code McpToolAdminService.ManagedToolOverrideMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code Boolean}, and {@code McpToolAdminService.ManagedToolOverrideMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolOverrideMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolOverrideMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            Boolean enabled,
            /**
             * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.ManagedToolOverrideMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code McpToolAdminService.ManagedToolOverrideMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolOverrideMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolOverrideMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverId,
            /**
             * 中文说明：保存 additionalPermissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpToolAdminService.ManagedToolOverrideMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by additional permissions; its type is {@code Set<String>}, and {@code McpToolAdminService.ManagedToolOverrideMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolOverrideMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolOverrideMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> additionalPermissions,
            /**
             * 中文说明：保存 minimumRiskLevel 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.ManagedToolOverrideMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by minimum risk level; its type is {@code String}, and {@code McpToolAdminService.ManagedToolOverrideMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolOverrideMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolOverrideMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String minimumRiskLevel,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpToolAdminService.ManagedToolOverrideMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code McpToolAdminService.ManagedToolOverrideMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolOverrideMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolOverrideMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedRevision,
            /**
             * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpToolAdminService.ManagedToolOverrideMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code McpToolAdminService.ManagedToolOverrideMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolOverrideMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolOverrideMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedDraftRevision,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.ManagedToolOverrideMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code McpToolAdminService.ManagedToolOverrideMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolOverrideMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolOverrideMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeReason
    ) {
    }

    /**
     * 中文说明：{@code RemoteToolMutation} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责远程工具Mutation相关的职责与边界。
     * English summary: {@code RemoteToolMutation} is an immutable data carrier in the current Gateway module; it owns the remote tool mutation-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverId 参数 服务器Id；parameter server id。
     * @param name 参数 name；parameter name。
     * @param description 参数 description；parameter description。
     * @param remoteMountId 参数 远程MountId；parameter remote mount id。
     * @param inputSchema 参数 input模式；parameter input schema。
     * @param outputSchema 参数 output模式；parameter output schema。
     * @param annotations 参数 annotations；parameter annotations。
     * @param requiredPermissions 参数 requiredPermissions；parameter required permissions。
     * @param riskLevel 参数 riskLevel；parameter risk level。
     * @param idempotent 参数 idempotent；parameter idempotent。
     * @param enabled 参数 enabled；parameter enabled。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record RemoteToolMutation(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.RemoteToolMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpToolAdminService.RemoteToolMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.RemoteToolMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code McpToolAdminService.RemoteToolMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverId,
            /**
             * 中文说明：保存 name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.RemoteToolMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by name; its type is {@code String}, and {@code McpToolAdminService.RemoteToolMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String name,
            /**
             * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.RemoteToolMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code McpToolAdminService.RemoteToolMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String description,
            /**
             * 中文说明：保存 远程MountId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.RemoteToolMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by remote mount id; its type is {@code String}, and {@code McpToolAdminService.RemoteToolMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String remoteMountId,
            /**
             * 中文说明：保存 input模式 对应的状态、依赖或配置值；字段类型为 {@code Object}，由 {@code McpToolAdminService.RemoteToolMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by input schema; its type is {@code Object}, and {@code McpToolAdminService.RemoteToolMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            Object inputSchema,
            /**
             * 中文说明：保存 output模式 对应的状态、依赖或配置值；字段类型为 {@code Object}，由 {@code McpToolAdminService.RemoteToolMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by output schema; its type is {@code Object}, and {@code McpToolAdminService.RemoteToolMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            Object outputSchema,
            /**
             * 中文说明：保存 annotations 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code McpToolAdminService.RemoteToolMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by annotations; its type is {@code Map<String, String>}, and {@code McpToolAdminService.RemoteToolMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, String> annotations,
            /**
             * 中文说明：保存 requiredPermissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpToolAdminService.RemoteToolMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by required permissions; its type is {@code Set<String>}, and {@code McpToolAdminService.RemoteToolMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> requiredPermissions,
            /**
             * 中文说明：保存 riskLevel 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.RemoteToolMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by risk level; its type is {@code String}, and {@code McpToolAdminService.RemoteToolMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String riskLevel,
            /**
             * 中文说明：保存 idempotent 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpToolAdminService.RemoteToolMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by idempotent; its type is {@code boolean}, and {@code McpToolAdminService.RemoteToolMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean idempotent,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpToolAdminService.RemoteToolMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code McpToolAdminService.RemoteToolMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpToolAdminService.RemoteToolMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code McpToolAdminService.RemoteToolMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedRevision,
            /**
             * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpToolAdminService.RemoteToolMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code McpToolAdminService.RemoteToolMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedDraftRevision,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.RemoteToolMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code McpToolAdminService.RemoteToolMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeReason
    ) {
    }

    /**
     * 中文说明：{@code MutationControl} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责MutationControl相关的职责与边界。
     * English summary: {@code MutationControl} is an immutable data carrier in the current Gateway module; it owns the mutation control-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record MutationControl(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.MutationControl} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpToolAdminService.MutationControl} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.MutationControl} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.MutationControl}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpToolAdminService.MutationControl} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code McpToolAdminService.MutationControl} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.MutationControl} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.MutationControl}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedRevision,
            /**
             * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpToolAdminService.MutationControl} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code McpToolAdminService.MutationControl} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.MutationControl} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.MutationControl}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedDraftRevision,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.MutationControl} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code McpToolAdminService.MutationControl} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.MutationControl} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.MutationControl}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeReason
    ) {
    }

    /**
     * 中文说明：{@code ManagedToolView} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Managed工具View相关的职责与边界。
     * English summary: {@code ManagedToolView} is an immutable data carrier in the current Gateway module; it owns the managed tool view-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param toolId 参数 工具Id；parameter tool id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param operationId 参数 操作Id；parameter operation id。
     * @param operationKey 参数 操作键；parameter operation key。
     * @param name 参数 name；parameter name。
     * @param description 参数 description；parameter description。
     * @param operationProtocol 参数 操作Protocol；parameter operation protocol。
     * @param inputSchema 参数 input模式；parameter input schema。
     * @param outputSchema 参数 output模式；parameter output schema。
     * @param codeServerId 参数 code服务器Id；parameter code server id。
     * @param codeServerCode 参数 code服务器Code；parameter code server code。
     * @param serverId 参数 服务器Id；parameter server id。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param codePermissions 参数 codePermissions；parameter code permissions。
     * @param additionalPermissions 参数 additionalPermissions；parameter additional permissions。
     * @param effectivePermissions 参数 effectivePermissions；parameter effective permissions。
     * @param codeRiskLevel 参数 codeRiskLevel；parameter code risk level。
     * @param minimumRiskLevel 参数 minimumRiskLevel；parameter minimum risk level。
     * @param effectiveRiskLevel 参数 effectiveRiskLevel；parameter effective risk level。
     * @param idempotent 参数 idempotent；parameter idempotent。
     * @param enabled 参数 enabled；parameter enabled。
     * @param overrideRevision 参数 overrideRevision；parameter override revision。
     */
    public record ManagedToolView(
            /**
             * 中文说明：保存 工具Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tool id; its type is {@code String}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String toolId,
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 操作Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by operation id; its type is {@code String}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String operationId,
            /**
             * 中文说明：保存 操作键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by operation key; its type is {@code String}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String operationKey,
            /**
             * 中文说明：保存 name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by name; its type is {@code String}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String name,
            /**
             * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String description,
            /**
             * 中文说明：保存 操作Protocol 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by operation protocol; its type is {@code String}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String operationProtocol,
            /**
             * 中文说明：保存 input模式 对应的状态、依赖或配置值；字段类型为 {@code Object}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by input schema; its type is {@code Object}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Object inputSchema,
            /**
             * 中文说明：保存 output模式 对应的状态、依赖或配置值；字段类型为 {@code Object}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by output schema; its type is {@code Object}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Object outputSchema,
            /**
             * 中文说明：保存 code服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by code server id; its type is {@code String}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String codeServerId,
            /**
             * 中文说明：保存 code服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by code server code; its type is {@code String}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String codeServerCode,
            /**
             * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverId,
            /**
             * 中文说明：保存 服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server code; its type is {@code String}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverCode,
            /**
             * 中文说明：保存 codePermissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by code permissions; its type is {@code Set<String>}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> codePermissions,
            /**
             * 中文说明：保存 additionalPermissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by additional permissions; its type is {@code Set<String>}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> additionalPermissions,
            /**
             * 中文说明：保存 effectivePermissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by effective permissions; its type is {@code Set<String>}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> effectivePermissions,
            /**
             * 中文说明：保存 codeRiskLevel 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by code risk level; its type is {@code String}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String codeRiskLevel,
            /**
             * 中文说明：保存 minimumRiskLevel 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by minimum risk level; its type is {@code String}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String minimumRiskLevel,
            /**
             * 中文说明：保存 effectiveRiskLevel 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by effective risk level; its type is {@code String}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String effectiveRiskLevel,
            /**
             * 中文说明：保存 idempotent 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by idempotent; its type is {@code boolean}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean idempotent,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 overrideRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpToolAdminService.ManagedToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by override revision; its type is {@code long}, and {@code McpToolAdminService.ManagedToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.ManagedToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.ManagedToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            long overrideRevision
    ) {
    }

    /**
     * 中文说明：{@code RemoteToolView} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责远程工具View相关的职责与边界。
     * English summary: {@code RemoteToolView} is an immutable data carrier in the current Gateway module; it owns the remote tool view-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverId 参数 服务器Id；parameter server id。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param name 参数 name；parameter name。
     * @param description 参数 description；parameter description。
     * @param remoteMountId 参数 远程MountId；parameter remote mount id。
     * @param inputSchema 参数 input模式；parameter input schema。
     * @param outputSchema 参数 output模式；parameter output schema。
     * @param annotations 参数 annotations；parameter annotations。
     * @param requiredPermissions 参数 requiredPermissions；parameter required permissions。
     * @param riskLevel 参数 riskLevel；parameter risk level。
     * @param idempotent 参数 idempotent；parameter idempotent。
     * @param enabled 参数 enabled；parameter enabled。
     * @param revision 参数 revision；parameter revision。
     */
    public record RemoteToolView(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.RemoteToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code McpToolAdminService.RemoteToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.RemoteToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpToolAdminService.RemoteToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.RemoteToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code McpToolAdminService.RemoteToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverId,
            /**
             * 中文说明：保存 服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.RemoteToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server code; its type is {@code String}, and {@code McpToolAdminService.RemoteToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverCode,
            /**
             * 中文说明：保存 name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.RemoteToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by name; its type is {@code String}, and {@code McpToolAdminService.RemoteToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String name,
            /**
             * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.RemoteToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code McpToolAdminService.RemoteToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String description,
            /**
             * 中文说明：保存 远程MountId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.RemoteToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by remote mount id; its type is {@code String}, and {@code McpToolAdminService.RemoteToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String remoteMountId,
            /**
             * 中文说明：保存 input模式 对应的状态、依赖或配置值；字段类型为 {@code Object}，由 {@code McpToolAdminService.RemoteToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by input schema; its type is {@code Object}, and {@code McpToolAdminService.RemoteToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Object inputSchema,
            /**
             * 中文说明：保存 output模式 对应的状态、依赖或配置值；字段类型为 {@code Object}，由 {@code McpToolAdminService.RemoteToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by output schema; its type is {@code Object}, and {@code McpToolAdminService.RemoteToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Object outputSchema,
            /**
             * 中文说明：保存 annotations 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code McpToolAdminService.RemoteToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by annotations; its type is {@code Map<String, String>}, and {@code McpToolAdminService.RemoteToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, String> annotations,
            /**
             * 中文说明：保存 requiredPermissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpToolAdminService.RemoteToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by required permissions; its type is {@code Set<String>}, and {@code McpToolAdminService.RemoteToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> requiredPermissions,
            /**
             * 中文说明：保存 riskLevel 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpToolAdminService.RemoteToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by risk level; its type is {@code String}, and {@code McpToolAdminService.RemoteToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String riskLevel,
            /**
             * 中文说明：保存 idempotent 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpToolAdminService.RemoteToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by idempotent; its type is {@code boolean}, and {@code McpToolAdminService.RemoteToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean idempotent,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpToolAdminService.RemoteToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code McpToolAdminService.RemoteToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpToolAdminService.RemoteToolView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code McpToolAdminService.RemoteToolView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpToolAdminService.RemoteToolView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpToolAdminService.RemoteToolView}; do not couple callers to its representation when the owning type exposes an API.
             */
            long revision
    ) {
    }
}
