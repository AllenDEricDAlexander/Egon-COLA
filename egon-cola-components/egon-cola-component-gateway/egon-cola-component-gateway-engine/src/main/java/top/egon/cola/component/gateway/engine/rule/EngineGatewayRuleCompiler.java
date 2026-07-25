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
import top.egon.cola.component.gateway.engine.rpc.RpcMethodIndex;
import top.egon.cola.component.gateway.engine.rpc.RpcMethodIndexCompiler;
import top.egon.cola.component.gateway.engine.rpc.RuntimeRpcRoute;
import top.egon.cola.component.gateway.engine.discovery.GatewayProviderPolicyCompiler;
import top.egon.cola.component.gateway.engine.security.GatewaySecurityCapabilityRegistry;
import top.egon.cola.component.gateway.engine.security.GatewaySecurityPolicyCompiler;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficPolicyCompiler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    private final GatewaySecurityPolicyCompiler securityPolicyCompiler;

    public EngineGatewayRuleCompiler() {
        this(GatewaySecurityCapabilityRegistry.empty());
    }

    public EngineGatewayRuleCompiler(
            GatewaySecurityCapabilityRegistry capabilities) {
        securityPolicyCompiler = new GatewaySecurityPolicyCompiler(
                capabilities
        );
    }

    public CompiledGatewayRules compile(GatewayRuleSnapshot snapshot) {
        GatewayRuleContent content = snapshot.content();
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
                                    operation.attributes(),
                                    snapshot.releaseId()
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
        List<GatewayRuntimePolicy> runtimePolicies = new ArrayList<>();
        runtimePolicies.addAll(content.providerPolicies());
        runtimePolicies.addAll(content.trafficPolicies());
        runtimePolicies.addAll(content.securityPolicies());
        runtimePolicies.addAll(content.corsPolicies());
        Map<String, GatewaySecurityPolicy> securityPolicies =
                securityPolicyCompiler.compile(
                        runtimePolicies,
                        policyProtocols
                );
        var providerPolicies = providerPolicyCompiler.compile(
                content.providerPolicies()
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
                });
        return new CompiledGatewayRules(
                snapshot,
                httpCompiler.compile(httpRoutes),
                rpcCompiler.compile(rpcRoutes),
                services,
                providerPolicies,
                trafficPolicyCompiler.compile(runtimePolicies),
                securityPolicies
        );
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
                Duration.ofMillis(timeoutMillis)
        );
    }

    private Map<String, String> routeMetadata(
            Map<String, String> attributes,
            String releaseId) {
        Map<String, String> metadata = new LinkedHashMap<>(attributes);
        metadata.put("releaseId", releaseId);
        return Map.copyOf(metadata);
    }

    private ProviderServiceKey serviceKey(
            GatewayProviderServiceRef service) {
        return new ProviderServiceKey(
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
