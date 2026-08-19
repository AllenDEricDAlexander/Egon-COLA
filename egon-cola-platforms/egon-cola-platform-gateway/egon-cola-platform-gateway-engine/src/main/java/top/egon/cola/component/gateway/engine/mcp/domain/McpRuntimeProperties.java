package top.egon.cola.component.gateway.engine.mcp.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Validated runtime limits for MCP transports and optional capabilities.
 * 补充说明 / Supplementary summary: {@code McpRuntimeProperties} 是配置属性模型，位于当前 Gateway 模块的相关包中，负责MCP运行时Properties相关的职责与边界。
 * English supplement: {@code McpRuntimeProperties} is a mcp runtime properties properties in the current Gateway module; it owns the mcp runtime properties-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@ConfigurationProperties(prefix = "egon.cola.component.gateway.engine.mcp")
public class McpRuntimeProperties {

    /**
     * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code McpRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private boolean enabled = true;

    /**
     * 中文说明：保存 制品Root 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by artifact root; its type is {@code String}, and {@code McpRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private String artifactRoot = System.getProperty("java.io.tmpdir")
            + "/egon-cola/gateway-mcp-artifacts";

    /**
     * 中文说明：保存 会话Ttl 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by session ttl; its type is {@code Duration}, and {@code McpRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private Duration sessionTtl = Duration.ofMinutes(30);

    /**
     * 中文说明：保存 streamWait 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by stream wait; its type is {@code Duration}, and {@code McpRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private Duration streamWait = Duration.ofSeconds(15);

    /**
     * 中文说明：保存 maximum请求Bytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by maximum request bytes; its type is {@code long}, and {@code McpRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private long maximumRequestBytes = 16L * 1024 * 1024;

    /**
     * 中文说明：保存 maximum资源Bytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by maximum resource bytes; its type is {@code long}, and {@code McpRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private long maximumResourceBytes = 64L * 1024 * 1024;

    /**
     * 中文说明：保存 maximumApp制品Bytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by maximum app artifact bytes; its type is {@code long}, and {@code McpRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private long maximumAppArtifactBytes = 16L * 1024 * 1024;

    /**
     * 中文说明：保存 maximumSubscriptionsPer客户端 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code McpRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by maximum subscriptions per client; its type is {@code int}, and {@code McpRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private int maximumSubscriptionsPerClient = 100;

    /**
     * 中文说明：保存 maximumActiveTasksPer客户端 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code McpRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by maximum active tasks per client; its type is {@code int}, and {@code McpRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private int maximumActiveTasksPerClient = 100;

    /**
     * 中文说明：保存 tasks 对应的状态、依赖或配置值；字段类型为 {@code Tasks}，由 {@code McpRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by tasks; its type is {@code Tasks}, and {@code McpRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Tasks tasks = new Tasks();

    /**
     * 中文说明：保存 远程 对应的状态、依赖或配置值；字段类型为 {@code Remote}，由 {@code McpRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by remote; its type is {@code Remote}, and {@code McpRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Remote remote = new Remote();

    /**
     * 中文说明：保存 安全 对应的状态、依赖或配置值；字段类型为 {@code Security}，由 {@code McpRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by security; its type is {@code Security}, and {@code McpRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Security security = new Security();

    /**
     * 中文说明：保存 审计 对应的状态、依赖或配置值；字段类型为 {@code Audit}，由 {@code McpRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by audit; its type is {@code Audit}, and {@code McpRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Audit audit = new Audit();

    /**
     * 中文说明：执行 validate 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    public void validate() {
        artifactRoot = required(artifactRoot, "artifactRoot");
        sessionTtl = positive(sessionTtl, "sessionTtl");
        streamWait = positive(streamWait, "streamWait");
        maximumRequestBytes = size(
                maximumRequestBytes,
                "maximumRequestBytes"
        );
        maximumResourceBytes = size(
                maximumResourceBytes,
                "maximumResourceBytes"
        );
        maximumAppArtifactBytes = size(
                maximumAppArtifactBytes,
                "maximumAppArtifactBytes"
        );
        if (maximumAppArtifactBytes > maximumResourceBytes) {
            throw new IllegalArgumentException(
                    "MCP maximumAppArtifactBytes exceeds resource limit"
            );
        }
        bounded(
                maximumSubscriptionsPerClient,
                "maximumSubscriptionsPerClient"
        );
        bounded(maximumActiveTasksPerClient, "maximumActiveTasksPerClient");
        tasks.validate();
        remote.validate();
        security.validate();
        audit.validate();
    }

    /**
     * 中文说明：执行 isEnabled 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the is enabled operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.isEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 isEnabled 的处理结果；returns the result of the operation.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 中文说明：执行 setEnabled 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set enabled operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.setEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param enabled 参数 enabled；parameter enabled。
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 中文说明：执行 get制品Root 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get artifact root operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.getArtifactRoot(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 get制品Root 的处理结果；returns the result of the operation.
     */
    public String getArtifactRoot() {
        return artifactRoot;
    }

    /**
     * 中文说明：执行 set制品Root 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set artifact root operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.setArtifactRoot(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param artifactRoot 参数 制品Root；parameter artifact root。
     */
    public void setArtifactRoot(String artifactRoot) {
        this.artifactRoot = artifactRoot;
    }

    /**
     * 中文说明：执行 get会话Ttl 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get session ttl operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.getSessionTtl(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 get会话Ttl 的处理结果；returns the result of the operation.
     */
    public Duration getSessionTtl() {
        return sessionTtl;
    }

    /**
     * 中文说明：执行 set会话Ttl 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set session ttl operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.setSessionTtl(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param sessionTtl 参数 会话Ttl；parameter session ttl。
     */
    public void setSessionTtl(Duration sessionTtl) {
        this.sessionTtl = sessionTtl;
    }

    /**
     * 中文说明：执行 getStreamWait 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get stream wait operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.getStreamWait(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getStreamWait 的处理结果；returns the result of the operation.
     */
    public Duration getStreamWait() {
        return streamWait;
    }

    /**
     * 中文说明：执行 setStreamWait 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set stream wait operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.setStreamWait(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param streamWait 参数 streamWait；parameter stream wait。
     */
    public void setStreamWait(Duration streamWait) {
        this.streamWait = streamWait;
    }

    /**
     * 中文说明：执行 getMaximum请求Bytes 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get maximum request bytes operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.getMaximumRequestBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getMaximum请求Bytes 的处理结果；returns the result of the operation.
     */
    public long getMaximumRequestBytes() {
        return maximumRequestBytes;
    }

    /**
     * 中文说明：执行 setMaximum请求Bytes 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set maximum request bytes operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.setMaximumRequestBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param maximumRequestBytes 参数 maximum请求Bytes；parameter maximum request bytes。
     */
    public void setMaximumRequestBytes(long maximumRequestBytes) {
        this.maximumRequestBytes = maximumRequestBytes;
    }

    /**
     * 中文说明：执行 getMaximum资源Bytes 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get maximum resource bytes operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.getMaximumResourceBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getMaximum资源Bytes 的处理结果；returns the result of the operation.
     */
    public long getMaximumResourceBytes() {
        return maximumResourceBytes;
    }

    /**
     * 中文说明：执行 setMaximum资源Bytes 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set maximum resource bytes operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.setMaximumResourceBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param maximumResourceBytes 参数 maximum资源Bytes；parameter maximum resource bytes。
     */
    public void setMaximumResourceBytes(long maximumResourceBytes) {
        this.maximumResourceBytes = maximumResourceBytes;
    }

    /**
     * 中文说明：执行 getMaximumApp制品Bytes 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get maximum app artifact bytes operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.getMaximumAppArtifactBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getMaximumApp制品Bytes 的处理结果；returns the result of the operation.
     */
    public long getMaximumAppArtifactBytes() {
        return maximumAppArtifactBytes;
    }

    /**
     * 中文说明：执行 setMaximumApp制品Bytes 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set maximum app artifact bytes operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.setMaximumAppArtifactBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param maximumAppArtifactBytes 参数 maximumApp制品Bytes；parameter maximum app artifact bytes。
     */
    public void setMaximumAppArtifactBytes(long maximumAppArtifactBytes) {
        this.maximumAppArtifactBytes = maximumAppArtifactBytes;
    }

    /**
     * 中文说明：执行 getMaximumSubscriptionsPer客户端 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get maximum subscriptions per client operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.getMaximumSubscriptionsPerClient(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getMaximumSubscriptionsPer客户端 的处理结果；returns the result of the operation.
     */
    public int getMaximumSubscriptionsPerClient() {
        return maximumSubscriptionsPerClient;
    }

    /**
     * 中文说明：执行 setMaximumSubscriptionsPer客户端 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set maximum subscriptions per client operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.setMaximumSubscriptionsPerClient(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param maximumSubscriptionsPerClient 参数 maximumSubscriptionsPer客户端；parameter maximum subscriptions per client。
     */
    public void setMaximumSubscriptionsPerClient(
            int maximumSubscriptionsPerClient) {
        this.maximumSubscriptionsPerClient = maximumSubscriptionsPerClient;
    }

    /**
     * 中文说明：执行 getMaximumActiveTasksPer客户端 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get maximum active tasks per client operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.getMaximumActiveTasksPerClient(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getMaximumActiveTasksPer客户端 的处理结果；returns the result of the operation.
     */
    public int getMaximumActiveTasksPerClient() {
        return maximumActiveTasksPerClient;
    }

    /**
     * 中文说明：执行 setMaximumActiveTasksPer客户端 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set maximum active tasks per client operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.setMaximumActiveTasksPerClient(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param maximumActiveTasksPerClient 参数 maximumActiveTasksPer客户端；parameter maximum active tasks per client。
     */
    public void setMaximumActiveTasksPerClient(
            int maximumActiveTasksPerClient) {
        this.maximumActiveTasksPerClient = maximumActiveTasksPerClient;
    }

    /**
     * 中文说明：执行 getTasks 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get tasks operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.getTasks(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getTasks 的处理结果；returns the result of the operation.
     */
    public Tasks getTasks() {
        return tasks;
    }

    /**
     * 中文说明：执行 get远程 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get remote operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.getRemote(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 get远程 的处理结果；returns the result of the operation.
     */
    public Remote getRemote() {
        return remote;
    }

    /**
     * 中文说明：执行 get安全 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get security operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.getSecurity(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 get安全 的处理结果；returns the result of the operation.
     */
    public Security getSecurity() {
        return security;
    }

    /**
     * 中文说明：执行 get审计 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get audit operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.getAudit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 get审计 的处理结果；returns the result of the operation.
     */
    public Audit getAudit() {
        return audit;
    }

    /**
     * 中文说明：{@code Tasks} 是类型，位于当前 Gateway 模块的相关包中，负责Tasks相关的职责与边界。
     * English summary: {@code Tasks} is a type in the current Gateway module; it owns the tasks-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public static class Tasks {

        /**
         * 中文说明：保存 租约Duration 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpRuntimeProperties.Tasks} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by lease duration; its type is {@code Duration}, and {@code McpRuntimeProperties.Tasks} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties.Tasks} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties.Tasks}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration leaseDuration = Duration.ofSeconds(30);

        /**
         * 中文说明：保存 pollInterval 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpRuntimeProperties.Tasks} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by poll interval; its type is {@code Duration}, and {@code McpRuntimeProperties.Tasks} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties.Tasks} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties.Tasks}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration pollInterval = Duration.ofSeconds(2);

        /**
         * 中文说明：保存 defaultTtl 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpRuntimeProperties.Tasks} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by default ttl; its type is {@code Duration}, and {@code McpRuntimeProperties.Tasks} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties.Tasks} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties.Tasks}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration defaultTtl = Duration.ofHours(24);

        /**
         * 中文说明：保存 cleanupInterval 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpRuntimeProperties.Tasks} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by cleanup interval; its type is {@code Duration}, and {@code McpRuntimeProperties.Tasks} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties.Tasks} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties.Tasks}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration cleanupInterval = Duration.ofMinutes(10);

        /**
         * 中文说明：执行 validate 操作；该方法是 {@code McpRuntimeProperties.Tasks} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the validate operation; this method is the invocation entry point on {@code McpRuntimeProperties.Tasks} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Tasks.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        private void validate() {
            leaseDuration = positive(leaseDuration, "tasks.leaseDuration");
            pollInterval = positive(pollInterval, "tasks.pollInterval");
            defaultTtl = positive(defaultTtl, "tasks.defaultTtl");
            cleanupInterval = positive(
                    cleanupInterval,
                    "tasks.cleanupInterval"
            );
            if (pollInterval.compareTo(leaseDuration) >= 0) {
                throw new IllegalArgumentException(
                        "MCP task pollInterval must be shorter than leaseDuration"
                );
            }
        }

        /**
         * 中文说明：执行 get租约Duration 操作；该方法是 {@code McpRuntimeProperties.Tasks} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get lease duration operation; this method is the invocation entry point on {@code McpRuntimeProperties.Tasks} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Tasks.getLeaseDuration(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 get租约Duration 的处理结果；returns the result of the operation.
         */
        public Duration getLeaseDuration() {
            return leaseDuration;
        }

        /**
         * 中文说明：执行 set租约Duration 操作；该方法是 {@code McpRuntimeProperties.Tasks} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set lease duration operation; this method is the invocation entry point on {@code McpRuntimeProperties.Tasks} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Tasks.setLeaseDuration(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param leaseDuration 参数 租约Duration；parameter lease duration。
         */
        public void setLeaseDuration(Duration leaseDuration) {
            this.leaseDuration = leaseDuration;
        }

        /**
         * 中文说明：执行 getPollInterval 操作；该方法是 {@code McpRuntimeProperties.Tasks} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get poll interval operation; this method is the invocation entry point on {@code McpRuntimeProperties.Tasks} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Tasks.getPollInterval(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getPollInterval 的处理结果；returns the result of the operation.
         */
        public Duration getPollInterval() {
            return pollInterval;
        }

        /**
         * 中文说明：执行 setPollInterval 操作；该方法是 {@code McpRuntimeProperties.Tasks} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set poll interval operation; this method is the invocation entry point on {@code McpRuntimeProperties.Tasks} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Tasks.setPollInterval(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param pollInterval 参数 pollInterval；parameter poll interval。
         */
        public void setPollInterval(Duration pollInterval) {
            this.pollInterval = pollInterval;
        }

        /**
         * 中文说明：执行 getDefaultTtl 操作；该方法是 {@code McpRuntimeProperties.Tasks} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get default ttl operation; this method is the invocation entry point on {@code McpRuntimeProperties.Tasks} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Tasks.getDefaultTtl(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getDefaultTtl 的处理结果；returns the result of the operation.
         */
        public Duration getDefaultTtl() {
            return defaultTtl;
        }

        /**
         * 中文说明：执行 setDefaultTtl 操作；该方法是 {@code McpRuntimeProperties.Tasks} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set default ttl operation; this method is the invocation entry point on {@code McpRuntimeProperties.Tasks} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Tasks.setDefaultTtl(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param defaultTtl 参数 defaultTtl；parameter default ttl。
         */
        public void setDefaultTtl(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
        }

        /**
         * 中文说明：执行 getCleanupInterval 操作；该方法是 {@code McpRuntimeProperties.Tasks} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get cleanup interval operation; this method is the invocation entry point on {@code McpRuntimeProperties.Tasks} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Tasks.getCleanupInterval(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getCleanupInterval 的处理结果；returns the result of the operation.
         */
        public Duration getCleanupInterval() {
            return cleanupInterval;
        }

        /**
         * 中文说明：执行 setCleanupInterval 操作；该方法是 {@code McpRuntimeProperties.Tasks} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set cleanup interval operation; this method is the invocation entry point on {@code McpRuntimeProperties.Tasks} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Tasks.setCleanupInterval(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param cleanupInterval 参数 cleanupInterval；parameter cleanup interval。
         */
        public void setCleanupInterval(Duration cleanupInterval) {
            this.cleanupInterval = cleanupInterval;
        }
    }

    /**
     * 中文说明：{@code Remote} 是类型，位于当前 Gateway 模块的相关包中，负责远程相关的职责与边界。
     * English summary: {@code Remote} is a type in the current Gateway module; it owns the remote-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public static class Remote {

        /**
         * 中文说明：保存 发现超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpRuntimeProperties.Remote} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by discovery timeout; its type is {@code Duration}, and {@code McpRuntimeProperties.Remote} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties.Remote} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties.Remote}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration discoveryTimeout = Duration.ofSeconds(20);

        /**
         * 中文说明：保存 调用超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpRuntimeProperties.Remote} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by call timeout; its type is {@code Duration}, and {@code McpRuntimeProperties.Remote} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties.Remote} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties.Remote}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration callTimeout = Duration.ofSeconds(60);

        /**
         * 中文说明：保存 健康Interval 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpRuntimeProperties.Remote} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by health interval; its type is {@code Duration}, and {@code McpRuntimeProperties.Remote} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties.Remote} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties.Remote}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration healthInterval = Duration.ofSeconds(30);

        /**
         * 中文说明：保存 capabilitySyncInterval 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpRuntimeProperties.Remote} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by capability sync interval; its type is {@code Duration}, and {@code McpRuntimeProperties.Remote} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties.Remote} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties.Remote}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration capabilitySyncInterval = Duration.ofMinutes(5);

        /**
         * 中文说明：保存 circuitOpenDuration 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code McpRuntimeProperties.Remote} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by circuit open duration; its type is {@code Duration}, and {@code McpRuntimeProperties.Remote} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties.Remote} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties.Remote}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration circuitOpenDuration = Duration.ofSeconds(30);

        /**
         * 中文说明：保存 maximumConcurrentCalls 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code McpRuntimeProperties.Remote} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by maximum concurrent calls; its type is {@code int}, and {@code McpRuntimeProperties.Remote} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties.Remote} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties.Remote}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int maximumConcurrentCalls = 32;

        /**
         * 中文说明：保存 failureThreshold 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code McpRuntimeProperties.Remote} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by failure threshold; its type is {@code int}, and {@code McpRuntimeProperties.Remote} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties.Remote} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties.Remote}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int failureThreshold = 3;

        /**
         * 中文说明：保存 tokenForwarding 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpRuntimeProperties.Remote} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by token forwarding; its type is {@code boolean}, and {@code McpRuntimeProperties.Remote} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties.Remote} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties.Remote}; do not couple callers to its representation when the owning type exposes an API.
         */
        private boolean tokenForwarding;

        /**
         * 中文说明：执行 validate 操作；该方法是 {@code McpRuntimeProperties.Remote} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the validate operation; this method is the invocation entry point on {@code McpRuntimeProperties.Remote} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Remote.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        private void validate() {
            discoveryTimeout = positive(
                    discoveryTimeout,
                    "remote.discoveryTimeout"
            );
            callTimeout = positive(callTimeout, "remote.callTimeout");
            healthInterval = positive(
                    healthInterval,
                    "remote.healthInterval"
            );
            capabilitySyncInterval = positive(
                    capabilitySyncInterval,
                    "remote.capabilitySyncInterval"
            );
            circuitOpenDuration = positive(
                    circuitOpenDuration,
                    "remote.circuitOpenDuration"
            );
            bounded(maximumConcurrentCalls, "remote.maximumConcurrentCalls");
            bounded(failureThreshold, "remote.failureThreshold");
            if (tokenForwarding) {
                throw new IllegalArgumentException(
                        "MCP remote token forwarding must remain disabled"
                );
            }
        }

        /**
         * 中文说明：执行 get发现超时 操作；该方法是 {@code McpRuntimeProperties.Remote} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get discovery timeout operation; this method is the invocation entry point on {@code McpRuntimeProperties.Remote} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Remote.getDiscoveryTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 get发现超时 的处理结果；returns the result of the operation.
         */
        public Duration getDiscoveryTimeout() {
            return discoveryTimeout;
        }

        /**
         * 中文说明：执行 set发现超时 操作；该方法是 {@code McpRuntimeProperties.Remote} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set discovery timeout operation; this method is the invocation entry point on {@code McpRuntimeProperties.Remote} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Remote.setDiscoveryTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param discoveryTimeout 参数 发现超时；parameter discovery timeout。
         */
        public void setDiscoveryTimeout(Duration discoveryTimeout) {
            this.discoveryTimeout = discoveryTimeout;
        }

        /**
         * 中文说明：执行 get调用超时 操作；该方法是 {@code McpRuntimeProperties.Remote} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get call timeout operation; this method is the invocation entry point on {@code McpRuntimeProperties.Remote} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Remote.getCallTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 get调用超时 的处理结果；returns the result of the operation.
         */
        public Duration getCallTimeout() {
            return callTimeout;
        }

        /**
         * 中文说明：执行 set调用超时 操作；该方法是 {@code McpRuntimeProperties.Remote} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set call timeout operation; this method is the invocation entry point on {@code McpRuntimeProperties.Remote} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Remote.setCallTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param callTimeout 参数 调用超时；parameter call timeout。
         */
        public void setCallTimeout(Duration callTimeout) {
            this.callTimeout = callTimeout;
        }

        /**
         * 中文说明：执行 get健康Interval 操作；该方法是 {@code McpRuntimeProperties.Remote} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get health interval operation; this method is the invocation entry point on {@code McpRuntimeProperties.Remote} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Remote.getHealthInterval(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 get健康Interval 的处理结果；returns the result of the operation.
         */
        public Duration getHealthInterval() {
            return healthInterval;
        }

        /**
         * 中文说明：执行 set健康Interval 操作；该方法是 {@code McpRuntimeProperties.Remote} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set health interval operation; this method is the invocation entry point on {@code McpRuntimeProperties.Remote} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Remote.setHealthInterval(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param healthInterval 参数 健康Interval；parameter health interval。
         */
        public void setHealthInterval(Duration healthInterval) {
            this.healthInterval = healthInterval;
        }

        /**
         * 中文说明：执行 getCapabilitySyncInterval 操作；该方法是 {@code McpRuntimeProperties.Remote} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get capability sync interval operation; this method is the invocation entry point on {@code McpRuntimeProperties.Remote} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Remote.getCapabilitySyncInterval(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getCapabilitySyncInterval 的处理结果；returns the result of the operation.
         */
        public Duration getCapabilitySyncInterval() {
            return capabilitySyncInterval;
        }

        /**
         * 中文说明：执行 setCapabilitySyncInterval 操作；该方法是 {@code McpRuntimeProperties.Remote} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set capability sync interval operation; this method is the invocation entry point on {@code McpRuntimeProperties.Remote} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Remote.setCapabilitySyncInterval(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param capabilitySyncInterval 参数 capabilitySyncInterval；parameter capability sync interval。
         */
        public void setCapabilitySyncInterval(
                Duration capabilitySyncInterval) {
            this.capabilitySyncInterval = capabilitySyncInterval;
        }

        /**
         * 中文说明：执行 getCircuitOpenDuration 操作；该方法是 {@code McpRuntimeProperties.Remote} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get circuit open duration operation; this method is the invocation entry point on {@code McpRuntimeProperties.Remote} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Remote.getCircuitOpenDuration(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getCircuitOpenDuration 的处理结果；returns the result of the operation.
         */
        public Duration getCircuitOpenDuration() {
            return circuitOpenDuration;
        }

        /**
         * 中文说明：执行 setCircuitOpenDuration 操作；该方法是 {@code McpRuntimeProperties.Remote} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set circuit open duration operation; this method is the invocation entry point on {@code McpRuntimeProperties.Remote} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Remote.setCircuitOpenDuration(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param circuitOpenDuration 参数 circuitOpenDuration；parameter circuit open duration。
         */
        public void setCircuitOpenDuration(Duration circuitOpenDuration) {
            this.circuitOpenDuration = circuitOpenDuration;
        }

        /**
         * 中文说明：执行 getMaximumConcurrentCalls 操作；该方法是 {@code McpRuntimeProperties.Remote} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get maximum concurrent calls operation; this method is the invocation entry point on {@code McpRuntimeProperties.Remote} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Remote.getMaximumConcurrentCalls(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getMaximumConcurrentCalls 的处理结果；returns the result of the operation.
         */
        public int getMaximumConcurrentCalls() {
            return maximumConcurrentCalls;
        }

        /**
         * 中文说明：执行 setMaximumConcurrentCalls 操作；该方法是 {@code McpRuntimeProperties.Remote} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set maximum concurrent calls operation; this method is the invocation entry point on {@code McpRuntimeProperties.Remote} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Remote.setMaximumConcurrentCalls(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param maximumConcurrentCalls 参数 maximumConcurrentCalls；parameter maximum concurrent calls。
         */
        public void setMaximumConcurrentCalls(int maximumConcurrentCalls) {
            this.maximumConcurrentCalls = maximumConcurrentCalls;
        }

        /**
         * 中文说明：执行 getFailureThreshold 操作；该方法是 {@code McpRuntimeProperties.Remote} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get failure threshold operation; this method is the invocation entry point on {@code McpRuntimeProperties.Remote} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Remote.getFailureThreshold(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getFailureThreshold 的处理结果；returns the result of the operation.
         */
        public int getFailureThreshold() {
            return failureThreshold;
        }

        /**
         * 中文说明：执行 setFailureThreshold 操作；该方法是 {@code McpRuntimeProperties.Remote} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set failure threshold operation; this method is the invocation entry point on {@code McpRuntimeProperties.Remote} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Remote.setFailureThreshold(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param failureThreshold 参数 failureThreshold；parameter failure threshold。
         */
        public void setFailureThreshold(int failureThreshold) {
            this.failureThreshold = failureThreshold;
        }

        /**
         * 中文说明：执行 isTokenForwarding 操作；该方法是 {@code McpRuntimeProperties.Remote} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the is token forwarding operation; this method is the invocation entry point on {@code McpRuntimeProperties.Remote} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Remote.isTokenForwarding(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 isTokenForwarding 的处理结果；returns the result of the operation.
         */
        public boolean isTokenForwarding() {
            return tokenForwarding;
        }

        /**
         * 中文说明：执行 setTokenForwarding 操作；该方法是 {@code McpRuntimeProperties.Remote} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set token forwarding operation; this method is the invocation entry point on {@code McpRuntimeProperties.Remote} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Remote.setTokenForwarding(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param tokenForwarding 参数 tokenForwarding；parameter token forwarding。
         */
        public void setTokenForwarding(boolean tokenForwarding) {
            this.tokenForwarding = tokenForwarding;
        }
    }

    /**
     * 中文说明：{@code Security} 是类型，位于当前 Gateway 模块的相关包中，负责安全相关的职责与边界。
     * English summary: {@code Security} is a type in the current Gateway module; it owns the security-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public static class Security {

        /**
         * 中文说明：保存 originValidation 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpRuntimeProperties.Security} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by origin validation; its type is {@code boolean}, and {@code McpRuntimeProperties.Security} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties.Security} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties.Security}; do not couple callers to its representation when the owning type exposes an API.
         */
        private boolean originValidation = true;

        /**
         * 中文说明：保存 protected资源元数据 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpRuntimeProperties.Security} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by protected resource metadata; its type is {@code boolean}, and {@code McpRuntimeProperties.Security} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties.Security} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties.Security}; do not couple callers to its representation when the owning type exposes an API.
         */
        private boolean protectedResourceMetadata = true;

        /**
         * 中文说明：保存 tokenForwarding 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpRuntimeProperties.Security} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by token forwarding; its type is {@code boolean}, and {@code McpRuntimeProperties.Security} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties.Security} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties.Security}; do not couple callers to its representation when the owning type exposes an API.
         */
        private boolean tokenForwarding;

        /**
         * 中文说明：执行 validate 操作；该方法是 {@code McpRuntimeProperties.Security} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the validate operation; this method is the invocation entry point on {@code McpRuntimeProperties.Security} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Security.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        private void validate() {
            if (tokenForwarding) {
                throw new IllegalArgumentException(
                        "MCP security token forwarding must remain disabled"
                );
            }
        }

        /**
         * 中文说明：执行 isOriginValidation 操作；该方法是 {@code McpRuntimeProperties.Security} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the is origin validation operation; this method is the invocation entry point on {@code McpRuntimeProperties.Security} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Security.isOriginValidation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 isOriginValidation 的处理结果；returns the result of the operation.
         */
        public boolean isOriginValidation() {
            return originValidation;
        }

        /**
         * 中文说明：执行 setOriginValidation 操作；该方法是 {@code McpRuntimeProperties.Security} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set origin validation operation; this method is the invocation entry point on {@code McpRuntimeProperties.Security} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Security.setOriginValidation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param originValidation 参数 originValidation；parameter origin validation。
         */
        public void setOriginValidation(boolean originValidation) {
            this.originValidation = originValidation;
        }

        /**
         * 中文说明：执行 isProtected资源元数据 操作；该方法是 {@code McpRuntimeProperties.Security} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the is protected resource metadata operation; this method is the invocation entry point on {@code McpRuntimeProperties.Security} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Security.isProtectedResourceMetadata(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 isProtected资源元数据 的处理结果；returns the result of the operation.
         */
        public boolean isProtectedResourceMetadata() {
            return protectedResourceMetadata;
        }

        /**
         * 中文说明：执行 setProtected资源元数据 操作；该方法是 {@code McpRuntimeProperties.Security} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set protected resource metadata operation; this method is the invocation entry point on {@code McpRuntimeProperties.Security} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Security.setProtectedResourceMetadata(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param protectedResourceMetadata 参数 protected资源元数据；parameter protected resource metadata。
         */
        public void setProtectedResourceMetadata(
                boolean protectedResourceMetadata) {
            this.protectedResourceMetadata = protectedResourceMetadata;
        }

        /**
         * 中文说明：执行 isTokenForwarding 操作；该方法是 {@code McpRuntimeProperties.Security} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the is token forwarding operation; this method is the invocation entry point on {@code McpRuntimeProperties.Security} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Security.isTokenForwarding(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 isTokenForwarding 的处理结果；returns the result of the operation.
         */
        public boolean isTokenForwarding() {
            return tokenForwarding;
        }

        /**
         * 中文说明：执行 setTokenForwarding 操作；该方法是 {@code McpRuntimeProperties.Security} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set token forwarding operation; this method is the invocation entry point on {@code McpRuntimeProperties.Security} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Security.setTokenForwarding(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param tokenForwarding 参数 tokenForwarding；parameter token forwarding。
         */
        public void setTokenForwarding(boolean tokenForwarding) {
            this.tokenForwarding = tokenForwarding;
        }
    }

    /**
     * 中文说明：{@code Audit} 是类型，位于当前 Gateway 模块的相关包中，负责审计相关的职责与边界。
     * English summary: {@code Audit} is a type in the current Gateway module; it owns the audit-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public static class Audit {

        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpRuntimeProperties.Audit} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code McpRuntimeProperties.Audit} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties.Audit} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties.Audit}; do not couple callers to its representation when the owning type exposes an API.
         */
        private boolean enabled = true;

        /**
         * 中文说明：保存 bodyLogEnabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpRuntimeProperties.Audit} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by body log enabled; its type is {@code boolean}, and {@code McpRuntimeProperties.Audit} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpRuntimeProperties.Audit} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeProperties.Audit}; do not couple callers to its representation when the owning type exposes an API.
         */
        private boolean bodyLogEnabled;

        /**
         * 中文说明：执行 validate 操作；该方法是 {@code McpRuntimeProperties.Audit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the validate operation; this method is the invocation entry point on {@code McpRuntimeProperties.Audit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Audit.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        private void validate() {
            if (bodyLogEnabled) {
                throw new IllegalArgumentException(
                        "MCP audit body logging must remain disabled"
                );
            }
        }

        /**
         * 中文说明：执行 isEnabled 操作；该方法是 {@code McpRuntimeProperties.Audit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the is enabled operation; this method is the invocation entry point on {@code McpRuntimeProperties.Audit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Audit.isEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 isEnabled 的处理结果；returns the result of the operation.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 中文说明：执行 setEnabled 操作；该方法是 {@code McpRuntimeProperties.Audit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set enabled operation; this method is the invocation entry point on {@code McpRuntimeProperties.Audit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Audit.setEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param enabled 参数 enabled；parameter enabled。
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 中文说明：执行 isBodyLogEnabled 操作；该方法是 {@code McpRuntimeProperties.Audit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the is body log enabled operation; this method is the invocation entry point on {@code McpRuntimeProperties.Audit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Audit.isBodyLogEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 isBodyLogEnabled 的处理结果；returns the result of the operation.
         */
        public boolean isBodyLogEnabled() {
            return bodyLogEnabled;
        }

        /**
         * 中文说明：执行 setBodyLogEnabled 操作；该方法是 {@code McpRuntimeProperties.Audit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set body log enabled operation; this method is the invocation entry point on {@code McpRuntimeProperties.Audit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.Audit.setBodyLogEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param bodyLogEnabled 参数 bodyLogEnabled；parameter body log enabled。
         */
        public void setBodyLogEnabled(boolean bodyLogEnabled) {
            this.bodyLogEnabled = bodyLogEnabled;
        }
    }

    /**
     * 中文说明：执行 positive 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the positive operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.positive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 positive 的处理结果；returns the result of the operation.
     */
    private static Duration positive(Duration value, String field) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(
                    "MCP " + field + " must be positive"
            );
        }
        return value;
    }

    /**
     * 中文说明：执行 size 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the size operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.size(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 size 的处理结果；returns the result of the operation.
     */
    private static long size(long value, String field) {
        if (value < 1L || value > 1024L * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "MCP " + field + " is outside its safe range"
            );
        }
        return value;
    }

    /**
     * 中文说明：执行 bounded 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bounded operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.bounded(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     */
    private static void bounded(int value, String field) {
        if (value < 1 || value > 10_000) {
            throw new IllegalArgumentException(
                    "MCP " + field + " is outside its safe range"
            );
        }
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code McpRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code McpRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeProperties.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MCP " + field + " is required");
        }
        return value.trim();
    }
}
