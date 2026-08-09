package top.egon.cola.component.gateway.mcp.app;

import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeApp;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore;
import top.egon.cola.component.gateway.mcp.resource.McpResourceDriver;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Resolves active MCP Apps and revalidates stored bytes on every read.
 */
public final class McpAppRuntime {

    private final Supplier<CompiledMcpRules> rules;

    private final McpAppArtifactStore.Reader artifacts;

    private final McpAppSecurityValidator validator;

    public McpAppRuntime(
            Supplier<CompiledMcpRules> rules,
            McpAppArtifactStore.Reader artifacts,
            McpAppSecurityValidator validator) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.artifacts = Objects.requireNonNull(artifacts, "artifacts");
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    public AppContent read(String serverCode, String resourceUri) {
        McpRuntimeApp app = active().appsByQualifiedName().values().stream()
                .filter(McpRuntimeApp::enabled)
                .filter(candidate -> candidate.serverCode().equals(serverCode))
                .filter(candidate -> candidate.resourceUri().equals(resourceUri))
                .findFirst()
                .orElseThrow(() -> McpResourceDriver.rejected(
                        "MCP App resource was not found"
                ));
        McpAppArtifactStore.ArtifactContent artifact = artifacts.read(
                new McpAppArtifactStore.ReadRequest(
                        app.artifactReference(),
                        app.artifactSha256(),
                        app.artifactSizeBytes()
                )
        );
        validator.validate(app, artifact);
        return new AppContent(app, artifact.content(), metadata(app));
    }

    private Map<String, Object> metadata(McpRuntimeApp app) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("sandbox", "allow-scripts");
        value.put("content-security-policy", app.contentSecurityPolicy());
        value.put("cache-control", "no-store");
        value.put("x-content-type-options", "nosniff");
        value.put("cookies", "disabled");
        value.put("permissions", app.permissions());
        value.put("allowed-tools", app.allowedTools());
        return Map.copyOf(value);
    }

    private CompiledMcpRules active() {
        CompiledMcpRules value = rules.get();
        return value == null ? CompiledMcpRules.empty() : value;
    }

    public record AppContent(
            McpRuntimeApp app,
            byte[] content,
            Map<String, Object> responseMetadata
    ) {

        public AppContent {
            app = Objects.requireNonNull(app, "app");
            content = Objects.requireNonNull(content, "content").clone();
            responseMetadata = Map.copyOf(responseMetadata);
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }
}
