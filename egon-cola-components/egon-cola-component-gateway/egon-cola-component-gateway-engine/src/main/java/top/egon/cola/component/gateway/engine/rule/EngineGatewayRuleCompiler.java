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
import top.egon.cola.component.gateway.engine.rpc.RpcMethodIndex;
import top.egon.cola.component.gateway.engine.rpc.RpcMethodIndexCompiler;
import top.egon.cola.component.gateway.engine.rpc.RuntimeRpcRoute;
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
                            Map.of()
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
        return new CompiledGatewayRules(
                snapshot,
                httpCompiler.compile(httpRoutes),
                rpcCompiler.compile(rpcRoutes),
                services,
                trafficPolicyCompiler.compile(content.trafficPolicies())
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
