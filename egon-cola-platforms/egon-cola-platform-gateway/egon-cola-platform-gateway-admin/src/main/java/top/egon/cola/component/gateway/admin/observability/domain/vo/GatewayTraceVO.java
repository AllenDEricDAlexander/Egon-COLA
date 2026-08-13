package top.egon.cola.component.gateway.admin.observability.domain.vo;


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
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;

import java.time.Instant;
import java.util.List;

import top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO;
import top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayTraceQueryDTO;
import top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO;
import top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO;
import top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayProtocolCallDTO;

/**
 * 中文说明：{@code GatewayTraceVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责GatewayTraceVO相关的职责与边界。
 * English summary: {@code GatewayTraceVO} is an immutable data carrier in the current Gateway module; it owns the trace summary-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param eventId 参数 事件Id；parameter event id。
 * @param traceId 参数 traceId；parameter trace id。
 * @param startedAt 参数 startedAt；parameter started at。
 * @param durationMs 参数 durationMs；parameter duration ms。
 * @param protocol 参数 protocol；parameter protocol。
 * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
 * @param operationKey 参数 操作键；parameter operation key。
 * @param statusCategory 参数 statusCategory；parameter status category。
 * @param engineInstanceId 参数 引擎InstanceId；parameter engine instance id。
 * @param providerService 参数 提供方服务；parameter provider service。
 */
public record GatewayTraceVO(
        /**
         * 中文说明：保存 事件Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by event id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String eventId,
        /**
         * 中文说明：保存 traceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by trace id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String traceId,
        /**
         * 中文说明：保存 startedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by started at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant startedAt,
        /**
         * 中文说明：保存 durationMs 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by duration ms; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long durationMs,
        /**
         * 中文说明：保存 protocol 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by protocol; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String protocol,
        /**
         * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String gatewayGroupId,
        /**
         * 中文说明：保存 操作键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by operation key; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String operationKey,
        /**
         * 中文说明：保存 statusCategory 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by status category; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String statusCategory,
        /**
         * 中文说明：保存 引擎InstanceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by engine instance id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String engineInstanceId,
        /**
         * 中文说明：保存 提供方服务 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by provider service; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String providerService
) {
}
