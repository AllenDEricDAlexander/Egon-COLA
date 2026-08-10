package top.egon.cola.platform.rbac3.admin.authorization;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.authorization.application.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3ServicePrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.DataScopeDecision;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.contract.authorization.FieldAccessLevel;
import top.egon.cola.platform.rbac3.contract.authorization.FieldPolicyDecision;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationDecisionServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");
    private static final String PERMISSION = "finance:payment:approve";

    @Test
    void returnsTypedFunctionDataAndFieldDecisionsFromOneExactSnapshot() {
        AuthorizationDecisionService service = service(snapshot(true), false);

        var result = service.decide(servicePrincipal("finance-web"), request(PERMISSION));

        assertThat(result.functionDecision().decision()).isEqualTo(Decision.ALLOW);
        assertThat(result.dataScopeDecision().scopeType()).isEqualTo("DEPT_TREE");
        assertThat(result.dataScopeDecision().allowedDeptIds()).containsExactly("31001");
        assertThat(result.fieldPolicyDecision().fields())
                .containsEntry("bankAccount", new FieldPolicyDecision.FieldAccess(
                        FieldAccessLevel.MASKED_READ, "BANK_ACCOUNT"))
                .containsEntry("amount", new FieldPolicyDecision.FieldAccess(
                        FieldAccessLevel.WRITE, null));
        assertThat(result.snapshotChecksum()).isEqualTo("sha256:snapshot-1");
    }

    @Test
    void functionDenySuppressesDataAndFieldDetails() {
        AuthorizationDecisionService service = service(snapshot(true), false);

        var result = service.decide(
                servicePrincipal("finance-web"), request("finance:payment:delete"));

        assertThat(result.functionDecision().decision()).isEqualTo(Decision.DENY);
        assertThat(result.functionDecision().reasonCode()).isEqualTo("PERMISSION_DENIED");
        assertThat(result.dataScopeDecision()).isNull();
        assertThat(result.fieldPolicyDecision()).isNull();
    }

    @Test
    void rejectsApplicationBindingAndEveryStaleTokenVersion() {
        AuthorizationDecisionService service = service(snapshot(true), false);

        assertThatThrownBy(() -> service.decide(
                servicePrincipal("inventory-web"), request(PERMISSION)))
                .isInstanceOfSatisfying(Rbac3RuleViolation.class,
                        error -> assertThat(error.reasonCode())
                                .isEqualTo("APPLICATION_BINDING_DENIED"));
        assertVersionFailure(service, new AuthorizationDecisionService.TokenVersions(42, 2, 18),
                "AUTH_VERSION_MISMATCH");
        assertVersionFailure(service, new AuthorizationDecisionService.TokenVersions(43, 1, 18),
                "SESSION_VERSION_MISMATCH");
        assertVersionFailure(service, new AuthorizationDecisionService.TokenVersions(43, 2, 17),
                "POLICY_VERSION_MISMATCH");
    }

    @Test
    void verifiesFenceFailClosedAndDoesNotExposeAnotherApplicationsSnapshot() {
        AuthorizationDecisionService fencedService = service(snapshot(true), true);

        assertThat(fencedService.verifyFence(
                servicePrincipal("finance-web"), "tenant-1", "session-1").decision())
                .isEqualTo(Decision.DENY);
        assertThatThrownBy(() -> fencedService.decide(
                servicePrincipal("finance-web"), request(PERMISSION)))
                .isInstanceOfSatisfying(Rbac3RuleViolation.class,
                        error -> assertThat(error.reasonCode())
                                .isEqualTo("AUTH_PROPAGATION_PENDING"));
        assertThatThrownBy(() -> fencedService.snapshot(
                servicePrincipal("finance-web"), "tenant-1", "session-1"))
                .isInstanceOfSatisfying(Rbac3RuleViolation.class,
                        error -> assertThat(error.reasonCode())
                                .isEqualTo("AUTH_PROPAGATION_PENDING"));

        AuthorizationDecisionService service = service(snapshot(true), false);
        assertThat(service.snapshot(
                servicePrincipal("finance-web"), "tenant-1", "session-1")
                .appContexts()).extracting(AppAuthorizationContext::applicationCode)
                .containsExactly("finance-web");
        assertThatThrownBy(() -> service.snapshot(
                servicePrincipal("inventory-web"), "tenant-1", "session-1"))
                .isInstanceOfSatisfying(Rbac3RuleViolation.class,
                        error -> assertThat(error.reasonCode())
                                .isEqualTo("APPLICATION_BINDING_DENIED"));
    }

    @Test
    void allowsOnlyAnActiveIdentityWithTheTargetApplicationEntryPermission() {
        AuthorizationDecisionService service = resourceAccessService(
                "alice-sub", "tenant-1", snapshot(true), false);

        var result = service.decideResourceAccess(
                servicePrincipal("idp-admin", "*"), resourceAccessRequest("finance-web"));

        assertThat(result.decision()).isEqualTo(Decision.ALLOW);
        assertThat(result.reasonCode()).isEqualTo("ALLOW");
        assertThat(result.authVersion()).isEqualTo(43L);
        assertThat(result.sessionVersion()).isEqualTo(2L);
        assertThat(result.policyVersion()).isEqualTo(18L);
        assertThat(result.decidedAt()).isEqualTo(NOW);
    }

    @Test
    void deniesMissingEntryPermissionWithoutReturningAuthorizationDetails() {
        AuthorizationDecisionService service = resourceAccessService(
                "alice-sub", "tenant-1", snapshot(false), false);

        var result = service.decideResourceAccess(
                servicePrincipal("idp-admin", "*"), resourceAccessRequest("finance-web"));

        assertThat(result.decision()).isEqualTo(Decision.DENY);
        assertThat(result.reasonCode()).isEqualTo("ENTRY_PERMISSION_DENIED");
        assertThat(result.authVersion()).isEqualTo(43L);
        assertThat(result.sessionVersion()).isEqualTo(2L);
        assertThat(result.policyVersion()).isEqualTo(18L);
    }

    @Test
    void deniesInactiveOrMismatchedIdentitySessionAndAnUnknownApplication() {
        AuthorizationDecisionService inactive = new AuthorizationDecisionService(
                (tenantId, sessionId) -> {
                    throw new Rbac3RuleViolation("RESOURCE_NOT_FOUND");
                },
                (tenantId, sessionId) -> false,
                Clock.fixed(NOW, ZoneOffset.UTC));

        var inactiveResult = inactive.decideResourceAccess(
                servicePrincipal("idp-admin", "*"), resourceAccessRequest("finance-web"));

        assertThat(inactiveResult.decision()).isEqualTo(Decision.DENY);
        assertThat(inactiveResult.reasonCode()).isEqualTo("IDENTITY_SESSION_INACTIVE");
        assertThat(inactiveResult.authVersion()).isNull();
        assertThat(inactiveResult.sessionVersion()).isNull();
        assertThat(inactiveResult.policyVersion()).isNull();

        AuthorizationDecisionService wrongIdentity = resourceAccessService(
                "another-sub", "tenant-1", snapshot(true), false);
        assertThat(wrongIdentity.decideResourceAccess(
                servicePrincipal("idp-admin", "*"), resourceAccessRequest("finance-web"))
                .reasonCode()).isEqualTo("IDENTITY_SESSION_INACTIVE");

        AuthorizationDecisionService wrongTenant = resourceAccessService(
                "alice-sub", "tenant-2", snapshot(true), false);
        assertThat(wrongTenant.decideResourceAccess(
                servicePrincipal("idp-admin", "*"), resourceAccessRequest("finance-web"))
                .reasonCode()).isEqualTo("IDENTITY_SESSION_INACTIVE");

        AuthorizationDecisionService service = resourceAccessService(
                "alice-sub", "tenant-1", snapshot(true), false);
        var wrongApplication = service.decideResourceAccess(
                servicePrincipal("idp-admin", "*"), resourceAccessRequest("inventory-web"));
        assertThat(wrongApplication.decision()).isEqualTo(Decision.DENY);
        assertThat(wrongApplication.reasonCode()).isEqualTo("APPLICATION_BINDING_DENIED");
    }

    @Test
    void deniesFencedSessionAndRejectsAServiceOutsideTheRequestedTenant() {
        AuthorizationDecisionService fenced = resourceAccessService(
                "alice-sub", "tenant-1", snapshot(true), true);

        var result = fenced.decideResourceAccess(
                servicePrincipal("idp-admin", "*"), resourceAccessRequest("finance-web"));

        assertThat(result.decision()).isEqualTo(Decision.DENY);
        assertThat(result.reasonCode()).isEqualTo("AUTH_PROPAGATION_PENDING");
        assertThat(result.authVersion()).isNull();
        assertThatThrownBy(() -> fenced.decideResourceAccess(
                servicePrincipal("idp-admin", "tenant-2"),
                resourceAccessRequest("finance-web")))
                .isInstanceOfSatisfying(Rbac3RuleViolation.class,
                        error -> assertThat(error.reasonCode())
                                .isEqualTo("SERVICE_IDENTITY_DENIED"));
    }

    @Test
    void propagatesDecisionStoreFailureForFailClosedTransportMapping() {
        AuthorizationDecisionService unavailable = new AuthorizationDecisionService(
                (tenantId, sessionId) -> {
                    throw new IllegalStateException("decision store unavailable");
                },
                (tenantId, sessionId) -> false,
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> unavailable.decideResourceAccess(
                servicePrincipal("idp-admin", "*"), resourceAccessRequest("finance-web")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("decision store unavailable");
    }

    private void assertVersionFailure(
            AuthorizationDecisionService service,
            AuthorizationDecisionService.TokenVersions versions,
            String reasonCode) {
        AuthorizationDecisionService.DecisionRequest request = request(PERMISSION);
        var stale = new AuthorizationDecisionService.DecisionRequest(
                request.subject(), request.permissionCode(), request.resource(),
                request.requestedDecisions(), versions);
        assertThatThrownBy(() -> service.decide(servicePrincipal("finance-web"), stale))
                .isInstanceOfSatisfying(Rbac3RuleViolation.class,
                        error -> assertThat(error.reasonCode()).isEqualTo(reasonCode));
    }

    private AuthorizationDecisionService service(
            SessionAuthorizationSnapshot snapshot,
            boolean fenced) {
        return new AuthorizationDecisionService(
                (tenantId, sessionId) -> new AuthorizationDecisionService.SnapshotRecord(
                        tenantId, "user-1", snapshot),
                (tenantId, sessionId) -> fenced,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private AuthorizationDecisionService resourceAccessService(
            String identitySub,
            String recordTenantId,
            SessionAuthorizationSnapshot snapshot,
            boolean fenced) {
        return new AuthorizationDecisionService(
                (tenantId, sessionId) -> new AuthorizationDecisionService.SnapshotRecord(
                        recordTenantId, identitySub, "user-1", snapshot),
                (tenantId, sessionId) -> fenced,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private AuthorizationDecisionService.ResourceAccessRequest resourceAccessRequest(
            String applicationCode) {
        return new AuthorizationDecisionService.ResourceAccessRequest(
                "alice-sub", "tenant-1", "session-1", applicationCode, PERMISSION);
    }

    private AuthorizationDecisionService.DecisionRequest request(String permission) {
        return new AuthorizationDecisionService.DecisionRequest(
                new AuthorizationDecisionService.Subject(
                        "tenant-1", "user-1", "session-1"),
                permission,
                new AuthorizationDecisionService.Resource(
                        "finance-web", "payment-approvals"),
                EnumSet.of(
                        AuthorizationDecisionService.DecisionType.FUNCTION,
                        AuthorizationDecisionService.DecisionType.DATA_SCOPE,
                        AuthorizationDecisionService.DecisionType.FIELD),
                new AuthorizationDecisionService.TokenVersions(43, 2, 18));
    }

    private CurrentRbac3ServicePrincipal servicePrincipal(String applicationCode) {
        return servicePrincipal(applicationCode, "tenant-1");
    }

    private CurrentRbac3ServicePrincipal servicePrincipal(
            String applicationCode,
            String tenantId) {
        return new CurrentRbac3ServicePrincipal(
                tenantId, "finance-service", applicationCode,
                "prod", "default", "credential-1",
                Set.of("service:authorization:decide", "service:authorization:snapshot"));
    }

    private SessionAuthorizationSnapshot snapshot(boolean includePermission) {
        DataScopeDecision dataScope = new DataScopeDecision(
                Decision.ALLOW, "ALLOW", "tenant-1", "user-1", PERMISSION,
                "DEPT_TREE", false, Set.of(), false, Set.of("31001"), true,
                Set.of(), false, null, "directory-8", 18, 43, 2, 18,
                List.of("data-rule-1"), NOW);
        FieldPolicyDecision fieldPolicy = new FieldPolicyDecision(
                Decision.ALLOW, "ALLOW", "tenant-1", "user-1", PERMISSION,
                "finance-web", "payment-approvals",
                Map.of(
                        "bankAccount", new FieldPolicyDecision.FieldAccess(
                                FieldAccessLevel.MASKED_READ, "BANK_ACCOUNT"),
                        "amount", new FieldPolicyDecision.FieldAccess(
                                FieldAccessLevel.WRITE, null)),
                43, 2, 18, List.of("field-rule-1"), NOW);
        AppAuthorizationContext app = new AppAuthorizationContext(
                "app-1", "finance-web", List.of("role-root-1"),
                List.of("assignment-1"), List.of("role-root-1", "role-child-1"),
                includePermission ? Set.of(PERMISSION) : Set.of(),
                Map.of(PERMISSION, dataScope),
                Map.of(PERMISSION + ":finance-web:payment-approvals", fieldPolicy),
                List.of(), "payment-list");
        return new SessionAuthorizationSnapshot(
                "session-1", 43, 2, 18, List.of(app),
                "sha256:snapshot-1", NOW.minusSeconds(5));
    }
}
