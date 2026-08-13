package top.egon.cola.component.gateway.admin.mcp.domain.vo;


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
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.JdbcMcpApprovalRepository;
import top.egon.cola.component.gateway.mcp.security.McpSecurityDigests;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpApprovalRequestDTO;

/**
 * 中文说明：{@code McpApprovalOwnerVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责审批Owner相关的职责与边界。
 * English summary: {@code McpApprovalOwnerVO} is an immutable data carrier in the current Gateway module; it owns the approval owner-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param subjectId 参数 subjectId；parameter subject id。
 * @param tenantId 参数 tenantId；parameter tenant id。
 * @param clientId 参数 客户端Id；parameter client id。
 */
public record McpApprovalOwnerVO(
        /**
         * 中文说明：保存 subjectId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by subject id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String subjectId,
        /**
         * 中文说明：保存 tenantId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by tenant id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String tenantId,
        /**
         * 中文说明：保存 客户端Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by client id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String clientId
) {

    /**
     * 中文说明：创建 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param subjectId 参数 subjectId；parameter subject id。
     * @param tenantId 参数 tenantId；parameter tenant id。
     * @param clientId 参数 客户端Id；parameter client id。
     */
    public McpApprovalOwnerVO {
        subjectId = required(subjectId, "subjectId");
        tenantId = required(tenantId, "tenantId");
        clientId = required(clientId, "clientId");
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
