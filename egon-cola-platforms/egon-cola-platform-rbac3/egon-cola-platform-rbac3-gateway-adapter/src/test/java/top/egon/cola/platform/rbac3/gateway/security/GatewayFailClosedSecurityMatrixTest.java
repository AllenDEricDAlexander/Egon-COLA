package top.egon.cola.platform.rbac3.gateway.security;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.SecurityDecision;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayFailClosedSecurityMatrixTest {

    private static final Instant NOW = Instant.parse("2026-07-30T08:00:00Z");

    @Test
    void mappingOrRuntimeFailuresNeverBecomeAuthorizationAllow() {
        var providers = Set.of(
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

    private static GatewayAuthContext context() {
        return new GatewayAuthContext(
                AccessZone.PUBLIC, GatewayProtocol.HTTP, "operation", "route",
                "policy", "/payments", "POST", Set.of("bearer"),
                new GatewayPrincipal(
                        "user", "USER", "tenant", null, true, java.util.Map.of()),
                "127.0.0.1", "trace", "request", NOW.plusSeconds(5),
                "release");
    }
}
