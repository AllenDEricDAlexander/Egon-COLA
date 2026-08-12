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
import top.egon.cola.component.gateway.mcp.remote.McpRemoteEndpointValidator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * 中文说明：{@code McpRuleCompiler} 是编译器，位于当前 Gateway 模块的相关包中，负责MCP规则Compiler相关的职责与边界。
 * English summary: {@code McpRuleCompiler} is a mcp rule compiler compiler in the current Gateway module; it owns the mcp rule compiler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpRuleCompiler {

    /**
     * 中文说明：执行 compile 操作；该方法是 {@code McpRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the compile operation; this method is the invocation entry point on {@code McpRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuleCompiler.compile(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @return 返回 compile 的处理结果；returns the result of the operation.
     */
    public CompiledMcpRules compile(McpRuleContent content) {
        return compile(content, null);
    }

    /**
     * 中文说明：执行 compile 操作；该方法是 {@code McpRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the compile operation; this method is the invocation entry point on {@code McpRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuleCompiler.compile(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @param operationIds 参数 操作Ids；parameter operation ids。
     * @return 返回 compile 的处理结果；returns the result of the operation.
     */
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
            content.remoteProviders().forEach(this::validateProvider);
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
            apps.keySet().stream()
                    .filter(resources::containsKey)
                    .findFirst()
                    .ifPresent(key -> {
                        throw invalid(
                                "MCP app and resource names must be unique: "
                                        + key.substring(
                                        key.indexOf('\u0000') + 1
                                )
                        );
                    });
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

    /**
     * 中文说明：执行 compileTools 操作；该方法是 {@code McpRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the compile tools operation; this method is the invocation entry point on {@code McpRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuleCompiler.compileTools(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @param servers 参数 servers；parameter servers。
     * @param operationIds 参数 操作Ids；parameter operation ids。
     * @param mounts 参数 mounts；parameter mounts。
     * @return 返回 compileTools 的处理结果；returns the result of the operation.
     */
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
            validateToolInvocation(tool);
            requirePrimitive(mounts, tool.remoteMountId(), "TOOL");
        });
        return tools;
    }

    /**
     * 中文说明：执行 validate工具Invocation 操作；该方法是 {@code McpRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate tool invocation operation; this method is the invocation entry point on {@code McpRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuleCompiler.validateToolInvocation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param tool 参数 工具；parameter tool。
     */
    private void validateToolInvocation(McpRuntimeTool tool) {
        if (tool.operationId() == null) {
            if (tool.operationProtocol() != null) {
                throw invalid(
                        "remote MCP Tool cannot declare a local Operation"
                );
            }
            return;
        }
        if (tool.operationProtocol() == null
                || !Set.of("HTTP", "RPC").contains(tool.operationProtocol())) {
            throw invalid(
                    "local MCP Tool requires HTTP or RPC operation protocol"
            );
        }
    }

    /**
     * 中文说明：执行 compileResources 操作；该方法是 {@code McpRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the compile resources operation; this method is the invocation entry point on {@code McpRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuleCompiler.compileResources(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @param servers 参数 servers；parameter servers。
     * @param operationIds 参数 操作Ids；parameter operation ids。
     * @param mounts 参数 mounts；parameter mounts。
     * @return 返回 compileResources 的处理结果；returns the result of the operation.
     */
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
            requirePrimitive(
                    mounts,
                    resource.remoteMountId(),
                    "RESOURCE"
            );
        });
        return resources;
    }

    /**
     * 中文说明：执行 compileTemplates 操作；该方法是 {@code McpRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the compile templates operation; this method is the invocation entry point on {@code McpRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuleCompiler.compileTemplates(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @param servers 参数 servers；parameter servers。
     * @param operationIds 参数 操作Ids；parameter operation ids。
     * @param mounts 参数 mounts；parameter mounts。
     * @return 返回 compileTemplates 的处理结果；returns the result of the operation.
     */
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
            requirePrimitive(
                    mounts,
                    template.remoteMountId(),
                    "RESOURCE_TEMPLATE"
            );
        });
        return templates;
    }

    /**
     * 中文说明：执行 compilePrompts 操作；该方法是 {@code McpRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the compile prompts operation; this method is the invocation entry point on {@code McpRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuleCompiler.compilePrompts(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @param servers 参数 servers；parameter servers。
     * @param operationIds 参数 操作Ids；parameter operation ids。
     * @param mounts 参数 mounts；parameter mounts。
     * @return 返回 compilePrompts 的处理结果；returns the result of the operation.
     */
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
            requirePrimitive(mounts, prompt.remoteMountId(), "PROMPT");
        });
        return prompts;
    }

    /**
     * 中文说明：执行 compile任务Policies 操作；该方法是 {@code McpRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the compile task policies operation; this method is the invocation entry point on {@code McpRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuleCompiler.compileTaskPolicies(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @param servers 参数 servers；parameter servers。
     * @param tools 参数 tools；parameter tools。
     * @return 返回 compile任务Policies 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 compileApps 操作；该方法是 {@code McpRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the compile apps operation; this method is the invocation entry point on {@code McpRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuleCompiler.compileApps(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @param servers 参数 servers；parameter servers。
     * @param tools 参数 tools；parameter tools。
     * @return 返回 compileApps 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 validateMounts 操作；该方法是 {@code McpRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate mounts operation; this method is the invocation entry point on {@code McpRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuleCompiler.validateMounts(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param mounts 参数 mounts；parameter mounts。
     * @param servers 参数 servers；parameter servers。
     * @param providers 参数 providers；parameter providers。
     */
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
            McpRuntimeRemoteProvider provider = providers.get(
                    mount.providerCode()
            );
            if (!provider.capabilityFingerprint().equals(
                    mount.capabilityFingerprint()
            )) {
                throw invalid(
                        "MCP remote capability fingerprint has drifted: "
                                + mount.mountId()
                );
            }
            Set<String> supported = Set.of(
                    "TOOL",
                    "RESOURCE",
                    "RESOURCE_TEMPLATE",
                    "PROMPT",
                    "COMPLETION",
                    "APP",
                    "TASK",
                    "SUBSCRIPTION"
            );
            if (mount.primitiveTypes().isEmpty()
                    || !supported.containsAll(mount.primitiveTypes())) {
                throw invalid(
                        "MCP remote mount contains unsupported primitives: "
                                + mount.mountId()
                );
            }
            if (!Set.of("REJECT", "KEEP_LOCAL", "REPLACE").contains(
                    mount.conflictPolicy().toUpperCase(Locale.ROOT)
            )) {
                throw invalid(
                        "MCP remote conflict policy is unsupported: "
                                + mount.mountId()
                );
            }
        });
    }

    /**
     * 中文说明：执行 validate提供方 操作；该方法是 {@code McpRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate provider operation; this method is the invocation entry point on {@code McpRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuleCompiler.validateProvider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     */
    private void validateProvider(McpRuntimeRemoteProvider provider) {
        String transport = provider.transportType().toUpperCase(Locale.ROOT);
        if (!Set.of(
                "STREAMABLE_HTTP",
                "LEGACY_SSE",
                "STDIO_MANAGED"
        ).contains(transport)) {
            throw invalid(
                    "MCP remote transport is unsupported: "
                            + provider.providerCode()
            );
        }
        if ("STDIO_MANAGED".equals(transport)) {
            return;
        }
        try {
            McpRemoteEndpointValidator.requireSafe(
                    provider.endpointReference()
            );
        } catch (IllegalArgumentException failure) {
            throw invalid(
                    "MCP remote endpoint must be a safe HTTP(S) URI: "
                            + provider.providerCode()
            );
        }
    }

    /**
     * 中文说明：执行 requirePrimitive 操作；该方法是 {@code McpRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require primitive operation; this method is the invocation entry point on {@code McpRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuleCompiler.requirePrimitive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param mounts 参数 mounts；parameter mounts。
     * @param mountId 参数 mountId；parameter mount id。
     * @param primitiveType 参数 primitiveType；parameter primitive type。
     */
    private void requirePrimitive(
            Map<String, McpRuntimeRemoteMount> mounts,
            String mountId,
            String primitiveType) {
        if (mountId == null) {
            return;
        }
        McpRuntimeRemoteMount mount = mounts.get(mountId);
        if (mount != null && !mount.primitiveTypes().contains(primitiveType)) {
            throw invalid(
                    "MCP remote mount does not expose " + primitiveType
                            + ": " + mountId
            );
        }
    }

    /**
     * 中文说明：执行 validateBinding 操作；该方法是 {@code McpRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate binding operation; this method is the invocation entry point on {@code McpRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuleCompiler.validateBinding(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param capability 参数 capability；parameter capability。
     * @param operationId 参数 操作Id；parameter operation id。
     * @param remoteMountId 参数 远程MountId；parameter remote mount id。
     * @param operationIds 参数 操作Ids；parameter operation ids。
     * @param mounts 参数 mounts；parameter mounts。
     */
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

    /**
     * 中文说明：执行 validateOptionalBinding 操作；该方法是 {@code McpRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate optional binding operation; this method is the invocation entry point on {@code McpRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuleCompiler.validateOptionalBinding(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param capability 参数 capability；parameter capability。
     * @param operationId 参数 操作Id；parameter operation id。
     * @param remoteMountId 参数 远程MountId；parameter remote mount id。
     * @param operationIds 参数 操作Ids；parameter operation ids。
     * @param mounts 参数 mounts；parameter mounts。
     */
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

    /**
     * 中文说明：执行 require服务器 操作；该方法是 {@code McpRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the require server operation; this method is the invocation entry point on {@code McpRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuleCompiler.requireServer(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param servers 参数 servers；parameter servers。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param capability 参数 capability；parameter capability。
     */
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

    /**
     * 中文说明：执行 qualified索引 操作；该方法是 {@code McpRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the qualified index operation; this method is the invocation entry point on {@code McpRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuleCompiler.qualifiedIndex(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param name 参数 name；parameter name。
     * @param label 参数 label；parameter label。
     * @return 返回 qualified索引 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 索引 操作；该方法是 {@code McpRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the index operation; this method is the invocation entry point on {@code McpRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuleCompiler.index(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @param key 参数 键；parameter key。
     * @param label 参数 label；parameter label。
     * @return 返回 索引 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 invalid 操作；该方法是 {@code McpRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invalid operation; this method is the invocation entry point on {@code McpRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuleCompiler.invalid(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param detail 参数 detail；parameter detail。
     * @return 返回 invalid 的处理结果；returns the result of the operation.
     */
    private IllegalArgumentException invalid(String detail) {
        String message = detail == null ? "invalid MCP rule" : detail;
        return new IllegalArgumentException(
                "MCP_RULE_COMPILE_FAILED: " + message
        );
    }
}
