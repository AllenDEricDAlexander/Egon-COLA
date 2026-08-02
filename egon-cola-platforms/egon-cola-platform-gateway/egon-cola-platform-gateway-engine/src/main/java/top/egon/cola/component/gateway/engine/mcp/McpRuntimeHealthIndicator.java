package top.egon.cola.component.gateway.engine.mcp;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteProvider;
import top.egon.cola.component.gateway.engine.rule.CompiledGatewayRules;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleActivationApplier;
import top.egon.cola.component.gateway.mcp.remote.McpRemoteClientPool;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Secret-free MCP readiness summary for the active immutable Release.
 */
public final class McpRuntimeHealthIndicator implements HealthIndicator {

    private final GatewayRuleActivationApplier activation;

    private final boolean sessionStoreAvailable;

    private final boolean taskStoreAvailable;

    private final Path artifactRoot;

    private final McpRemoteClientPool remoteClients;

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

    private List<String> protocols(CompiledMcpRules rules) {
        return rules.serversByCode().values().stream()
                .filter(server -> server.enabled())
                .flatMap(server -> server.dialects().stream())
                .map(dialect -> dialect.protocolVersion())
                .distinct()
                .sorted()
                .toList();
    }

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

    private String artifactState() {
        return Files.isDirectory(artifactRoot)
                && Files.isReadable(artifactRoot)
                ? "AVAILABLE"
                : "UNAVAILABLE";
    }

    private String availability(boolean available) {
        return available ? "AVAILABLE" : "UNAVAILABLE";
    }
}
