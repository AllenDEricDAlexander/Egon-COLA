package top.egon.cola.component.gateway.contract.mcp.rule;

import java.util.Comparator;
import java.util.List;

/**
 * 一份可发布的 MCP 规则内容。
 *
 * <p>集中承载本地 Server、自动或手工 Tool、Resource、Prompt、Task、App 以及远程挂载定义，
 * 构造时会按稳定键排序，发布前通过 {@link McpRuleValidator} 校验跨对象引用。
 */
public record McpRuleContent(
        List<McpRuntimeServer> servers,
        List<McpRuntimeTool> tools,
        List<McpRuntimeResource> resources,
        List<McpRuntimeResourceTemplate> resourceTemplates,
        List<McpRuntimePrompt> prompts,
        List<McpRuntimeTaskPolicy> taskPolicies,
        List<McpRuntimeApp> apps,
        List<McpRuntimeRemoteProvider> remoteProviders,
        List<McpRuntimeRemoteMount> remoteMounts
) {

    public McpRuleContent {
        servers = McpContractSupport.sorted(
                servers,
                Comparator.comparing(McpRuntimeServer::serverCode)
                        .thenComparing(McpRuntimeServer::serverId)
        );
        tools = McpContractSupport.sorted(
                tools,
                Comparator.comparing(McpRuntimeTool::serverCode)
                        .thenComparing(McpRuntimeTool::name)
                        .thenComparing(McpRuntimeTool::toolId)
        );
        resources = McpContractSupport.sorted(
                resources,
                Comparator.comparing(McpRuntimeResource::serverCode)
                        .thenComparing(McpRuntimeResource::name)
                        .thenComparing(McpRuntimeResource::resourceId)
        );
        resourceTemplates = McpContractSupport.sorted(
                resourceTemplates,
                Comparator.comparing(McpRuntimeResourceTemplate::serverCode)
                        .thenComparing(McpRuntimeResourceTemplate::name)
                        .thenComparing(McpRuntimeResourceTemplate::templateId)
        );
        prompts = McpContractSupport.sorted(
                prompts,
                Comparator.comparing(McpRuntimePrompt::serverCode)
                        .thenComparing(McpRuntimePrompt::name)
                        .thenComparing(McpRuntimePrompt::promptId)
        );
        taskPolicies = McpContractSupport.sorted(
                taskPolicies,
                Comparator.comparing(McpRuntimeTaskPolicy::serverCode)
                        .thenComparing(McpRuntimeTaskPolicy::toolName)
                        .thenComparing(McpRuntimeTaskPolicy::taskPolicyId)
        );
        apps = McpContractSupport.sorted(
                apps,
                Comparator.comparing(McpRuntimeApp::serverCode)
                        .thenComparing(McpRuntimeApp::name)
                        .thenComparing(McpRuntimeApp::version)
        );
        remoteProviders = McpContractSupport.sorted(
                remoteProviders,
                Comparator.comparing(McpRuntimeRemoteProvider::providerCode)
                        .thenComparing(McpRuntimeRemoteProvider::providerId)
        );
        remoteMounts = McpContractSupport.sorted(
                remoteMounts,
                Comparator.comparing(McpRuntimeRemoteMount::serverCode)
                        .thenComparing(McpRuntimeRemoteMount::namespace)
                        .thenComparing(McpRuntimeRemoteMount::mountId)
        );
    }

    public static McpRuleContent empty() {
        return new McpRuleContent(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    public void validate() {
        McpRuleValidator.validateServers(servers);
        McpRuleValidator.validateCapabilityNames(
                tools,
                resources,
                resourceTemplates,
                prompts,
                apps
        );
        McpRuleValidator.validateRemoteNamespaces(
                remoteProviders,
                remoteMounts
        );
    }
}
