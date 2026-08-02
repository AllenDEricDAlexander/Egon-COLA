package top.egon.cola.component.gateway.admin.mcp.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpArtifactMetadataStore;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeApp;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteMount;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteProvider;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResource;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResourceTemplate;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTaskPolicy;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class McpValidationService {

    private static final Pattern PERMISSION = Pattern.compile(
            "^[a-z][a-z0-9._-]*(?::[A-Za-z0-9._*-]+)+$"
    );

    private static final Set<String> RISK_LEVELS = Set.of(
            "LOW",
            "MEDIUM",
            "HIGH",
            "CRITICAL"
    );

    private final GatewayCatalogStore catalog;

    private final JdbcMcpArtifactMetadataStore artifacts;

    private final ObjectMapper objectMapper;

    public McpValidationService(
            GatewayCatalogStore catalog,
            JdbcMcpArtifactMetadataStore artifacts,
            ObjectMapper objectMapper) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.artifacts = Objects.requireNonNull(artifacts, "artifacts");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        ).copy();
    }

    public ValidationReport validate(McpRuleContent content) {
        List<ValidationFinding> findings = new ArrayList<>();
        try {
            requireValid(content);
        } catch (McpValidationException failure) {
            findings.add(new ValidationFinding(
                    failure.path(),
                    failure.code(),
                    failure.getMessage()
            ));
        } catch (IllegalArgumentException failure) {
            findings.add(new ValidationFinding(
                    "$",
                    "GATEWAY_MCP_RULE_INVALID",
                    failure.getMessage()
            ));
        }
        return new ValidationReport(findings.isEmpty(), List.copyOf(findings));
    }

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
        if ("LOCAL_TEMPLATE".equals(prompt.sourceType())
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

    private void validateRemote(
            List<McpRuntimeRemoteProvider> providers,
            List<McpRuntimeRemoteMount> mounts) {
        Map<String, McpRuntimeRemoteProvider> byCode = new HashMap<>();
        providers.forEach(provider -> byCode.put(
                provider.providerCode(),
                provider
        ));
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

    private void validateBinding(
            String sourceType,
            String operationId,
            String remoteMountId,
            String path) {
        if ("LOCAL_TEMPLATE".equals(sourceType)) {
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

    private void invalid(String code, String path, String message) {
        throw error(code, path, message);
    }

    private McpValidationException error(
            String code,
            String path,
            String message) {
        return new McpValidationException(code, path, message);
    }

    public record ValidationReport(
            boolean valid,
            List<ValidationFinding> findings
    ) {
    }

    public record ValidationFinding(
            String path,
            String code,
            String message
    ) {
    }
}
