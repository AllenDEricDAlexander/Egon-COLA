package top.egon.cola.component.gateway.admin.shared.domain.po;


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
import java.time.Instant;
import java.util.Map;
import java.util.Optional;


/**
 * 中文说明：{@code IdempotencyPO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Record相关的职责与边界。
 * English summary: {@code IdempotencyPO} is an immutable data carrier in the current Gateway module; it owns the record-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param scopeType 参数 scopeType；parameter scope type。
 * @param scopeId 参数 scopeId；parameter scope id。
 * @param key 参数 键；parameter key。
 * @param payloadSha256 参数 payloadSha256；parameter payload sha256。
 * @param resourceId 参数 资源Id；parameter resource id。
 * @param response 参数 响应；parameter response。
 * @param createdAt 参数 createdAt；parameter created at。
 * @param expiresAt 参数 expiresAt；parameter expires at。
 */
public record IdempotencyPO(
        /**
         * 中文说明：保存 scopeType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by scope type; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String scopeType,
        /**
         * 中文说明：保存 scopeId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by scope id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String scopeId,
        /**
         * 中文说明：保存 键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by key; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String key,
        /**
         * 中文说明：保存 payloadSha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by payload sha256; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String payloadSha256,
        /**
         * 中文说明：保存 资源Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by resource id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String resourceId,
        /**
         * 中文说明：保存 响应 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by response; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> response,
        /**
         * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant createdAt,
        /**
         * 中文说明：保存 expiresAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expires at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.shared.domain.po.IdempotencyPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant expiresAt
) {
}
