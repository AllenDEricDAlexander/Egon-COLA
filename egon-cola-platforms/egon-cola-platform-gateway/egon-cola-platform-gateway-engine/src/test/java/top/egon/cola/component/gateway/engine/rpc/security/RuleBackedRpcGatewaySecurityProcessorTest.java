package top.egon.cola.component.gateway.engine.rpc.security;

import top.egon.cola.component.gateway.engine.rpc.service.RpcMethodIndex;

import top.egon.cola.component.gateway.engine.rpc.domain.RuntimeRpcRoute;

import io.grpc.Metadata;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;
import top.egon.cola.component.gateway.core.route.HttpRouteCompiler;
import top.egon.cola.component.gateway.core.security.AuthenticationDecision;
import top.egon.cola.component.gateway.core.security.AuthenticationMode;
import top.egon.cola.component.gateway.core.security.AuthorizationDecision;
import top.egon.cola.component.gateway.core.security.AuthorizationDecisionMode;
import top.egon.cola.component.gateway.core.security.CredentialExtractionResult;
import top.egon.cola.component.gateway.core.security.CredentialForwardingMode;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayAuthenticationProvider;
import top.egon.cola.component.gateway.core.security.GatewayAuthorizationProvider;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.component.gateway.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.gateway.core.security.SecurityFailureMode;
import top.egon.cola.component.gateway.engine.rule.domain.CompiledGatewayRules;
import top.egon.cola.component.gateway.engine.common.security.service.GatewaySecurityCapabilityRegistry;
import top.egon.cola.component.gateway.engine.common.security.service.GatewaySecurityChain;
import top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class RuleBackedRpcGatewaySecurityProcessorTest {

    private static final String TOKEN = "verified.header.payload.signature";

    @Test
    void returnsOnlyVerifiedForwardingCredential() {
        GatewayCredential verified = new GatewayCredential(
                "bearer",
                TOKEN,
                Map.of()
        );
        AtomicReference<GatewayAuthContext> authorization =
                new AtomicReference<>();
        RuleBackedRpcGatewaySecurityProcessor processor = processor(
                verified,
                authorization
        );
        Metadata inbound = new Metadata();
        inbound.put(
                RpcMetadataKeys.AUTHORIZATION,
                "Bearer forged-inbound-token"
        );

        GatewayRpcSecurityProcessor.Outcome outcome = processor.authorize(
                route(Set.of("security")),
                inbound,
                "00000000000000000000000000000001",
                null
        ).block();

        assertSame(verified, outcome.forwardingCredential());
        assertEquals(TOKEN, outcome.forwardingCredential().tokenReference());
        assertEquals(Set.of("authorization"), outcome.fieldsToRemove());
        assertEquals(Map.of(
                "idp.biz-code", "finance",
                "idp.app-code", "billing",
                "idp.env", "prod"
        ), authorization.get().attributes());
    }

    @Test
    void anonymousOutcomeNeverUsesRawInboundAuthorization() {
        Metadata inbound = new Metadata();
        inbound.put(
                RpcMetadataKeys.AUTHORIZATION,
                "Bearer forged-inbound-token"
        );
        RuleBackedRpcGatewaySecurityProcessor processor = processor(
                new GatewayCredential("bearer", TOKEN, Map.of()),
                new AtomicReference<>()
        );

        GatewayRpcSecurityProcessor.Outcome outcome = processor.authorize(
                route(Set.of()),
                inbound,
                "00000000000000000000000000000001",
                null
        ).block();

        assertNull(outcome.forwardingCredential());
    }

    @Test
    void bindsIdpResourceScopeToResolvedRpcTarget() {
        Map<String, String> attributes =
                RuleBackedRpcGatewaySecurityProcessor.securityAttributes(
                        route(Set.of())
                );

        assertEquals(Map.of(
                "idp.biz-code", "finance",
                "idp.app-code", "billing",
                "idp.env", "prod"
        ), attributes);
    }

    private RuleBackedRpcGatewaySecurityProcessor processor(
            GatewayCredential credential,
            AtomicReference<GatewayAuthContext> authorization
    ) {
        CredentialExtractionResult extraction =
                new CredentialExtractionResult(
                        List.of(credential),
                        Set.of("authorization"),
                        null
                );
        GatewayCredentialExtractor extractor =
                new GatewayCredentialExtractor() {
                    @Override
                    public String extractorId() {
                        return "extractor";
                    }

                    @Override
                    public String credentialType() {
                        return "bearer";
                    }

                    @Override
                    public org.reactivestreams.Publisher
                            <CredentialExtractionResult> extract(
                                    GatewayExchange exchange,
                                    GatewaySecurityPolicy policy
                            ) {
                        return Mono.just(extraction);
                    }
                };
        GatewayAuthenticationProvider authentication =
                new GatewayAuthenticationProvider() {
                    @Override
                    public String providerId() {
                        return "authentication";
                    }

                    @Override
                    public Set<String> supportedCredentialTypes() {
                        return Set.of("bearer");
                    }

                    @Override
                    public org.reactivestreams.Publisher
                            <AuthenticationDecision> authenticate(
                                    GatewayAuthContext context,
                                    GatewayCredential current
                            ) {
                        return Mono.just(AuthenticationDecision.allow(
                                new GatewayPrincipal(
                                        "user-1",
                                        "USER",
                                        "tenant-1",
                                        null,
                                        true,
                                        Map.of()
                                )
                        ));
                    }
                };
        GatewayAuthorizationProvider authorizer =
                new GatewayAuthorizationProvider() {
                    @Override
                    public String providerId() {
                        return "authorization";
                    }

                    @Override
                    public org.reactivestreams.Publisher
                            <AuthorizationDecision> authorize(
                                    GatewayAuthContext context
                            ) {
                        authorization.set(context);
                        return Mono.just(AuthorizationDecision.allow());
                    }
                };
        GatewaySecurityChain chain = new GatewaySecurityChain(
                new GatewaySecurityCapabilityRegistry(
                        List.of(extractor),
                        List.of(authentication),
                        List.of(authorizer),
                        List.of()
                )
        );
        return new RuleBackedRpcGatewaySecurityProcessor(
                chain,
                this::rules,
                "engine-1"
        );
    }

    private CompiledGatewayRules rules() {
        GatewayRuleContent content = new GatewayRuleContent(
                "group",
                "group",
                "prod",
                "default",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        GatewaySecurityPolicy policy = new GatewaySecurityPolicy(
                "security",
                AuthenticationMode.REQUIRED,
                List.of("extractor"),
                List.of("authentication"),
                List.of("authorization"),
                AuthorizationDecisionMode.ALL_ALLOW,
                null,
                Duration.ofSeconds(1),
                SecurityFailureMode.FAIL_CLOSED,
                CredentialForwardingMode.ORIGINAL_BEARER
        );
        return new CompiledGatewayRules(
                new GatewayRuleSnapshot(
                        "v1",
                        "release-1",
                        Instant.EPOCH,
                        "content",
                        "artifact",
                        content
                ),
                new HttpRouteCompiler().compile(List.of()),
                RpcMethodIndex.empty(),
                Set.of(),
                Map.of(),
                Map.of(),
                Map.of("security", policy),
                Map.of()
        );
    }

    private RuntimeRpcRoute route(Set<String> policyRefs) {
        return new RuntimeRpcRoute(
                "route-1",
                "invoice.query",
                "billing.Invoice/Query",
                new ProviderServiceKey(
                        "finance",
                        "billing",
                        "prod",
                        "default",
                        ProviderProtocolType.RPC,
                        "billing.Invoice",
                        "default",
                        "v1",
                        "grpc"
                ),
                "QueryRequest",
                "QueryResponse",
                "sha256",
                policyRefs,
                GatewayResponseMode.TRANSPARENT,
                true,
                Duration.ofSeconds(3)
        );
    }
}
