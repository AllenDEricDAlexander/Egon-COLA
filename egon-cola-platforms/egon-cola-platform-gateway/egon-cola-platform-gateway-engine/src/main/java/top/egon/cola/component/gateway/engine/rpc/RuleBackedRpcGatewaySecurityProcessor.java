package top.egon.cola.component.gateway.engine.rpc;

import io.grpc.Deadline;
import io.grpc.Metadata;
import reactor.core.publisher.Mono;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.context.GatewayContext;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.context.GatewayStage;
import top.egon.cola.component.gateway.core.exchange.DefaultGatewayResponse;
import top.egon.cola.component.gateway.core.exchange.EmptyGatewayBody;
import top.egon.cola.component.gateway.core.exchange.GatewayBody;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.exchange.GatewayHeaders;
import top.egon.cola.component.gateway.core.exchange.GatewayRequest;
import top.egon.cola.component.gateway.core.exchange.GatewayResponse;
import top.egon.cola.component.gateway.core.exchange.ImmutableGatewayHeaders;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.gateway.engine.rule.CompiledGatewayRules;
import top.egon.cola.component.gateway.engine.security.GatewaySecurityChain;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class RuleBackedRpcGatewaySecurityProcessor
        implements GatewayRpcSecurityProcessor {

    private final GatewaySecurityChain chain;

    private final Supplier<CompiledGatewayRules> rules;

    private final String engineNodeId;

    public RuleBackedRpcGatewaySecurityProcessor(
            GatewaySecurityChain chain,
            Supplier<CompiledGatewayRules> rules,
            String engineNodeId) {
        this.chain = Objects.requireNonNull(chain, "chain");
        this.rules = Objects.requireNonNull(rules, "rules");
        if (engineNodeId == null || engineNodeId.isBlank()) {
            throw new IllegalArgumentException("engineNodeId is required");
        }
        this.engineNodeId = engineNodeId.trim();
    }

    @Override
    public Mono<Outcome> authorize(
            RuntimeRpcRoute route,
            Metadata inboundMetadata,
            String traceId,
            Deadline inboundDeadline) {
        CompiledGatewayRules current = rules.get();
        List<GatewaySecurityPolicy> policies = route.policyRefs().stream()
                .map(current.securityPolicies()::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        GatewaySecurityPolicy::policyId
                ))
                .toList();
        if (policies.isEmpty()) {
            return Mono.just(Outcome.anonymous());
        }
        if (policies.size() != 1) {
            return Mono.error(new IllegalArgumentException(
                    "an operation must reference exactly one security policy"
            ));
        }
        GatewaySecurityPolicy policy = policies.getFirst();
        Instant startedAt = Instant.now();
        Instant deadline = startedAt.plus(effectiveTimeout(
                policy.providerTimeout(),
                inboundDeadline
        ));
        String requestId = UuidV7.simpleString();
        GatewayContext gatewayContext = new GatewayContext(
                requestId,
                traceId,
                null,
                null,
                AccessZone.INTERNAL,
                current.snapshot().content().gatewayGroupId(),
                engineNodeId,
                route.operationId(),
                route.routeId(),
                current.snapshot().releaseId(),
                GatewayPrincipal.anonymous(),
                null,
                deadline,
                startedAt,
                GatewayStage.ROUTE_MATCHED,
                List.of(),
                List.of()
        );
        GatewayAuthContext authContext = new GatewayAuthContext(
                AccessZone.INTERNAL,
                GatewayProtocol.RPC,
                route.operationId(),
                route.routeId(),
                policy.policyId(),
                route.fullMethodName(),
                null,
                java.util.Set.of(),
                GatewayPrincipal.anonymous(),
                "rpc-internal",
                traceId,
                requestId,
                deadline,
                current.snapshot().releaseId(),
                securityAttributes(route)
        );
        GatewayExchange exchange = new RpcExchange(
                new RpcRequest(
                        requestId,
                        traceId,
                        new ImmutableGatewayHeaders(headers(inboundMetadata))
                ),
                gatewayContext
        );
        return chain.execute(
                        exchange,
                        authContext,
                        policy,
                        GatewayProtocol.RPC
                )
                .map(result -> new Outcome(
                        result.trustedIdentity(),
                        result.fieldsToRemove()
                ));
    }

    static Map<String, String> securityAttributes(RuntimeRpcRoute route) {
        ProviderServiceKey target = route.targetService();
        return Map.of(
                "idp.biz-code", target.bizCode(),
                "idp.app-code", target.appCode(),
                "idp.env", target.env()
        );
    }

    private Duration effectiveTimeout(
            Duration policyTimeout,
            Deadline inboundDeadline) {
        if (inboundDeadline == null) {
            return policyTimeout;
        }
        Duration inbound = Duration.ofNanos(Math.max(
                1,
                inboundDeadline.timeRemaining(TimeUnit.NANOSECONDS)
        ));
        return inbound.compareTo(policyTimeout) < 0
                ? inbound
                : policyTimeout;
    }

    private Map<String, List<String>> headers(Metadata metadata) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String name : metadata.keys()) {
            if (name.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                continue;
            }
            String value = metadata.get(Metadata.Key.of(
                    name,
                    Metadata.ASCII_STRING_MARSHALLER
            ));
            if (value != null) {
                result.computeIfAbsent(
                        name,
                        ignored -> new java.util.ArrayList<>()
                ).add(value);
            }
        }
        return result;
    }

    private record RpcRequest(
            String requestId,
            String traceId,
            GatewayHeaders headers
    ) implements GatewayRequest {

        @Override
        public GatewayProtocol protocol() {
            return GatewayProtocol.RPC;
        }

        @Override
        public AccessZone accessZone() {
            return AccessZone.INTERNAL;
        }

        @Override
        public GatewayBody body() {
            return EmptyGatewayBody.INSTANCE;
        }
    }

    private record RpcExchange(
            GatewayRequest request,
            GatewayContext context
    ) implements GatewayExchange {

        @Override
        public GatewayResponse response() {
            return DefaultGatewayResponse.success(EmptyGatewayBody.INSTANCE);
        }
    }
}
