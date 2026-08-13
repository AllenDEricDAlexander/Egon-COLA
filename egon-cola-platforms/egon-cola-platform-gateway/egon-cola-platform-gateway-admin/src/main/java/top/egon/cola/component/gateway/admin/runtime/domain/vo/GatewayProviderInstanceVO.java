package top.egon.cola.component.gateway.admin.runtime.domain.vo;


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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigClientInstance;
import top.egon.cola.component.ddc.model.management.DdcInstanceStatus;
import top.egon.cola.component.ddc.model.management.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceInstance;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceKey;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementServiceSnapshot;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.release.service.GatewayReleaseService;
import top.egon.cola.component.gateway.admin.release.repository.GatewayReleaseRepository;
import top.egon.cola.component.gateway.admin.config.GatewayAdminProperties;
import top.egon.cola.component.gateway.admin.group.domain.po.GatewayGroupPO;
import top.egon.cola.component.gateway.admin.group.repository.GatewayGroupRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import top.egon.cola.component.gateway.admin.runtime.domain.dto.GatewayProviderQueryDTO;
import top.egon.cola.component.gateway.admin.runtime.service.GatewayRuleExpectation;
import top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts;

/**
 * 中文说明：{@code GatewayProviderInstanceVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责提供方Instance投影相关的职责与边界。
 * English summary: {@code GatewayProviderInstanceVO} is an immutable data carrier in the current Gateway module; it owns the provider instance projection-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param serviceKey 参数 服务键；parameter service key。
 * @param protocol 参数 protocol；parameter protocol。
 * @param serviceName 参数 服务Name；parameter service name。
 * @param group 参数 group；parameter group。
 * @param version 参数 version；parameter version。
 * @param instanceId 参数 instanceId；parameter instance id。
 * @param leaseId 参数 租约Id；parameter lease id。
 * @param host 参数 host；parameter host。
 * @param port 参数 port；parameter port。
 * @param region 参数 region；parameter region。
 * @param zone 参数 zone；parameter zone。
 * @param weight 参数 weight；parameter weight。
 * @param tags 参数 tags；parameter tags。
 * @param definitionSetId 参数 定义SetId；parameter definition set id。
 * @param status 参数 status；parameter status。
 * @param expireAt 参数 expireAt；parameter expire at。
 * @param observedAt 参数 observedAt；parameter observed at。
 */
public record GatewayProviderInstanceVO(
        /**
         * 中文说明：保存 服务键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by service key; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String serviceKey,
        /**
         * 中文说明：保存 protocol 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by protocol; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String protocol,
        /**
         * 中文说明：保存 服务Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by service name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String serviceName,
        /**
         * 中文说明：保存 group 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by group; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String group,
        /**
         * 中文说明：保存 version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by version; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String version,
        /**
         * 中文说明：保存 instanceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by instance id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String instanceId,
        /**
         * 中文说明：保存 租约Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by lease id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String leaseId,
        /**
         * 中文说明：保存 host 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by host; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String host,
        /**
         * 中文说明：保存 port 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by port; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int port,
        /**
         * 中文说明：保存 region 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by region; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String region,
        /**
         * 中文说明：保存 zone 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by zone; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String zone,
        /**
         * 中文说明：保存 weight 对应的状态、依赖或配置值；字段类型为 {@code Integer}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by weight; its type is {@code Integer}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Integer weight,
        /**
         * 中文说明：保存 tags 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by tags; its type is {@code Map<String, String>}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, String> tags,
        /**
         * 中文说明：保存 定义SetId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by definition set id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String definitionSetId,
        /**
         * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String status,
        /**
         * 中文说明：保存 expireAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expire at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant expireAt,
        /**
         * 中文说明：保存 observedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by observed at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant observedAt
) {

    /**
     * 中文说明：创建 {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param serviceKey 参数 服务键；parameter service key。
     * @param protocol 参数 protocol；parameter protocol。
     * @param serviceName 参数 服务Name；parameter service name。
     * @param group 参数 group；parameter group。
     * @param version 参数 version；parameter version。
     * @param instanceId 参数 instanceId；parameter instance id。
     * @param leaseId 参数 租约Id；parameter lease id。
     * @param host 参数 host；parameter host。
     * @param port 参数 port；parameter port。
     * @param region 参数 region；parameter region。
     * @param zone 参数 zone；parameter zone。
     * @param weight 参数 weight；parameter weight。
     * @param tags 参数 tags；parameter tags。
     * @param definitionSetId 参数 定义SetId；parameter definition set id。
     * @param status 参数 status；parameter status。
     * @param expireAt 参数 expireAt；parameter expire at。
     * @param observedAt 参数 observedAt；parameter observed at。
     */
    public GatewayProviderInstanceVO {
        tags = Map.copyOf(tags);
    }
}
