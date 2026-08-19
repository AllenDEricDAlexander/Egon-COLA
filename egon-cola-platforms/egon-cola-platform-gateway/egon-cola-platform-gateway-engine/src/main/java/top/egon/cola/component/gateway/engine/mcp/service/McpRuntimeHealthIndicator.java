package top.egon.cola.component.gateway.engine.mcp.service;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteProvider;
import top.egon.cola.component.gateway.engine.rule.domain.CompiledGatewayRules;
import top.egon.cola.component.gateway.engine.rule.service.GatewayRuleActivationApplier;
import top.egon.cola.component.gateway.mcp.remote.service.McpRemoteClientPool;
import top.egon.cola.component.gateway.mcp.rule.domain.CompiledMcpRules;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Secret-free MCP readiness summary for the active immutable Release.
 * 补充说明 / Supplementary summary: {@code McpRuntimeHealthIndicator} 是类型，位于当前 Gateway 模块的相关包中，负责MCP运行时健康Indicator相关的职责与边界。
 * English supplement: {@code McpRuntimeHealthIndicator} is a type in the current Gateway module; it owns the mcp runtime health indicator-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpRuntimeHealthIndicator implements HealthIndicator {

    /**
     * 中文说明：保存 activation 对应的状态、依赖或配置值；字段类型为 {@code GatewayRuleActivationApplier}，由 {@code McpRuntimeHealthIndicator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by activation; its type is {@code GatewayRuleActivationApplier}, and {@code McpRuntimeHealthIndicator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRuntimeHealthIndicator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeHealthIndicator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRuleActivationApplier activation;

    /**
     * 中文说明：保存 会话存储Available 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpRuntimeHealthIndicator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by session store available; its type is {@code boolean}, and {@code McpRuntimeHealthIndicator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRuntimeHealthIndicator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeHealthIndicator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final boolean sessionStoreAvailable;

    /**
     * 中文说明：保存 任务存储Available 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpRuntimeHealthIndicator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by task store available; its type is {@code boolean}, and {@code McpRuntimeHealthIndicator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRuntimeHealthIndicator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeHealthIndicator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final boolean taskStoreAvailable;

    /**
     * 中文说明：保存 制品Root 对应的状态、依赖或配置值；字段类型为 {@code Path}，由 {@code McpRuntimeHealthIndicator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by artifact root; its type is {@code Path}, and {@code McpRuntimeHealthIndicator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRuntimeHealthIndicator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeHealthIndicator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Path artifactRoot;

    /**
     * 中文说明：保存 远程Clients 对应的状态、依赖或配置值；字段类型为 {@code McpRemoteClientPool}，由 {@code McpRuntimeHealthIndicator} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by remote clients; its type is {@code McpRemoteClientPool}, and {@code McpRuntimeHealthIndicator} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpRuntimeHealthIndicator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpRuntimeHealthIndicator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpRemoteClientPool remoteClients;

    /**
     * 中文说明：创建 {@code McpRuntimeHealthIndicator} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpRuntimeHealthIndicator} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param activation 参数 activation；parameter activation。
     * @param sessionStoreAvailable 参数 会话存储Available；parameter session store available。
     * @param taskStoreAvailable 参数 任务存储Available；parameter task store available。
     * @param artifactRoot 参数 制品Root；parameter artifact root。
     * @param remoteClients 参数 远程Clients；parameter remote clients。
     */
    public McpRuntimeHealthIndicator(
            GatewayRuleActivationApplier activation,
            boolean sessionStoreAvailable,
            boolean taskStoreAvailable,
            Path artifactRoot,
            McpRemoteClientPool remoteClients) {
        this.activation = Objects.requireNonNull(activation, "activation");
        this.sessionStoreAvailable = sessionStoreAvailable;
        this.taskStoreAvailable = taskStoreAvailable;
        this.artifactRoot = Objects.requireNonNull(
                artifactRoot,
                "artifactRoot"
        ).toAbsolutePath().normalize();
        this.remoteClients = Objects.requireNonNull(
                remoteClients,
                "remoteClients"
        );
    }

    /**
     * 中文说明：执行 健康 操作；该方法是 {@code McpRuntimeHealthIndicator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the health operation; this method is the invocation entry point on {@code McpRuntimeHealthIndicator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeHealthIndicator.health(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 健康 的处理结果；returns the result of the operation.
     */
    @Override
    public Health health() {
        CompiledGatewayRules active = activation.active();
        if (active == null) {
            return Health.status("OUT_OF_SERVICE")
                    .withDetail("activeReleaseId", "")
                    .withDetail("protocols", List.of())
                    .withDetail("sessions", availability(
                            sessionStoreAvailable
                    ))
                    .withDetail("tasks", availability(taskStoreAvailable))
                    .withDetail("artifacts", "NOT_ACTIVE")
                    .withDetail("remoteProviders", Map.of())
                    .build();
        }
        CompiledMcpRules mcp = active.mcpRules();
        boolean tasksRequired = mcp.taskPoliciesByQualifiedTool().values()
                .stream()
                .anyMatch(policy -> policy.enabled() && policy.durable());
        boolean artifactsRequired = mcp.appsByQualifiedName().values()
                .stream()
                .anyMatch(app -> app.enabled());
        String artifactState = artifactsRequired
                ? artifactState()
                : "NOT_REQUIRED";
        Map<String, Object> remotes = remoteHealth(mcp);
        boolean remoteOpen = remotes.values().stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .anyMatch(value -> "OPEN".equals(value.get("state")));
        boolean degraded = activation.status().degraded()
                || !sessionStoreAvailable
                || tasksRequired && !taskStoreAvailable
                || artifactsRequired && !"AVAILABLE".equals(artifactState)
                || remoteOpen;
        Health.Builder result = degraded
                ? Health.status("DEGRADED")
                : Health.up();
        return result
                .withDetail(
                        "activeReleaseId",
                        active.snapshot().releaseId()
                )
                .withDetail("protocols", protocols(mcp))
                .withDetail("sessions", availability(
                        sessionStoreAvailable
                ))
                .withDetail(
                        "tasks",
                        tasksRequired
                                ? availability(taskStoreAvailable)
                                : "NOT_REQUIRED"
                )
                .withDetail("artifacts", artifactState)
                .withDetail("remoteProviders", remotes)
                .build();
    }

    /**
     * 中文说明：执行 protocols 操作；该方法是 {@code McpRuntimeHealthIndicator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the protocols operation; this method is the invocation entry point on {@code McpRuntimeHealthIndicator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeHealthIndicator.protocols(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param rules 参数 rules；parameter rules。
     * @return 返回 protocols 的处理结果；returns the result of the operation.
     */
    private List<String> protocols(CompiledMcpRules rules) {
        return rules.serversByCode().values().stream()
                .filter(server -> server.enabled())
                .flatMap(server -> server.dialects().stream())
                .map(dialect -> dialect.protocolVersion())
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * 中文说明：执行 远程健康 操作；该方法是 {@code McpRuntimeHealthIndicator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the remote health operation; this method is the invocation entry point on {@code McpRuntimeHealthIndicator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeHealthIndicator.remoteHealth(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param rules 参数 rules；parameter rules。
     * @return 返回 远程健康 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> remoteHealth(CompiledMcpRules rules) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        rules.remoteProvidersByCode().values().stream()
                .filter(McpRuntimeRemoteProvider::enabled)
                .sorted(java.util.Comparator.comparing(
                        McpRuntimeRemoteProvider::providerCode
                ))
                .forEach(provider -> {
                    McpRemoteClientPool.Health health = remoteClients.health(
                            provider
                    );
                    result.put(provider.providerCode(), Map.of(
                            "state", health.state(),
                            "consecutiveFailures",
                            health.consecutiveFailures(),
                            "availablePermits",
                            health.availablePermits()
                    ));
                });
        return Map.copyOf(result);
    }

    /**
     * 中文说明：执行 制品State 操作；该方法是 {@code McpRuntimeHealthIndicator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the artifact state operation; this method is the invocation entry point on {@code McpRuntimeHealthIndicator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeHealthIndicator.artifactState(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 制品State 的处理结果；returns the result of the operation.
     */
    private String artifactState() {
        return Files.isDirectory(artifactRoot)
                && Files.isReadable(artifactRoot)
                ? "AVAILABLE"
                : "UNAVAILABLE";
    }

    /**
     * 中文说明：执行 availability 操作；该方法是 {@code McpRuntimeHealthIndicator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the availability operation; this method is the invocation entry point on {@code McpRuntimeHealthIndicator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpRuntimeHealthIndicator.availability(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param available 参数 available；parameter available。
     * @return 返回 availability 的处理结果；returns the result of the operation.
     */
    private String availability(boolean available) {
        return available ? "AVAILABLE" : "UNAVAILABLE";
    }
}
