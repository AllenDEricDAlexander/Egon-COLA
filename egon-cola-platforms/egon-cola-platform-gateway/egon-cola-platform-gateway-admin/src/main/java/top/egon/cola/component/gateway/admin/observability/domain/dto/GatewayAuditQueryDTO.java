package top.egon.cola.component.gateway.admin.observability.domain.dto;


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
import top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO;
import top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO;
import top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO;
import top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO;

/**
 * 中文说明：{@code GatewayAuditQueryDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责审计Query相关的职责与边界。
 * English summary: {@code GatewayAuditQueryDTO} is an immutable data carrier in the current Gateway module; it owns the audit query-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param env 参数 env；parameter env。
 * @param namespace 参数 命名空间；parameter namespace。
 * @param actorId 参数 actorId；parameter actor id。
 * @param resourceId 参数 资源Id；parameter resource id。
 * @param traceId 参数 traceId；parameter trace id。
 * @param successful 参数 successful；parameter successful。
 * @param page 参数 page；parameter page。
 * @param size 参数 size；parameter size。
 */
public record GatewayAuditQueryDTO(
        /**
         * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String env,
        /**
         * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String namespace,
        /**
         * 中文说明：保存 actorId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by actor id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String actorId,
        /**
         * 中文说明：保存 资源Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by resource id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String resourceId,
        /**
         * 中文说明：保存 traceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by trace id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String traceId,
        /**
         * 中文说明：保存 successful 对应的状态、依赖或配置值；字段类型为 {@code Boolean}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by successful; its type is {@code Boolean}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Boolean successful,
        /**
         * 中文说明：保存 page 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by page; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int page,
        /**
         * 中文说明：保存 size 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by size; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int size
) {

    /**
     * 中文说明：创建 {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param actorId 参数 actorId；parameter actor id。
     * @param resourceId 参数 资源Id；parameter resource id。
     * @param traceId 参数 traceId；parameter trace id。
     * @param successful 参数 successful；parameter successful。
     * @param page 参数 page；parameter page。
     * @param size 参数 size；parameter size。
     */
    public GatewayAuditQueryDTO {
        if (page < 1 || size < 1 || size > 200) {
            throw new IllegalArgumentException(
                    "invalid audit page request"
            );
        }
    }
}
