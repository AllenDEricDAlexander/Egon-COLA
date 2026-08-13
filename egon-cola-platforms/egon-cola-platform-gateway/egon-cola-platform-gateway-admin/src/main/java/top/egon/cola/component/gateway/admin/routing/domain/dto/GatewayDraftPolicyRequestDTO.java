package top.egon.cola.component.gateway.admin.routing.domain.dto;


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
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.gateway.admin.routing.service.GatewayDraftService;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;

import java.util.Map;


/**
 * 中文说明：{@code GatewayDraftPolicyRequestDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责策略请求相关的职责与边界。
 * English summary: {@code GatewayDraftPolicyRequestDTO} is an immutable data carrier in the current Gateway module; it owns the policy request-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param policyType 参数 策略Type；parameter policy type。
 * @param policyScope 参数 策略Scope；parameter policy scope。
 * @param content 参数 content；parameter content。
 * @param enabled 参数 enabled；parameter enabled。
 * @param expectedRevision 参数 expectedRevision；parameter expected revision。
 * @param idempotencyKey 参数 idempotency键；parameter idempotency key。
 * @param changeReason 参数 changeReason；parameter change reason。
 */
public record GatewayDraftPolicyRequestDTO(
        /**
         * 中文说明：保存 策略Type 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by policy type; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String policyType,
        /**
         * 中文说明：保存 策略Scope 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by policy scope; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String policyScope,
        /**
         * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotNull Map<String, Object> content,
        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean enabled,
        /**
         * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @PositiveOrZero long expectedRevision,
        /**
         * 中文说明：保存 idempotency键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by idempotency key; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String idempotencyKey,
        /**
         * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.routing.domain.dto.GatewayDraftPolicyRequestDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        @NotBlank String changeReason
) {
}
