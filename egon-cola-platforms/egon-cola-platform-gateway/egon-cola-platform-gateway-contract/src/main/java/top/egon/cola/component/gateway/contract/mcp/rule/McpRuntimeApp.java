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
        String artifactSha256,
        String mimeType,
        String contentSecurityPolicy,
        Set<String> permissions,
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
        artifactSha256 = McpContractSupport.required(
                artifactSha256,
                "artifactSha256"
        );
        mimeType = McpContractSupport.required(mimeType, "mimeType");
        contentSecurityPolicy = McpContractSupport.required(
                contentSecurityPolicy,
                "contentSecurityPolicy"
        );
        permissions = McpContractSupport.sortedStrings(permissions);
        allowedTools = McpContractSupport.sortedStrings(allowedTools);
    }
}
