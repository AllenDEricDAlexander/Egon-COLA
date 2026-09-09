package top.egon.cola.component.yuheng.runtime.security.service;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import top.egon.cola.component.yuheng.contract.protocol.AccessZone;
import top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol;
import top.egon.cola.component.yuheng.core.context.GatewayPrincipal;
import top.egon.cola.component.yuheng.core.exchange.DefaultGatewayResponse;
import top.egon.cola.component.yuheng.core.exchange.EmptyGatewayBody;
import top.egon.cola.component.yuheng.core.exchange.GatewayExchange;
import top.egon.cola.component.yuheng.core.exchange.GatewayRequest;
import top.egon.cola.component.yuheng.core.exchange.ImmutableGatewayHeaders;
import top.egon.cola.component.yuheng.core.security.AuthenticationDecision;
import top.egon.cola.component.yuheng.core.security.AuthenticationMode;
import top.egon.cola.component.yuheng.core.security.AuthorizationDecisionMode;
import top.egon.cola.component.yuheng.core.security.CredentialExtractionResult;
import top.egon.cola.component.yuheng.core.security.CredentialForwardingMode;
import top.egon.cola.component.yuheng.core.security.GatewayAuthContext;
import top.egon.cola.component.yuheng.core.security.GatewayAuthenticationProvider;
import top.egon.cola.component.yuheng.core.security.GatewayCredential;
import top.egon.cola.component.yuheng.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.yuheng.core.security.GatewayIdentityMapper;
import top.egon.cola.component.yuheng.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.yuheng.core.security.SecurityFailureMode;
import top.egon.cola.component.yuheng.core.security.TrustedIdentity;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class GatewayIdentityOnlySecurityTest {

    @Test
    void authenticatesAndForwardsOriginalBearerWithoutAuthorizationProvider() {
        GatewayCredential credential = new GatewayCredential(
                "bearer", "original-token", Map.of());
        GatewayCredentialExtractor extractor = new Extractor(credential);
        GatewayAuthenticationProvider authentication = new Authentication();
        GatewayIdentityMapper mapper = new Mapper();
        GatewaySecurityChain chain = new GatewaySecurityChain(
                new GatewaySecurityCapabilityRegistry(
                        List.of(extractor), List.of(authentication),
                        List.of(), List.of(mapper)));

        StepVerifier.create(chain.execute(
                        exchange(), context(), policy(), GatewayProtocol.HTTP))
                .assertNext(result -> {
                    assertEquals("identity-1",
                            result.context().principal().principalId());
                    assertSame(credential, result.forwardingCredential());
                    assertEquals("identity-1",
                            result.trustedIdentity().httpHeaders().get(
                                    "X-Egon-Identity-Sub"));
                })
                .verifyComplete();
    }

    private GatewaySecurityPolicy policy() {
        return new GatewaySecurityPolicy(
                "security", AuthenticationMode.REQUIRED, List.of("tianquan-shoubing-bearer"),
                List.of("tianquan-shoubing-jwt"), List.of(),
                AuthorizationDecisionMode.ALL_ALLOW, "tianquan-shoubing-identity",
                Duration.ofSeconds(1), SecurityFailureMode.FAIL_CLOSED,
                CredentialForwardingMode.ORIGINAL_BEARER);
    }

    private GatewayAuthContext context() {
        return new GatewayAuthContext(
                AccessZone.PUBLIC, GatewayProtocol.HTTP, "operation", "route",
                "security", "/orders", "GET", Set.of(),
                GatewayPrincipal.anonymous(), "127.0.0.1", "trace", "request",
                Instant.now().plusSeconds(5), "release");
    }

    private GatewayExchange exchange() {
        GatewayRequest request = new GatewayRequest() {
            public String requestId() { return "request"; }
            public String traceId() { return "trace"; }
            public GatewayProtocol protocol() { return GatewayProtocol.HTTP; }
            public AccessZone accessZone() { return AccessZone.PUBLIC; }
            public top.egon.cola.component.yuheng.core.exchange.GatewayHeaders headers() {
                return new ImmutableGatewayHeaders(Map.of(
                        "authorization", List.of("Bearer original-token")));
            }
            public top.egon.cola.component.yuheng.core.exchange.GatewayBody body() {
                return EmptyGatewayBody.INSTANCE;
            }
        };
        return new GatewayExchange() {
            public GatewayRequest request() { return request; }
            public top.egon.cola.component.yuheng.core.context.GatewayContext context() {
                return null;
            }
            public top.egon.cola.component.yuheng.core.exchange.GatewayResponse response() {
                return DefaultGatewayResponse.success(EmptyGatewayBody.INSTANCE);
            }
        };
    }

    private record Extractor(GatewayCredential credential)
            implements GatewayCredentialExtractor {
        public String extractorId() { return "tianquan-shoubing-bearer"; }
        public String credentialType() { return "bearer"; }
        public org.reactivestreams.Publisher<CredentialExtractionResult> extract(
                GatewayExchange exchange, GatewaySecurityPolicy policy) {
            return Mono.just(new CredentialExtractionResult(
                    List.of(credential), Set.of("authorization"), null));
        }
    }

    private static final class Authentication
            implements GatewayAuthenticationProvider {
        public String providerId() { return "tianquan-shoubing-jwt"; }
        public Set<String> supportedCredentialTypes() { return Set.of("bearer"); }
        public org.reactivestreams.Publisher<AuthenticationDecision> authenticate(
                GatewayAuthContext context, GatewayCredential credential) {
            return Mono.just(AuthenticationDecision.allow(new GatewayPrincipal(
                    "identity-1", "USER", "tenant-1", null, true, Map.of())));
        }
    }

    private static final class Mapper implements GatewayIdentityMapper {
        public String mapperId() { return "tianquan-shoubing-identity"; }
        public Set<GatewayProtocol> supportedProtocols() {
            return Set.of(GatewayProtocol.HTTP);
        }
        public TrustedIdentity map(GatewayAuthContext context) {
            return new TrustedIdentity(Map.of(
                    "X-Egon-Identity-Sub",
                    context.principal().principalId()), Map.of());
        }
    }
}
