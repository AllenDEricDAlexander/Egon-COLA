package top.egon.cola.component.gateway.engine.rule;

import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;
import top.egon.cola.component.gateway.core.route.HttpRouteCompiler;
import top.egon.cola.component.gateway.core.route.RuntimeHttpRoute;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.gateway.core.transport.GatewayRouteProfileResolver;
import top.egon.cola.component.gateway.core.transport.GatewayTransportDefaults;
import top.egon.cola.component.gateway.core.transport.GatewayTransportPolicyOverrides;
import top.egon.cola.component.gateway.core.transport.GatewayTransportSafetyLimits;
import top.egon.cola.component.gateway.engine.rpc.RpcMethodIndex;
import top.egon.cola.component.gateway.engine.rpc.RpcMethodIndexCompiler;
import top.egon.cola.component.gateway.engine.rpc.RuntimeRpcRoute;
import top.egon.cola.component.gateway.engine.discovery.GatewayProviderPolicyCompiler;
import top.egon.cola.component.gateway.engine.cors.GatewayCorsPolicyCompiler;
import top.egon.cola.component.gateway.engine.security.GatewaySecurityCapabilityRegistry;
import top.egon.cola.component.gateway.engine.security.GatewaySecurityPolicyCompiler;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficPolicyCompiler;
import top.egon.cola.component.gateway.engine.traffic.RuntimeTrafficPolicy;
import top.egon.cola.component.gateway.engine.traffic.TrafficPolicyType;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;
import top.egon.cola.component.gateway.mcp.rule.McpRuleCompiler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Function;

public final class EngineGatewayRuleCompiler {

    private final HttpRouteCompiler httpCompiler = new HttpRouteCompiler();

    private final RpcMethodIndexCompiler rpcCompiler =
            new RpcMethodIndexCompiler();

    private final GatewayTrafficPolicyCompiler trafficPolicyCompiler =
            new GatewayTrafficPolicyCompiler();

    private final GatewayProviderPolicyCompiler providerPolicyCompiler =
            new GatewayProviderPolicyCompiler();

    private final GatewayCorsPolicyCompiler corsPolicyCompiler =
            new GatewayCorsPolicyCompiler();

    private final GatewaySecurityPolicyCompiler securityPolicyCompiler;

    private final GatewayRouteProfileResolver transportResolver =
            new GatewayRouteProfileResolver();

    private final GatewayTransportDefaults transportDefaults;

    private final GatewayTransportSafetyLimits transportSafetyLimits;

    private final McpRuleCompiler mcpCompiler = new McpRuleCompiler();

    public EngineGatewayRuleCompiler() {
        this(
                GatewaySecurityCapabilityRegistry.empty(),
                GatewayTransportDefaults.legacy(),
                GatewayTransportSafetyLimits.specDefaults()
        );
    }

    public EngineGatewayRuleCompiler(
            GatewaySecurityCapabilityRegistry capabilities) {
        this(
                capabilities,
                GatewayTransportDefaults.legacy(),
                GatewayTransportSafetyLimits.specDefaults()
        );
    }

    public EngineGatewayRuleCompiler(
            GatewaySecurityCapabilityRegistry capabilities,
            GatewayTransportDefaults transportDefaults,
            GatewayTransportSafetyLimits transportSafetyLimits) {
        securityPolicyCompiler = new GatewaySecurityPolicyCompiler(
                capabilities
        );
        this.transportDefaults = Objects.requireNonNull(
                transportDefaults,
                "transportDefaults"
        );
        this.transportSafetyLimits = Objects.requireNonNull(
                transportSafetyLimits,
                "transportSafetyLimits"
        );
    }

    public CompiledGatewayRules compile(GatewayRuleSnapshot snapshot) {
        GatewayRuleContent content = snapshot.content();
        CompiledMcpRules mcpRules = mcpCompiler.compile(
                content.mcp(),
                content.operations().stream()
                        .map(GatewayRuntimeOperation::operationId)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet())
        );
        List<GatewayRuntimePolicy> runtimePolicies = new ArrayList<>();
        runtimePolicies.addAll(content.providerPolicies());
        runtimePolicies.addAll(content.trafficPolicies());
        runtimePolicies.addAll(content.securityPolicies());
        runtimePolicies.addAll(content.corsPolicies());
        Map<String, RuntimeTrafficPolicy> trafficPolicies =
                trafficPolicyCompiler.compile(runtimePolicies);
        Map<String, GatewayRuntimeOperation> operations = content.operations()
                .stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        GatewayRuntimeOperation::operationId,
                        Function.identity()
                ));
        List<RuntimeHttpRoute> httpRoutes = content.routes().stream()
                .filter(route -> route.enabled())
                .map(route -> {
                    GatewayRuntimeOperation operation = operations.get(
                            route.operationId()
                    );
                    if (operation == null) {
                        throw new IllegalArgumentException(
                                "GATEWAY_RULE_COMPILE_FAILED: unknown operation"
                        );
                    }
                    return new RuntimeHttpRoute(
                            route.routeId(),
                            operation.operationId(),
                            content.gatewayGroupId(),
                            route.accessZones(),
                            route.host(),
                            Set.of(route.httpMethod()),
                            route.pathPattern(),
                            operation.externalAccessible(),
                            serviceKey(operation.providerService()),
                            operation.policyRefs(),
                            route.priority(),
                            GatewayResponseMode.valueOf(
                                    operation.responseMode()
                            ),
                            routeMetadata(
                                    operation,
                                    snapshot.releaseId()
                            ),
                            transportResolver.resolve(
                                    route.transportPolicy(),
                                    transportDefaults,
                                    transportOverrides(
                                            operation.policyRefs(),
                                            trafficPolicies
                                    ),
                                    transportSafetyLimits
                            )
                    );
                })
                .toList();
        List<RuntimeRpcRoute> rpcRoutes = content.operations().stream()
                .filter(operation -> operation.protocol()
                        == GatewayProtocol.RPC)
                .filter(operation -> !operation.deprecated())
                .map(this::rpcRoute)
                .toList();
        Set<ProviderServiceKey> services = new LinkedHashSet<>();
        content.operations().stream()
                .filter(operation -> !operation.deprecated())
                .map(GatewayRuntimeOperation::providerService)
                .map(this::serviceKey)
                .forEach(services::add);
        Map<String, Set<GatewayProtocol>> policyProtocols =
                new LinkedHashMap<>();
        content.operations().stream()
                .filter(operation -> !operation.deprecated())
                .forEach(operation -> operation.policyRefs().forEach(
                        policyId -> policyProtocols.computeIfAbsent(
                                policyId,
                                ignored -> new LinkedHashSet<>()
                        ).add(operation.protocol())
                ));
        Map<String, GatewaySecurityPolicy> securityPolicies =
                securityPolicyCompiler.compile(
                        runtimePolicies,
                        policyProtocols
                );
        var providerPolicies = providerPolicyCompiler.compile(
                content.providerPolicies()
        );
        var corsPolicies = corsPolicyCompiler.compile(
                content.corsPolicies()
        );
        content.operations().stream()
                .filter(operation -> !operation.deprecated())
                .forEach(operation -> {
                    long securityReferences = operation.policyRefs().stream()
                            .filter(securityPolicies::containsKey)
                            .count();
                    if (securityReferences > 1) {
                        throw new IllegalArgumentException(
                                "GATEWAY_RULE_COMPILE_FAILED: operation "
                                        + operation.operationId()
                                        + " references multiple security "
                                        + "policies"
                        );
                    }
                    long loadBalanceReferences = operation.policyRefs()
                            .stream()
                            .map(providerPolicies::get)
                            .filter(java.util.Objects::nonNull)
                            .filter(policy -> policy.type()
                                    == top.egon.cola.component.gateway.engine
                                    .discovery.RuntimeProviderPolicy.Type
                                    .LOAD_BALANCE)
                            .count();
                    if (loadBalanceReferences > 1) {
                        throw new IllegalArgumentException(
                                "GATEWAY_RULE_COMPILE_FAILED: operation "
                                        + operation.operationId()
                                        + " references multiple load balance "
                                        + "policies"
                        );
                    }
                    long corsReferences = operation.policyRefs().stream()
                            .filter(corsPolicies::containsKey)
                            .count();
                    if (corsReferences > 1) {
                        throw new IllegalArgumentException(
                                "GATEWAY_RULE_COMPILE_FAILED: operation "
                                        + operation.operationId()
                                        + " references multiple CORS policies"
                        );
                    }
                });
        return new CompiledGatewayRules(
                snapshot,
                httpCompiler.compile(httpRoutes),
                rpcCompiler.compile(rpcRoutes),
                services,
                providerPolicies,
                trafficPolicies,
                securityPolicies,
                corsPolicies,
                mcpRules
        );
    }

    private GatewayTransportPolicyOverrides transportOverrides(
            Set<String> policyRefs,
            Map<String, RuntimeTrafficPolicy> policies) {
        OptionalLong requestLimit = OptionalLong.empty();
        OptionalLong responseLimit = OptionalLong.empty();
        Optional<Duration> totalTimeout = Optional.empty();
        for (String policyRef : policyRefs) {
            RuntimeTrafficPolicy policy = policies.get(policyRef);
            if (policy == null || !policy.enabled()) {
                continue;
            }
            if (policy.type() == TrafficPolicyType.REQUEST_SIZE) {
                requestLimit = minimum(
                        requestLimit,
                        longValue(policy.parameters().get("maxBytes"))
                );
            } else if (policy.type() == TrafficPolicyType.RESPONSE_SIZE) {
                responseLimit = minimum(
                        responseLimit,
                        longValue(policy.parameters().get("maxBytes"))
                );
            } else if (policy.type() == TrafficPolicyType.TIMEOUT) {
                Duration configured = timeout(policy.parameters());
                if (configured != null
                        && (totalTimeout.isEmpty()
                        || configured.compareTo(totalTimeout.orElseThrow())
                        < 0)) {
                    totalTimeout = Optional.of(configured);
                }
            }
        }
        return new GatewayTransportPolicyOverrides(
                requestLimit,
                responseLimit,
                totalTimeout
        );
    }

    private OptionalLong minimum(OptionalLong current, long candidate) {
        return current.isEmpty()
                ? OptionalLong.of(candidate)
                : OptionalLong.of(Math.min(current.getAsLong(), candidate));
    }

    private Duration timeout(Map<String, Object> parameters) {
        Object value = parameters.get("timeout");
        if (value == null) {
            value = parameters.get("timeoutMillis");
        }
        if (value == null) {
            return null;
        }
        if (value instanceof Duration duration) {
            return duration;
        }
        if (value instanceof Number number) {
            return Duration.ofMillis(number.longValue());
        }
        String text = value.toString();
        return text.startsWith("P")
                ? Duration.parse(text)
                : Duration.ofMillis(Long.parseLong(text));
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(Objects.requireNonNull(value).toString());
    }

    private RuntimeRpcRoute rpcRoute(GatewayRuntimeOperation operation) {
        String descriptorSha = operation.attributes().get(
                "descriptorSha256"
        );
        if (descriptorSha == null || descriptorSha.isBlank()) {
            throw new IllegalArgumentException(
                    "GATEWAY_RULE_COMPILE_FAILED: RPC descriptor is missing"
            );
        }
        long timeoutMillis = Long.parseLong(
                operation.attributes().getOrDefault("timeoutMillis", "3000")
        );
        return new RuntimeRpcRoute(
                operation.operationId(),
                operation.operationId(),
                operation.methodIdentity(),
                serviceKey(operation.providerService()),
                operation.requestSchema(),
                operation.responseSchema(),
                descriptorSha,
                operation.policyRefs(),
                GatewayResponseMode.valueOf(operation.responseMode()),
                Boolean.parseBoolean(operation.attributes().getOrDefault(
                        "idempotent",
                        "false"
                )),
                Duration.ofMillis(timeoutMillis)
        );
    }

    private Map<String, String> routeMetadata(
            GatewayRuntimeOperation operation,
            String releaseId) {
        Map<String, String> metadata = new LinkedHashMap<>(
                operation.attributes()
        );
        metadata.put("releaseId", releaseId);
        metadata.put("protocol", operation.protocol().name());
        metadata.put("methodIdentity", operation.methodIdentity());
        if (operation.requestSchema() != null) {
            metadata.put("requestSchema", operation.requestSchema());
        }
        if (operation.responseSchema() != null) {
            metadata.put("responseSchema", operation.responseSchema());
        }
        return Map.copyOf(metadata);
    }

    private ProviderServiceKey serviceKey(
            GatewayProviderServiceRef service) {
        return new ProviderServiceKey(
                service.bizCode(),
                service.appCode(),
                service.env(),
                service.namespace(),
                service.protocol() == GatewayProtocol.HTTP
                        ? ProviderProtocolType.HTTP
                        : ProviderProtocolType.RPC,
                service.serviceName(),
                service.group(),
                service.version(),
                service.transport()
        );
    }

}
