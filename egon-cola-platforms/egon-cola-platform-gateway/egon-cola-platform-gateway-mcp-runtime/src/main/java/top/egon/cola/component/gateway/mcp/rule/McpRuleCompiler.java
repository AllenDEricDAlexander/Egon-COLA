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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class McpRuleCompiler {

    public CompiledMcpRules compile(McpRuleContent content) {
        return compile(content, null);
    }

    public CompiledMcpRules compile(
            McpRuleContent content,
            Set<String> operationIds) {
        Objects.requireNonNull(content, "content");
        try {
            content.validate();
            Map<String, McpRuntimeServer> servers = index(
                    content.servers(),
                    McpRuntimeServer::serverCode,
                    "server code"
            );
            Map<String, McpRuntimeRemoteProvider> providers = index(
                    content.remoteProviders(),
                    McpRuntimeRemoteProvider::providerCode,
                    "remote provider code"
            );
            Map<String, McpRuntimeRemoteMount> mounts = index(
                    content.remoteMounts(),
                    McpRuntimeRemoteMount::mountId,
                    "remote mount id"
            );
            validateMounts(content.remoteMounts(), servers, providers);
            Map<String, McpRuntimeTool> tools = compileTools(
                    content,
                    servers,
                    operationIds,
                    mounts
            );
            Map<String, McpRuntimeResource> resources = compileResources(
                    content,
                    servers,
                    operationIds,
                    mounts
            );
            Map<String, McpRuntimeResourceTemplate> templates =
                    compileTemplates(content, servers, operationIds, mounts);
            Map<String, McpRuntimePrompt> prompts = compilePrompts(
                    content,
                    servers,
                    operationIds,
                    mounts
            );
            Map<String, McpRuntimeTaskPolicy> taskPolicies =
                    compileTaskPolicies(content, servers, tools);
            Map<String, McpRuntimeApp> apps = compileApps(
                    content,
                    servers,
                    tools
            );
            return new CompiledMcpRules(
                    content,
                    servers,
                    tools,
                    resources,
                    templates,
                    prompts,
                    taskPolicies,
                    apps,
                    providers,
                    mounts
            );
        } catch (IllegalArgumentException failure) {
            if (failure.getMessage() != null
                    && failure.getMessage().startsWith(
                    "MCP_RULE_COMPILE_FAILED:"
            )) {
                throw failure;
            }
            throw invalid(failure.getMessage());
        }
    }

    private Map<String, McpRuntimeTool> compileTools(
            McpRuleContent content,
            Map<String, McpRuntimeServer> servers,
            Set<String> operationIds,
            Map<String, McpRuntimeRemoteMount> mounts) {
        Map<String, McpRuntimeTool> tools = qualifiedIndex(
                content.tools(),
                McpRuntimeTool::serverCode,
                McpRuntimeTool::name,
                "tool"
        );
        content.tools().forEach(tool -> {
            requireServer(servers, tool.serverCode(), "tool");
            validateBinding(
                    "tool",
                    tool.operationId(),
                    tool.remoteMountId(),
                    operationIds,
                    mounts
            );
        });
        return tools;
    }

    private Map<String, McpRuntimeResource> compileResources(
            McpRuleContent content,
            Map<String, McpRuntimeServer> servers,
            Set<String> operationIds,
            Map<String, McpRuntimeRemoteMount> mounts) {
        Map<String, McpRuntimeResource> resources = qualifiedIndex(
                content.resources(),
                McpRuntimeResource::serverCode,
                McpRuntimeResource::name,
                "resource"
        );
        content.resources().forEach(resource -> {
            requireServer(servers, resource.serverCode(), "resource");
            validateOptionalBinding(
                    "resource",
                    resource.operationId(),
                    resource.remoteMountId(),
                    operationIds,
                    mounts
            );
        });
        return resources;
    }

    private Map<String, McpRuntimeResourceTemplate> compileTemplates(
            McpRuleContent content,
            Map<String, McpRuntimeServer> servers,
            Set<String> operationIds,
            Map<String, McpRuntimeRemoteMount> mounts) {
        Map<String, McpRuntimeResourceTemplate> templates = qualifiedIndex(
                content.resourceTemplates(),
                McpRuntimeResourceTemplate::serverCode,
                McpRuntimeResourceTemplate::name,
                "resource template"
        );
        content.resourceTemplates().forEach(template -> {
            requireServer(
                    servers,
                    template.serverCode(),
                    "resource template"
            );
            validateOptionalBinding(
                    "resource template",
                    template.operationId(),
                    template.remoteMountId(),
                    operationIds,
                    mounts
            );
        });
        return templates;
    }

    private Map<String, McpRuntimePrompt> compilePrompts(
            McpRuleContent content,
            Map<String, McpRuntimeServer> servers,
            Set<String> operationIds,
            Map<String, McpRuntimeRemoteMount> mounts) {
        Map<String, McpRuntimePrompt> prompts = qualifiedIndex(
                content.prompts(),
                McpRuntimePrompt::serverCode,
                McpRuntimePrompt::name,
                "prompt"
        );
        content.prompts().forEach(prompt -> {
            requireServer(servers, prompt.serverCode(), "prompt");
            validateOptionalBinding(
                    "prompt",
                    prompt.operationId(),
                    prompt.remoteMountId(),
                    operationIds,
                    mounts
            );
        });
        return prompts;
    }

    private Map<String, McpRuntimeTaskPolicy> compileTaskPolicies(
            McpRuleContent content,
            Map<String, McpRuntimeServer> servers,
            Map<String, McpRuntimeTool> tools) {
        Map<String, McpRuntimeTaskPolicy> policies = qualifiedIndex(
                content.taskPolicies(),
                McpRuntimeTaskPolicy::serverCode,
                McpRuntimeTaskPolicy::toolName,
                "task policy"
        );
        content.taskPolicies().forEach(policy -> {
            requireServer(servers, policy.serverCode(), "task policy");
            if (!tools.containsKey(CompiledMcpRules.qualified(
                    policy.serverCode(),
                    policy.toolName()
            ))) {
                throw invalid(
                        "MCP task policy references unknown tool: "
                                + policy.toolName()
                );
            }
        });
        return policies;
    }

    private Map<String, McpRuntimeApp> compileApps(
            McpRuleContent content,
            Map<String, McpRuntimeServer> servers,
            Map<String, McpRuntimeTool> tools) {
        Map<String, McpRuntimeApp> apps = qualifiedIndex(
                content.apps(),
                McpRuntimeApp::serverCode,
                McpRuntimeApp::name,
                "app"
        );
        content.apps().forEach(app -> {
            requireServer(servers, app.serverCode(), "app");
            app.allowedTools().forEach(toolName -> {
                if (!tools.containsKey(CompiledMcpRules.qualified(
                        app.serverCode(),
                        toolName
                ))) {
                    throw invalid(
                            "MCP app references unknown tool: " + toolName
                    );
                }
            });
        });
        return apps;
    }

    private void validateMounts(
            List<McpRuntimeRemoteMount> mounts,
            Map<String, McpRuntimeServer> servers,
            Map<String, McpRuntimeRemoteProvider> providers) {
        mounts.forEach(mount -> {
            requireServer(servers, mount.serverCode(), "remote mount");
            if (!providers.containsKey(mount.providerCode())) {
                throw invalid(
                        "MCP remote mount references unknown provider: "
                                + mount.providerCode()
                );
            }
        });
    }

    private void validateBinding(
            String capability,
            String operationId,
            String remoteMountId,
            Set<String> operationIds,
            Map<String, McpRuntimeRemoteMount> mounts) {
        if (operationId == null && remoteMountId == null) {
            throw invalid(
                    "MCP " + capability + " requires a source binding"
            );
        }
        validateOptionalBinding(
                capability,
                operationId,
                remoteMountId,
                operationIds,
                mounts
        );
    }

    private void validateOptionalBinding(
            String capability,
            String operationId,
            String remoteMountId,
            Set<String> operationIds,
            Map<String, McpRuntimeRemoteMount> mounts) {
        if (operationId != null && remoteMountId != null) {
            throw invalid(
                    "MCP " + capability + " has multiple source bindings"
            );
        }
        if (operationId != null && operationIds != null
                && !operationIds.contains(operationId)) {
            throw invalid(
                    "MCP " + capability
                            + " references unknown operation: "
                            + operationId
            );
        }
        if (remoteMountId != null && !mounts.containsKey(remoteMountId)) {
            throw invalid(
                    "MCP " + capability
                            + " references unknown remote mount: "
                            + remoteMountId
            );
        }
    }

    private void requireServer(
            Map<String, McpRuntimeServer> servers,
            String serverCode,
            String capability) {
        if (!servers.containsKey(serverCode)) {
            throw invalid(
                    "MCP " + capability + " references unknown server: "
                            + serverCode
            );
        }
    }

    private <T> Map<String, T> qualifiedIndex(
            List<T> values,
            Function<T, String> serverCode,
            Function<T, String> name,
            String label) {
        return index(
                values,
                value -> CompiledMcpRules.qualified(
                        serverCode.apply(value),
                        name.apply(value)
                ),
                label
        );
    }

    private <T> Map<String, T> index(
            List<T> values,
            Function<T, String> key,
            String label) {
        LinkedHashMap<String, T> indexed = new LinkedHashMap<>();
        values.forEach(value -> {
            String candidate = key.apply(value);
            if (indexed.putIfAbsent(candidate, value) != null) {
                throw invalid("duplicate MCP " + label + ": " + candidate);
            }
        });
        return Map.copyOf(indexed);
    }

    private IllegalArgumentException invalid(String detail) {
        String message = detail == null ? "invalid MCP rule" : detail;
        return new IllegalArgumentException(
                "MCP_RULE_COMPILE_FAILED: " + message
        );
    }
}
