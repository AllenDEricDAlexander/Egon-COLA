package top.egon.cola.component.yuheng.runtime.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import top.egon.cola.component.yuheng.contract.protocol.AccessZone;
import top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.yuheng.core.context.GatewayPrincipal;
import top.egon.cola.component.yuheng.core.exchange.GatewayExchange;
import top.egon.cola.component.yuheng.core.security.AuthenticationDecision;
import top.egon.cola.component.yuheng.core.security.AuthenticationMode;
import top.egon.cola.component.yuheng.core.security.AuthorizationDecision;
import top.egon.cola.component.yuheng.core.security.AuthorizationDecisionMode;
import top.egon.cola.component.yuheng.core.security.CredentialExtractionResult;
import top.egon.cola.component.yuheng.core.security.CredentialForwardingMode;
import top.egon.cola.component.yuheng.core.security.GatewayAuthContext;
import top.egon.cola.component.yuheng.core.security.GatewayAuthenticationProvider;
import top.egon.cola.component.yuheng.core.security.GatewayAuthorizationProvider;
import top.egon.cola.component.yuheng.core.security.GatewayCredential;
import top.egon.cola.component.yuheng.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.yuheng.core.security.GatewayRouteSecurityType;
import top.egon.cola.component.yuheng.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.yuheng.core.security.SecurityFailureMode;
import top.egon.cola.component.yuheng.runtime.security.domain.GatewaySecurityException;

/** Exercises the real registry/compiler/chain; the scope port computes against actual immutable context values. */
class GatewayMandatoryScopeContractTest {
    private static final String REQUIRED = "orders:read";

    @Test
    void mandatoryDenyRunsBeforeOrdinaryAnyOrAllAndUsesFinalPolicyScopes() {
        for (var mode : AuthorizationDecisionMode.values()) {
            var calls = new AtomicInteger();
            var registry = registry(Set.of("orders:other"), calls, false, true);
            var policy = policy(mode, GatewayRouteSecurityType.BUSINESS_PROTECTED, Set.of(REQUIRED));
            StepVerifier.create(new GatewaySecurityChain(registry).execute(exchange(), context(), policy, GatewayProtocol.HTTP))
                    .expectErrorSatisfies(error -> {
                        var failure = (GatewaySecurityException) error;
                        assertEquals("YUHENG_AUTHORIZATION_DENIED", failure.code());
                        assertEquals(403, failure.httpStatus());
                    }).verify();
            assertEquals(0, calls.get());
        }
    }

    @Test
    void identityProtectedOrdinaryListStaysEmptyButMandatoryScopeStillRuns() {
        var calls = new AtomicInteger();
        var policy = policy(AuthorizationDecisionMode.ALL_ALLOW, GatewayRouteSecurityType.IDENTITY_PROTECTED, Set.of(REQUIRED));
        assertTrue(policy.authorizationProviderIds().isEmpty());
        StepVerifier.create(new GatewaySecurityChain(registry(Set.of(REQUIRED), calls, false, true))
                        .execute(exchange(), context(), policy, GatewayProtocol.HTTP))
                .assertNext(result -> {
                    assertEquals(Set.of(REQUIRED), result.context().requiredOAuthScopes());
                    assertEquals("operator", result.context().principal().principalId());
                }).verifyComplete();
        assertEquals(0, calls.get());
        StepVerifier.create(new GatewaySecurityChain(registry(Set.of(), calls, false, true))
                        .execute(exchange(), context(), policy, GatewayProtocol.HTTP))
                .expectError(GatewaySecurityException.class).verify();
    }

    @Test
    void mandatoryErrorDoesNotRunAnOrdinaryProvider() {
        var calls = new AtomicInteger();
        StepVerifier.create(new GatewaySecurityChain(registry(Set.of(REQUIRED), calls, true, true)).execute(exchange(), context(),
                        policy(AuthorizationDecisionMode.ANY_ALLOW, GatewayRouteSecurityType.BUSINESS_PROTECTED, Set.of(REQUIRED)), GatewayProtocol.HTTP))
                .expectErrorSatisfies(error -> assertEquals(503, ((GatewaySecurityException) error).httpStatus())).verify();
        assertEquals(0, calls.get());
    }

    @Test
    void missingOrNonMandatoryDependencyAndMissingPublishedScopeCannotCompile() {
        var configured = registry(Set.of(REQUIRED), new AtomicInteger(), false, true);
        var source = new GatewayRuntimePolicy("security", "SECURITY", "ROUTE", Map.of(
                "authenticationMode", "REQUIRED", "credentialExtractorIds", List.of("extractor"),
                "authenticationProviderIds", List.of("auth"), "authorizationProviderIds", List.of("ordinary"),
                "routeSecurityType", "BUSINESS_PROTECTED"));
        assertThrows(IllegalArgumentException.class, () -> new GatewaySecurityPolicyCompiler(configured).compile(List.of(source), Map.of()));
        var absent = new GatewaySecurityCapabilityRegistry(List.of(extractor()), List.of(authenticator(Set.of(REQUIRED))), List.of(), List.of());
        assertThrows(IllegalArgumentException.class, () -> absent.validate(
                policy(AuthorizationDecisionMode.ALL_ALLOW, GatewayRouteSecurityType.IDENTITY_PROTECTED, Set.of(REQUIRED)), Set.of(GatewayProtocol.HTTP)));
        assertThrows(IllegalArgumentException.class, () -> registry(Set.of(REQUIRED), new AtomicInteger(), false, false).validate(
                policy(AuthorizationDecisionMode.ALL_ALLOW, GatewayRouteSecurityType.IDENTITY_PROTECTED, Set.of(REQUIRED)), Set.of(GatewayProtocol.HTTP)));
    }

    @Test
    void publishedScopeArrayMustBeUniqueAsciiBusinessValuesAndSurviveCompilation() {
        var registry = registry(Set.of(REQUIRED), new AtomicInteger(), false, true);
        var compiler = new GatewaySecurityPolicyCompiler(registry);
        for (Object value : List.of(List.of("openid"), List.of("orders:read", "orders:read"), List.of("读取"), "orders:read", List.of(1))) {
            assertThrows(IllegalArgumentException.class, () -> compiler.compile(List.of(source(value)), Map.of()));
        }
        var compiled = compiler.compile(List.of(source(List.of(REQUIRED))), Map.of()).get("security");
        assertEquals(Set.of(REQUIRED), compiled.requiredOAuthScopes());
    }

    private GatewayRuntimePolicy source(Object scopes) {
        return new GatewayRuntimePolicy("security", "SECURITY", "ROUTE", Map.of(
                "authenticationMode", "REQUIRED", "credentialExtractorIds", List.of("extractor"), "authenticationProviderIds", List.of("auth"),
                "authorizationProviderIds", List.of("ordinary"), "routeSecurityType", "BUSINESS_PROTECTED", "requiredOAuthScopes", scopes));
    }

    private GatewaySecurityCapabilityRegistry registry(Set<String> scopes, AtomicInteger ordinaryCalls, boolean unavailable, boolean mandatory) {
        var scope = new GatewayAuthorizationProvider() {
            @Override public String providerId() { return "scope"; }
            @Override public boolean mandatory() { return mandatory; }
            @Override public Publisher<AuthorizationDecision> authorize(GatewayAuthContext context) {
                if (unavailable) { return Mono.error(new IllegalStateException("controlled state outage")); }
                @SuppressWarnings("unchecked") Set<String> verified = (Set<String>) context.principal().trustedAuthenticationEvidence();
                return Mono.just(!context.requiredOAuthScopes().isEmpty() && verified.containsAll(context.requiredOAuthScopes())
                        ? AuthorizationDecision.allow() : AuthorizationDecision.deny("SCOPE_MISSING"));
            }
        };
        var ordinary = new GatewayAuthorizationProvider() {
            @Override public String providerId() { return "ordinary"; }
            @Override public Publisher<AuthorizationDecision> authorize(GatewayAuthContext context) {
                ordinaryCalls.incrementAndGet(); return Mono.just(AuthorizationDecision.allow());
            }
        };
        return new GatewaySecurityCapabilityRegistry(List.of(extractor()), List.of(authenticator(scopes)), List.of(scope, ordinary), List.of());
    }

    private GatewayAuthenticationProvider authenticator(Set<String> scopes) {
        return new GatewayAuthenticationProvider() {
            @Override public String providerId() { return "auth"; }
            @Override public Set<String> supportedCredentialTypes() { return Set.of("bearer"); }
            @Override public Set<String> requiredAuthorizationProviderIds() { return Set.of("scope"); }
            @Override public Publisher<AuthenticationDecision> authenticate(GatewayAuthContext context, GatewayCredential credential) {
                return Mono.just(AuthenticationDecision.allow(new GatewayPrincipal("operator", "USER", "1001", null, true, Map.of(), Set.copyOf(scopes))));
            }
        };
    }

    private GatewayCredentialExtractor extractor() {
        return new GatewayCredentialExtractor() {
            @Override public String extractorId() { return "extractor"; }
            @Override public String credentialType() { return "bearer"; }
            @Override public Publisher<CredentialExtractionResult> extract(GatewayExchange exchange, GatewaySecurityPolicy policy) {
                return Mono.just(new CredentialExtractionResult(List.of(new GatewayCredential("bearer", "controlled", Map.of())), Set.of(), null));
            }
        };
    }

    private GatewaySecurityPolicy policy(AuthorizationDecisionMode mode, GatewayRouteSecurityType type, Set<String> scopes) {
        return new GatewaySecurityPolicy("security", AuthenticationMode.REQUIRED, List.of("extractor"), List.of("auth"),
                type == GatewayRouteSecurityType.IDENTITY_PROTECTED ? List.of() : List.of("ordinary"), mode, null, Duration.ofSeconds(5),
                SecurityFailureMode.FAIL_CLOSED, CredentialForwardingMode.NONE, type, null, scopes);
    }

    private GatewayAuthContext context() {
        return new GatewayAuthContext(AccessZone.PUBLIC, GatewayProtocol.HTTP, "orders.read", "orders", "security", "/orders", "GET",
                Set.of(), GatewayPrincipal.anonymous(), "127.0.0.1", "trace", "request", Instant.now().plusSeconds(30), "release", Map.of(),
                Set.of("caller:forged"));
    }

    private GatewayExchange exchange() {
        return new GatewayExchange() {
            @Override public top.egon.cola.component.yuheng.core.exchange.GatewayRequest request() { return null; }
            @Override public top.egon.cola.component.yuheng.core.context.GatewayContext context() { return null; }
            @Override public top.egon.cola.component.yuheng.core.exchange.GatewayResponse response() { return null; }
        };
    }
}
