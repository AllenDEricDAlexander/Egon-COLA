package top.egon.cola.component.gateway.admin.mcp.domain.dto;


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

import top.egon.cola.component.gateway.admin.mcp.domain.vo.McpServerVO;
import top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO;
import top.egon.cola.component.gateway.admin.mcp.domain.vo.McpCapabilityPreviewVO;

/**
 * 中文说明：{@code McpArtifactUploadDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责制品Upload相关的职责与边界。
 * English summary: {@code McpArtifactUploadDTO} is an immutable data carrier in the current Gateway module; it owns the artifact upload-related responsibility and boundary.
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
public record McpArtifactUploadDTO(
        /**
         * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String gatewayGroupId,
        /**
         * 中文说明：保存 appCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by app code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String appCode,
        /**
         * 中文说明：保存 version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by version; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String version,
        /**
         * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String displayName,
        /**
         * 中文说明：保存 资源Uri 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by resource uri; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String resourceUri,
        /**
         * 中文说明：保存 mimeType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by mime type; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String mimeType,
        /**
         * 中文说明：保存 content安全策略 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by content security policy; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String contentSecurityPolicy,
        /**
         * 中文说明：保存 permissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by permissions; its type is {@code Set<String>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> permissions,
        /**
         * 中文说明：保存 allowedOrigins 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by allowed origins; its type is {@code Set<String>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> allowedOrigins,
        /**
         * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code byte[]}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code byte[]}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        byte[] content,
        /**
         * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long expectedRevision,
        /**
         * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long expectedDraftRevision,
        /**
         * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String changeReason
) {

    /**
     * 中文说明：创建 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
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
    public McpArtifactUploadDTO {
        content = Objects.requireNonNull(content, "content").clone();
        permissions = Set.copyOf(permissions);
        allowedOrigins = Set.copyOf(allowedOrigins);
    }

    /**
     * 中文说明：执行 content 操作；该方法是 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the content operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpArtifactUploadDTO.content(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 content 的处理结果；returns the result of the operation.
     */
    @Override
    public byte[] content() {
        return content.clone();
    }
}
