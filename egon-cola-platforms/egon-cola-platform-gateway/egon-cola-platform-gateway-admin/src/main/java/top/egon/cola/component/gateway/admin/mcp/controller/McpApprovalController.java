package top.egon.cola.component.gateway.admin.mcp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.McpApprovalRequestDTO;
import top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO;
import top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalVO;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.JdbcMcpApprovalRepository;
import top.egon.cola.component.gateway.mcp.security.McpSecurityDigests;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * 中文说明：{@code McpApprovalController} 是接口控制器，位于当前 Gateway 模块的相关包中，负责MCP审批控制器相关的职责与边界。
 * English summary: {@code McpApprovalController} is a mcp approval controller controller in the current Gateway module; it owns the mcp approval controller-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@RestController
@RequestMapping("/api/v1/gateway/admin/mcp/approvals")
@PreAuthorize("hasAnyAuthority('CAP_gateway:mcp:approve','CAP_*')")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "gateway-admin",
        entityDomainName = "Gateway Admin 管理实体域",
        code = "gateway-admin-mcp-approval-controller",
        name = "McpApprovalController 管理接口组")
public class McpApprovalController {

    /**
     * 中文说明：表示 TOKENBYTES 这一固定值；它属于 {@code McpApprovalController} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value token bytes; it is a state, type, or protocol value of {@code McpApprovalController} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpApprovalController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpApprovalController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final int TOKEN_BYTES = 32;

    /**
     * 中文说明：保存 approvals 对应的状态、依赖或配置值；字段类型为 {@code JdbcMcpApprovalRepository}，由 {@code McpApprovalController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by approvals; its type is {@code JdbcMcpApprovalRepository}, and {@code McpApprovalController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpApprovalController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpApprovalController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcMcpApprovalRepository approvals;
    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code McpApprovalController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code McpApprovalController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpApprovalController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpApprovalController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;
    /**
     * 中文说明：保存 random 对应的状态、依赖或配置值；字段类型为 {@code SecureRandom}，由 {@code McpApprovalController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by random; its type is {@code SecureRandom}, and {@code McpApprovalController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpApprovalController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpApprovalController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final SecureRandom random;
    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code McpApprovalController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code McpApprovalController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpApprovalController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpApprovalController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：创建 {@code McpApprovalController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpApprovalController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param approvals 参数 approvals；parameter approvals。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    @Autowired
    public McpApprovalController(
            JdbcMcpApprovalRepository approvals,
            ObjectMapper objectMapper) {
        this(
                approvals,
                objectMapper,
                new SecureRandom(),
                Clock.systemUTC()
        );
    }

    /**
     * 中文说明：创建 {@code McpApprovalController} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpApprovalController} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param approvals 参数 approvals；parameter approvals。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param random 参数 random；parameter random。
     * @param clock 参数 clock；parameter clock。
     */
    McpApprovalController(
            JdbcMcpApprovalRepository approvals,
            ObjectMapper objectMapper,
            SecureRandom random,
            Clock clock) {
        this.approvals = Objects.requireNonNull(approvals, "approvals");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
        this.random = Objects.requireNonNull(random, "random");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 中文说明：执行 issue 操作；该方法是 {@code McpApprovalController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the issue operation; this method is the invocation entry point on {@code McpApprovalController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpApprovalController.issue(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param authentication 参数 authentication；parameter authentication。
     * @return 返回 issue 的处理结果；returns the result of the operation.
     */
    @GatewayOperation(externalAccessible = true)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public McpApprovalVO issue(
            @Valid @RequestBody McpApprovalRequestDTO request,
            Authentication authentication) {
        McpApprovalOwnerVO owner = owner(authentication);
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plusSeconds(request.ttlSeconds());
        String token = token();
        String id = UUID.randomUUID().toString();
        approvals.issue(new top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO(
                id,
                McpSecurityDigests.token(token),
                owner.subjectId(),
                owner.tenantId(),
                owner.clientId(),
                request.serverCode(),
                request.toolName(),
                McpSecurityDigests.arguments(
                        objectMapper,
                        request.arguments()
                ),
                issuedAt,
                expiresAt
        ));
        return new McpApprovalVO(id, token, expiresAt);
    }

    /**
     * 中文说明：执行 owner 操作；该方法是 {@code McpApprovalController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the owner operation; this method is the invocation entry point on {@code McpApprovalController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpApprovalController.owner(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param authentication 参数 authentication；parameter authentication。
     * @return 返回 owner 的处理结果；returns the result of the operation.
     */
    private McpApprovalOwnerVO owner(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException(
                    "GATEWAY_ADMIN_AUTHENTICATION_REQUIRED"
            );
        }
        if (authentication.getPrincipal()
                instanceof IdentityPrincipal principal) {
            return new McpApprovalOwnerVO(
                    principal.subject(),
                    principal.tenantId(),
                    principal.audience().stream()
                            .sorted()
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException(
                                    "GATEWAY_ADMIN_RESOURCE_AUDIENCE_REQUIRED"
                            ))
            );
        }
        throw new IllegalStateException(
                "GATEWAY_ADMIN_IDENTITY_PRINCIPAL_REQUIRED"
        );
    }

    /**
     * 中文说明：执行 token 操作；该方法是 {@code McpApprovalController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the token operation; this method is the invocation entry point on {@code McpApprovalController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpApprovalController.token(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 token 的处理结果；returns the result of the operation.
     */
    private String token() {
        byte[] value = new byte[TOKEN_BYTES];
        random.nextBytes(value);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value);
    }






}
