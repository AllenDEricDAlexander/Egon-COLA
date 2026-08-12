package top.egon.cola.component.gateway.admin.mcp.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.GatewayAdminIdempotencyConflictException;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.IdempotencyStore;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftRepository;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpArtifactMetadataStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpCapabilityDraftStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpRemoteProviderStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpTaskStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.McpServerEntity;
import top.egon.cola.component.gateway.admin.mcp.persistence.McpServerRepository;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore;
import top.egon.cola.component.gateway.mcp.app.McpAppSecurityValidator;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.admin.rule.GatewayRuleCanonicalizer;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.domain.GatewayAdminRevisionConflictException;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 中文说明：{@code McpControlPlaneService} 是服务组件，位于当前 Gateway 模块的相关包中，负责MCPControlPlane服务相关的职责与边界。
 * English summary: {@code McpControlPlaneService} is a mcp control plane service service in the current Gateway module; it owns the mcp control plane service-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Service
public class McpControlPlaneService {

    /**
     * 中文说明：表示 IDEMPOTENCYSCOPE 这一固定值；它属于 {@code McpControlPlaneService} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value idempotency scope; it is a state, type, or protocol value of {@code McpControlPlaneService} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final String IDEMPOTENCY_SCOPE = "GATEWAY_MCP";

    /**
     * 中文说明：表示 任务IDEMPOTENCYSCOPE 这一固定值；它属于 {@code McpControlPlaneService} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value task idempotency scope; it is a state, type, or protocol value of {@code McpControlPlaneService} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final String TASK_IDEMPOTENCY_SCOPE = "GATEWAY_MCP_TASK";

    /**
     * 中文说明：保存 servers 对应的状态、依赖或配置值；字段类型为 {@code McpServerRepository}，由 {@code McpControlPlaneService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by servers; its type is {@code McpServerRepository}, and {@code McpControlPlaneService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpServerRepository servers;

    /**
     * 中文说明：保存 capabilities 对应的状态、依赖或配置值；字段类型为 {@code JdbcMcpCapabilityDraftStore}，由 {@code McpControlPlaneService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by capabilities; its type is {@code JdbcMcpCapabilityDraftStore}, and {@code McpControlPlaneService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcMcpCapabilityDraftStore capabilities;

    /**
     * 中文说明：保存 远程 对应的状态、依赖或配置值；字段类型为 {@code JdbcMcpRemoteProviderStore}，由 {@code McpControlPlaneService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by remote; its type is {@code JdbcMcpRemoteProviderStore}, and {@code McpControlPlaneService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcMcpRemoteProviderStore remote;

    /**
     * 中文说明：保存 artifacts 对应的状态、依赖或配置值；字段类型为 {@code JdbcMcpArtifactMetadataStore}，由 {@code McpControlPlaneService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by artifacts; its type is {@code JdbcMcpArtifactMetadataStore}, and {@code McpControlPlaneService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcMcpArtifactMetadataStore artifacts;

    /**
     * 中文说明：保存 制品Writer 对应的状态、依赖或配置值；字段类型为 {@code McpAppArtifactStore.Writer}，由 {@code McpControlPlaneService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by artifact writer; its type is {@code McpAppArtifactStore.Writer}, and {@code McpControlPlaneService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpAppArtifactStore.Writer artifactWriter;

    /**
     * 中文说明：保存 制品Reader 对应的状态、依赖或配置值；字段类型为 {@code McpAppArtifactStore.Reader}，由 {@code McpControlPlaneService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by artifact reader; its type is {@code McpAppArtifactStore.Reader}, and {@code McpControlPlaneService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpAppArtifactStore.Reader artifactReader;

    /**
     * 中文说明：保存 app安全 对应的状态、依赖或配置值；字段类型为 {@code McpAppSecurityValidator}，由 {@code McpControlPlaneService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by app security; its type is {@code McpAppSecurityValidator}, and {@code McpControlPlaneService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpAppSecurityValidator appSecurity;

    /**
     * 中文说明：保存 tasks 对应的状态、依赖或配置值；字段类型为 {@code JdbcMcpTaskStore}，由 {@code McpControlPlaneService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by tasks; its type is {@code JdbcMcpTaskStore}, and {@code McpControlPlaneService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcMcpTaskStore tasks;

    /**
     * 中文说明：保存 drafts 对应的状态、依赖或配置值；字段类型为 {@code GatewayDraftRepository}，由 {@code McpControlPlaneService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by drafts; its type is {@code GatewayDraftRepository}, and {@code McpControlPlaneService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayDraftRepository drafts;

    /**
     * 中文说明：保存 idempotency 对应的状态、依赖或配置值；字段类型为 {@code IdempotencyStore}，由 {@code McpControlPlaneService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by idempotency; its type is {@code IdempotencyStore}, and {@code McpControlPlaneService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final IdempotencyStore idempotency;

    /**
     * 中文说明：保存 audits 对应的状态、依赖或配置值；字段类型为 {@code GatewayAuditLogRepository}，由 {@code McpControlPlaneService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by audits; its type is {@code GatewayAuditLogRepository}, and {@code McpControlPlaneService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayAuditLogRepository audits;

    /**
     * 中文说明：保存 content工厂 对应的状态、依赖或配置值；字段类型为 {@code McpReleaseContentFactory}，由 {@code McpControlPlaneService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by content factory; its type is {@code McpReleaseContentFactory}, and {@code McpControlPlaneService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpReleaseContentFactory contentFactory;

    /**
     * 中文说明：保存 validation 对应的状态、依赖或配置值；字段类型为 {@code McpValidationService}，由 {@code McpControlPlaneService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by validation; its type is {@code McpValidationService}, and {@code McpControlPlaneService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpValidationService validation;

    /**
     * 中文说明：保存 canonicalizer 对应的状态、依赖或配置值；字段类型为 {@code GatewayRuleCanonicalizer}，由 {@code McpControlPlaneService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by canonicalizer; its type is {@code GatewayRuleCanonicalizer}, and {@code McpControlPlaneService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRuleCanonicalizer canonicalizer =
            new GatewayRuleCanonicalizer();

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code McpControlPlaneService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code McpControlPlaneService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：创建 {@code McpControlPlaneService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpControlPlaneService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param servers 参数 servers；parameter servers。
     * @param capabilities 参数 capabilities；parameter capabilities。
     * @param remote 参数 远程；parameter remote。
     * @param artifacts 参数 artifacts；parameter artifacts。
     * @param artifactWriter 参数 制品Writer；parameter artifact writer。
     * @param artifactReader 参数 制品Reader；parameter artifact reader。
     * @param appSecurity 参数 app安全；parameter app security。
     * @param tasks 参数 tasks；parameter tasks。
     * @param drafts 参数 drafts；parameter drafts。
     * @param idempotency 参数 idempotency；parameter idempotency。
     * @param audits 参数 audits；parameter audits。
     * @param contentFactory 参数 content工厂；parameter content factory。
     * @param validation 参数 validation；parameter validation。
     */
    @Autowired
    public McpControlPlaneService(
            McpServerRepository servers,
            JdbcMcpCapabilityDraftStore capabilities,
            JdbcMcpRemoteProviderStore remote,
            JdbcMcpArtifactMetadataStore artifacts,
            McpAppArtifactStore.Writer artifactWriter,
            McpAppArtifactStore.Reader artifactReader,
            McpAppSecurityValidator appSecurity,
            JdbcMcpTaskStore tasks,
            GatewayDraftRepository drafts,
            IdempotencyStore idempotency,
            GatewayAuditLogRepository audits,
            McpReleaseContentFactory contentFactory,
            McpValidationService validation) {
        this(
                servers,
                capabilities,
                remote,
                artifacts,
                artifactWriter,
                artifactReader,
                appSecurity,
                tasks,
                drafts,
                idempotency,
                audits,
                contentFactory,
                validation,
                Clock.systemUTC()
        );
    }

    /**
     * 中文说明：创建 {@code McpControlPlaneService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpControlPlaneService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param servers 参数 servers；parameter servers。
     * @param capabilities 参数 capabilities；parameter capabilities。
     * @param remote 参数 远程；parameter remote。
     * @param artifacts 参数 artifacts；parameter artifacts。
     * @param artifactWriter 参数 制品Writer；parameter artifact writer。
     * @param artifactReader 参数 制品Reader；parameter artifact reader。
     * @param appSecurity 参数 app安全；parameter app security。
     * @param tasks 参数 tasks；parameter tasks。
     * @param drafts 参数 drafts；parameter drafts。
     * @param idempotency 参数 idempotency；parameter idempotency。
     * @param audits 参数 audits；parameter audits。
     * @param contentFactory 参数 content工厂；parameter content factory。
     * @param validation 参数 validation；parameter validation。
     * @param clock 参数 clock；parameter clock。
     */
    McpControlPlaneService(
            McpServerRepository servers,
            JdbcMcpCapabilityDraftStore capabilities,
            JdbcMcpRemoteProviderStore remote,
            JdbcMcpArtifactMetadataStore artifacts,
            McpAppArtifactStore.Writer artifactWriter,
            McpAppArtifactStore.Reader artifactReader,
            McpAppSecurityValidator appSecurity,
            JdbcMcpTaskStore tasks,
            GatewayDraftRepository drafts,
            IdempotencyStore idempotency,
            GatewayAuditLogRepository audits,
            McpReleaseContentFactory contentFactory,
            McpValidationService validation,
            Clock clock) {
        this.servers = servers;
        this.capabilities = capabilities;
        this.remote = remote;
        this.artifacts = artifacts;
        this.artifactWriter = artifactWriter;
        this.artifactReader = artifactReader;
        this.appSecurity = appSecurity;
        this.tasks = tasks;
        this.drafts = drafts;
        this.idempotency = idempotency;
        this.audits = audits;
        this.contentFactory = contentFactory;
        this.validation = validation;
        this.clock = clock;
    }

    /**
     * 中文说明：执行 listServers 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the list servers operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.listServers(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 listServers 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public List<ServerView> listServers(String gatewayGroupId) {
        return servers
                .findAllByGatewayGroupIdAndDeletedFalseOrderByServerCode(
                        required(gatewayGroupId, "gatewayGroupId")
                )
                .stream()
                .map(this::view)
                .toList();
    }

    /**
     * 中文说明：执行 get服务器 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get server operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.getServer(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 get服务器 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public ServerView getServer(String id) {
        return view(requiredServer(id));
    }

    /**
     * 中文说明：执行 create服务器 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create server operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.createServer(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param command 参数 command；parameter command。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 create服务器 的处理结果；returns the result of the operation.
     */
    @Transactional
    public MutationResult createServer(
            ServerMutation command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("CREATE_SERVER", command);
        MutationResult replay = replay(
                command.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        requireCreateRevision(command.expectedRevision());
        Instant now = clock.instant();
        GatewayDraftEntity gatewayDraft = editable(
                command.gatewayGroupId(),
                command.expectedDraftRevision()
        );
        String id = UuidV7.simpleString();
        servers.saveAndFlush(new McpServerEntity(
                id,
                command.gatewayGroupId(),
                command.serverCode(),
                command.displayName(),
                command.description(),
                command.instructions(),
                command.dialects(),
                command.resourceUri(),
                command.listCacheTtlSeconds(),
                actor,
                now
        ));
        return finish(
                gatewayDraft,
                id,
                0,
                "MCP_SERVER",
                "CREATE",
                command.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of("serverCode", command.serverCode()),
                now
        );
    }

    /**
     * 中文说明：执行 update服务器 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update server operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.updateServer(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param command 参数 command；parameter command。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 update服务器 的处理结果；returns the result of the operation.
     */
    @Transactional
    public MutationResult updateServer(
            String id,
            ServerMutation command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("UPDATE_SERVER", Map.of(
                "id", id,
                "command", command
        ));
        MutationResult replay = replay(
                command.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        McpServerEntity server = requiredServer(id);
        requireGroup(command.gatewayGroupId(), server.getGatewayGroupId());
        GatewayDraftEntity gatewayDraft = editable(
                command.gatewayGroupId(),
                command.expectedDraftRevision()
        );
        Instant now = clock.instant();
        server.update(
                command.displayName(),
                command.description(),
                command.instructions(),
                command.dialects(),
                command.resourceUri(),
                command.listCacheTtlSeconds(),
                command.enabled(),
                command.expectedRevision(),
                actor,
                now
        );
        servers.flush();
        return finish(
                gatewayDraft,
                id,
                command.expectedRevision() + 1,
                "MCP_SERVER",
                "UPDATE",
                command.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of("serverCode", server.getServerCode()),
                now
        );
    }

    /**
     * 中文说明：执行 delete服务器 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete server operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.deleteServer(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param control 参数 control；parameter control。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 delete服务器 的处理结果；returns the result of the operation.
     */
    @Transactional
    public MutationResult deleteServer(
            String id,
            MutationControl control,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("DELETE_SERVER", Map.of(
                "id", id,
                "control", control
        ));
        MutationResult replay = replay(
                control.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        McpServerEntity server = requiredServer(id);
        requireGroup(control.gatewayGroupId(), server.getGatewayGroupId());
        GatewayDraftEntity gatewayDraft = editable(
                control.gatewayGroupId(),
                control.expectedDraftRevision()
        );
        Instant now = clock.instant();
        server.softDelete(control.expectedRevision(), actor, now);
        servers.flush();
        return finish(
                gatewayDraft,
                id,
                control.expectedRevision() + 1,
                "MCP_SERVER",
                "DELETE",
                control.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of("serverCode", server.getServerCode()),
                now
        );
    }

    /**
     * 中文说明：执行 capabilities 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the capabilities operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.capabilities(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverId 参数 服务器Id；parameter server id。
     * @param kind 参数 kind；parameter kind。
     * @return 返回 capabilities 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public List<JdbcMcpCapabilityDraftStore.CapabilityDraft> capabilities(
            String gatewayGroupId,
            String serverId,
            JdbcMcpCapabilityDraftStore.CapabilityKind kind) {
        requiredServerInGroup(serverId, gatewayGroupId);
        return capabilities.load(gatewayGroupId)
                .capabilities(kind)
                .stream()
                .filter(item -> item.serverId().equals(serverId))
                .toList();
    }

    /**
     * 中文说明：执行 putCapability 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the put capability operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.putCapability(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param kind 参数 kind；parameter kind。
     * @param command 参数 command；parameter command。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 putCapability 的处理结果；returns the result of the operation.
     */
    @Transactional
    public MutationResult putCapability(
            String id,
            JdbcMcpCapabilityDraftStore.CapabilityKind kind,
            CapabilityMutation command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String resourceId = id == null ? UuidV7.simpleString() : id;
        String digest = digest("PUT_" + kind, Map.of(
                "id", resourceId,
                "command", command
        ));
        MutationResult replay = replay(
                command.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        requiredServerInGroup(command.serverId(), command.gatewayGroupId());
        if (id != null) {
            var existing = requiredCapability(
                    command.gatewayGroupId(),
                    kind,
                    id
            );
            if (!existing.serverId().equals(command.serverId())) {
                throw new IllegalArgumentException(
                        "MCP capability cannot move between Servers"
                );
            }
        }
        GatewayDraftEntity gatewayDraft = editable(
                command.gatewayGroupId(),
                command.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = capabilities.save(
                new JdbcMcpCapabilityDraftStore.CapabilityDraft(
                        kind,
                        resourceId,
                        command.gatewayGroupId(),
                        command.serverId(),
                        command.name(),
                        command.content(),
                        command.enabled(),
                        command.expectedRevision()
                ),
                command.expectedRevision(),
                actor,
                now
        );
        return finish(
                gatewayDraft,
                resourceId,
                mutation.revision(),
                "MCP_" + kind,
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
     * 中文说明：执行 deleteCapability 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete capability operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.deleteCapability(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param kind 参数 kind；parameter kind。
     * @param control 参数 control；parameter control。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 deleteCapability 的处理结果；returns the result of the operation.
     */
    @Transactional
    public MutationResult deleteCapability(
            String id,
            JdbcMcpCapabilityDraftStore.CapabilityKind kind,
            MutationControl control,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("DELETE_" + kind, Map.of(
                "id", id,
                "control", control
        ));
        MutationResult replay = replay(
                control.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        requiredCapability(control.gatewayGroupId(), kind, id);
        GatewayDraftEntity gatewayDraft = editable(
                control.gatewayGroupId(),
                control.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = capabilities.softDelete(
                kind,
                id,
                control.expectedRevision(),
                actor,
                now
        );
        return finish(
                gatewayDraft,
                id,
                mutation.revision(),
                "MCP_" + kind,
                "DELETE",
                control.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of(),
                now
        );
    }

    /**
     * 中文说明：执行 preview 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the preview operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.preview(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 preview 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public Preview preview(String gatewayGroupId) {
        McpRuleContent content = contentFactory.preview(gatewayGroupId);
        return new Preview(content, validation.validate(content));
    }

    /**
     * 中文说明：执行 validate 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 validate 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public McpValidationService.ValidationReport validate(
            String gatewayGroupId) {
        return validation.validate(contentFactory.preview(gatewayGroupId));
    }

    /**
     * 中文说明：执行 providers 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the providers operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.providers(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 providers 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public List<JdbcMcpRemoteProviderStore.RemoteProviderDraft> providers(
            String gatewayGroupId) {
        return remote.providers(gatewayGroupId);
    }

    /**
     * 中文说明：执行 put提供方 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the put provider operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.putProvider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param command 参数 command；parameter command。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 put提供方 的处理结果；returns the result of the operation.
     */
    @Transactional
    public MutationResult putProvider(
            String id,
            RemoteProviderMutation command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String resourceId = id == null ? UuidV7.simpleString() : id;
        String digest = digest("PUT_REMOTE_PROVIDER", Map.of(
                "id", resourceId,
                "command", command
        ));
        MutationResult replay = replay(
                command.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        if (id != null) {
            requiredProvider(command.gatewayGroupId(), id);
        }
        GatewayDraftEntity gatewayDraft = editable(
                command.gatewayGroupId(),
                command.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = remote.saveProvider(
                new JdbcMcpRemoteProviderStore.RemoteProviderDraft(
                        resourceId,
                        command.gatewayGroupId(),
                        command.providerCode(),
                        command.content(),
                        command.enabled(),
                        command.expectedRevision()
                ),
                command.expectedRevision(),
                actor,
                now
        );
        return finish(
                gatewayDraft,
                resourceId,
                mutation.revision(),
                "MCP_REMOTE_PROVIDER",
                id == null ? "CREATE" : "UPDATE",
                command.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of("providerCode", command.providerCode()),
                now
        );
    }

    /**
     * 中文说明：执行 delete提供方 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete provider operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.deleteProvider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param control 参数 control；parameter control。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 delete提供方 的处理结果；returns the result of the operation.
     */
    @Transactional
    public MutationResult deleteProvider(
            String id,
            MutationControl control,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("DELETE_REMOTE_PROVIDER", Map.of(
                "id", id,
                "control", control
        ));
        MutationResult replay = replay(
                control.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        requiredProvider(control.gatewayGroupId(), id);
        GatewayDraftEntity gatewayDraft = editable(
                control.gatewayGroupId(),
                control.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = remote.softDeleteProvider(
                id,
                control.expectedRevision(),
                actor,
                now
        );
        return finish(
                gatewayDraft,
                id,
                mutation.revision(),
                "MCP_REMOTE_PROVIDER",
                "DELETE",
                control.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of(),
                now
        );
    }

    /**
     * 中文说明：执行 远程Capabilities 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the remote capabilities operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.remoteCapabilities(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param providerId 参数 提供方Id；parameter provider id。
     * @return 返回 远程Capabilities 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public List<JdbcMcpRemoteProviderStore.RemoteCapability>
            remoteCapabilities(String providerId) {
        return remote.capabilities(providerId);
    }

    /**
     * 中文说明：执行 mounts 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mounts operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.mounts(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 mounts 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public List<JdbcMcpRemoteProviderStore.RemoteMountDraft> mounts(
            String gatewayGroupId) {
        return remote.mounts(gatewayGroupId);
    }

    /**
     * 中文说明：执行 putMount 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the put mount operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.putMount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param command 参数 command；parameter command。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 putMount 的处理结果；returns the result of the operation.
     */
    @Transactional
    public MutationResult putMount(
            String id,
            RemoteMountMutation command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String resourceId = id == null ? UuidV7.simpleString() : id;
        String digest = digest("PUT_REMOTE_MOUNT", Map.of(
                "id", resourceId,
                "command", command
        ));
        MutationResult replay = replay(
                command.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        requiredServerInGroup(command.serverId(), command.gatewayGroupId());
        requiredProvider(command.gatewayGroupId(), command.providerId());
        GatewayDraftEntity gatewayDraft = editable(
                command.gatewayGroupId(),
                command.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = remote.saveMount(
                new JdbcMcpRemoteProviderStore.RemoteMountDraft(
                        resourceId,
                        command.gatewayGroupId(),
                        command.serverId(),
                        command.providerId(),
                        command.namespace(),
                        command.capabilityFingerprint(),
                        command.content(),
                        command.enabled(),
                        command.expectedRevision()
                ),
                command.expectedRevision(),
                actor,
                now
        );
        return finish(
                gatewayDraft,
                resourceId,
                mutation.revision(),
                "MCP_REMOTE_MOUNT",
                id == null ? "CREATE" : "UPDATE",
                command.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of("namespace", command.namespace()),
                now
        );
    }

    /**
     * 中文说明：执行 deleteMount 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete mount operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.deleteMount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param control 参数 control；parameter control。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 deleteMount 的处理结果；returns the result of the operation.
     */
    @Transactional
    public MutationResult deleteMount(
            String id,
            MutationControl control,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("DELETE_REMOTE_MOUNT", Map.of(
                "id", id,
                "control", control
        ));
        MutationResult replay = replay(
                control.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        boolean exists = remote.mounts(control.gatewayGroupId()).stream()
                .anyMatch(item -> item.id().equals(id));
        if (!exists) {
            throw notFound("MCP Remote Mount", id);
        }
        GatewayDraftEntity gatewayDraft = editable(
                control.gatewayGroupId(),
                control.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = remote.softDeleteMount(
                id,
                control.expectedRevision(),
                actor,
                now
        );
        return finish(
                gatewayDraft,
                id,
                mutation.revision(),
                "MCP_REMOTE_MOUNT",
                "DELETE",
                control.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of(),
                now
        );
    }

    /**
     * 中文说明：执行 artifacts 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the artifacts operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.artifacts(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 artifacts 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public List<JdbcMcpArtifactMetadataStore.ArtifactMetadata> artifacts(
            String gatewayGroupId) {
        return artifacts.list(gatewayGroupId);
    }

    /**
     * 中文说明：执行 制品 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the artifact operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.artifact(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 制品 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public JdbcMcpArtifactMetadataStore.ArtifactMetadata artifact(String id) {
        return artifacts.find(id).orElseThrow(() -> notFound(
                "MCP App artifact",
                id
        ));
    }

    /**
     * 中文说明：执行 upload制品 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the upload artifact operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.uploadArtifact(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param command 参数 command；parameter command。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 upload制品 的处理结果；returns the result of the operation.
     */
    @Transactional
    public MutationResult uploadArtifact(
            ArtifactUpload command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        byte[] content = command.content();
        String digest = sha256(content);
        var artifactContent = new McpAppArtifactStore.ArtifactContent(
                content,
                digest,
                content.length
        );
        ArtifactMutation mutation = new ArtifactMutation(
                command.gatewayGroupId(),
                command.appCode(),
                command.version(),
                command.displayName(),
                command.resourceUri(),
                "apps/" + command.appCode() + "/" + command.version()
                        + "/index.html",
                digest,
                content.length,
                command.mimeType(),
                command.contentSecurityPolicy(),
                command.permissions(),
                command.allowedOrigins(),
                command.expectedRevision(),
                command.expectedDraftRevision(),
                command.changeReason()
        );
        validateArtifact(mutation, artifactContent);
        McpAppArtifactStore.StoredArtifact stored = artifactWriter.write(
                new McpAppArtifactStore.WriteRequest(
                        command.appCode(),
                        command.version(),
                        content,
                        digest
                )
        );
        return registerArtifact(new ArtifactMutation(
                mutation.gatewayGroupId(),
                mutation.appCode(),
                mutation.version(),
                mutation.displayName(),
                mutation.resourceUri(),
                stored.artifactReference(),
                stored.sha256(),
                stored.sizeBytes(),
                mutation.mimeType(),
                mutation.contentSecurityPolicy(),
                mutation.permissions(),
                mutation.allowedOrigins(),
                mutation.expectedRevision(),
                mutation.expectedDraftRevision(),
                mutation.changeReason()
        ), idempotencyKey, actor, request);
    }

    /**
     * 中文说明：执行 register制品 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the register artifact operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.registerArtifact(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param command 参数 command；parameter command。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 register制品 的处理结果；returns the result of the operation.
     */
    @Transactional
    public MutationResult registerArtifact(
            ArtifactMutation command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("REGISTER_ARTIFACT", command);
        MutationResult replay = replay(
                command.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        requireCreateRevision(command.expectedRevision());
        McpAppArtifactStore.ArtifactContent artifactContent =
                artifactReader.read(new McpAppArtifactStore.ReadRequest(
                        command.artifactReference(),
                        command.sha256(),
                        command.sizeBytes()
                ));
        validateArtifact(command, artifactContent);
        GatewayDraftEntity gatewayDraft = editable(
                command.gatewayGroupId(),
                command.expectedDraftRevision()
        );
        Instant now = clock.instant();
        String id = UuidV7.simpleString();
        artifacts.save(new JdbcMcpArtifactMetadataStore.ArtifactMetadata(
                id,
                command.gatewayGroupId(),
                command.appCode(),
                command.version(),
                command.displayName(),
                command.resourceUri(),
                command.artifactReference(),
                command.sha256(),
                command.sizeBytes(),
                command.mimeType(),
                command.contentSecurityPolicy(),
                command.permissions(),
                command.allowedOrigins(),
                actor.actorId(),
                now
        ));
        return finish(
                gatewayDraft,
                id,
                0,
                "MCP_APP_ARTIFACT",
                "REGISTER",
                command.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of(
                        "appCode", command.appCode(),
                        "version", command.version(),
                        "sha256", command.sha256()
                ),
                now
        );
    }

    /**
     * 中文说明：执行 validate制品 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate artifact operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.validateArtifact(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param command 参数 command；parameter command。
     * @param artifact 参数 制品；parameter artifact。
     */
    private void validateArtifact(
            ArtifactMutation command,
            McpAppArtifactStore.ArtifactContent artifact) {
        String serverCode;
        try {
            serverCode = URI.create(command.resourceUri()).getRawAuthority();
        } catch (IllegalArgumentException failure) {
            throw new McpAppArtifactStore.ArtifactRejectedException(
                    "MCP App resource URI is invalid",
                    failure
            );
        }
        try {
            appSecurity.validate(new McpAppSecurityValidator.Manifest(
                    serverCode,
                    command.appCode(),
                    command.version(),
                    command.resourceUri(),
                    command.sha256(),
                    command.sizeBytes(),
                    command.mimeType(),
                    command.contentSecurityPolicy(),
                    command.permissions(),
                    command.allowedOrigins()
            ), artifact);
        } catch (McpProtocolException failure) {
            throw new McpAppArtifactStore.ArtifactRejectedException(
                    failure.getMessage(),
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 sha256 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the sha256 operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.sha256(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @return 返回 sha256 的处理结果；returns the result of the operation.
     */
    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(content)
            );
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is unavailable", failure);
        }
    }

    /**
     * 中文说明：执行 revoke制品 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the revoke artifact operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.revokeArtifact(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param control 参数 control；parameter control。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 revoke制品 的处理结果；returns the result of the operation.
     */
    @Transactional
    public MutationResult revokeArtifact(
            String id,
            MutationControl control,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("REVOKE_ARTIFACT", Map.of(
                "id", id,
                "control", control
        ));
        MutationResult replay = replay(
                control.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        JdbcMcpArtifactMetadataStore.ArtifactMetadata artifact = artifact(id);
        requireGroup(control.gatewayGroupId(), artifact.gatewayGroupId());
        GatewayDraftEntity gatewayDraft = editable(
                control.gatewayGroupId(),
                control.expectedDraftRevision()
        );
        if (!artifacts.revoke(id)) {
            throw new IllegalStateException(
                    "GATEWAY_MCP_ARTIFACT_ALREADY_REVOKED"
            );
        }
        Instant now = clock.instant();
        return finish(
                gatewayDraft,
                id,
                0,
                "MCP_APP_ARTIFACT",
                "REVOKE",
                control.changeReason(),
                idempotencyKey,
                digest,
                actor,
                request,
                Map.of(
                        "appCode", artifact.appCode(),
                        "version", artifact.version(),
                        "sha256", artifact.sha256()
                ),
                now
        );
    }

    /**
     * 中文说明：执行 tasks 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the tasks operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.tasks(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param tenantId 参数 tenantId；parameter tenant id。
     * @param clientId 参数 客户端Id；parameter client id。
     * @return 返回 tasks 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public List<JdbcMcpTaskStore.TaskRecord> tasks(
            String tenantId,
            String clientId) {
        return tasks.list(required(tenantId, "tenantId"), clientId);
    }

    /**
     * 中文说明：执行 任务 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the task operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.task(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 任务 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public JdbcMcpTaskStore.TaskRecord task(String id) {
        return tasks.find(id).orElseThrow(() -> notFound("MCP Task", id));
    }

    /**
     * 中文说明：执行 cancel任务 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the cancel task operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.cancelTask(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 cancel任务 的处理结果；returns the result of the operation.
     */
    @Transactional
    public boolean cancelTask(
            String id,
            long expectedRevision,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String scopeId = "TASK:" + required(id, "id");
        String digest = digest("CANCEL_TASK", Map.of(
                "id", id,
                "expectedRevision", expectedRevision
        ));
        Boolean replay = replayTask(scopeId, idempotencyKey, digest);
        if (replay != null) {
            return replay;
        }
        JdbcMcpTaskStore.TaskRecord task = task(id);
        Instant now = clock.instant();
        boolean cancelled = tasks.cancel(id, expectedRevision, now);
        if (!cancelled) {
            return false;
        }
        audit(
                actor,
                request,
                "MCP_TASK",
                id,
                "CANCEL",
                Map.of(
                        "tenantId", task.tenantId(),
                        "serverCode", task.serverCode(),
                        "toolName", task.toolName()
                ),
                now
        );
        idempotency.save(new IdempotencyStore.Record(
                TASK_IDEMPOTENCY_SCOPE,
                scopeId,
                required(idempotencyKey, "idempotencyKey"),
                digest,
                id,
                Map.of("cancelled", true),
                now,
                now.plus(Duration.ofDays(7))
        ));
        return true;
    }

    /**
     * 中文说明：执行 replay任务 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the replay task operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.replayTask(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param scopeId 参数 scopeId；parameter scope id。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param payloadDigest 参数 payloadDigest；parameter payload digest。
     * @return 返回 replay任务 的处理结果；returns the result of the operation.
     */
    private Boolean replayTask(
            String scopeId,
            String idempotencyKey,
            String payloadDigest) {
        String key = required(idempotencyKey, "idempotencyKey");
        IdempotencyStore.Record existing = idempotency.find(
                TASK_IDEMPOTENCY_SCOPE,
                scopeId,
                key
        ).orElse(null);
        if (existing == null) {
            return null;
        }
        if (!existing.payloadSha256().equals(payloadDigest)) {
            throw new GatewayAdminIdempotencyConflictException();
        }
        return Boolean.TRUE.equals(existing.response().get("cancelled"));
    }

    /**
     * 中文说明：执行 finish 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the finish operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.finish(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
    private MutationResult finish(
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
        MutationResult result = new MutationResult(
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
        audit(
                actor,
                request,
                resourceType,
                resourceId,
                action,
                summary,
                now
        );
        return result;
    }

    /**
     * 中文说明：执行 replay 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the replay operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.replay(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
     * @param payloadDigest 参数 payloadDigest；parameter payload digest。
     * @return 返回 replay 的处理结果；returns the result of the operation.
     */
    private MutationResult replay(
            String gatewayGroupId,
            String idempotencyKey,
            String payloadDigest) {
        String key = required(idempotencyKey, "idempotencyKey");
        IdempotencyStore.Record existing = idempotency.find(
                IDEMPOTENCY_SCOPE,
                gatewayGroupId,
                key
        ).orElse(null);
        if (existing == null) {
            return null;
        }
        if (!existing.payloadSha256().equals(payloadDigest)) {
            throw new GatewayAdminIdempotencyConflictException();
        }
        return new MutationResult(
                number(existing.response(), "draftRevision"),
                existing.resourceId(),
                number(existing.response(), "resourceRevision"),
                true
        );
    }

    /**
     * 中文说明：执行 审计 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audit operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.audit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @param resourceType 参数 资源Type；parameter resource type。
     * @param resourceId 参数 资源Id；parameter resource id。
     * @param action 参数 action；parameter action。
     * @param summary 参数 summary；parameter summary。
     * @param now 参数 now；parameter now。
     */
    private void audit(
            AdminActor actor,
            RequestAuditContext request,
            String resourceType,
            String resourceId,
            String action,
            Map<String, Object> summary,
            Instant now) {
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
    }

    /**
     * 中文说明：执行 required服务器 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required server operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.requiredServer(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 required服务器 的处理结果；returns the result of the operation.
     */
    private McpServerEntity requiredServer(String id) {
        return servers.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> notFound("MCP Server", id));
    }

    /**
     * 中文说明：执行 required服务器InGroup 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required server in group operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.requiredServerInGroup(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     */
    private void requiredServerInGroup(String id, String gatewayGroupId) {
        requireGroup(gatewayGroupId, requiredServer(id).getGatewayGroupId());
    }

    /**
     * 中文说明：执行 requiredCapability 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required capability operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.requiredCapability(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param kind 参数 kind；parameter kind。
     * @param id 参数 id；parameter id。
     * @return 返回 requiredCapability 的处理结果；returns the result of the operation.
     */
    private JdbcMcpCapabilityDraftStore.CapabilityDraft requiredCapability(
            String gatewayGroupId,
            JdbcMcpCapabilityDraftStore.CapabilityKind kind,
            String id) {
        return capabilities.load(gatewayGroupId).capabilities(kind)
                .stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("MCP " + kind, id));
    }

    /**
     * 中文说明：执行 required提供方 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required provider operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.requiredProvider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param id 参数 id；parameter id。
     * @return 返回 required提供方 的处理结果；returns the result of the operation.
     */
    private JdbcMcpRemoteProviderStore.RemoteProviderDraft requiredProvider(
            String gatewayGroupId,
            String id) {
        return remote.providers(gatewayGroupId).stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .orElseThrow(() -> notFound("MCP Remote Provider", id));
    }

    /**
     * 中文说明：执行 editable 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the editable operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.editable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @return 返回 editable 的处理结果；returns the result of the operation.
     */
    private GatewayDraftEntity editable(
            String gatewayGroupId,
            long expectedRevision) {
        GatewayDraftEntity draft = drafts.findById(gatewayGroupId)
                .orElseThrow(() -> notFound(
                        "Gateway Draft",
                        gatewayGroupId
                ));
        draft.assertEditable(expectedRevision);
        return draft;
    }

    /**
     * 中文说明：执行 requireGroup 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require group operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.requireGroup(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param expected 参数 expected；parameter expected。
     * @param actual 参数 actual；parameter actual。
     */
    private void requireGroup(String expected, String actual) {
        if (!Objects.equals(expected, actual)) {
            throw new IllegalArgumentException(
                    "MCP resource belongs to another Gateway Group"
            );
        }
    }

    /**
     * 中文说明：执行 requireCreateRevision 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require create revision operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.requireCreateRevision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     */
    private void requireCreateRevision(long expectedRevision) {
        if (expectedRevision != 0) {
            throw new GatewayAdminRevisionConflictException(0);
        }
    }

    /**
     * 中文说明：执行 digest 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the digest operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.digest(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 number 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the number operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.number(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 notFound 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the not found operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.notFound(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 required 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 view 操作；该方法是 {@code McpControlPlaneService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the view operation; this method is the invocation entry point on {@code McpControlPlaneService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.view(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param server 参数 服务器；parameter server。
     * @return 返回 view 的处理结果；returns the result of the operation.
     */
    private ServerView view(McpServerEntity server) {
        return new ServerView(
                server.getId(),
                server.getGatewayGroupId(),
                server.getServerCode(),
                server.getDisplayName(),
                server.getDescription(),
                server.getInstructions(),
                server.getDialects(),
                server.getResourceUri(),
                server.getListCacheTtlSeconds(),
                server.isEnabled(),
                server.getRevision(),
                server.getCreatedAt(),
                server.getUpdatedAt()
        );
    }

    /**
     * 中文说明：{@code ServerMutation} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责服务器Mutation相关的职责与边界。
     * English summary: {@code ServerMutation} is an immutable data carrier in the current Gateway module; it owns the server mutation-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param displayName 参数 displayName；parameter display name。
     * @param description 参数 description；parameter description。
     * @param instructions 参数 instructions；parameter instructions。
     * @param dialects 参数 dialects；parameter dialects。
     * @param resourceUri 参数 资源Uri；parameter resource uri。
     * @param listCacheTtlSeconds 参数 listCacheTtlSeconds；parameter list cache ttl seconds。
     * @param enabled 参数 enabled；parameter enabled。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record ServerMutation(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ServerMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpControlPlaneService.ServerMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ServerMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server code; its type is {@code String}, and {@code McpControlPlaneService.ServerMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverCode,
            /**
             * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ServerMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code McpControlPlaneService.ServerMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String displayName,
            /**
             * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ServerMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code McpControlPlaneService.ServerMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String description,
            /**
             * 中文说明：保存 instructions 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ServerMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by instructions; its type is {@code String}, and {@code McpControlPlaneService.ServerMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String instructions,
            /**
             * 中文说明：保存 dialects 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpControlPlaneService.ServerMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by dialects; its type is {@code Set<String>}, and {@code McpControlPlaneService.ServerMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> dialects,
            /**
             * 中文说明：保存 资源Uri 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ServerMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by resource uri; its type is {@code String}, and {@code McpControlPlaneService.ServerMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String resourceUri,
            /**
             * 中文说明：保存 listCacheTtlSeconds 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpControlPlaneService.ServerMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by list cache ttl seconds; its type is {@code long}, and {@code McpControlPlaneService.ServerMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            long listCacheTtlSeconds,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpControlPlaneService.ServerMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code McpControlPlaneService.ServerMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpControlPlaneService.ServerMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code McpControlPlaneService.ServerMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedRevision,
            /**
             * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpControlPlaneService.ServerMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code McpControlPlaneService.ServerMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedDraftRevision,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ServerMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code McpControlPlaneService.ServerMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeReason
    ) {
    }

    /**
     * 中文说明：{@code CapabilityMutation} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责CapabilityMutation相关的职责与边界。
     * English summary: {@code CapabilityMutation} is an immutable data carrier in the current Gateway module; it owns the capability mutation-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverId 参数 服务器Id；parameter server id。
     * @param name 参数 name；parameter name。
     * @param content 参数 content；parameter content。
     * @param enabled 参数 enabled；parameter enabled。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record CapabilityMutation(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.CapabilityMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpControlPlaneService.CapabilityMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.CapabilityMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.CapabilityMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.CapabilityMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code McpControlPlaneService.CapabilityMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.CapabilityMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.CapabilityMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverId,
            /**
             * 中文说明：保存 name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.CapabilityMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by name; its type is {@code String}, and {@code McpControlPlaneService.CapabilityMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.CapabilityMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.CapabilityMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String name,
            /**
             * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpControlPlaneService.CapabilityMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code Map<String, Object>}, and {@code McpControlPlaneService.CapabilityMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.CapabilityMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.CapabilityMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> content,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpControlPlaneService.CapabilityMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code McpControlPlaneService.CapabilityMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.CapabilityMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.CapabilityMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpControlPlaneService.CapabilityMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code McpControlPlaneService.CapabilityMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.CapabilityMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.CapabilityMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedRevision,
            /**
             * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpControlPlaneService.CapabilityMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code McpControlPlaneService.CapabilityMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.CapabilityMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.CapabilityMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedDraftRevision,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.CapabilityMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code McpControlPlaneService.CapabilityMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.CapabilityMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.CapabilityMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeReason
    ) {
    }

    /**
     * 中文说明：{@code RemoteProviderMutation} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责远程提供方Mutation相关的职责与边界。
     * English summary: {@code RemoteProviderMutation} is an immutable data carrier in the current Gateway module; it owns the remote provider mutation-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param providerCode 参数 提供方Code；parameter provider code。
     * @param content 参数 content；parameter content。
     * @param enabled 参数 enabled；parameter enabled。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record RemoteProviderMutation(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.RemoteProviderMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpControlPlaneService.RemoteProviderMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.RemoteProviderMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.RemoteProviderMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 提供方Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.RemoteProviderMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by provider code; its type is {@code String}, and {@code McpControlPlaneService.RemoteProviderMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.RemoteProviderMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.RemoteProviderMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String providerCode,
            /**
             * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpControlPlaneService.RemoteProviderMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code Map<String, Object>}, and {@code McpControlPlaneService.RemoteProviderMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.RemoteProviderMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.RemoteProviderMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> content,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpControlPlaneService.RemoteProviderMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code McpControlPlaneService.RemoteProviderMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.RemoteProviderMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.RemoteProviderMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpControlPlaneService.RemoteProviderMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code McpControlPlaneService.RemoteProviderMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.RemoteProviderMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.RemoteProviderMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedRevision,
            /**
             * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpControlPlaneService.RemoteProviderMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code McpControlPlaneService.RemoteProviderMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.RemoteProviderMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.RemoteProviderMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedDraftRevision,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.RemoteProviderMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code McpControlPlaneService.RemoteProviderMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.RemoteProviderMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.RemoteProviderMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeReason
    ) {
    }

    /**
     * 中文说明：{@code RemoteMountMutation} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责远程MountMutation相关的职责与边界。
     * English summary: {@code RemoteMountMutation} is an immutable data carrier in the current Gateway module; it owns the remote mount mutation-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverId 参数 服务器Id；parameter server id。
     * @param providerId 参数 提供方Id；parameter provider id。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param capabilityFingerprint 参数 capabilityFingerprint；parameter capability fingerprint。
     * @param content 参数 content；parameter content。
     * @param enabled 参数 enabled；parameter enabled。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record RemoteMountMutation(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.RemoteMountMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpControlPlaneService.RemoteMountMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.RemoteMountMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.RemoteMountMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.RemoteMountMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code McpControlPlaneService.RemoteMountMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.RemoteMountMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.RemoteMountMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverId,
            /**
             * 中文说明：保存 提供方Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.RemoteMountMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by provider id; its type is {@code String}, and {@code McpControlPlaneService.RemoteMountMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.RemoteMountMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.RemoteMountMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String providerId,
            /**
             * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.RemoteMountMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code McpControlPlaneService.RemoteMountMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.RemoteMountMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.RemoteMountMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String namespace,
            /**
             * 中文说明：保存 capabilityFingerprint 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.RemoteMountMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by capability fingerprint; its type is {@code String}, and {@code McpControlPlaneService.RemoteMountMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.RemoteMountMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.RemoteMountMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String capabilityFingerprint,
            /**
             * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpControlPlaneService.RemoteMountMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code Map<String, Object>}, and {@code McpControlPlaneService.RemoteMountMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.RemoteMountMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.RemoteMountMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> content,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpControlPlaneService.RemoteMountMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code McpControlPlaneService.RemoteMountMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.RemoteMountMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.RemoteMountMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpControlPlaneService.RemoteMountMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code McpControlPlaneService.RemoteMountMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.RemoteMountMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.RemoteMountMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedRevision,
            /**
             * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpControlPlaneService.RemoteMountMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code McpControlPlaneService.RemoteMountMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.RemoteMountMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.RemoteMountMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedDraftRevision,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.RemoteMountMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code McpControlPlaneService.RemoteMountMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.RemoteMountMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.RemoteMountMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeReason
    ) {
    }

    /**
     * 中文说明：{@code ArtifactMutation} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责制品Mutation相关的职责与边界。
     * English summary: {@code ArtifactMutation} is an immutable data carrier in the current Gateway module; it owns the artifact mutation-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param appCode 参数 appCode；parameter app code。
     * @param version 参数 version；parameter version。
     * @param displayName 参数 displayName；parameter display name。
     * @param resourceUri 参数 资源Uri；parameter resource uri。
     * @param artifactReference 参数 制品Reference；parameter artifact reference。
     * @param sha256 参数 sha256；parameter sha256。
     * @param sizeBytes 参数 sizeBytes；parameter size bytes。
     * @param mimeType 参数 mimeType；parameter mime type。
     * @param contentSecurityPolicy 参数 content安全策略；parameter content security policy。
     * @param permissions 参数 permissions；parameter permissions。
     * @param allowedOrigins 参数 allowedOrigins；parameter allowed origins。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record ArtifactMutation(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ArtifactMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpControlPlaneService.ArtifactMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 appCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ArtifactMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by app code; its type is {@code String}, and {@code McpControlPlaneService.ArtifactMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String appCode,
            /**
             * 中文说明：保存 version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ArtifactMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by version; its type is {@code String}, and {@code McpControlPlaneService.ArtifactMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String version,
            /**
             * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ArtifactMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code McpControlPlaneService.ArtifactMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String displayName,
            /**
             * 中文说明：保存 资源Uri 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ArtifactMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by resource uri; its type is {@code String}, and {@code McpControlPlaneService.ArtifactMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String resourceUri,
            /**
             * 中文说明：保存 制品Reference 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ArtifactMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by artifact reference; its type is {@code String}, and {@code McpControlPlaneService.ArtifactMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String artifactReference,
            /**
             * 中文说明：保存 sha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ArtifactMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by sha256; its type is {@code String}, and {@code McpControlPlaneService.ArtifactMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String sha256,
            /**
             * 中文说明：保存 sizeBytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpControlPlaneService.ArtifactMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by size bytes; its type is {@code long}, and {@code McpControlPlaneService.ArtifactMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            long sizeBytes,
            /**
             * 中文说明：保存 mimeType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ArtifactMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by mime type; its type is {@code String}, and {@code McpControlPlaneService.ArtifactMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String mimeType,
            /**
             * 中文说明：保存 content安全策略 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ArtifactMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content security policy; its type is {@code String}, and {@code McpControlPlaneService.ArtifactMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String contentSecurityPolicy,
            /**
             * 中文说明：保存 permissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpControlPlaneService.ArtifactMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by permissions; its type is {@code Set<String>}, and {@code McpControlPlaneService.ArtifactMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> permissions,
            /**
             * 中文说明：保存 allowedOrigins 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpControlPlaneService.ArtifactMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by allowed origins; its type is {@code Set<String>}, and {@code McpControlPlaneService.ArtifactMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> allowedOrigins,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpControlPlaneService.ArtifactMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code McpControlPlaneService.ArtifactMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedRevision,
            /**
             * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpControlPlaneService.ArtifactMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code McpControlPlaneService.ArtifactMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedDraftRevision,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ArtifactMutation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code McpControlPlaneService.ArtifactMutation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactMutation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactMutation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeReason
    ) {
    }

    /**
     * 中文说明：{@code ArtifactUpload} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责制品Upload相关的职责与边界。
     * English summary: {@code ArtifactUpload} is an immutable data carrier in the current Gateway module; it owns the artifact upload-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param appCode 参数 appCode；parameter app code。
     * @param version 参数 version；parameter version。
     * @param displayName 参数 displayName；parameter display name。
     * @param resourceUri 参数 资源Uri；parameter resource uri。
     * @param mimeType 参数 mimeType；parameter mime type。
     * @param contentSecurityPolicy 参数 content安全策略；parameter content security policy。
     * @param permissions 参数 permissions；parameter permissions。
     * @param allowedOrigins 参数 allowedOrigins；parameter allowed origins。
     * @param content 参数 content；parameter content。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record ArtifactUpload(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ArtifactUpload} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpControlPlaneService.ArtifactUpload} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactUpload} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactUpload}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 appCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ArtifactUpload} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by app code; its type is {@code String}, and {@code McpControlPlaneService.ArtifactUpload} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactUpload} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactUpload}; do not couple callers to its representation when the owning type exposes an API.
             */
            String appCode,
            /**
             * 中文说明：保存 version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ArtifactUpload} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by version; its type is {@code String}, and {@code McpControlPlaneService.ArtifactUpload} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactUpload} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactUpload}; do not couple callers to its representation when the owning type exposes an API.
             */
            String version,
            /**
             * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ArtifactUpload} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code McpControlPlaneService.ArtifactUpload} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactUpload} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactUpload}; do not couple callers to its representation when the owning type exposes an API.
             */
            String displayName,
            /**
             * 中文说明：保存 资源Uri 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ArtifactUpload} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by resource uri; its type is {@code String}, and {@code McpControlPlaneService.ArtifactUpload} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactUpload} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactUpload}; do not couple callers to its representation when the owning type exposes an API.
             */
            String resourceUri,
            /**
             * 中文说明：保存 mimeType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ArtifactUpload} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by mime type; its type is {@code String}, and {@code McpControlPlaneService.ArtifactUpload} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactUpload} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactUpload}; do not couple callers to its representation when the owning type exposes an API.
             */
            String mimeType,
            /**
             * 中文说明：保存 content安全策略 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ArtifactUpload} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content security policy; its type is {@code String}, and {@code McpControlPlaneService.ArtifactUpload} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactUpload} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactUpload}; do not couple callers to its representation when the owning type exposes an API.
             */
            String contentSecurityPolicy,
            /**
             * 中文说明：保存 permissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpControlPlaneService.ArtifactUpload} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by permissions; its type is {@code Set<String>}, and {@code McpControlPlaneService.ArtifactUpload} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactUpload} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactUpload}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> permissions,
            /**
             * 中文说明：保存 allowedOrigins 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpControlPlaneService.ArtifactUpload} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by allowed origins; its type is {@code Set<String>}, and {@code McpControlPlaneService.ArtifactUpload} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactUpload} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactUpload}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> allowedOrigins,
            /**
             * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code byte[]}，由 {@code McpControlPlaneService.ArtifactUpload} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code byte[]}, and {@code McpControlPlaneService.ArtifactUpload} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactUpload} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactUpload}; do not couple callers to its representation when the owning type exposes an API.
             */
            byte[] content,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpControlPlaneService.ArtifactUpload} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code McpControlPlaneService.ArtifactUpload} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactUpload} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactUpload}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedRevision,
            /**
             * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpControlPlaneService.ArtifactUpload} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code McpControlPlaneService.ArtifactUpload} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactUpload} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactUpload}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedDraftRevision,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ArtifactUpload} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code McpControlPlaneService.ArtifactUpload} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ArtifactUpload} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ArtifactUpload}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeReason
    ) {

        /**
         * 中文说明：创建 {@code McpControlPlaneService.ArtifactUpload} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpControlPlaneService.ArtifactUpload} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
         * @param appCode 参数 appCode；parameter app code。
         * @param version 参数 version；parameter version。
         * @param displayName 参数 displayName；parameter display name。
         * @param resourceUri 参数 资源Uri；parameter resource uri。
         * @param mimeType 参数 mimeType；parameter mime type。
         * @param contentSecurityPolicy 参数 content安全策略；parameter content security policy。
         * @param permissions 参数 permissions；parameter permissions。
         * @param allowedOrigins 参数 allowedOrigins；parameter allowed origins。
         * @param content 参数 content；parameter content。
         * @param expectedRevision 参数 expectedRevision；parameter expected revision。
         * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
         * @param changeReason 参数 changeReason；parameter change reason。
         */
        public ArtifactUpload {
            content = Objects.requireNonNull(content, "content").clone();
            permissions = Set.copyOf(permissions);
            allowedOrigins = Set.copyOf(allowedOrigins);
        }

        /**
         * 中文说明：执行 content 操作；该方法是 {@code McpControlPlaneService.ArtifactUpload} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the content operation; this method is the invocation entry point on {@code McpControlPlaneService.ArtifactUpload} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpControlPlaneService.ArtifactUpload.content(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 content 的处理结果；returns the result of the operation.
         */
        @Override
        public byte[] content() {
            return content.clone();
        }
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
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.MutationControl} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpControlPlaneService.MutationControl} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.MutationControl} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.MutationControl}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpControlPlaneService.MutationControl} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code McpControlPlaneService.MutationControl} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.MutationControl} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.MutationControl}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedRevision,
            /**
             * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpControlPlaneService.MutationControl} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code McpControlPlaneService.MutationControl} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.MutationControl} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.MutationControl}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedDraftRevision,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.MutationControl} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code McpControlPlaneService.MutationControl} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.MutationControl} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.MutationControl}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeReason
    ) {
    }

    /**
     * 中文说明：{@code ServerView} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责服务器View相关的职责与边界。
     * English summary: {@code ServerView} is an immutable data carrier in the current Gateway module; it owns the server view-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param id 参数 id；parameter id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param displayName 参数 displayName；parameter display name。
     * @param description 参数 description；parameter description。
     * @param instructions 参数 instructions；parameter instructions。
     * @param dialects 参数 dialects；parameter dialects。
     * @param resourceUri 参数 资源Uri；parameter resource uri。
     * @param listCacheTtlSeconds 参数 listCacheTtlSeconds；parameter list cache ttl seconds。
     * @param enabled 参数 enabled；parameter enabled。
     * @param revision 参数 revision；parameter revision。
     * @param createdAt 参数 createdAt；parameter created at。
     * @param updatedAt 参数 updatedAt；parameter updated at。
     */
    public record ServerView(
            /**
             * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ServerView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code McpControlPlaneService.ServerView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String id,
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ServerView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpControlPlaneService.ServerView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ServerView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server code; its type is {@code String}, and {@code McpControlPlaneService.ServerView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverCode,
            /**
             * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ServerView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code McpControlPlaneService.ServerView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String displayName,
            /**
             * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ServerView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code McpControlPlaneService.ServerView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String description,
            /**
             * 中文说明：保存 instructions 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ServerView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by instructions; its type is {@code String}, and {@code McpControlPlaneService.ServerView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String instructions,
            /**
             * 中文说明：保存 dialects 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpControlPlaneService.ServerView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by dialects; its type is {@code Set<String>}, and {@code McpControlPlaneService.ServerView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> dialects,
            /**
             * 中文说明：保存 资源Uri 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.ServerView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by resource uri; its type is {@code String}, and {@code McpControlPlaneService.ServerView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String resourceUri,
            /**
             * 中文说明：保存 listCacheTtlSeconds 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpControlPlaneService.ServerView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by list cache ttl seconds; its type is {@code long}, and {@code McpControlPlaneService.ServerView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerView}; do not couple callers to its representation when the owning type exposes an API.
             */
            long listCacheTtlSeconds,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpControlPlaneService.ServerView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code McpControlPlaneService.ServerView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerView}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpControlPlaneService.ServerView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code McpControlPlaneService.ServerView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerView}; do not couple callers to its representation when the owning type exposes an API.
             */
            long revision,
            /**
             * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code McpControlPlaneService.ServerView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code McpControlPlaneService.ServerView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant createdAt,
            /**
             * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code McpControlPlaneService.ServerView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code McpControlPlaneService.ServerView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.ServerView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.ServerView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant updatedAt
    ) {
    }

    /**
     * 中文说明：{@code MutationResult} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责MutationResult相关的职责与边界。
     * English summary: {@code MutationResult} is an immutable data carrier in the current Gateway module; it owns the mutation result-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param draftRevision 参数 草稿Revision；parameter draft revision。
     * @param resourceId 参数 资源Id；parameter resource id。
     * @param resourceRevision 参数 资源Revision；parameter resource revision。
     * @param replayed 参数 replayed；parameter replayed。
     */
    public record MutationResult(
            /**
             * 中文说明：保存 草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpControlPlaneService.MutationResult} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by draft revision; its type is {@code long}, and {@code McpControlPlaneService.MutationResult} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.MutationResult} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.MutationResult}; do not couple callers to its representation when the owning type exposes an API.
             */
            long draftRevision,
            /**
             * 中文说明：保存 资源Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpControlPlaneService.MutationResult} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by resource id; its type is {@code String}, and {@code McpControlPlaneService.MutationResult} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.MutationResult} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.MutationResult}; do not couple callers to its representation when the owning type exposes an API.
             */
            String resourceId,
            /**
             * 中文说明：保存 资源Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpControlPlaneService.MutationResult} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by resource revision; its type is {@code long}, and {@code McpControlPlaneService.MutationResult} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.MutationResult} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.MutationResult}; do not couple callers to its representation when the owning type exposes an API.
             */
            long resourceRevision,
            /**
             * 中文说明：保存 replayed 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpControlPlaneService.MutationResult} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by replayed; its type is {@code boolean}, and {@code McpControlPlaneService.MutationResult} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.MutationResult} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.MutationResult}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean replayed
    ) {
    }

    /**
     * 中文说明：{@code Preview} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Preview相关的职责与边界。
     * English summary: {@code Preview} is an immutable data carrier in the current Gateway module; it owns the preview-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param content 参数 content；parameter content。
     * @param validation 参数 validation；parameter validation。
     */
    public record Preview(
            /**
             * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code McpRuleContent}，由 {@code McpControlPlaneService.Preview} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code McpRuleContent}, and {@code McpControlPlaneService.Preview} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.Preview} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.Preview}; do not couple callers to its representation when the owning type exposes an API.
             */
            McpRuleContent content,
            /**
             * 中文说明：保存 validation 对应的状态、依赖或配置值；字段类型为 {@code McpValidationService.ValidationReport}，由 {@code McpControlPlaneService.Preview} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by validation; its type is {@code McpValidationService.ValidationReport}, and {@code McpControlPlaneService.Preview} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpControlPlaneService.Preview} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService.Preview}; do not couple callers to its representation when the owning type exposes an API.
             */
            McpValidationService.ValidationReport validation
    ) {
    }
}
