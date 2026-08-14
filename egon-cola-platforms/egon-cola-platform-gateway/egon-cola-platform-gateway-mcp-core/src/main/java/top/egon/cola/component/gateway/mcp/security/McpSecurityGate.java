package top.egon.cola.component.gateway.mcp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResource;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResourceTemplate;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.gateway.core.mcp.security.McpApprovalPort;
import top.egon.cola.component.gateway.core.mcp.security.McpAuthorizationPort;
import top.egon.cola.component.gateway.core.mcp.security.McpAuthorizationRequest;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * 中文说明：{@code McpSecurityGate} 是类型，位于当前 Gateway 模块的相关包中，负责MCP安全Gate相关的职责与边界。
 * English summary: {@code McpSecurityGate} is a type in the current Gateway module; it owns the mcp security gate-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpSecurityGate {

    /**
     * 中文说明：保存 授权 对应的状态、依赖或配置值；字段类型为 {@code McpAuthorizationPort}，由 {@code McpSecurityGate} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by authorization; its type is {@code McpAuthorizationPort}, and {@code McpSecurityGate} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpSecurityGate} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSecurityGate}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpAuthorizationPort authorization;
    /**
     * 中文说明：保存 approvals 对应的状态、依赖或配置值；字段类型为 {@code McpApprovalPort}，由 {@code McpSecurityGate} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by approvals; its type is {@code McpApprovalPort}, and {@code McpSecurityGate} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpSecurityGate} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSecurityGate}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpApprovalPort approvals;
    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code McpSecurityGate} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code McpSecurityGate} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpSecurityGate} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSecurityGate}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：创建 {@code McpSecurityGate} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpSecurityGate} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param authorization 参数 授权；parameter authorization。
     * @param approvals 参数 approvals；parameter approvals。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public McpSecurityGate(
            McpAuthorizationPort authorization,
            McpApprovalPort approvals,
            ObjectMapper objectMapper) {
        this.authorization = Objects.requireNonNull(
                authorization,
                "authorization"
        );
        this.approvals = Objects.requireNonNull(approvals, "approvals");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * 中文说明：执行 authorize工具调用 操作；该方法是 {@code McpSecurityGate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authorize tool call operation; this method is the invocation entry point on {@code McpSecurityGate} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityGate.authorizeToolCall(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param tool 参数 工具；parameter tool。
     * @param identity 参数 身份；parameter identity。
     * @param arguments 参数 arguments；parameter arguments。
     * @param approvalToken 参数 审批Token；parameter approval token。
     * @return 返回 authorize工具调用 的处理结果；returns the result of the operation.
     */
    public Publisher<Void> authorizeToolCall(
            McpRuntimeTool tool,
            IdentityContext identity,
            Map<String, Object> arguments,
            String approvalToken) {
        Objects.requireNonNull(tool, "tool");
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(arguments, "arguments");
        McpAuthorizationRequest request = identity.request(
                requiredPermissions(tool)
        );
        return authorize(request)
                .then(approveIfRequired(
                        tool,
                        identity,
                        arguments,
                        approvalToken
                ));
    }

    /**
     * 中文说明：执行 authorize资源Read 操作；该方法是 {@code McpSecurityGate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authorize resource read operation; this method is the invocation entry point on {@code McpSecurityGate} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityGate.authorizeResourceRead(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param resource 参数 资源；parameter resource。
     * @param identity 参数 身份；parameter identity。
     * @return 返回 authorize资源Read 的处理结果；returns the result of the operation.
     */
    public Publisher<Void> authorizeResourceRead(
            McpRuntimeResource resource,
            IdentityContext identity) {
        Objects.requireNonNull(resource, "resource");
        return authorize(identity.request(resourcePermissions(
                resource.serverCode(),
                resource.name(),
                resource.requiredPermissions()
        )));
    }

    /**
     * 中文说明：执行 authorize提示词 操作；该方法是 {@code McpSecurityGate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authorize prompt operation; this method is the invocation entry point on {@code McpSecurityGate} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityGate.authorizePrompt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param prompt 参数 提示词；parameter prompt。
     * @param identity 参数 身份；parameter identity。
     * @return 返回 authorize提示词 的处理结果；returns the result of the operation.
     */
    public Publisher<Void> authorizePrompt(
            McpRuntimePrompt prompt,
            IdentityContext identity) {
        Objects.requireNonNull(prompt, "prompt");
        return authorize(identity.request(primitivePermissions(
                prompt.serverCode(),
                "prompt",
                prompt.name(),
                "get",
                prompt.requiredPermissions()
        )));
    }

    /**
     * 中文说明：执行 authorize任务Action 操作；该方法是 {@code McpSecurityGate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authorize task action operation; this method is the invocation entry point on {@code McpSecurityGate} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityGate.authorizeTaskAction(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param toolName 参数 工具Name；parameter tool name。
     * @param action 参数 action；parameter action。
     * @param identity 参数 身份；parameter identity。
     * @return 返回 authorize任务Action 的处理结果；returns the result of the operation.
     */
    public Publisher<Void> authorizeTaskAction(
            String serverCode,
            String toolName,
            String action,
            IdentityContext identity) {
        Objects.requireNonNull(identity, "identity");
        return authorize(identity.request(Set.of(
                "mcp:" + Objects.requireNonNull(serverCode, "serverCode")
                        + ":tool:"
                        + Objects.requireNonNull(toolName, "toolName")
                        + ":task:"
                        + Objects.requireNonNull(action, "action")
        )));
    }

    /**
     * 中文说明：执行 authorize资源Read 操作；该方法是 {@code McpSecurityGate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authorize resource read operation; this method is the invocation entry point on {@code McpSecurityGate} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityGate.authorizeResourceRead(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param template 参数 模板；parameter template。
     * @param identity 参数 身份；parameter identity。
     * @return 返回 authorize资源Read 的处理结果；returns the result of the operation.
     */
    public Publisher<Void> authorizeResourceRead(
            McpRuntimeResourceTemplate template,
            IdentityContext identity) {
        Objects.requireNonNull(template, "template");
        return authorize(identity.request(resourcePermissions(
                template.serverCode(),
                template.name(),
                template.requiredPermissions()
        )));
    }

    /**
     * 中文说明：执行 authorize 操作；该方法是 {@code McpSecurityGate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authorize operation; this method is the invocation entry point on {@code McpSecurityGate} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityGate.authorize(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 authorize 的处理结果；returns the result of the operation.
     */
    private Mono<Void> authorize(McpAuthorizationRequest request) {
        return Mono.from(authorization.authorize(request))
                .switchIfEmpty(Mono.error(forbidden(
                        "RBAC3_AUTHORIZATION_EMPTY",
                        null
                )))
                .flatMap(decision -> decision.allowed()
                        ? Mono.<Void>empty()
                        : Mono.<Void>error(forbidden(
                        decision.reasonCode(),
                        decision
                )))
                .onErrorMap(
                        failure -> !(failure instanceof McpProtocolException),
                        failure -> forbidden(
                                "RBAC3_AUTHORIZATION_UNAVAILABLE",
                                null
                        )
                );
    }

    /**
     * 中文说明：执行 资源Permissions 操作；该方法是 {@code McpSecurityGate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the resource permissions operation; this method is the invocation entry point on {@code McpSecurityGate} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityGate.resourcePermissions(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param name 参数 name；parameter name。
     * @param declared 参数 declared；parameter declared。
     * @return 返回 资源Permissions 的处理结果；returns the result of the operation.
     */
    private Set<String> resourcePermissions(
            String serverCode,
            String name,
            Set<String> declared) {
        return primitivePermissions(
                serverCode,
                "resource",
                name,
                "read",
                declared
        );
    }

    /**
     * 中文说明：执行 primitivePermissions 操作；该方法是 {@code McpSecurityGate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the primitive permissions operation; this method is the invocation entry point on {@code McpSecurityGate} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityGate.primitivePermissions(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param primitive 参数 primitive；parameter primitive。
     * @param name 参数 name；parameter name。
     * @param action 参数 action；parameter action。
     * @param declared 参数 declared；parameter declared。
     * @return 返回 primitivePermissions 的处理结果；returns the result of the operation.
     */
    private Set<String> primitivePermissions(
            String serverCode,
            String primitive,
            String name,
            String action,
            Set<String> declared) {
        TreeSet<String> permissions = new TreeSet<>(declared);
        permissions.add("mcp:" + serverCode + ':' + primitive + ':'
                + name + ':' + action);
        return Set.copyOf(permissions);
    }

    /**
     * 中文说明：执行 approveIfRequired 操作；该方法是 {@code McpSecurityGate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the approve if required operation; this method is the invocation entry point on {@code McpSecurityGate} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityGate.approveIfRequired(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param tool 参数 工具；parameter tool。
     * @param identity 参数 身份；parameter identity。
     * @param arguments 参数 arguments；parameter arguments。
     * @param approvalToken 参数 审批Token；parameter approval token。
     * @return 返回 approveIfRequired 的处理结果；returns the result of the operation.
     */
    private Mono<Void> approveIfRequired(
            McpRuntimeTool tool,
            IdentityContext identity,
            Map<String, Object> arguments,
            String approvalToken) {
        if (!requiresApproval(tool)) {
            return Mono.empty();
        }
        if (approvalToken == null || approvalToken.isBlank()) {
            return Mono.error(new McpProtocolException(
                    McpErrorCode.MCP_APPROVAL_REQUIRED,
                    "MCP approval is required for this tool"
            ));
        }
        McpApprovalPort.ConsumptionRequest request =
                new McpApprovalPort.ConsumptionRequest(
                        McpSecurityDigests.token(approvalToken),
                        identity.subjectId(),
                        identity.tenantId(),
                        identity.clientId(),
                        tool.serverCode(),
                        tool.name(),
                        McpSecurityDigests.arguments(objectMapper, arguments)
                );
        return Mono.from(approvals.consume(request))
                .switchIfEmpty(Mono.just(McpApprovalPort.Result.UNAVAILABLE))
                .flatMap(result -> switch (result) {
                    case APPROVED -> Mono.empty();
                    case MISMATCH -> Mono.error(new McpProtocolException(
                            McpErrorCode.MCP_APPROVAL_MISMATCH,
                            "MCP approval does not match this request"
                    ));
                    case CONSUMED -> Mono.error(new McpProtocolException(
                            McpErrorCode.MCP_APPROVAL_CONSUMED,
                            "MCP approval was already consumed"
                    ));
                    case UNAVAILABLE -> Mono.error(forbidden(
                            "MCP_APPROVAL_UNAVAILABLE",
                            null
                    ));
                });
    }

    /**
     * 中文说明：执行 requiredPermissions 操作；该方法是 {@code McpSecurityGate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required permissions operation; this method is the invocation entry point on {@code McpSecurityGate} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityGate.requiredPermissions(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param tool 参数 工具；parameter tool。
     * @return 返回 requiredPermissions 的处理结果；returns the result of the operation.
     */
    private Set<String> requiredPermissions(McpRuntimeTool tool) {
        TreeSet<String> permissions = new TreeSet<>(
                tool.requiredPermissions()
        );
        permissions.add("mcp:" + tool.serverCode()
                + ":tool:" + tool.name() + ":call");
        return Set.copyOf(permissions);
    }

    /**
     * 中文说明：执行 requires审批 操作；该方法是 {@code McpSecurityGate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the requires approval operation; this method is the invocation entry point on {@code McpSecurityGate} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityGate.requiresApproval(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param tool 参数 工具；parameter tool。
     * @return 返回 requires审批 的处理结果；returns the result of the operation.
     */
    private boolean requiresApproval(McpRuntimeTool tool) {
        String risk = tool.riskLevel().toUpperCase(Locale.ROOT);
        return "HIGH".equals(risk) || "CRITICAL".equals(risk);
    }

    /**
     * 中文说明：执行 forbidden 操作；该方法是 {@code McpSecurityGate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the forbidden operation; this method is the invocation entry point on {@code McpSecurityGate} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityGate.forbidden(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param reasonCode 参数 reasonCode；parameter reason code。
     * @param decision 参数 decision；parameter decision。
     * @return 返回 forbidden 的处理结果；returns the result of the operation.
     */
    private McpProtocolException forbidden(
            String reasonCode,
            McpAuthorizationPort.Decision decision) {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("reasonCode", reasonCode);
        if (decision != null) {
            data.put("authVersion", decision.authVersion());
            data.put("contextVersion", decision.contextVersion());
            data.put("policyVersion", decision.policyVersion());
        }
        return new McpProtocolException(
                McpErrorCode.MCP_FORBIDDEN,
                "MCP authorization was denied",
                Map.copyOf(data)
        );
    }

    /**
     * 中文说明：{@code IdentityContext} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责身份Context相关的职责与边界。
     * English summary: {@code IdentityContext} is an immutable data carrier in the current Gateway module; it owns the identity context-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param issuer 参数 issuer；parameter issuer。
     * @param subjectId 参数 subjectId；parameter subject id。
     * @param tenantId 参数 tenantId；parameter tenant id。
     * @param clientId 参数 客户端Id；parameter client id。
     * @param tokenId 参数 tokenId；parameter token id。
     * @param resourceUri 参数 资源Uri；parameter resource uri。
     * @param issuedAt 参数 issuedAt；parameter issued at。
     * @param expiresAt 参数 expiresAt；parameter expires at。
     * @param minimumAuthVersion 参数 minimum认证Version；parameter minimum auth version。
     * @param minimumContextVersion 参数 minimumContextVersion；parameter minimum context version。
     * @param minimumPolicyVersion 参数 minimum策略Version；parameter minimum policy version。
     */
    public record IdentityContext(
            /**
             * 中文说明：保存 issuer 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpSecurityGate.IdentityContext} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by issuer; its type is {@code String}, and {@code McpSecurityGate.IdentityContext} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSecurityGate.IdentityContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSecurityGate.IdentityContext}; do not couple callers to its representation when the owning type exposes an API.
             */
            String issuer,
            /**
             * 中文说明：保存 subjectId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpSecurityGate.IdentityContext} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by subject id; its type is {@code String}, and {@code McpSecurityGate.IdentityContext} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSecurityGate.IdentityContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSecurityGate.IdentityContext}; do not couple callers to its representation when the owning type exposes an API.
             */
            String subjectId,
            /**
             * 中文说明：保存 tenantId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpSecurityGate.IdentityContext} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tenant id; its type is {@code String}, and {@code McpSecurityGate.IdentityContext} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSecurityGate.IdentityContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSecurityGate.IdentityContext}; do not couple callers to its representation when the owning type exposes an API.
             */
            String tenantId,
            /**
             * 中文说明：保存 客户端Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpSecurityGate.IdentityContext} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by client id; its type is {@code String}, and {@code McpSecurityGate.IdentityContext} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSecurityGate.IdentityContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSecurityGate.IdentityContext}; do not couple callers to its representation when the owning type exposes an API.
             */
            String clientId,
            /**
             * 中文说明：保存 tokenId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpSecurityGate.IdentityContext} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by token id; its type is {@code String}, and {@code McpSecurityGate.IdentityContext} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSecurityGate.IdentityContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSecurityGate.IdentityContext}; do not couple callers to its representation when the owning type exposes an API.
             */
            String tokenId,
            /**
             * 中文说明：保存 资源Uri 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpSecurityGate.IdentityContext} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by resource uri; its type is {@code String}, and {@code McpSecurityGate.IdentityContext} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSecurityGate.IdentityContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSecurityGate.IdentityContext}; do not couple callers to its representation when the owning type exposes an API.
             */
            String resourceUri,
            /**
             * 中文说明：保存 issuedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code McpSecurityGate.IdentityContext} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by issued at; its type is {@code Instant}, and {@code McpSecurityGate.IdentityContext} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSecurityGate.IdentityContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSecurityGate.IdentityContext}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant issuedAt,
            /**
             * 中文说明：保存 expiresAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code McpSecurityGate.IdentityContext} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expires at; its type is {@code Instant}, and {@code McpSecurityGate.IdentityContext} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSecurityGate.IdentityContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSecurityGate.IdentityContext}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant expiresAt,
            /**
             * 中文说明：保存 minimum认证Version 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpSecurityGate.IdentityContext} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by minimum auth version; its type is {@code long}, and {@code McpSecurityGate.IdentityContext} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSecurityGate.IdentityContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSecurityGate.IdentityContext}; do not couple callers to its representation when the owning type exposes an API.
             */
            long minimumAuthVersion,
            /**
             * 中文说明：保存 minimumContextVersion 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpSecurityGate.IdentityContext} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by minimum context version; its type is {@code long}, and {@code McpSecurityGate.IdentityContext} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSecurityGate.IdentityContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSecurityGate.IdentityContext}; do not couple callers to its representation when the owning type exposes an API.
             */
            long minimumContextVersion,
            /**
             * 中文说明：保存 minimum策略Version 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpSecurityGate.IdentityContext} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by minimum policy version; its type is {@code long}, and {@code McpSecurityGate.IdentityContext} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSecurityGate.IdentityContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSecurityGate.IdentityContext}; do not couple callers to its representation when the owning type exposes an API.
             */
            long minimumPolicyVersion
    ) {

        /**
         * 中文说明：创建 {@code McpSecurityGate.IdentityContext} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpSecurityGate.IdentityContext} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param issuer 参数 issuer；parameter issuer。
         * @param subjectId 参数 subjectId；parameter subject id。
         * @param tenantId 参数 tenantId；parameter tenant id。
         * @param clientId 参数 客户端Id；parameter client id。
         * @param tokenId 参数 tokenId；parameter token id。
         * @param resourceUri 参数 资源Uri；parameter resource uri。
         * @param issuedAt 参数 issuedAt；parameter issued at。
         * @param expiresAt 参数 expiresAt；parameter expires at。
         * @param minimumAuthVersion 参数 minimum认证Version；parameter minimum auth version。
         * @param minimumContextVersion 参数 minimumContextVersion；parameter minimum context version。
         * @param minimumPolicyVersion 参数 minimum策略Version；parameter minimum policy version。
         */
        public IdentityContext {
            issuer = required(issuer, "issuer");
            subjectId = required(subjectId, "subjectId");
            tenantId = required(tenantId, "tenantId");
            clientId = required(clientId, "clientId");
            tokenId = required(tokenId, "tokenId");
            resourceUri = required(resourceUri, "resourceUri");
            issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
            expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
            if (!expiresAt.isAfter(issuedAt)) {
                throw new IllegalArgumentException(
                        "expiresAt must be after issuedAt"
                );
            }
            nonNegative(minimumAuthVersion, "minimumAuthVersion");
            nonNegative(minimumContextVersion, "minimumContextVersion");
            nonNegative(minimumPolicyVersion, "minimumPolicyVersion");
        }

        /**
         * 中文说明：执行 请求 操作；该方法是 {@code McpSecurityGate.IdentityContext} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the request operation; this method is the invocation entry point on {@code McpSecurityGate.IdentityContext} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityGate.IdentityContext.request(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param permissions 参数 permissions；parameter permissions。
         * @return 返回 请求 的处理结果；returns the result of the operation.
         */
        private McpAuthorizationRequest request(Set<String> permissions) {
            return new McpAuthorizationRequest(
                    issuer,
                    subjectId,
                    tenantId,
                    clientId,
                    tokenId,
                    resourceUri,
                    issuedAt,
                    expiresAt,
                    permissions,
                    minimumAuthVersion,
                    minimumContextVersion,
                    minimumPolicyVersion
            );
        }

        /**
         * 中文说明：执行 from 操作；该方法是 {@code McpSecurityGate.IdentityContext} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the from operation; this method is the invocation entry point on {@code McpSecurityGate.IdentityContext} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityGate.IdentityContext.from(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param attributes 参数 attributes；parameter attributes。
         * @return 返回 from 的处理结果；returns the result of the operation.
         */
        public static IdentityContext from(
                Map<String, Object> attributes) {
            Objects.requireNonNull(attributes, "attributes");
            return new IdentityContext(
                    text(attributes, "identity.issuer", "idp.issuer"),
                    text(attributes, "identity.subject", "callerId"),
                    text(attributes, "identity.tenant-id", "tenantId"),
                    text(
                            attributes,
                            "identity.client-id",
                            "idp.client-id",
                            "idp.audience"
                    ),
                    text(
                            attributes,
                            "identity.token-id",
                            "idp.token-id"
                    ),
                    text(attributes, "identity.resource-uri", "idp.resource-uri"),
                    instant(
                            attributes,
                            "identity.issued-at",
                            "idp.issued-at"
                    ),
                    instant(
                            attributes,
                            "identity.expires-at",
                            "idp.expires-at"
                    ),
                    optionalNumber(attributes, "rbac3.auth-version"),
                    optionalNumber(attributes, "rbac3.context-version"),
                    optionalNumber(attributes, "rbac3.policy-version")
            );
        }

        /**
         * 中文说明：执行 text 操作；该方法是 {@code McpSecurityGate.IdentityContext} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the text operation; this method is the invocation entry point on {@code McpSecurityGate.IdentityContext} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityGate.IdentityContext.text(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param attributes 参数 attributes；parameter attributes。
         * @param keys 参数 keys；parameter keys。
         * @return 返回 text 的处理结果；returns the result of the operation.
         */
        private static String text(
                Map<String, Object> attributes,
                String... keys) {
            Object value = first(attributes, keys);
            if (!(value instanceof String text) || text.isBlank()) {
                throw new IllegalArgumentException(
                        keys[0] + " is required"
                );
            }
            return text.trim();
        }

        /**
         * 中文说明：执行 optionalNumber 操作；该方法是 {@code McpSecurityGate.IdentityContext} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the optional number operation; this method is the invocation entry point on {@code McpSecurityGate.IdentityContext} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityGate.IdentityContext.optionalNumber(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param attributes 参数 attributes；parameter attributes。
         * @param key 参数 键；parameter key。
         * @return 返回 optionalNumber 的处理结果；returns the result of the operation.
         */
        private static long optionalNumber(
                Map<String, Object> attributes,
                String key) {
            Object value = attributes.get(key);
            if (value == null) {
                return 0L;
            }
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String text && !text.isBlank()) {
                return Long.parseLong(text.trim());
            }
            throw new IllegalArgumentException(key + " must be a number");
        }

        /**
         * 中文说明：执行 instant 操作；该方法是 {@code McpSecurityGate.IdentityContext} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the instant operation; this method is the invocation entry point on {@code McpSecurityGate.IdentityContext} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityGate.IdentityContext.instant(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param attributes 参数 attributes；parameter attributes。
         * @param keys 参数 keys；parameter keys。
         * @return 返回 instant 的处理结果；returns the result of the operation.
         */
        private static Instant instant(
                Map<String, Object> attributes,
                String... keys) {
            Object value = first(attributes, keys);
            if (value instanceof Instant instant) {
                return instant;
            }
            if (value instanceof String text && !text.isBlank()) {
                return Instant.parse(text.trim());
            }
            throw new IllegalArgumentException(keys[0] + " is required");
        }

        /**
         * 中文说明：执行 first 操作；该方法是 {@code McpSecurityGate.IdentityContext} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the first operation; this method is the invocation entry point on {@code McpSecurityGate.IdentityContext} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityGate.IdentityContext.first(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param attributes 参数 attributes；parameter attributes。
         * @param keys 参数 keys；parameter keys。
         * @return 返回 first 的处理结果；returns the result of the operation.
         */
        private static Object first(
                Map<String, Object> attributes,
                String... keys) {
            for (String key : keys) {
                Object value = attributes.get(key);
                if (value != null) {
                    return value;
                }
            }
            return null;
        }

        /**
         * 中文说明：执行 nonNegative 操作；该方法是 {@code McpSecurityGate.IdentityContext} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the non negative operation; this method is the invocation entry point on {@code McpSecurityGate.IdentityContext} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityGate.IdentityContext.nonNegative(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param value 参数 值；parameter value。
         * @param field 参数 field；parameter field。
         */
        private static void nonNegative(long value, String field) {
            if (value < 0L) {
                throw new IllegalArgumentException(
                        field + " must not be negative"
                );
            }
        }

        /**
         * 中文说明：执行 required 操作；该方法是 {@code McpSecurityGate.IdentityContext} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the required operation; this method is the invocation entry point on {@code McpSecurityGate.IdentityContext} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityGate.IdentityContext.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
