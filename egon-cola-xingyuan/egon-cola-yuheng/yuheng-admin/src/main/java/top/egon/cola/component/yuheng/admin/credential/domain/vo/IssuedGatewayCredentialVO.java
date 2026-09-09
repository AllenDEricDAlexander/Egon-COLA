package top.egon.cola.component.yuheng.admin.credential.domain.vo;


import top.egon.cola.component.yuheng.admin.application.controller.*;
import top.egon.cola.component.yuheng.admin.application.domain.dto.*;
import top.egon.cola.component.yuheng.admin.application.domain.exception.*;
import top.egon.cola.component.yuheng.admin.application.domain.po.*;
import top.egon.cola.component.yuheng.admin.application.domain.vo.*;
import top.egon.cola.component.yuheng.admin.application.repository.*;
import top.egon.cola.component.yuheng.admin.application.service.*;
import top.egon.cola.component.yuheng.admin.auth.controller.*;
import top.egon.cola.component.yuheng.admin.bootstrap.*;
import top.egon.cola.component.yuheng.admin.catalog.controller.*;
import top.egon.cola.component.yuheng.admin.catalog.domain.dto.*;
import top.egon.cola.component.yuheng.admin.catalog.domain.enums.*;
import top.egon.cola.component.yuheng.admin.catalog.domain.po.*;
import top.egon.cola.component.yuheng.admin.catalog.domain.vo.*;
import top.egon.cola.component.yuheng.admin.catalog.repository.*;
import top.egon.cola.component.yuheng.admin.catalog.repository.jdbc.*;
import top.egon.cola.component.yuheng.admin.catalog.service.*;
import top.egon.cola.component.yuheng.admin.config.*;
import top.egon.cola.component.yuheng.admin.config.properties.*;
import top.egon.cola.component.yuheng.admin.credential.controller.*;
import top.egon.cola.component.yuheng.admin.credential.domain.dto.*;
import top.egon.cola.component.yuheng.admin.credential.domain.po.*;
import top.egon.cola.component.yuheng.admin.credential.domain.vo.*;
import top.egon.cola.component.yuheng.admin.credential.repository.*;
import top.egon.cola.component.yuheng.admin.credential.repository.jdbc.*;
import top.egon.cola.component.yuheng.admin.credential.service.*;
import top.egon.cola.component.yuheng.admin.group.controller.*;
import top.egon.cola.component.yuheng.admin.group.domain.dto.*;
import top.egon.cola.component.yuheng.admin.group.domain.po.*;
import top.egon.cola.component.yuheng.admin.group.domain.vo.*;
import top.egon.cola.component.yuheng.admin.group.repository.*;
import top.egon.cola.component.yuheng.admin.group.service.*;
import top.egon.cola.component.yuheng.admin.mcp.controller.*;
import top.egon.cola.component.yuheng.admin.mcp.domain.dto.*;
import top.egon.cola.component.yuheng.admin.mcp.domain.enums.*;
import top.egon.cola.component.yuheng.admin.mcp.domain.exception.*;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.*;
import top.egon.cola.component.yuheng.admin.mcp.domain.vo.*;
import top.egon.cola.component.yuheng.admin.mcp.repository.*;
import top.egon.cola.component.yuheng.admin.mcp.repository.filesystem.*;
import top.egon.cola.component.yuheng.admin.mcp.repository.jdbc.*;
import top.egon.cola.component.yuheng.admin.mcp.service.*;
import top.egon.cola.component.yuheng.admin.observability.controller.*;
import top.egon.cola.component.yuheng.admin.observability.controller.message.*;
import top.egon.cola.component.yuheng.admin.observability.controller.scheduled.*;
import top.egon.cola.component.yuheng.admin.observability.domain.dto.*;
import top.egon.cola.component.yuheng.admin.observability.domain.enums.*;
import top.egon.cola.component.yuheng.admin.observability.domain.po.*;
import top.egon.cola.component.yuheng.admin.observability.domain.vo.*;
import top.egon.cola.component.yuheng.admin.observability.repository.*;
import top.egon.cola.component.yuheng.admin.observability.repository.jdbc.*;
import top.egon.cola.component.yuheng.admin.observability.service.*;
import top.egon.cola.component.yuheng.admin.release.controller.*;
import top.egon.cola.component.yuheng.admin.release.controller.scheduled.*;
import top.egon.cola.component.yuheng.admin.release.domain.*;
import top.egon.cola.component.yuheng.admin.release.domain.dto.*;
import top.egon.cola.component.yuheng.admin.release.domain.enums.*;
import top.egon.cola.component.yuheng.admin.release.domain.po.*;
import top.egon.cola.component.yuheng.admin.release.domain.vo.*;
import top.egon.cola.component.yuheng.admin.release.repository.*;
import top.egon.cola.component.yuheng.admin.release.repository.jdbc.*;
import top.egon.cola.component.yuheng.admin.release.service.*;
import top.egon.cola.component.yuheng.admin.reporting.controller.openapi.*;
import top.egon.cola.component.yuheng.admin.reporting.controller.scheduled.*;
import top.egon.cola.component.yuheng.admin.reporting.domain.dto.*;
import top.egon.cola.component.yuheng.admin.reporting.domain.po.*;
import top.egon.cola.component.yuheng.admin.reporting.domain.vo.*;
import top.egon.cola.component.yuheng.admin.reporting.repository.*;
import top.egon.cola.component.yuheng.admin.reporting.repository.jdbc.*;
import top.egon.cola.component.yuheng.admin.reporting.service.*;
import top.egon.cola.component.yuheng.admin.routing.controller.*;
import top.egon.cola.component.yuheng.admin.routing.domain.*;
import top.egon.cola.component.yuheng.admin.routing.domain.dto.*;
import top.egon.cola.component.yuheng.admin.routing.domain.po.*;
import top.egon.cola.component.yuheng.admin.routing.domain.vo.*;
import top.egon.cola.component.yuheng.admin.routing.repository.*;
import top.egon.cola.component.yuheng.admin.routing.repository.jdbc.*;
import top.egon.cola.component.yuheng.admin.routing.service.*;
import top.egon.cola.component.yuheng.admin.rule.domain.dto.*;
import top.egon.cola.component.yuheng.admin.rule.domain.vo.*;
import top.egon.cola.component.yuheng.admin.rule.service.*;
import top.egon.cola.component.yuheng.admin.runtime.controller.*;
import top.egon.cola.component.yuheng.admin.runtime.domain.dto.*;
import top.egon.cola.component.yuheng.admin.runtime.domain.vo.*;
import top.egon.cola.component.yuheng.admin.runtime.service.*;
import top.egon.cola.component.yuheng.admin.scope.controller.*;
import top.egon.cola.component.yuheng.admin.scope.domain.*;
import top.egon.cola.component.yuheng.admin.scope.domain.dto.*;
import top.egon.cola.component.yuheng.admin.scope.domain.vo.*;
import top.egon.cola.component.yuheng.admin.scope.service.*;
import top.egon.cola.component.yuheng.admin.shared.controller.*;
import top.egon.cola.component.yuheng.admin.shared.domain.*;
import top.egon.cola.component.yuheng.admin.shared.domain.enums.*;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.*;
import top.egon.cola.component.yuheng.admin.shared.domain.po.*;
import top.egon.cola.component.yuheng.admin.shared.domain.vo.*;
import top.egon.cola.component.yuheng.admin.shared.repository.*;
import top.egon.cola.component.yuheng.admin.shared.repository.jdbc.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminNotFoundException;
import top.egon.cola.component.yuheng.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.application.repository.GatewayApplicationRepository;
import top.egon.cola.component.yuheng.admin.observability.domain.po.GatewayAuditLogPO;
import top.egon.cola.component.yuheng.admin.observability.repository.GatewayAuditLogRepository;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;


/**
 * 中文说明：{@code IssuedGatewayCredentialVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Issued凭证相关的职责与边界。
 * English summary: {@code IssuedGatewayCredentialVO} is an immutable data carrier in the current Gateway module; it owns the issued credential-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param id 参数 id；parameter id。
 * @param accessKey 参数 access键；parameter access key。
 * @param secret 参数 secret；parameter secret。
 * @param status 参数 status；parameter status。
 * @param validFrom 参数 validFrom；parameter valid from。
 * @param validUntil 参数 validUntil；parameter valid until。
 */
public record IssuedGatewayCredentialVO(
        /**
         * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String id,
        /**
         * 中文说明：保存 access键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by access key; its type is {@code String}, and {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String accessKey,
        /**
         * 中文说明：保存 secret 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by secret; its type is {@code String}, and {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String secret,
        /**
         * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code String}, and {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String status,
        /**
         * 中文说明：保存 validFrom 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by valid from; its type is {@code Instant}, and {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant validFrom,
        /**
         * 中文说明：保存 validUntil 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by valid until; its type is {@code Instant}, and {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.yuheng.admin.credential.domain.vo.IssuedGatewayCredentialVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant validUntil
) {
}
