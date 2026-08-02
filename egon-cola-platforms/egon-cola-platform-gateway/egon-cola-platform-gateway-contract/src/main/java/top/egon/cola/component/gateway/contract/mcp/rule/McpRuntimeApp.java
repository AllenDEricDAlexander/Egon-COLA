package top.egon.cola.component.gateway.contract.mcp.rule;

import java.util.Set;

public record McpRuntimeApp(
        String appId,
        String serverCode,
        String name,
        String appCode,
        String version,
        String resourceUri,
        String artifactId,
        String artifactReference,
        String artifactSha256,
        long artifactSizeBytes,
        String mimeType,
        String contentSecurityPolicy,
        Set<String> permissions,
        Set<String> allowedOrigins,
        Set<String> allowedTools,
        boolean enabled
) {

    public McpRuntimeApp {
        appId = McpContractSupport.required(appId, "appId");
        serverCode = McpContractSupport.required(serverCode, "serverCode");
        name = McpContractSupport.required(name, "name");
        appCode = McpContractSupport.required(appCode, "appCode");
        version = McpContractSupport.required(version, "version");
        resourceUri = McpContractSupport.required(resourceUri, "resourceUri");
        artifactId = McpContractSupport.required(artifactId, "artifactId");
        artifactReference = McpContractSupport.required(
                artifactReference,
                "artifactReference"
        );
        artifactSha256 = McpContractSupport.required(
                artifactSha256,
                "artifactSha256"
        );
        if (artifactSizeBytes < 1L
                || artifactSizeBytes > 16L * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "artifactSizeBytes is outside the supported range"
            );
        }
        mimeType = McpContractSupport.required(mimeType, "mimeType");
        contentSecurityPolicy = McpContractSupport.required(
                contentSecurityPolicy,
                "contentSecurityPolicy"
        );
        permissions = McpContractSupport.sortedStrings(permissions);
        allowedOrigins = McpContractSupport.sortedStrings(allowedOrigins);
        allowedTools = McpContractSupport.sortedStrings(allowedTools);
    }
}
