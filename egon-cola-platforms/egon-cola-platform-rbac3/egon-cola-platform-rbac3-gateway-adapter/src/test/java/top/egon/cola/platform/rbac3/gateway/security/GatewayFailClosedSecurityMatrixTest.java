package top.egon.cola.platform.rbac3.gateway.security;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.component.gateway.core.security.SecurityDecision;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayFailClosedSecurityMatrixTest {

    private static final Instant NOW = Instant.parse("2026-07-30T08:00:00Z");

    @Test
    void tokenAndSessionVerificationFailuresDenyAuthentication() {
        List<Rbac3JwtSessionAuthenticationProvider> providers = List.of(
                new Rbac3JwtSessionAuthenticationProvider(
                        token -> { throw new IllegalArgumentException("invalid jwt"); },
                        claims -> { }),
                new Rbac3JwtSessionAuthenticationProvider(
                        token -> null,
                        claims -> { throw new IllegalStateException("runtime lost"); }));

        for (Rbac3JwtSessionAuthenticationProvider provider : providers) {
            var decision = Mono.from(provider.authenticate(
                    context(), new GatewayCredential("bearer", "secret", Map.of())))
                    .block();
            assertEquals(SecurityDecision.DENY, decision.decision());
            assertEquals("RBAC3_AUTHENTICATION_FAILED", decision.reason());
        }
    }

    @Test
    void mappingOrRuntimeFailuresNeverBecomeAuthorizationAllow() {
        List<Rbac3PermissionAuthorizationProvider> providers = List.of(
                new Rbac3PermissionAuthorizationProvider(
                        ignored -> { throw new IllegalStateException("redis lost"); }),
                new Rbac3PermissionAuthorizationProvider(
                        ignored -> { throw new IllegalArgumentException("mapping conflict"); }));

        for (Rbac3PermissionAuthorizationProvider provider : providers) {
            var decision = Mono.from(provider.authorize(context())).block();
            assertEquals(SecurityDecision.ERROR, decision.decision());
            assertEquals("RBAC3_AUTHORIZATION_RUNTIME_UNAVAILABLE",
                    decision.reason());
        }
    }

    @Test
    void malformedCredentialTypesAreRejectedBeforeVerification() {
        var provider = new Rbac3JwtSessionAuthenticationProvider(
                token -> { throw new AssertionError("must not verify"); },
                claims -> { throw new AssertionError("must not read session"); });

        var decision = Mono.from(provider.authenticate(
                context(), new GatewayCredential("api-key", "secret", Map.of())))
                .block();

        assertEquals(SecurityDecision.DENY, decision.decision());
        assertEquals("RBAC3_CREDENTIAL_TYPE_INVALID", decision.reason());
    }

    private static GatewayAuthContext context() {
        return new GatewayAuthContext(
                AccessZone.PUBLIC, GatewayProtocol.HTTP, "operation", "route",
                "policy", "/payments", "POST", Set.of("bearer"),
                new GatewayPrincipal(
                        "user", "USER", "tenant", null, true,
                        Map.of("rbac3.session-id", "session",
                                "rbac3.auth-version", "3",
                                "rbac3.session-version", "5",
                                "rbac3.policy-version", "7")),
                "127.0.0.1", "trace", "request", NOW.plusSeconds(5),
                "release");
    }
}
