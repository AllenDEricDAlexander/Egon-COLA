package top.egon.cola.component.gateway.mcp.remote.service;

import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteMount;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteProvider;
import top.egon.cola.component.gateway.mcp.rule.domain.CompiledMcpRules;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Applies the immutable namespace and rename rules from an Active Release.
 * 补充说明 / Supplementary summary: {@code McpNamespaceRouter} 是类型，位于当前 Gateway 模块的相关包中，负责MCP命名空间Router相关的职责与边界。
 * English supplement: {@code McpNamespaceRouter} is a type in the current Gateway module; it owns the mcp namespace router-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpNamespaceRouter {

    /**
     * 中文说明：执行 binding 操作；该方法是 {@code McpNamespaceRouter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the binding operation; this method is the invocation entry point on {@code McpNamespaceRouter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpNamespaceRouter.binding(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param rules 参数 rules；parameter rules。
     * @param mountId 参数 mountId；parameter mount id。
     * @param primitiveType 参数 primitiveType；parameter primitive type。
     * @param exposedName 参数 exposedName；parameter exposed name。
     * @return 返回 binding 的处理结果；returns the result of the operation.
     */
    public Binding binding(
            CompiledMcpRules rules,
            String mountId,
            String primitiveType,
            String exposedName) {
        Objects.requireNonNull(rules, "rules");
        McpRuntimeRemoteMount mount = rules.remoteMountsById().get(mountId);
        if (mount == null || !mount.enabled()) {
            throw new IllegalArgumentException("remote MCP mount is unavailable");
        }
        McpRuntimeRemoteProvider provider = rules.remoteProvidersByCode().get(
                mount.providerCode()
        );
        if (provider == null || !provider.enabled()) {
            throw new IllegalArgumentException(
                    "remote MCP Provider is unavailable"
            );
        }
        if (!provider.capabilityFingerprint().equals(
                mount.capabilityFingerprint()
        )) {
            throw new IllegalArgumentException(
                    "remote MCP capability fingerprint has drifted"
            );
        }
        String type = required(primitiveType, "primitiveType")
                .toUpperCase(Locale.ROOT);
        if (!mount.primitiveTypes().contains(type)) {
            throw new IllegalArgumentException(
                    "remote MCP primitive is not mounted"
            );
        }
        return new Binding(
                mount,
                provider,
                remoteName(mount, exposedName)
        );
    }

    /**
     * Rename rules are stored as {@code remoteName -> localName}.
     * 补充说明 / Supplementary summary: 执行 exposedName 操作；该方法是 {@code McpNamespaceRouter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English supplement: Executes the exposed name operation; this method is the invocation entry point on {@code McpNamespaceRouter} and performs the corresponding runtime, management, or protocol work.
     * 用法 / Usage: 调用方式 / Usage: {@code McpNamespaceRouter.exposedName(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    public String exposedName(
            McpRuntimeRemoteMount mount,
            String remoteName) {
        String remote = required(remoteName, "remoteName");
        String local = mount.renameRules().getOrDefault(remote, remote);
        return mount.namespace() + "." + local;
    }

    /**
     * 中文说明：执行 远程Name 操作；该方法是 {@code McpNamespaceRouter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the remote name operation; this method is the invocation entry point on {@code McpNamespaceRouter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpNamespaceRouter.remoteName(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param mount 参数 mount；parameter mount。
     * @param exposedName 参数 exposedName；parameter exposed name。
     * @return 返回 远程Name 的处理结果；returns the result of the operation.
     */
    public String remoteName(
            McpRuntimeRemoteMount mount,
            String exposedName) {
        String exposed = required(exposedName, "exposedName");
        String prefix = mount.namespace() + ".";
        if (!exposed.startsWith(prefix) || exposed.length() == prefix.length()) {
            throw new IllegalArgumentException(
                    "remote MCP capability is outside its namespace"
            );
        }
        String local = exposed.substring(prefix.length());
        return mount.renameRules().entrySet().stream()
                .filter(entry -> entry.getValue().equals(local))
                .map(java.util.Map.Entry::getKey)
                .findFirst()
                .orElse(local);
    }

    /**
     * 中文说明：执行 merge 操作；该方法是 {@code McpNamespaceRouter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the merge operation; this method is the invocation entry point on {@code McpNamespaceRouter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpNamespaceRouter.merge(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param existing 参数 existing；parameter existing。
     * @param mount 参数 mount；parameter mount。
     * @param remoteName 参数 远程Name；parameter remote name。
     * @return 返回 merge 的处理结果；returns the result of the operation.
     */
    public Set<String> merge(
            Set<String> existing,
            McpRuntimeRemoteMount mount,
            String remoteName) {
        LinkedHashSet<String> result = new LinkedHashSet<>(
                Objects.requireNonNull(existing, "existing")
        );
        String exposed = exposedName(mount, remoteName);
        if (!result.contains(exposed)) {
            result.add(exposed);
            return Set.copyOf(result);
        }
        switch (mount.conflictPolicy().toUpperCase(Locale.ROOT)) {
            case "KEEP_LOCAL" -> {
                return Set.copyOf(result);
            }
            case "REPLACE" -> {
                result.remove(exposed);
                result.add(exposed);
                return Set.copyOf(result);
            }
            case "REJECT" -> throw new IllegalArgumentException(
                    "remote MCP capability conflicts with an existing name"
            );
            default -> throw new IllegalArgumentException(
                    "remote MCP conflict policy is unsupported"
            );
        }
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code McpNamespaceRouter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code McpNamespaceRouter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpNamespaceRouter.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "remote MCP " + field + " is required"
            );
        }
        return value.trim();
    }

    /**
     * 中文说明：{@code Binding} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Binding相关的职责与边界。
     * English summary: {@code Binding} is an immutable data carrier in the current Gateway module; it owns the binding-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param mount 参数 mount；parameter mount。
     * @param provider 参数 提供方；parameter provider。
     * @param remoteName 参数 远程Name；parameter remote name。
     */
    public record Binding(
            /**
             * 中文说明：保存 mount 对应的状态、依赖或配置值；字段类型为 {@code McpRuntimeRemoteMount}，由 {@code McpNamespaceRouter.Binding} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by mount; its type is {@code McpRuntimeRemoteMount}, and {@code McpNamespaceRouter.Binding} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpNamespaceRouter.Binding} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpNamespaceRouter.Binding}; do not couple callers to its representation when the owning type exposes an API.
             */
            McpRuntimeRemoteMount mount,
            /**
             * 中文说明：保存 提供方 对应的状态、依赖或配置值；字段类型为 {@code McpRuntimeRemoteProvider}，由 {@code McpNamespaceRouter.Binding} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by provider; its type is {@code McpRuntimeRemoteProvider}, and {@code McpNamespaceRouter.Binding} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpNamespaceRouter.Binding} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpNamespaceRouter.Binding}; do not couple callers to its representation when the owning type exposes an API.
             */
            McpRuntimeRemoteProvider provider,
            /**
             * 中文说明：保存 远程Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpNamespaceRouter.Binding} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by remote name; its type is {@code String}, and {@code McpNamespaceRouter.Binding} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpNamespaceRouter.Binding} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpNamespaceRouter.Binding}; do not couple callers to its representation when the owning type exposes an API.
             */
            String remoteName
    ) {

        /**
         * 中文说明：创建 {@code McpNamespaceRouter.Binding} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpNamespaceRouter.Binding} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param mount 参数 mount；parameter mount。
         * @param provider 参数 提供方；parameter provider。
         * @param remoteName 参数 远程Name；parameter remote name。
         */
        public Binding {
            mount = Objects.requireNonNull(mount, "mount");
            provider = Objects.requireNonNull(provider, "provider");
            remoteName = Objects.requireNonNull(remoteName, "remoteName");
        }
    }
}
