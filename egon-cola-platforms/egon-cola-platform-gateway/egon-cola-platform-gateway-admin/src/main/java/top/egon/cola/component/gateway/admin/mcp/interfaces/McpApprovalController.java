package top.egon.cola.component.gateway.admin.mcp.interfaces;

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
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpApprovalStore;
import top.egon.cola.component.gateway.mcp.security.McpSecurityDigests;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
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
public class McpApprovalController {

    /**
     * 中文说明：表示 TOKENBYTES 这一固定值；它属于 {@code McpApprovalController} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value token bytes; it is a state, type, or protocol value of {@code McpApprovalController} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpApprovalController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpApprovalController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final int TOKEN_BYTES = 32;

    /**
     * 中文说明：保存 approvals 对应的状态、依赖或配置值；字段类型为 {@code JdbcMcpApprovalStore}，由 {@code McpApprovalController} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by approvals; its type is {@code JdbcMcpApprovalStore}, and {@code McpApprovalController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpApprovalController} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpApprovalController}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcMcpApprovalStore approvals;
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
            JdbcMcpApprovalStore approvals,
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
            JdbcMcpApprovalStore approvals,
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
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApprovalResponse issue(
            @Valid @RequestBody ApprovalRequest request,
            Authentication authentication) {
        ApprovalOwner owner = owner(authentication);
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plusSeconds(request.ttlSeconds());
        String token = token();
        String id = UUID.randomUUID().toString();
        approvals.issue(new JdbcMcpApprovalStore.Approval(
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
        return new ApprovalResponse(id, token, expiresAt);
    }

    /**
     * 中文说明：执行 owner 操作；该方法是 {@code McpApprovalController} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the owner operation; this method is the invocation entry point on {@code McpApprovalController} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpApprovalController.owner(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param authentication 参数 authentication；parameter authentication。
     * @return 返回 owner 的处理结果；returns the result of the operation.
     */
    private ApprovalOwner owner(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException(
                    "GATEWAY_ADMIN_AUTHENTICATION_REQUIRED"
            );
        }
        if (authentication.getPrincipal()
                instanceof IdentityPrincipal principal) {
            return new ApprovalOwner(
                    principal.subject(),
                    principal.tenantId(),
                    principal.clientId()
            );
        }
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return new ApprovalOwner(
                    jwt.getToken().getSubject(),
                    jwt.getToken().getClaimAsString("tid"),
                    jwt.getToken().getClaimAsString("client_id")
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

    /**
     * 中文说明：{@code ApprovalRequest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责审批请求相关的职责与边界。
     * English summary: {@code ApprovalRequest} is an immutable data carrier in the current Gateway module; it owns the approval request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param toolName 参数 工具Name；parameter tool name。
     * @param arguments 参数 arguments；parameter arguments。
     * @param ttlSeconds 参数 ttlSeconds；parameter ttl seconds。
     */
    public record ApprovalRequest(
            /**
             * 中文说明：保存 服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpApprovalController.ApprovalRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server code; its type is {@code String}, and {@code McpApprovalController.ApprovalRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpApprovalController.ApprovalRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpApprovalController.ApprovalRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String serverCode,
            /**
             * 中文说明：保存 工具Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpApprovalController.ApprovalRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tool name; its type is {@code String}, and {@code McpApprovalController.ApprovalRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpApprovalController.ApprovalRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpApprovalController.ApprovalRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotBlank String toolName,
            /**
             * 中文说明：保存 arguments 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpApprovalController.ApprovalRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by arguments; its type is {@code Map<String, Object>}, and {@code McpApprovalController.ApprovalRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpApprovalController.ApprovalRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpApprovalController.ApprovalRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @NotNull Map<String, Object> arguments,
            /**
             * 中文说明：保存 ttlSeconds 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpApprovalController.ApprovalRequest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by ttl seconds; its type is {@code long}, and {@code McpApprovalController.ApprovalRequest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpApprovalController.ApprovalRequest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpApprovalController.ApprovalRequest}; do not couple callers to its representation when the owning type exposes an API.
             */
            @Min(1) @Max(300) long ttlSeconds
    ) {
    }

    /**
     * 中文说明：{@code ApprovalResponse} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责审批响应相关的职责与边界。
     * English summary: {@code ApprovalResponse} is an immutable data carrier in the current Gateway module; it owns the approval response-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param approvalId 参数 审批Id；parameter approval id。
     * @param approvalToken 参数 审批Token；parameter approval token。
     * @param expiresAt 参数 expiresAt；parameter expires at。
     */
    public record ApprovalResponse(
            /**
             * 中文说明：保存 审批Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpApprovalController.ApprovalResponse} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by approval id; its type is {@code String}, and {@code McpApprovalController.ApprovalResponse} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpApprovalController.ApprovalResponse} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpApprovalController.ApprovalResponse}; do not couple callers to its representation when the owning type exposes an API.
             */
            String approvalId,
            /**
             * 中文说明：保存 审批Token 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpApprovalController.ApprovalResponse} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by approval token; its type is {@code String}, and {@code McpApprovalController.ApprovalResponse} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpApprovalController.ApprovalResponse} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpApprovalController.ApprovalResponse}; do not couple callers to its representation when the owning type exposes an API.
             */
            String approvalToken,
            /**
             * 中文说明：保存 expiresAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code McpApprovalController.ApprovalResponse} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expires at; its type is {@code Instant}, and {@code McpApprovalController.ApprovalResponse} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpApprovalController.ApprovalResponse} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpApprovalController.ApprovalResponse}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant expiresAt
    ) {

        /**
         * 中文说明：执行 toString 操作；该方法是 {@code McpApprovalController.ApprovalResponse} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the to string operation; this method is the invocation entry point on {@code McpApprovalController.ApprovalResponse} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpApprovalController.ApprovalResponse.toString(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 toString 的处理结果；returns the result of the operation.
         */
        @Override
        public String toString() {
            return "ApprovalResponse[approvalId=" + approvalId
                    + ", approvalToken=<redacted>, expiresAt="
                    + expiresAt + ']';
        }
    }

    /**
     * 中文说明：{@code ApprovalOwner} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责审批Owner相关的职责与边界。
     * English summary: {@code ApprovalOwner} is an immutable data carrier in the current Gateway module; it owns the approval owner-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param subjectId 参数 subjectId；parameter subject id。
     * @param tenantId 参数 tenantId；parameter tenant id。
     * @param clientId 参数 客户端Id；parameter client id。
     */
    private record ApprovalOwner(
            /**
             * 中文说明：保存 subjectId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpApprovalController.ApprovalOwner} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by subject id; its type is {@code String}, and {@code McpApprovalController.ApprovalOwner} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpApprovalController.ApprovalOwner} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpApprovalController.ApprovalOwner}; do not couple callers to its representation when the owning type exposes an API.
             */
            String subjectId,
            /**
             * 中文说明：保存 tenantId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpApprovalController.ApprovalOwner} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tenant id; its type is {@code String}, and {@code McpApprovalController.ApprovalOwner} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpApprovalController.ApprovalOwner} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpApprovalController.ApprovalOwner}; do not couple callers to its representation when the owning type exposes an API.
             */
            String tenantId,
            /**
             * 中文说明：保存 客户端Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpApprovalController.ApprovalOwner} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by client id; its type is {@code String}, and {@code McpApprovalController.ApprovalOwner} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpApprovalController.ApprovalOwner} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpApprovalController.ApprovalOwner}; do not couple callers to its representation when the owning type exposes an API.
             */
            String clientId
    ) {

        /**
         * 中文说明：创建 {@code McpApprovalController.ApprovalOwner} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpApprovalController.ApprovalOwner} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param subjectId 参数 subjectId；parameter subject id。
         * @param tenantId 参数 tenantId；parameter tenant id。
         * @param clientId 参数 客户端Id；parameter client id。
         */
        private ApprovalOwner {
            subjectId = required(subjectId, "subjectId");
            tenantId = required(tenantId, "tenantId");
            clientId = required(clientId, "clientId");
        }

        /**
         * 中文说明：执行 required 操作；该方法是 {@code McpApprovalController.ApprovalOwner} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the required operation; this method is the invocation entry point on {@code McpApprovalController.ApprovalOwner} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpApprovalController.ApprovalOwner.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
}
