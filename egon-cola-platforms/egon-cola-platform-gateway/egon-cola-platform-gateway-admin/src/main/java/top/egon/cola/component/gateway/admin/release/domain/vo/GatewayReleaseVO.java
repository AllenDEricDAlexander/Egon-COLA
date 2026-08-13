package top.egon.cola.component.gateway.admin.release.domain.vo;


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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishTarget;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.gateway.admin.catalog.repository.GatewayCatalogRepository;
import top.egon.cola.component.gateway.admin.routing.service.GatewayDraftService;
import top.egon.cola.component.gateway.admin.routing.repository.GatewayDraftRepository;
import top.egon.cola.component.gateway.admin.reporting.service.GatewayOperationSchemaValidator;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;
import top.egon.cola.component.gateway.admin.release.domain.enums.GatewayReleaseStatus;
import top.egon.cola.component.gateway.admin.observability.domain.po.GatewayAuditLogPO;
import top.egon.cola.component.gateway.admin.observability.repository.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.routing.domain.po.GatewayDraftPO;
import top.egon.cola.component.gateway.admin.routing.repository.GatewayDraftJpaRepository;
import top.egon.cola.component.gateway.admin.group.domain.po.GatewayGroupPO;
import top.egon.cola.component.gateway.admin.group.repository.GatewayGroupRepository;
import top.egon.cola.component.gateway.admin.mcp.service.McpReleaseContentFactory;
import top.egon.cola.component.gateway.admin.rule.domain.vo.CompiledGatewayRelease;
import top.egon.cola.component.gateway.admin.routing.service.GatewayRouteDraftMapper;
import top.egon.cola.component.gateway.admin.routing.service.GatewayRouteTransportPolicyValidator;
import top.egon.cola.component.gateway.admin.rule.service.GatewayRuleCanonicalizer;
import top.egon.cola.component.gateway.admin.rule.service.GatewayRuleCompiler;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRpcDescriptor;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeRoute;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import top.egon.cola.component.gateway.admin.release.service.PreparedGatewayRelease;
import top.egon.cola.component.gateway.admin.release.domain.dto.GatewayReleaseCreateCommandDTO;
import top.egon.cola.component.gateway.admin.release.domain.dto.GatewayReleaseRollbackCommandDTO;

/**
 * 中文说明：{@code GatewayReleaseVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责发布View相关的职责与边界。
 * English summary: {@code GatewayReleaseVO} is an immutable data carrier in the current Gateway module; it owns the release view-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param releaseId 参数 发布Id；parameter release id。
 * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
 * @param draftRevision 参数 草稿Revision；parameter draft revision。
 * @param basedOnReleaseId 参数 basedOn发布Id；parameter based on release id。
 * @param rollbackOfReleaseId 参数 rollbackOf发布Id；parameter rollback of release id。
 * @param status 参数 status；parameter status。
 * @param partialApplied 参数 partialApplied；parameter partial applied。
 * @param changeId 参数 changeId；parameter change id。
 * @param validationReport 参数 validation报告；parameter validation report。
 * @param structuredDiff 参数 structuredDiff；parameter structured diff。
 * @param changeReason 参数 changeReason；parameter change reason。
 * @param createdAt 参数 createdAt；parameter created at。
 * @param updatedAt 参数 updatedAt；parameter updated at。
 * @param attempts 参数 attempts；parameter attempts。
 */
public record GatewayReleaseVO(
        /**
         * 中文说明：保存 发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by release id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String releaseId,
        /**
         * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String gatewayGroupId,
        /**
         * 中文说明：保存 草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by draft revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long draftRevision,
        /**
         * 中文说明：保存 basedOn发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by based on release id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String basedOnReleaseId,
        /**
         * 中文说明：保存 rollbackOf发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by rollback of release id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String rollbackOfReleaseId,
        /**
         * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code GatewayReleaseStatus}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code GatewayReleaseStatus}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        GatewayReleaseStatus status,
        /**
         * 中文说明：保存 partialApplied 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by partial applied; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean partialApplied,
        /**
         * 中文说明：保存 changeId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by change id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String changeId,
        /**
         * 中文说明：保存 validation报告 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by validation report; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> validationReport,
        /**
         * 中文说明：保存 structuredDiff 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by structured diff; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> structuredDiff,
        /**
         * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String changeReason,
        /**
         * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant createdAt,
        /**
         * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant updatedAt,
        /**
         * 中文说明：保存 attempts 对应的状态、依赖或配置值；字段类型为 {@code List<top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO>}，由 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by attempts; its type is {@code List<top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO>}, and {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.vo.GatewayReleaseVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        List<top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseAttemptPO> attempts
) {
}
