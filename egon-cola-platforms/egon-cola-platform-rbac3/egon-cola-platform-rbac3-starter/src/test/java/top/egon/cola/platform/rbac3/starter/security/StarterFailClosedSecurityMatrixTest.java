package top.egon.cola.platform.rbac3.starter.security;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.contract.authorization.PermissionRequest;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.authorization.DefaultAuthorizationService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StarterFailClosedSecurityMatrixTest {

    private static final Instant NOW = Instant.parse("2026-07-30T08:00:00Z");

    @Test
    void fencedMissingAndVersionUnavailableFactsNeverAllow() {
        List<DefaultAuthorizationService> services = List.of(
                service(new AuthorizationService.RuntimeAuthorizationContext(
                        claims(), snapshot(Set.of("payment:read")), true)),
                service(new AuthorizationService.RuntimeAuthorizationContext(
                        claims(), snapshot(Set.of()), false)),
                unavailable("AUTHORIZATION_VERSION_MISMATCH"),
                unavailable("AUTHORIZATION_RUNTIME_UNAVAILABLE"));

        assertEquals(List.of(
                        Decision.DENY,
                        Decision.DENY,
                        Decision.INDETERMINATE,
                        Decision.INDETERMINATE),
                services.stream()
                        .map(value -> value.requirePermission(
                                PermissionRequest.of("payment:read")).decision())
                        .toList());
    }

    @Test
    void missingTypedDataAndFieldPoliciesRemainDenied() {
        DefaultAuthorizationService service = service(
                new AuthorizationService.RuntimeAuthorizationContext(
                        claims(), snapshot(Set.of("payment:read")), false));

        assertEquals(Decision.DENY, service.decideDataScope(
                new AuthorizationService.DataScopeRequest("payment:read")).decision());
        assertEquals(Decision.DENY, service.decideFields(
                new AuthorizationService.FieldPolicyRequest(
                        "payment:read", "finance", "payment")).decision());
        assertEquals(Map.of(), service.decideFields(
                new AuthorizationService.FieldPolicyRequest(
                        "payment:read", "finance", "payment")).fields());
    }

    private DefaultAuthorizationService unavailable(String reason) {
        return new DefaultAuthorizationService(
                () -> {
                    throw new AuthorizationService.RuntimeUnavailableException(
                            reason, claims());
                },
                request -> AuthorizationService.OperationSodResult.allowed(),
                request -> AuthorizationService.FenceResult.allowed(NOW),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private DefaultAuthorizationService service(
            AuthorizationService.RuntimeAuthorizationContext context) {
        return new DefaultAuthorizationService(
                () -> context,
                request -> AuthorizationService.OperationSodResult.allowed(),
                request -> AuthorizationService.FenceResult.allowed(NOW),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static IdentityPrincipal claims() {
        return new IdentityPrincipal(
                "identity", "tenant", "session", "finance-web", "jti",
                1L, Set.of("finance"), NOW.minusSeconds(30),
                NOW.plusSeconds(300));
    }

    private static SystemAuthorizationSnapshot snapshot(Set<String> permissions) {
        return new SystemAuthorizationSnapshot(
                "tenant", "identity", "user", "session", "finance",
                3L, 5L, 7L, List.of("root"), permissions, Map.of(), Map.of(),
                "sha256:snapshot", NOW, NOW.plusSeconds(300));
    }
}
