package top.egon.cola.component.gateway.admin.mcp.service;


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
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminIdempotencyConflictException;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.shared.repository.IdempotencyRepository;
import top.egon.cola.component.gateway.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.gateway.admin.observability.domain.po.GatewayAuditLogPO;
import top.egon.cola.component.gateway.admin.observability.repository.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.routing.domain.po.GatewayDraftPO;
import top.egon.cola.component.gateway.admin.routing.repository.GatewayDraftJpaRepository;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.JdbcMcpArtifactMetadataRepository;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.JdbcMcpCapabilityDraftRepository;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.JdbcMcpRemoteProviderRepository;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.JdbcMcpTaskRepository;
import top.egon.cola.component.gateway.admin.mcp.domain.po.McpServerPO;
import top.egon.cola.component.gateway.admin.mcp.repository.McpServerRepository;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore;
import top.egon.cola.component.gateway.mcp.app.McpAppSecurityValidator;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.admin.rule.service.GatewayRuleCanonicalizer;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminRevisionConflictException;
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


import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpServerMutationDTO;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpCapabilityMutationDTO;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteProviderMutationDTO;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteMountMutationDTO;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactMutationDTO;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpMutationControlDTO;
import top.egon.cola.component.gateway.admin.mcp.domain.vo.McpServerVO;
import top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO;
import top.egon.cola.component.gateway.admin.mcp.domain.vo.McpCapabilityPreviewVO;
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
     * 中文说明：保存 capabilities 对应的状态、依赖或配置值；字段类型为 {@code JdbcMcpCapabilityDraftRepository}，由 {@code McpControlPlaneService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by capabilities; its type is {@code JdbcMcpCapabilityDraftRepository}, and {@code McpControlPlaneService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcMcpCapabilityDraftRepository capabilities;

    /**
     * 中文说明：保存 远程 对应的状态、依赖或配置值；字段类型为 {@code JdbcMcpRemoteProviderRepository}，由 {@code McpControlPlaneService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by remote; its type is {@code JdbcMcpRemoteProviderRepository}, and {@code McpControlPlaneService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcMcpRemoteProviderRepository remote;

    /**
     * 中文说明：保存 artifacts 对应的状态、依赖或配置值；字段类型为 {@code JdbcMcpArtifactMetadataRepository}，由 {@code McpControlPlaneService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by artifacts; its type is {@code JdbcMcpArtifactMetadataRepository}, and {@code McpControlPlaneService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcMcpArtifactMetadataRepository artifacts;

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
     * 中文说明：保存 tasks 对应的状态、依赖或配置值；字段类型为 {@code JdbcMcpTaskRepository}，由 {@code McpControlPlaneService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by tasks; its type is {@code JdbcMcpTaskRepository}, and {@code McpControlPlaneService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcMcpTaskRepository tasks;

    /**
     * 中文说明：保存 drafts 对应的状态、依赖或配置值；字段类型为 {@code GatewayDraftRepository}，由 {@code McpControlPlaneService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by drafts; its type is {@code GatewayDraftRepository}, and {@code McpControlPlaneService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayDraftJpaRepository drafts;

    /**
     * 中文说明：保存 idempotency 对应的状态、依赖或配置值；字段类型为 {@code IdempotencyRepository}，由 {@code McpControlPlaneService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by idempotency; its type is {@code IdempotencyRepository}, and {@code McpControlPlaneService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpControlPlaneService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpControlPlaneService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final IdempotencyRepository idempotency;

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
            JdbcMcpCapabilityDraftRepository capabilities,
            JdbcMcpRemoteProviderRepository remote,
            JdbcMcpArtifactMetadataRepository artifacts,
            McpAppArtifactStore.Writer artifactWriter,
            McpAppArtifactStore.Reader artifactReader,
            McpAppSecurityValidator appSecurity,
            JdbcMcpTaskRepository tasks,
            GatewayDraftJpaRepository drafts,
            IdempotencyRepository idempotency,
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
            JdbcMcpCapabilityDraftRepository capabilities,
            JdbcMcpRemoteProviderRepository remote,
            JdbcMcpArtifactMetadataRepository artifacts,
            McpAppArtifactStore.Writer artifactWriter,
            McpAppArtifactStore.Reader artifactReader,
            McpAppSecurityValidator appSecurity,
            JdbcMcpTaskRepository tasks,
            GatewayDraftJpaRepository drafts,
            IdempotencyRepository idempotency,
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
    public List<McpServerVO> listServers(String gatewayGroupId) {
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
    public McpServerVO getServer(String id) {
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
    public McpMutationResultVO createServer(
            McpServerMutationDTO command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("CREATE_SERVER", command);
        McpMutationResultVO replay = replay(
                command.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        requireCreateRevision(command.expectedRevision());
        Instant now = clock.instant();
        GatewayDraftPO gatewayDraft = editable(
                command.gatewayGroupId(),
                command.expectedDraftRevision()
        );
        String id = UuidV7.simpleString();
        servers.saveAndFlush(new McpServerPO(
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
    public McpMutationResultVO updateServer(
            String id,
            McpServerMutationDTO command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("UPDATE_SERVER", Map.of(
                "id", id,
                "command", command
        ));
        McpMutationResultVO replay = replay(
                command.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        McpServerPO server = requiredServer(id);
        requireGroup(command.gatewayGroupId(), server.getGatewayGroupId());
        GatewayDraftPO gatewayDraft = editable(
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
    public McpMutationResultVO deleteServer(
            String id,
            McpMutationControlDTO control,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("DELETE_SERVER", Map.of(
                "id", id,
                "control", control
        ));
        McpMutationResultVO replay = replay(
                control.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        McpServerPO server = requiredServer(id);
        requireGroup(control.gatewayGroupId(), server.getGatewayGroupId());
        GatewayDraftPO gatewayDraft = editable(
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
    public List<top.egon.cola.component.gateway.admin.mcp.domain.po.McpCapabilityRecordPO> capabilities(
            String gatewayGroupId,
            String serverId,
            top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum kind) {
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
    public McpMutationResultVO putCapability(
            String id,
            top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum kind,
            McpCapabilityMutationDTO command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String resourceId = id == null ? UuidV7.simpleString() : id;
        String digest = digest("PUT_" + kind, Map.of(
                "id", resourceId,
                "command", command
        ));
        McpMutationResultVO replay = replay(
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
        GatewayDraftPO gatewayDraft = editable(
                command.gatewayGroupId(),
                command.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = capabilities.save(
                new top.egon.cola.component.gateway.admin.mcp.domain.po.McpCapabilityRecordPO(
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
    public McpMutationResultVO deleteCapability(
            String id,
            top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum kind,
            McpMutationControlDTO control,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("DELETE_" + kind, Map.of(
                "id", id,
                "control", control
        ));
        McpMutationResultVO replay = replay(
                control.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        requiredCapability(control.gatewayGroupId(), kind, id);
        GatewayDraftPO gatewayDraft = editable(
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
    public McpCapabilityPreviewVO preview(String gatewayGroupId) {
        McpRuleContent content = contentFactory.preview(gatewayGroupId);
        return new McpCapabilityPreviewVO(content, validation.validate(content));
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
    public top.egon.cola.component.gateway.admin.mcp.domain.vo.McpValidationReportVO validate(
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
    public List<top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteProviderDraftPO> providers(
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
    public McpMutationResultVO putProvider(
            String id,
            McpRemoteProviderMutationDTO command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String resourceId = id == null ? UuidV7.simpleString() : id;
        String digest = digest("PUT_REMOTE_PROVIDER", Map.of(
                "id", resourceId,
                "command", command
        ));
        McpMutationResultVO replay = replay(
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
        GatewayDraftPO gatewayDraft = editable(
                command.gatewayGroupId(),
                command.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = remote.saveProvider(
                new top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteProviderDraftPO(
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
    public McpMutationResultVO deleteProvider(
            String id,
            McpMutationControlDTO control,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("DELETE_REMOTE_PROVIDER", Map.of(
                "id", id,
                "control", control
        ));
        McpMutationResultVO replay = replay(
                control.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        requiredProvider(control.gatewayGroupId(), id);
        GatewayDraftPO gatewayDraft = editable(
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
    public List<top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteCapabilityPO>
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
    public List<top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO> mounts(
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
    public McpMutationResultVO putMount(
            String id,
            McpRemoteMountMutationDTO command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String resourceId = id == null ? UuidV7.simpleString() : id;
        String digest = digest("PUT_REMOTE_MOUNT", Map.of(
                "id", resourceId,
                "command", command
        ));
        McpMutationResultVO replay = replay(
                command.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        requiredServerInGroup(command.serverId(), command.gatewayGroupId());
        requiredProvider(command.gatewayGroupId(), command.providerId());
        GatewayDraftPO gatewayDraft = editable(
                command.gatewayGroupId(),
                command.expectedDraftRevision()
        );
        Instant now = clock.instant();
        var mutation = remote.saveMount(
                new top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO(
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
    public McpMutationResultVO deleteMount(
            String id,
            McpMutationControlDTO control,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("DELETE_REMOTE_MOUNT", Map.of(
                "id", id,
                "control", control
        ));
        McpMutationResultVO replay = replay(
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
        GatewayDraftPO gatewayDraft = editable(
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
    public List<top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO> artifacts(
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
    public top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO artifact(String id) {
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
    public McpMutationResultVO uploadArtifact(
            McpArtifactUploadDTO command,
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
        McpArtifactMutationDTO mutation = new McpArtifactMutationDTO(
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
        return registerArtifact(new McpArtifactMutationDTO(
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
    public McpMutationResultVO registerArtifact(
            McpArtifactMutationDTO command,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("REGISTER_ARTIFACT", command);
        McpMutationResultVO replay = replay(
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
        GatewayDraftPO gatewayDraft = editable(
                command.gatewayGroupId(),
                command.expectedDraftRevision()
        );
        Instant now = clock.instant();
        String id = UuidV7.simpleString();
        artifacts.save(new top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO(
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
            McpArtifactMutationDTO command,
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
    public McpMutationResultVO revokeArtifact(
            String id,
            McpMutationControlDTO control,
            String idempotencyKey,
            AdminActor actor,
            RequestAuditContext request) {
        String digest = digest("REVOKE_ARTIFACT", Map.of(
                "id", id,
                "control", control
        ));
        McpMutationResultVO replay = replay(
                control.gatewayGroupId(),
                idempotencyKey,
                digest
        );
        if (replay != null) {
            return replay;
        }
        top.egon.cola.component.gateway.admin.mcp.domain.po.McpArtifactMetadataPO artifact = artifact(id);
        requireGroup(control.gatewayGroupId(), artifact.gatewayGroupId());
        GatewayDraftPO gatewayDraft = editable(
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
    public List<top.egon.cola.component.gateway.admin.mcp.domain.po.McpTaskPO> tasks(
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
    public top.egon.cola.component.gateway.admin.mcp.domain.po.McpTaskPO task(String id) {
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
        top.egon.cola.component.gateway.admin.mcp.domain.po.McpTaskPO task = task(id);
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
        idempotency.save(new top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO(
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
        top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO existing = idempotency.find(
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
    private McpMutationResultVO finish(
            GatewayDraftPO draft,
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
        McpMutationResultVO result = new McpMutationResultVO(
                draft.getRevision(),
                resourceId,
                resourceRevision,
                false
        );
        idempotency.save(new top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO(
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
    private McpMutationResultVO replay(
            String gatewayGroupId,
            String idempotencyKey,
            String payloadDigest) {
        String key = required(idempotencyKey, "idempotencyKey");
        top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO existing = idempotency.find(
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
        return new McpMutationResultVO(
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
    private McpServerPO requiredServer(String id) {
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
    private top.egon.cola.component.gateway.admin.mcp.domain.po.McpCapabilityRecordPO requiredCapability(
            String gatewayGroupId,
            top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum kind,
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
    private top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteProviderDraftPO requiredProvider(
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
    private GatewayDraftPO editable(
            String gatewayGroupId,
            long expectedRevision) {
        GatewayDraftPO draft = drafts.findById(gatewayGroupId)
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
    private McpServerVO view(McpServerPO server) {
        return new McpServerVO(
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




















}
