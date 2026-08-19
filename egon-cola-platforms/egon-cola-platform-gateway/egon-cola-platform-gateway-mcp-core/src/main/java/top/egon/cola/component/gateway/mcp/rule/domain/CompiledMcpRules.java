package top.egon.cola.component.gateway.mcp.rule.domain;

import top.egon.cola.component.gateway.mcp.rule.service.McpRuleCompiler;

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

/**
 * 中文说明：{@code CompiledMcpRules} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责CompiledMCPRules相关的职责与边界。
 * English summary: {@code CompiledMcpRules} is an immutable data carrier in the current Gateway module; it owns the compiled mcp rules-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param content 参数 content；parameter content。
 * @param serversByCode 参数 serversByCode；parameter servers by code。
 * @param toolsByQualifiedName 参数 toolsByQualifiedName；parameter tools by qualified name。
 * @param resourcesByQualifiedName 参数 resourcesByQualifiedName；parameter resources by qualified name。
 * @param templatesByQualifiedName 参数 templatesByQualifiedName；parameter templates by qualified name。
 * @param promptsByQualifiedName 参数 promptsByQualifiedName；parameter prompts by qualified name。
 * @param taskPoliciesByQualifiedTool 参数 任务PoliciesByQualified工具；parameter task policies by qualified tool。
 * @param appsByQualifiedName 参数 appsByQualifiedName；parameter apps by qualified name。
 * @param remoteProvidersByCode 参数 远程ProvidersByCode；parameter remote providers by code。
 * @param remoteMountsById 参数 远程MountsById；parameter remote mounts by id。
 */
public record CompiledMcpRules(
        /**
         * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code McpRuleContent}，由 {@code CompiledMcpRules} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code McpRuleContent}, and {@code CompiledMcpRules} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledMcpRules} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledMcpRules}; do not couple callers to its representation when the owning type exposes an API.
         */
        McpRuleContent content,
        /**
         * 中文说明：保存 serversByCode 对应的状态、依赖或配置值；字段类型为 {@code Map<String, McpRuntimeServer>}，由 {@code CompiledMcpRules} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by servers by code; its type is {@code Map<String, McpRuntimeServer>}, and {@code CompiledMcpRules} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledMcpRules} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledMcpRules}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, McpRuntimeServer> serversByCode,
        /**
         * 中文说明：保存 toolsByQualifiedName 对应的状态、依赖或配置值；字段类型为 {@code Map<String, McpRuntimeTool>}，由 {@code CompiledMcpRules} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by tools by qualified name; its type is {@code Map<String, McpRuntimeTool>}, and {@code CompiledMcpRules} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledMcpRules} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledMcpRules}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, McpRuntimeTool> toolsByQualifiedName,
        /**
         * 中文说明：保存 resourcesByQualifiedName 对应的状态、依赖或配置值；字段类型为 {@code Map<String, McpRuntimeResource>}，由 {@code CompiledMcpRules} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by resources by qualified name; its type is {@code Map<String, McpRuntimeResource>}, and {@code CompiledMcpRules} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledMcpRules} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledMcpRules}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, McpRuntimeResource> resourcesByQualifiedName,
        /**
         * 中文说明：保存 templatesByQualifiedName 对应的状态、依赖或配置值；字段类型为 {@code Map<String, McpRuntimeResourceTemplate>}，由 {@code CompiledMcpRules} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by templates by qualified name; its type is {@code Map<String, McpRuntimeResourceTemplate>}, and {@code CompiledMcpRules} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledMcpRules} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledMcpRules}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, McpRuntimeResourceTemplate> templatesByQualifiedName,
        /**
         * 中文说明：保存 promptsByQualifiedName 对应的状态、依赖或配置值；字段类型为 {@code Map<String, McpRuntimePrompt>}，由 {@code CompiledMcpRules} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by prompts by qualified name; its type is {@code Map<String, McpRuntimePrompt>}, and {@code CompiledMcpRules} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledMcpRules} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledMcpRules}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, McpRuntimePrompt> promptsByQualifiedName,
        /**
         * 中文说明：保存 任务PoliciesByQualified工具 对应的状态、依赖或配置值；字段类型为 {@code Map<String, McpRuntimeTaskPolicy>}，由 {@code CompiledMcpRules} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by task policies by qualified tool; its type is {@code Map<String, McpRuntimeTaskPolicy>}, and {@code CompiledMcpRules} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledMcpRules} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledMcpRules}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, McpRuntimeTaskPolicy> taskPoliciesByQualifiedTool,
        /**
         * 中文说明：保存 appsByQualifiedName 对应的状态、依赖或配置值；字段类型为 {@code Map<String, McpRuntimeApp>}，由 {@code CompiledMcpRules} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by apps by qualified name; its type is {@code Map<String, McpRuntimeApp>}, and {@code CompiledMcpRules} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledMcpRules} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledMcpRules}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, McpRuntimeApp> appsByQualifiedName,
        /**
         * 中文说明：保存 远程ProvidersByCode 对应的状态、依赖或配置值；字段类型为 {@code Map<String, McpRuntimeRemoteProvider>}，由 {@code CompiledMcpRules} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by remote providers by code; its type is {@code Map<String, McpRuntimeRemoteProvider>}, and {@code CompiledMcpRules} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledMcpRules} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledMcpRules}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, McpRuntimeRemoteProvider> remoteProvidersByCode,
        /**
         * 中文说明：保存 远程MountsById 对应的状态、依赖或配置值；字段类型为 {@code Map<String, McpRuntimeRemoteMount>}，由 {@code CompiledMcpRules} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by remote mounts by id; its type is {@code Map<String, McpRuntimeRemoteMount>}, and {@code CompiledMcpRules} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledMcpRules} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledMcpRules}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, McpRuntimeRemoteMount> remoteMountsById
) {

    /**
     * 中文说明：创建 {@code CompiledMcpRules} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code CompiledMcpRules} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param content 参数 content；parameter content。
     * @param serversByCode 参数 serversByCode；parameter servers by code。
     * @param toolsByQualifiedName 参数 toolsByQualifiedName；parameter tools by qualified name。
     * @param resourcesByQualifiedName 参数 resourcesByQualifiedName；parameter resources by qualified name。
     * @param templatesByQualifiedName 参数 templatesByQualifiedName；parameter templates by qualified name。
     * @param promptsByQualifiedName 参数 promptsByQualifiedName；parameter prompts by qualified name。
     * @param taskPoliciesByQualifiedTool 参数 任务PoliciesByQualified工具；parameter task policies by qualified tool。
     * @param appsByQualifiedName 参数 appsByQualifiedName；parameter apps by qualified name。
     * @param remoteProvidersByCode 参数 远程ProvidersByCode；parameter remote providers by code。
     * @param remoteMountsById 参数 远程MountsById；parameter remote mounts by id。
     */
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

    /**
     * 中文说明：执行 empty 操作；该方法是 {@code CompiledMcpRules} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the empty operation; this method is the invocation entry point on {@code CompiledMcpRules} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code CompiledMcpRules.empty(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 empty 的处理结果；returns the result of the operation.
     */
    public static CompiledMcpRules empty() {
        return new McpRuleCompiler().compile(McpRuleContent.empty());
    }

    /**
     * 中文说明：执行 服务器 操作；该方法是 {@code CompiledMcpRules} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the server operation; this method is the invocation entry point on {@code CompiledMcpRules} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code CompiledMcpRules.server(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @return 返回 服务器 的处理结果；returns the result of the operation.
     */
    public Optional<McpRuntimeServer> server(String serverCode) {
        return Optional.ofNullable(serversByCode.get(serverCode));
    }

    /**
     * 中文说明：执行 工具 操作；该方法是 {@code CompiledMcpRules} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the tool operation; this method is the invocation entry point on {@code CompiledMcpRules} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code CompiledMcpRules.tool(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param name 参数 name；parameter name。
     * @return 返回 工具 的处理结果；returns the result of the operation.
     */
    public Optional<McpRuntimeTool> tool(String serverCode, String name) {
        return Optional.ofNullable(toolsByQualifiedName.get(
                qualified(serverCode, name)
        ));
    }

    /**
     * 中文说明：执行 远程Available 操作；该方法是 {@code CompiledMcpRules} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the remote available operation; this method is the invocation entry point on {@code CompiledMcpRules} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code CompiledMcpRules.remoteAvailable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param mountId 参数 mountId；parameter mount id。
     * @param primitiveType 参数 primitiveType；parameter primitive type。
     * @return 返回 远程Available 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 qualified 操作；该方法是 {@code CompiledMcpRules} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the qualified operation; this method is the invocation entry point on {@code CompiledMcpRules} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code CompiledMcpRules.qualified(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param name 参数 name；parameter name。
     * @return 返回 qualified 的处理结果；returns the result of the operation.
     */
    public static String qualified(String serverCode, String name) {
        return Objects.requireNonNull(serverCode, "serverCode")
                + "\u0000"
                + Objects.requireNonNull(name, "name");
    }

    /**
     * 中文说明：执行 immutable 操作；该方法是 {@code CompiledMcpRules} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the immutable operation; this method is the invocation entry point on {@code CompiledMcpRules} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code CompiledMcpRules.immutable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param field 参数 field；parameter field。
     * @return 返回 immutable 的处理结果；returns the result of the operation.
     */
    private static <K, V> Map<K, V> immutable(
            Map<K, V> source,
            String field) {
        return Map.copyOf(Objects.requireNonNull(source, field));
    }
}
