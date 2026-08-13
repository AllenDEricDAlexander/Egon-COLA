package top.egon.cola.component.gateway.admin.release.domain.po;


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
import top.egon.cola.component.gateway.admin.release.domain.enums.GatewayReleaseStatus;
import top.egon.cola.component.gateway.admin.rule.domain.vo.CompiledGatewayRelease;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * 中文说明：{@code GatewayReleaseTargetPO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责GatewayReleaseTargetPO相关的职责与边界。
 * English summary: {@code GatewayReleaseTargetPO} is an immutable data carrier in the current Gateway module; it owns the target record-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param instanceId 参数 instanceId；parameter instance id。
 * @param leaseId 参数 租约Id；parameter lease id。
 * @param status 参数 status；parameter status。
 * @param appliedVersion 参数 appliedVersion；parameter applied version。
 * @param appliedArtifactSha256 参数 applied制品Sha256；parameter applied artifact sha256。
 * @param errorCode 参数 errorCode；parameter error code。
 * @param observedAt 参数 observedAt；parameter observed at。
 */
public record GatewayReleaseTargetPO(
        /**
         * 中文说明：保存 instanceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by instance id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String instanceId,
        /**
         * 中文说明：保存 租约Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by lease id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String leaseId,
        /**
         * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String status,
        /**
         * 中文说明：保存 appliedVersion 对应的状态、依赖或配置值；字段类型为 {@code Long}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by applied version; its type is {@code Long}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Long appliedVersion,
        /**
         * 中文说明：保存 applied制品Sha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by applied artifact sha256; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String appliedArtifactSha256,
        /**
         * 中文说明：保存 errorCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by error code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String errorCode,
        /**
         * 中文说明：保存 observedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by observed at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.release.domain.po.GatewayReleaseTargetPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant observedAt
) {
}
