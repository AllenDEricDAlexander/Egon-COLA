package top.egon.cola.component.gateway.admin.catalog.domain.dto;


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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.gateway.admin.catalog.service.GatewayCatalogService;
import top.egon.cola.component.gateway.admin.catalog.repository.GatewayCatalogRepository;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;

import java.util.List;
import java.util.Map;

import top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCatalogResourceCreatedVO;

/**
 * 中文说明：{@code GatewayManualInterfaceGroupRequestDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Manual接口Group请求相关的职责与边界。
 * English summary: {@code GatewayManualInterfaceGroupRequestDTO} is an immutable data carrier in the current Gateway module; it owns the manual interface group request-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param businessCode 参数 businessCode；parameter business code。
 * @param businessName 参数 businessName；parameter business name。
 * @param entityCode 参数 entityCode；parameter entity code。
 * @param entityName 参数 entityName；parameter entity name。
 * @param interfaceGroupCode 参数 接口GroupCode；parameter interface group code。
 * @param interfaceGroupName 参数 接口GroupName；parameter interface group name。
 * @param className 参数 className；parameter class name。
 * @param description 参数 description；parameter description。
 */
public record GatewayManualInterfaceGroupRequestDTO(
        /**
         * 中文说明：保存 businessCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by business code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String businessCode,
        /**
         * 中文说明：保存 businessName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by business name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String businessName,
        /**
         * 中文说明：保存 entityCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by entity code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String entityCode,
        /**
         * 中文说明：保存 entityName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by entity name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String entityName,
        /**
         * 中文说明：保存 接口GroupCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by interface group code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String interfaceGroupCode,
        /**
         * 中文说明：保存 接口GroupName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by interface group name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String interfaceGroupName,
        /**
         * 中文说明：保存 className 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by class name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String className,
        /**
         * 中文说明：保存 description 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by description; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualInterfaceGroupRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String description
) {
}
