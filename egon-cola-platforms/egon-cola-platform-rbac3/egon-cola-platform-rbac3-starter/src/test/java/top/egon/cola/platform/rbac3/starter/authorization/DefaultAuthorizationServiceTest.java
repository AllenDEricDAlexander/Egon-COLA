package top.egon.cola.platform.rbac3.starter.authorization;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3TokenClaims;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.DataScopeDecision;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.contract.authorization.FieldAccessLevel;
import top.egon.cola.platform.rbac3.contract.authorization.FieldPolicyDecision;
import top.egon.cola.platform.rbac3.contract.authorization.PermissionRequest;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultAuthorizationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-30T08:00:00Z");

    @Test
    void allowsOnlyPermissionsAndTypedRulesFromTheVersionMatchedSnapshot() {
        DefaultAuthorizationService service = new DefaultAuthorizationService(
                () -> context(false),
                request -> AuthorizationService.OperationSodResult.allowed(),
                request -> AuthorizationService.FenceResult.allowed(NOW),
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertEquals(Decision.ALLOW,
                service.requirePermission(PermissionRequest.of("finance:payment:read")).decision());
        assertEquals(Decision.DENY,
                service.requirePermission(PermissionRequest.of("finance:payment:approve")).decision());
        assertEquals("DEPT_TREE",
                service.decideDataScope(new AuthorizationService.DataScopeRequest(
                        "finance:payment:read")).scopeType());
        assertEquals(FieldAccessLevel.MASKED_READ,
                service.decideFields(new AuthorizationService.FieldPolicyRequest(
                        "finance:payment:read", "finance", "payment"))
                        .fields().get("accountNo").level());
    }

    @Test
    void failsClosedWhenRuntimeIsFencedOrUnavailable() {
        DefaultAuthorizationService fenced = new DefaultAuthorizationService(
                () -> context(true),
                request -> AuthorizationService.OperationSodResult.allowed(),
                request -> AuthorizationService.FenceResult.allowed(NOW),
                Clock.fixed(NOW, ZoneOffset.UTC));
        DefaultAuthorizationService unavailable = new DefaultAuthorizationService(
                () -> {
                    throw new AuthorizationService.RuntimeUnavailableException(
                            "AUTHORIZATION_RUNTIME_UNAVAILABLE", claims());
                },
                request -> AuthorizationService.OperationSodResult.allowed(),
                request -> AuthorizationService.FenceResult.allowed(NOW),
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertEquals("AUTHORIZATION_FENCED",
                fenced.requirePermission(PermissionRequest.of("finance:payment:read"))
                        .reasonCode());
        assertEquals(Decision.INDETERMINATE,
                unavailable.requirePermission(PermissionRequest.of("finance:payment:read"))
                        .decision());
        assertEquals(Decision.INDETERMINATE,
                unavailable.decideDataScope(new AuthorizationService.DataScopeRequest(
                        "finance:payment:read")).decision());
    }

    private AuthorizationService.RuntimeAuthorizationContext context(boolean fenced) {
        return new AuthorizationService.RuntimeAuthorizationContext(
                claims(), snapshot(), fenced);
    }

    private Rbac3TokenClaims claims() {
        return new Rbac3TokenClaims(
                "rbac3", List.of("business"), "20001", "10001", "30001",
                1L, 2L, 3L, "jti-1", NOW.minusSeconds(30), NOW.minusSeconds(30),
                NOW.plusSeconds(300), "kid-1");
    }

    private SessionAuthorizationSnapshot snapshot() {
        return new SessionAuthorizationSnapshot(
                "30001", 1L, 2L, 3L,
                List.of(new AppAuthorizationContext(
                        "71001", "finance", List.of("50001"), List.of("60001"),
                        List.of("50001", "50002"), Set.of("finance:payment:read"),
                        Map.of("finance:payment:read", dataScope()),
                        Map.of("finance:payment:read:finance:payment", fieldPolicy()),
                        List.of(), "payment")),
                "sha256:snapshot", NOW);
    }

    private DataScopeDecision dataScope() {
        return new DataScopeDecision(
                Decision.ALLOW, "ALLOW", "10001", "20001", "finance:payment:read",
                "DEPT_TREE", false, Set.of(), false, Set.of("90001"), true,
                Set.of(), false, null, "5", 1L, 1L, 2L, 3L,
                List.of("rule-1"), NOW);
    }

    private FieldPolicyDecision fieldPolicy() {
        return new FieldPolicyDecision(
                Decision.ALLOW, "ALLOW", "10001", "20001", "finance:payment:read",
                "finance", "payment",
                Map.of("accountNo", new FieldPolicyDecision.FieldAccess(
                        FieldAccessLevel.MASKED_READ, "BANK_ACCOUNT")),
                1L, 2L, 3L, List.of("field-rule-1"), NOW);
    }
}
