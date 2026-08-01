package top.egon.cola.platform.rbac3.admin.assignment;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.assignment.application.AssignmentFacade;
import top.egon.cola.platform.rbac3.admin.management.application.ManagementPolicyFacade;
import top.egon.cola.platform.rbac3.admin.runtime.application.AuthorizationMutationCoordinator;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssignmentFacadeIT {

    @Test
    void assignmentUsesOnePolicyThenPersistsInsideMutationBoundary() {
        ManagementPolicyFacade management = mock(ManagementPolicyFacade.class);
        when(management.authorize(org.mockito.ArgumentMatchers.any()))
                .thenReturn("policy-1");
        AtomicBoolean saved = new AtomicBoolean();
        AssignmentFacade facade = new AssignmentFacade(
                management,
                request -> facts(),
                scope -> scope.action().get(),
                command -> {
                    saved.set(true);
                    return "assignment-1";
                },
                coordinator());

        var result = facade.assign(request());

        assertThat(saved).isTrue();
        assertThat(result.completed()).isTrue();
        assertThat(result.assignmentId()).isEqualTo("assignment-1");
    }

    @Test
    void selfAssignmentIsDeniedAfterOneCompleteManagementPolicyEvaluation() {
        ManagementPolicyFacade management = mock(ManagementPolicyFacade.class);
        when(management.authorize(org.mockito.ArgumentMatchers.any()))
                .thenReturn("policy-1");
        AssignmentFacade facade = new AssignmentFacade(
                management, request -> facts(), scope -> scope.action().get(),
                command -> "unexpected", coordinator());
        Instant now = Instant.parse("2026-07-30T08:00:00Z");
        var request = new AssignmentFacade.AssignRequest(
                "10001", "same-user", "same-user", "role-1", "DIRECT",
                now, now.plusSeconds(86400), "reason", "ticket", "MFA",
                false, 3L, "command-1", now);

        assertThatThrownBy(() -> facade.assign(request))
                .hasMessage("SELF_PRIVILEGE_ESCALATION_DENIED");
        org.mockito.Mockito.verify(management).authorize(
                org.mockito.ArgumentMatchers.any());
    }

    private AssignmentFacade.AssignmentFacts facts() {
        return new AssignmentFacade.AssignmentFacts(
                "root-1", "MEDIUM", false, "PUBLIC", 30,
                Set.of(), List.of(), List.of(),
                new AssignmentFacade.Cardinality("TENANT", "10001", 10, 0));
    }

    private AssignmentFacade.AssignRequest request() {
        Instant now = Instant.parse("2026-07-30T08:00:00Z");
        return new AssignmentFacade.AssignRequest(
                "10001", "operator", "target", "role-1", "DIRECT",
                now, now.plusSeconds(86400), "reason", "ticket", "MFA",
                false, 3L, "command-1", now);
    }

    private AuthorizationMutationCoordinator coordinator() {
        return new AuthorizationMutationCoordinator(
                new AuthorizationMutationCoordinator.MutationStore() {
                    public void prepare(AuthorizationMutationCoordinator.MutationRecord record) {
                    }

                    public void transition(
                            String mutationId,
                            AuthorizationMutationCoordinator.MutationStatus status,
                            String errorCode,
                            Instant now) {
                    }
                },
                new top.egon.cola.platform.rbac3.admin.runtime.application.AuthorizationFenceService(
                        new top.egon.cola.platform.rbac3.admin.runtime.application.AuthorizationFenceService.FenceStore() {
                            public void put(top.egon.cola.platform.rbac3.admin.runtime.application.AuthorizationFenceService.Fence fence) {
                            }

                            public void remove(String tenantId, String scopeType, String scopeId) {
                            }
                        }, java.time.Clock.systemUTC()),
                mutation -> {
                },
                supplier -> supplier.get(),
                () -> "mutation-1",
                java.time.Clock.systemUTC());
    }
}
