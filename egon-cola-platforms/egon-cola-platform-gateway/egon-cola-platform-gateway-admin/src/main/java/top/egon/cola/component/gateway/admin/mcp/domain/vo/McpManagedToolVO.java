package top.egon.cola.component.gateway.admin.mcp.domain.vo;


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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminIdempotencyConflictException;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.shared.repository.IdempotencyRepository;
import top.egon.cola.component.gateway.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;
import top.egon.cola.component.gateway.admin.observability.domain.po.GatewayAuditLogPO;
import top.egon.cola.component.gateway.admin.observability.repository.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.routing.domain.po.GatewayDraftPO;
import top.egon.cola.component.gateway.admin.routing.repository.GatewayDraftJpaRepository;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.JdbcMcpManagedToolOverrideRepository;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.JdbcMcpRemoteProviderRepository;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.JdbcMcpRemoteToolDraftRepository;
import top.egon.cola.component.gateway.admin.mcp.domain.po.McpServerPO;
import top.egon.cola.component.gateway.admin.mcp.repository.McpServerRepository;
import top.egon.cola.component.gateway.admin.rule.service.GatewayRuleCanonicalizer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpManagedToolOverrideMutationDTO;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolMutationDTO;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpToolMutationControlDTO;

/**
 * 中文说明：{@code McpManagedToolVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Managed工具View相关的职责与边界。
 * English summary: {@code McpManagedToolVO} is an immutable data carrier in the current Gateway module; it owns the managed tool view-related responsibility and boundary.
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
public record McpManagedToolVO(
        /**
         * 中文说明：保存 工具Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by tool id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String toolId,
        /**
         * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String gatewayGroupId,
        /**
         * 中文说明：保存 操作Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by operation id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String operationId,
        /**
         * 中文说明：保存 操作键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by operation key; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String operationKey,
        /**
         * 中文说明：保存 name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String name,
        /**
         * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String description,
        /**
         * 中文说明：保存 操作Protocol 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by operation protocol; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String operationProtocol,
        /**
         * 中文说明：保存 input模式 对应的状态、依赖或配置值；字段类型为 {@code Object}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by input schema; its type is {@code Object}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Object inputSchema,
        /**
         * 中文说明：保存 output模式 对应的状态、依赖或配置值；字段类型为 {@code Object}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by output schema; its type is {@code Object}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Object outputSchema,
        /**
         * 中文说明：保存 code服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by code server id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String codeServerId,
        /**
         * 中文说明：保存 code服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by code server code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String codeServerCode,
        /**
         * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String serverId,
        /**
         * 中文说明：保存 服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by server code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String serverCode,
        /**
         * 中文说明：保存 codePermissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by code permissions; its type is {@code Set<String>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> codePermissions,
        /**
         * 中文说明：保存 additionalPermissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by additional permissions; its type is {@code Set<String>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> additionalPermissions,
        /**
         * 中文说明：保存 effectivePermissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by effective permissions; its type is {@code Set<String>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> effectivePermissions,
        /**
         * 中文说明：保存 codeRiskLevel 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by code risk level; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String codeRiskLevel,
        /**
         * 中文说明：保存 minimumRiskLevel 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by minimum risk level; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String minimumRiskLevel,
        /**
         * 中文说明：保存 effectiveRiskLevel 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by effective risk level; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String effectiveRiskLevel,
        /**
         * 中文说明：保存 idempotent 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by idempotent; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean idempotent,
        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean enabled,
        /**
         * 中文说明：保存 overrideRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by override revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpManagedToolVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long overrideRevision
) {
}
