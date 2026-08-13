package top.egon.cola.component.gateway.admin.mcp.service;


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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import top.egon.cola.component.gateway.admin.catalog.repository.GatewayCatalogRepository;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.JdbcMcpArtifactMetadataRepository;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeApp;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteMount;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteProvider;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResource;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResourceTemplate;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTaskPolicy;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.gateway.mcp.remote.McpRemoteEndpointValidator;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;


import top.egon.cola.component.gateway.admin.mcp.domain.vo.McpValidationReportVO;
import top.egon.cola.component.gateway.admin.mcp.domain.vo.McpValidationFindingVO;
/**
 * 中文说明：{@code McpValidationService} 是服务组件，位于当前 Gateway 模块的相关包中，负责MCPValidation服务相关的职责与边界。
 * English summary: {@code McpValidationService} is a mcp validation service service in the current Gateway module; it owns the mcp validation service-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Service
public class McpValidationService {

    /**
     * 中文说明：表示 PERMISSION 这一固定值；它属于 {@code McpValidationService} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value permission; it is a state, type, or protocol value of {@code McpValidationService} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpValidationService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpValidationService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Pattern PERMISSION = Pattern.compile(
            "^[a-z][a-z0-9._-]*(?::[A-Za-z0-9._*-]+)+$"
    );

    /**
     * 中文说明：表示 RISKLEVELS 这一固定值；它属于 {@code McpValidationService} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value risk levels; it is a state, type, or protocol value of {@code McpValidationService} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpValidationService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpValidationService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Set<String> RISK_LEVELS = Set.of(
            "LOW",
            "MEDIUM",
            "HIGH",
            "CRITICAL"
    );

    /**
     * 中文说明：保存 目录 对应的状态、依赖或配置值；字段类型为 {@code GatewayCatalogRepository}，由 {@code McpValidationService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by catalog; its type is {@code GatewayCatalogRepository}, and {@code McpValidationService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpValidationService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpValidationService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCatalogRepository catalog;

    /**
     * 中文说明：保存 artifacts 对应的状态、依赖或配置值；字段类型为 {@code JdbcMcpArtifactMetadataRepository}，由 {@code McpValidationService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by artifacts; its type is {@code JdbcMcpArtifactMetadataRepository}, and {@code McpValidationService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpValidationService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpValidationService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcMcpArtifactMetadataRepository artifacts;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code McpValidationService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code McpValidationService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpValidationService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpValidationService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：创建 {@code McpValidationService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpValidationService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param catalog 参数 目录；parameter catalog。
     * @param artifacts 参数 artifacts；parameter artifacts。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public McpValidationService(
            GatewayCatalogRepository catalog,
            JdbcMcpArtifactMetadataRepository artifacts,
            ObjectMapper objectMapper) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.artifacts = Objects.requireNonNull(artifacts, "artifacts");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        ).copy();
    }

    /**
     * 中文说明：执行 validate 操作；该方法是 {@code McpValidationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate operation; this method is the invocation entry point on {@code McpValidationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationService.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @return 返回 validate 的处理结果；returns the result of the operation.
     */
    public McpValidationReportVO validate(McpRuleContent content) {
        List<McpValidationFindingVO> findings = new ArrayList<>();
        try {
            requireValid(content);
        } catch (McpValidationException failure) {
            findings.add(new McpValidationFindingVO(
                    failure.path(),
                    failure.code(),
                    failure.getMessage()
            ));
        } catch (IllegalArgumentException failure) {
            findings.add(new McpValidationFindingVO(
                    "$",
                    "GATEWAY_MCP_RULE_INVALID",
                    failure.getMessage()
            ));
        }
        return new McpValidationReportVO(findings.isEmpty(), List.copyOf(findings));
    }

    /**
     * 中文说明：执行 requireValid 操作；该方法是 {@code McpValidationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require valid operation; this method is the invocation entry point on {@code McpValidationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationService.requireValid(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     */
    public void requireValid(McpRuleContent content) {
        Objects.requireNonNull(content, "content").validate();
        Set<String> serverCodes = new HashSet<>();
        content.servers().forEach(server -> serverCodes.add(
                server.serverCode()
        ));
        Map<String, McpRuntimeTool> tools = new HashMap<>();
        content.tools().forEach(tool -> {
            requireServer(serverCodes, tool.serverCode(), "tools");
            validateTool(tool);
            tools.put(tool.serverCode() + "\u0000" + tool.name(), tool);
        });
        content.resources().forEach(resource -> {
            requireServer(serverCodes, resource.serverCode(), "resources");
            validateResource(resource);
        });
        content.resourceTemplates().forEach(template -> {
            requireServer(
                    serverCodes,
                    template.serverCode(),
                    "resourceTemplates"
            );
            validateResourceTemplate(template);
        });
        content.prompts().forEach(prompt -> {
            requireServer(serverCodes, prompt.serverCode(), "prompts");
            validatePrompt(prompt);
        });
        content.taskPolicies().forEach(policy -> validateTaskPolicy(
                policy,
                tools
        ));
        validateApps(content.apps(), tools, serverCodes);
        validateRemote(content.remoteProviders(), content.remoteMounts());
    }

    /**
     * 中文说明：执行 validate工具 操作；该方法是 {@code McpValidationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate tool operation; this method is the invocation entry point on {@code McpValidationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationService.validateTool(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param tool 参数 工具；parameter tool。
     */
    private void validateTool(McpRuntimeTool tool) {
        if (!RISK_LEVELS.contains(tool.riskLevel())) {
            invalid(
                    "GATEWAY_MCP_RISK_INVALID",
                    "tools." + tool.name() + ".riskLevel",
                    "unsupported MCP Tool risk level"
            );
        }
        validateBinding(
                tool.sourceType(),
                tool.operationId(),
                tool.remoteMountId(),
                "tools." + tool.name()
        );
        if ("LOCAL_OPERATION".equals(tool.sourceType())) {
            requireOperation(tool.operationId(), "tools." + tool.name());
        }
        validateSchema(tool.inputSchema(), "tools." + tool.name()
                + ".inputSchema");
        validateSchema(tool.outputSchema(), "tools." + tool.name()
                + ".outputSchema");
        validatePermissions(
                tool.requiredPermissions(),
                "tools." + tool.name() + ".requiredPermissions"
        );
    }

    /**
     * 中文说明：执行 validate资源 操作；该方法是 {@code McpValidationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate resource operation; this method is the invocation entry point on {@code McpValidationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationService.validateResource(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param resource 参数 资源；parameter resource。
     */
    private void validateResource(McpRuntimeResource resource) {
        validateUri(resource.uri(), "resources." + resource.name() + ".uri");
        validateDriverBinding(
                resource.driverType(),
                resource.operationId(),
                resource.remoteMountId(),
                "resources." + resource.name()
        );
        validatePermissions(
                resource.requiredPermissions(),
                "resources." + resource.name() + ".requiredPermissions"
        );
    }

    /**
     * 中文说明：执行 validate资源模板 操作；该方法是 {@code McpValidationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate resource template operation; this method is the invocation entry point on {@code McpValidationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationService.validateResourceTemplate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param template 参数 模板；parameter template。
     */
    private void validateResourceTemplate(
            McpRuntimeResourceTemplate template) {
        String path = "resourceTemplates." + template.name();
        validateTemplate(template.uriTemplate(), path + ".uriTemplate");
        validateDriverBinding(
                template.driverType(),
                template.operationId(),
                template.remoteMountId(),
                path
        );
        validatePermissions(
                template.requiredPermissions(),
                path + ".requiredPermissions"
        );
    }

    /**
     * 中文说明：执行 validate提示词 操作；该方法是 {@code McpValidationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate prompt operation; this method is the invocation entry point on {@code McpValidationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationService.validatePrompt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param prompt 参数 提示词；parameter prompt。
     */
    private void validatePrompt(McpRuntimePrompt prompt) {
        String path = "prompts." + prompt.name();
        validateBinding(
                "LOCAL_OPERATION".equals(prompt.sourceType())
                        ? "LOCAL_OPERATION"
                        : prompt.sourceType(),
                prompt.operationId(),
                prompt.remoteMountId(),
                path
        );
        if ("LOCAL_OPERATION".equals(prompt.sourceType())) {
            requireOperation(prompt.operationId(), path);
        }
        if (isLocalPromptTemplate(prompt.sourceType())
                && (prompt.template() == null || prompt.template().isBlank())) {
            invalid(
                    "GATEWAY_MCP_PROMPT_TEMPLATE_REQUIRED",
                    path + ".template",
                    "local prompt template is required"
            );
        }
        validatePermissions(prompt.requiredPermissions(), path
                + ".requiredPermissions");
    }

    /**
     * 中文说明：执行 validate任务策略 操作；该方法是 {@code McpValidationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate task policy operation; this method is the invocation entry point on {@code McpValidationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationService.validateTaskPolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @param tools 参数 tools；parameter tools。
     */
    private void validateTaskPolicy(
            McpRuntimeTaskPolicy policy,
            Map<String, McpRuntimeTool> tools) {
        if (!tools.containsKey(policy.serverCode() + "\u0000"
                + policy.toolName())) {
            invalid(
                    "GATEWAY_MCP_TASK_TOOL_NOT_FOUND",
                    "taskPolicies." + policy.taskPolicyId() + ".toolName",
                    "task policy references an unknown Tool"
            );
        }
        if (policy.executionTimeoutSeconds() == 0
                || policy.resultTtlSeconds() == 0) {
            invalid(
                    "GATEWAY_MCP_TASK_POLICY_INVALID",
                    "taskPolicies." + policy.taskPolicyId(),
                    "task timeout and result TTL must be positive"
            );
        }
    }

    /**
     * 中文说明：执行 validateApps 操作；该方法是 {@code McpValidationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate apps operation; this method is the invocation entry point on {@code McpValidationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationService.validateApps(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param apps 参数 apps；parameter apps。
     * @param tools 参数 tools；parameter tools。
     * @param serverCodes 参数 服务器Codes；parameter server codes。
     */
    private void validateApps(
            List<McpRuntimeApp> apps,
            Map<String, McpRuntimeTool> tools,
            Set<String> serverCodes) {
        for (McpRuntimeApp app : apps) {
            requireServer(serverCodes, app.serverCode(), "apps");
            var artifact = artifacts.find(app.artifactId())
                    .orElseThrow(() -> error(
                            "GATEWAY_MCP_ARTIFACT_NOT_FOUND",
                            "apps." + app.name() + ".artifactId",
                            "MCP App artifact was not found"
                    ));
            if (!artifact.sha256().equals(app.artifactSha256())
                    || !artifact.resourceUri().equals(app.resourceUri())) {
                invalid(
                        "GATEWAY_MCP_ARTIFACT_DIGEST_MISMATCH",
                        "apps." + app.name() + ".artifactSha256",
                        "MCP App artifact metadata is immutable"
                );
            }
            for (String tool : app.allowedTools()) {
                if (!tools.containsKey(app.serverCode() + "\u0000" + tool)) {
                    invalid(
                            "GATEWAY_MCP_APP_TOOL_NOT_FOUND",
                            "apps." + app.name() + ".allowedTools",
                            "MCP App references an unknown Tool"
                    );
                }
            }
            validatePermissions(
                    app.permissions(),
                    "apps." + app.name() + ".permissions"
            );
        }
    }

    /**
     * 中文说明：执行 validate远程 操作；该方法是 {@code McpValidationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate remote operation; this method is the invocation entry point on {@code McpValidationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationService.validateRemote(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param providers 参数 providers；parameter providers。
     * @param mounts 参数 mounts；parameter mounts。
     */
    private void validateRemote(
            List<McpRuntimeRemoteProvider> providers,
            List<McpRuntimeRemoteMount> mounts) {
        Map<String, McpRuntimeRemoteProvider> byCode = new HashMap<>();
        for (McpRuntimeRemoteProvider provider : providers) {
            try {
                McpRemoteEndpointValidator.requireSafe(
                        provider.endpointReference()
                );
            } catch (IllegalArgumentException failure) {
                invalid(
                        "GATEWAY_MCP_REMOTE_ENDPOINT_UNSAFE",
                        "remoteProviders." + provider.providerCode()
                                + ".endpointReference",
                        failure.getMessage()
                );
            }
            byCode.put(provider.providerCode(), provider);
        }
        for (McpRuntimeRemoteMount mount : mounts) {
            McpRuntimeRemoteProvider provider = byCode.get(
                    mount.providerCode()
            );
            if (provider == null) {
                invalid(
                        "GATEWAY_MCP_REMOTE_PROVIDER_NOT_FOUND",
                        "remoteMounts." + mount.mountId() + ".providerCode",
                        "remote MCP Provider was not found"
                );
            }
            if (!provider.capabilityFingerprint().equals(
                    mount.capabilityFingerprint()
            )) {
                invalid(
                        "GATEWAY_MCP_REMOTE_FINGERPRINT_STALE",
                        "remoteMounts." + mount.mountId()
                                + ".capabilityFingerprint",
                        "remote capability fingerprint must be rediscovered"
                );
            }
            validatePermissions(
                    mount.requiredPermissions(),
                    "remoteMounts." + mount.mountId()
                            + ".requiredPermissions"
            );
        }
    }

    /**
     * 中文说明：执行 validateBinding 操作；该方法是 {@code McpValidationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate binding operation; this method is the invocation entry point on {@code McpValidationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationService.validateBinding(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param sourceType 参数 sourceType；parameter source type。
     * @param operationId 参数 操作Id；parameter operation id。
     * @param remoteMountId 参数 远程MountId；parameter remote mount id。
     * @param path 参数 path；parameter path。
     */
    private void validateBinding(
            String sourceType,
            String operationId,
            String remoteMountId,
            String path) {
        if (isLocalPromptTemplate(sourceType)) {
            if (operationId != null || remoteMountId != null) {
                invalid(
                        "GATEWAY_MCP_BINDING_INVALID",
                        path,
                        "local template cannot bind an Operation or mount"
                );
            }
            return;
        }
        boolean local = "LOCAL_OPERATION".equals(sourceType)
                && operationId != null && remoteMountId == null;
        boolean remote = "REMOTE_MCP".equals(sourceType)
                && operationId == null && remoteMountId != null;
        if (!local && !remote) {
            invalid(
                    "GATEWAY_MCP_BINDING_INVALID",
                    path,
                    "MCP source binding is inconsistent"
            );
        }
    }

    /**
     * 中文说明：执行 isLocal提示词模板 操作；该方法是 {@code McpValidationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the is local prompt template operation; this method is the invocation entry point on {@code McpValidationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationService.isLocalPromptTemplate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param sourceType 参数 sourceType；parameter source type。
     * @return 返回 isLocal提示词模板 的处理结果；returns the result of the operation.
     */
    private boolean isLocalPromptTemplate(String sourceType) {
        return "LOCAL_TEMPLATE".equals(sourceType)
                || "STATIC_TEMPLATE".equals(sourceType)
                || "STRICT_TEMPLATE".equals(sourceType);
    }

    /**
     * 中文说明：执行 validate驱动器Binding 操作；该方法是 {@code McpValidationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate driver binding operation; this method is the invocation entry point on {@code McpValidationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationService.validateDriverBinding(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param driverType 参数 驱动器Type；parameter driver type。
     * @param operationId 参数 操作Id；parameter operation id。
     * @param remoteMountId 参数 远程MountId；parameter remote mount id。
     * @param path 参数 path；parameter path。
     */
    private void validateDriverBinding(
            String driverType,
            String operationId,
            String remoteMountId,
            String path) {
        if ("LOCAL_OPERATION".equals(driverType)) {
            validateBinding(driverType, operationId, remoteMountId, path);
            requireOperation(operationId, path);
        } else if ("REMOTE_MCP".equals(driverType)) {
            validateBinding(driverType, operationId, remoteMountId, path);
        } else if (operationId != null || remoteMountId != null) {
            invalid(
                    "GATEWAY_MCP_BINDING_INVALID",
                    path,
                    "local resource driver cannot bind an Operation or mount"
            );
        }
    }

    /**
     * 中文说明：执行 require操作 操作；该方法是 {@code McpValidationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require operation operation; this method is the invocation entry point on {@code McpValidationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationService.requireOperation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @param path 参数 path；parameter path。
     */
    private void requireOperation(String operationId, String path) {
        var operation = catalog.findOperation(operationId)
                .orElseThrow(() -> error(
                        "GATEWAY_MCP_OPERATION_NOT_FOUND",
                        path + ".operationId",
                        "gateway Operation " + operationId
                                + " was not found"
                ));
        if (catalog.loadDefinitions(operation.id()).isEmpty()) {
            invalid(
                    "GATEWAY_MCP_OPERATION_DEFINITION_NOT_FOUND",
                    path + ".operationId",
                    "gateway Operation has no active definition"
            );
        }
    }

    /**
     * 中文说明：执行 validate模式 操作；该方法是 {@code McpValidationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate schema operation; this method is the invocation entry point on {@code McpValidationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationService.validateSchema(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param schema 参数 模式；parameter schema。
     * @param path 参数 path；parameter path。
     */
    private void validateSchema(String schema, String path) {
        if (schema == null) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(schema);
            if (!root.isObject()) {
                invalid(
                        "GATEWAY_MCP_SCHEMA_INVALID",
                        path,
                        "JSON Schema root must be an object"
                );
            }
            root.findValues("$ref").forEach(reference -> {
                String value = reference.asText();
                if (!value.startsWith("#")) {
                    invalid(
                            "GATEWAY_MCP_SCHEMA_EXTERNAL_REF_FORBIDDEN",
                            path,
                            "external JSON Schema references are forbidden"
                    );
                }
            });
        } catch (JsonProcessingException failure) {
            invalid(
                    "GATEWAY_MCP_SCHEMA_INVALID",
                    path,
                    "JSON Schema cannot be parsed"
            );
        }
    }

    /**
     * 中文说明：执行 validateUri 操作；该方法是 {@code McpValidationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate uri operation; this method is the invocation entry point on {@code McpValidationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationService.validateUri(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param path 参数 path；parameter path。
     */
    private void validateUri(String value, String path) {
        try {
            URI uri = URI.create(value);
            if (uri.getScheme() == null || value.contains("..")) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException failure) {
            invalid(
                    "GATEWAY_MCP_RESOURCE_URI_INVALID",
                    path,
                    "resource URI must be absolute and path-safe"
            );
        }
    }

    /**
     * 中文说明：执行 validate模板 操作；该方法是 {@code McpValidationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate template operation; this method is the invocation entry point on {@code McpValidationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationService.validateTemplate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param path 参数 path；parameter path。
     */
    private void validateTemplate(String value, String path) {
        if (value.length() > 2048 || value.contains("..")) {
            invalid(
                    "GATEWAY_MCP_URI_TEMPLATE_INVALID",
                    path,
                    "resource URI template is too long or path-unsafe"
            );
        }
        String sample = value.replaceAll("\\{[A-Za-z][A-Za-z0-9_]*}", "x");
        if (sample.contains("{") || sample.contains("}")) {
            invalid(
                    "GATEWAY_MCP_URI_TEMPLATE_INVALID",
                    path,
                    "resource URI template variables are invalid"
            );
        }
        validateUri(sample, path);
    }

    /**
     * 中文说明：执行 validatePermissions 操作；该方法是 {@code McpValidationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate permissions operation; this method is the invocation entry point on {@code McpValidationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationService.validatePermissions(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @param path 参数 path；parameter path。
     */
    private void validatePermissions(Set<String> values, String path) {
        values.forEach(value -> {
            if (!PERMISSION.matcher(value).matches()) {
                invalid(
                        "GATEWAY_MCP_PERMISSION_INVALID",
                        path,
                        "permission name is invalid: " + value
                );
            }
        });
    }

    /**
     * 中文说明：执行 require服务器 操作；该方法是 {@code McpValidationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require server operation; this method is the invocation entry point on {@code McpValidationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationService.requireServer(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCodes 参数 服务器Codes；parameter server codes。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param path 参数 path；parameter path。
     */
    private void requireServer(
            Set<String> serverCodes,
            String serverCode,
            String path) {
        if (!serverCodes.contains(serverCode)) {
            invalid(
                    "GATEWAY_MCP_SERVER_NOT_FOUND",
                    path,
                    "MCP capability references an unknown Server"
            );
        }
    }

    /**
     * 中文说明：执行 invalid 操作；该方法是 {@code McpValidationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invalid operation; this method is the invocation entry point on {@code McpValidationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationService.invalid(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param code 参数 code；parameter code。
     * @param path 参数 path；parameter path。
     * @param message 参数 消息；parameter message。
     */
    private void invalid(String code, String path, String message) {
        throw error(code, path, message);
    }

    /**
     * 中文说明：执行 error 操作；该方法是 {@code McpValidationService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the error operation; this method is the invocation entry point on {@code McpValidationService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpValidationService.error(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param code 参数 code；parameter code。
     * @param path 参数 path；parameter path。
     * @param message 参数 消息；parameter message。
     * @return 返回 error 的处理结果；returns the result of the operation.
     */
    private McpValidationException error(
            String code,
            String path,
            String message) {
        return new McpValidationException(code, path, message);
    }




}
