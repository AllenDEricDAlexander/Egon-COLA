package top.egon.cola.component.gateway.admin.observability.domain.po;


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

import top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayTraceQueryDTO;
import top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO;
import top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO;
import top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO;
import top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayRequestPointDTO;
import top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayProtocolCallDTO;
import top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO;
import top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO;

/**
 * 中文说明：{@code GatewayConsumeFailurePO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责GatewayConsumeFailurePO相关的职责与边界。
 * English summary: {@code GatewayConsumeFailurePO} is an immutable data carrier in the current Gateway module; it owns the consume failure-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param id 参数 id；parameter id。
 * @param topic 参数 topic；parameter topic。
 * @param partition 参数 partition；parameter partition。
 * @param offset 参数 offset；parameter offset。
 * @param eventId 参数 事件Id；parameter event id。
 * @param failureCode 参数 failureCode；parameter failure code。
 * @param failureMessage 参数 failure消息；parameter failure message。
 * @param payloadSha256 参数 payloadSha256；parameter payload sha256。
 * @param payloadSize 参数 payloadSize；parameter payload size。
 * @param occurredAt 参数 occurredAt；parameter occurred at。
 */
public record GatewayConsumeFailurePO(
        /**
         * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String id,
        /**
         * 中文说明：保存 topic 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by topic; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String topic,
        /**
         * 中文说明：保存 partition 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by partition; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int partition,
        /**
         * 中文说明：保存 offset 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by offset; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long offset,
        /**
         * 中文说明：保存 事件Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by event id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String eventId,
        /**
         * 中文说明：保存 failureCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by failure code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String failureCode,
        /**
         * 中文说明：保存 failure消息 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by failure message; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String failureMessage,
        /**
         * 中文说明：保存 payloadSha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by payload sha256; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String payloadSha256,
        /**
         * 中文说明：保存 payloadSize 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by payload size; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int payloadSize,
        /**
         * 中文说明：保存 occurredAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by occurred at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant occurredAt
) {
}
