package top.egon.cola.component.gateway.engine.http;

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
import top.egon.cola.component.gateway.core.http.NormalizedHttpRequest;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.route.HttpRouteMatch;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.gateway.engine.rule.CompiledGatewayRules;
import top.egon.cola.component.gateway.engine.security.GatewaySecurityChain;
import top.egon.cola.component.gateway.engine.security.TrustedClientAddressResolver;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class RuleBackedHttpGatewaySecurityProcessor
        implements GatewayHttpSecurityProcessor {

    private final GatewaySecurityChain chain;

    private final Supplier<CompiledGatewayRules> rules;

    private final TrustedClientAddressResolver clientAddressResolver;

    private final String engineNodeId;

    public RuleBackedHttpGatewaySecurityProcessor(
            GatewaySecurityChain chain,
            Supplier<CompiledGatewayRules> rules,
            TrustedClientAddressResolver clientAddressResolver,
            String engineNodeId) {
        this.chain = Objects.requireNonNull(chain, "chain");
        this.rules = Objects.requireNonNull(rules, "rules");
        this.clientAddressResolver = Objects.requireNonNull(
                clientAddressResolver,
                "clientAddressResolver"
        );
        if (engineNodeId == null || engineNodeId.isBlank()) {
            throw new IllegalArgumentException("engineNodeId is required");
        }
        this.engineNodeId = engineNodeId.trim();
    }

    @Override
    public Mono<Outcome> authorize(
            AccessZone accessZone,
            GatewayInboundHttpRequest request,
            NormalizedHttpRequest normalized,
            HttpRouteMatch route,
            String traceId) {
        CompiledGatewayRules current = rules.get();
        List<GatewaySecurityPolicy> policies = route.route().policyRefs()
                .stream()
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
        String requestId = UuidV7.simpleString();
        Instant startedAt = Instant.now();
        Instant deadline = startedAt.plus(policy.providerTimeout());
        String remoteAddress = clientAddressResolver.resolve(
                request.remoteAddress(),
                request.headers()
        ).getHostAddress();
        GatewayContext gatewayContext = new GatewayContext(
                requestId,
                traceId,
                null,
                null,
                accessZone,
                route.route().gatewayGroupId(),
                engineNodeId,
                route.route().operationId(),
                route.route().routeId(),
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
                accessZone,
                GatewayProtocol.HTTP,
                route.route().operationId(),
                route.route().routeId(),
                policy.policyId(),
                normalized.normalizedPath(),
                normalized.method(),
                java.util.Set.of(),
                GatewayPrincipal.anonymous(),
                remoteAddress,
                traceId,
                requestId,
                deadline,
                current.snapshot().releaseId(),
                securityAttributes(route)
        );
        GatewayExchange exchange = new HttpExchange(
                new HttpRequest(
                        requestId,
                        traceId,
                        accessZone,
                        new ImmutableGatewayHeaders(normalized.headers()),
                        contentLength(normalized.headers())
                ),
                gatewayContext
        );
        return chain.execute(
                        exchange,
                        authContext,
                        policy,
                        GatewayProtocol.HTTP
                )
                .map(result -> new Outcome(
                        result.trustedIdentity(),
                        result.fieldsToRemove(),
                        result.forwardingCredential()
                ));
    }

    static java.util.Map<String, String> securityAttributes(HttpRouteMatch route) {
        java.util.Map<String, String> metadata = route.route().metadata();
        java.util.Map<String, String> attributes = new java.util.LinkedHashMap<>();
        ProviderServiceKey upstream = route.route().upstream();
        attributes.put("idp.biz-code", upstream.bizCode());
        attributes.put("idp.app-code", upstream.appCode());
        attributes.put("idp.env", upstream.env());
        copy(metadata, attributes, "applicationCode", "rbac3.application-code");
        copy(metadata, attributes, "definitionSetId", "rbac3.definition-set-id");
        if (!attributes.containsKey("rbac3.definition-set-id")) {
            copy(metadata, attributes, "gateway.definition-set-id",
                    "rbac3.definition-set-id");
        }
        copy(metadata, attributes, "mappingVersion", "rbac3.mapping-version");
        if (!attributes.containsKey("rbac3.mapping-version")) {
            copy(metadata, attributes, "publishedVersion", "rbac3.mapping-version");
        }
        if (!attributes.containsKey("rbac3.mapping-version")) {
            copy(metadata, attributes, "definitionVersion", "rbac3.mapping-version");
        }
        return java.util.Map.copyOf(attributes);
    }

    private static void copy(
            java.util.Map<String, String> source,
            java.util.Map<String, String> target,
            String sourceName,
            String targetName
    ) {
        String value = source.get(sourceName);
        if (value != null && !value.isBlank()) {
            target.put(targetName, value.trim());
        }
    }

    private long contentLength(
            java.util.Map<String, List<String>> headers) {
        List<String> values = headers.getOrDefault(
                "content-length",
                List.of()
        );
        if (values.isEmpty()) {
            return -1;
        }
        try {
            return Long.parseLong(values.getFirst());
        } catch (NumberFormatException invalid) {
            return -1;
        }
    }

    private record HttpRequest(
            String requestId,
            String traceId,
            AccessZone accessZone,
            GatewayHeaders headers,
            long contentLength
    ) implements GatewayRequest {

        @Override
        public GatewayProtocol protocol() {
            return GatewayProtocol.HTTP;
        }

        @Override
        public GatewayBody body() {
            return new GatewayBody() {
                @Override
                public long contentLength() {
                    return contentLength;
                }

                @Override
                public boolean replayable() {
                    return false;
                }
            };
        }
    }

    private record HttpExchange(
            GatewayRequest request,
            GatewayContext context
    ) implements GatewayExchange {

        @Override
        public GatewayResponse response() {
            return DefaultGatewayResponse.success(EmptyGatewayBody.INSTANCE);
        }
    }
}
