package top.egon.cola.component.gateway.mcp.rule;

import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeApp;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteMount;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteProvider;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResource;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResourceTemplate;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTaskPolicy;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record CompiledMcpRules(
        McpRuleContent content,
        Map<String, McpRuntimeServer> serversByCode,
        Map<String, McpRuntimeTool> toolsByQualifiedName,
        Map<String, McpRuntimeResource> resourcesByQualifiedName,
        Map<String, McpRuntimeResourceTemplate> templatesByQualifiedName,
        Map<String, McpRuntimePrompt> promptsByQualifiedName,
        Map<String, McpRuntimeTaskPolicy> taskPoliciesByQualifiedTool,
        Map<String, McpRuntimeApp> appsByQualifiedName,
        Map<String, McpRuntimeRemoteProvider> remoteProvidersByCode,
        Map<String, McpRuntimeRemoteMount> remoteMountsById
) {

    public CompiledMcpRules {
        content = Objects.requireNonNull(content, "content");
        serversByCode = immutable(serversByCode, "serversByCode");
        toolsByQualifiedName = immutable(
                toolsByQualifiedName,
                "toolsByQualifiedName"
        );
        resourcesByQualifiedName = immutable(
                resourcesByQualifiedName,
                "resourcesByQualifiedName"
        );
        templatesByQualifiedName = immutable(
                templatesByQualifiedName,
                "templatesByQualifiedName"
        );
        promptsByQualifiedName = immutable(
                promptsByQualifiedName,
                "promptsByQualifiedName"
        );
        taskPoliciesByQualifiedTool = immutable(
                taskPoliciesByQualifiedTool,
                "taskPoliciesByQualifiedTool"
        );
        appsByQualifiedName = immutable(
                appsByQualifiedName,
                "appsByQualifiedName"
        );
        remoteProvidersByCode = immutable(
                remoteProvidersByCode,
                "remoteProvidersByCode"
        );
        remoteMountsById = immutable(
                remoteMountsById,
                "remoteMountsById"
        );
    }

    public static CompiledMcpRules empty() {
        return new McpRuleCompiler().compile(McpRuleContent.empty());
    }

    public Optional<McpRuntimeServer> server(String serverCode) {
        return Optional.ofNullable(serversByCode.get(serverCode));
    }

    public Optional<McpRuntimeTool> tool(String serverCode, String name) {
        return Optional.ofNullable(toolsByQualifiedName.get(
                qualified(serverCode, name)
        ));
    }

    public boolean remoteAvailable(String mountId, String primitiveType) {
        if (mountId == null) {
            return true;
        }
        McpRuntimeRemoteMount mount = remoteMountsById.get(mountId);
        if (mount == null || !mount.enabled()
                || !mount.primitiveTypes().contains(
                Objects.requireNonNull(primitiveType, "primitiveType")
                        .toUpperCase(Locale.ROOT)
        )) {
            return false;
        }
        McpRuntimeRemoteProvider provider = remoteProvidersByCode.get(
                mount.providerCode()
        );
        return provider != null
                && provider.enabled()
                && provider.capabilityFingerprint().equals(
                mount.capabilityFingerprint()
        );
    }

    public static String qualified(String serverCode, String name) {
        return Objects.requireNonNull(serverCode, "serverCode")
                + "\u0000"
                + Objects.requireNonNull(name, "name");
    }

    private static <K, V> Map<K, V> immutable(
            Map<K, V> source,
            String field) {
        return Map.copyOf(Objects.requireNonNull(source, field));
    }
}
