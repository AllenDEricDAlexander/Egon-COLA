package top.egon.cola.component.gateway.admin.catalog.domain.po;


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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualHierarchyDTO;
import top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupScopeVO;
import top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCurrentOperationDefinitionVO;
import top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCatalogTreeVO;
import top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayBusinessNodeVO;
import top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayEntityNodeVO;
import top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO;
import top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayOperationNodeVO;

/**
 * 中文说明：{@code GatewayOperationDefinitionPO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责操作定义相关的职责与边界。
 * English summary: {@code GatewayOperationDefinitionPO} is an immutable data carrier in the current Gateway module; it owns the operation definition-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param id 参数 id；parameter id。
 * @param operationId 参数 操作Id；parameter operation id。
 * @param definitionVersion 参数 定义Version；parameter definition version。
 * @param definitionSha256 参数 定义Sha256；parameter definition sha256。
 * @param summary 参数 summary；parameter summary。
 * @param tags 参数 tags；parameter tags。
 * @param requestSchema 参数 请求模式；parameter request schema。
 * @param responseSchema 参数 响应模式；parameter response schema。
 * @param errorSchema 参数 error模式；parameter error schema。
 * @param descriptorSnapshot 参数 descriptorSnapshot；parameter descriptor snapshot。
 * @param attributes 参数 attributes；parameter attributes。
 * @param externalAccessible 参数 externalAccessible；parameter external accessible。
 * @param createdAt 参数 createdAt；parameter created at。
 * @param createdBy 参数 createdBy；parameter created by。
 */
public record GatewayOperationDefinitionPO(
        /**
         * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String id,
        /**
         * 中文说明：保存 操作Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by operation id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String operationId,
        /**
         * 中文说明：保存 定义Version 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by definition version; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long definitionVersion,
        /**
         * 中文说明：保存 定义Sha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by definition sha256; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String definitionSha256,
        /**
         * 中文说明：保存 summary 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by summary; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String summary,
        /**
         * 中文说明：保存 tags 对应的状态、依赖或配置值；字段类型为 {@code List<String>}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by tags; its type is {@code List<String>}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        List<String> tags,
        /**
         * 中文说明：保存 请求模式 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by request schema; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> requestSchema,
        /**
         * 中文说明：保存 响应模式 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by response schema; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> responseSchema,
        /**
         * 中文说明：保存 error模式 对应的状态、依赖或配置值；字段类型为 {@code List<Map<String, Object>>}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by error schema; its type is {@code List<Map<String, Object>>}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        List<Map<String, Object>> errorSchema,
        /**
         * 中文说明：保存 descriptorSnapshot 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by descriptor snapshot; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> descriptorSnapshot,
        /**
         * 中文说明：保存 attributes 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by attributes; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> attributes,
        /**
         * 中文说明：保存 externalAccessible 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by external accessible; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean externalAccessible,
        /**
         * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant createdAt,
        /**
         * 中文说明：保存 createdBy 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by created by; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String createdBy
) {
}
