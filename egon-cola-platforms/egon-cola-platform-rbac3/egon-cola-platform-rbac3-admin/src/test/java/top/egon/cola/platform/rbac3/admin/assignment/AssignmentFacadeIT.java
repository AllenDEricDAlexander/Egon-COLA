package top.egon.cola.platform.rbac3.admin.assignment;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.assignment.service.AssignmentFacade;
import top.egon.cola.platform.rbac3.admin.management.service.ManagementPolicyFacade;
import top.egon.cola.platform.rbac3.admin.runtime.service.AuthorizationMutationCoordinator;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import top.egon.cola.platform.rbac3.admin.assignment.domain.dto.RoleAssignmentDTO;
import top.egon.cola.platform.rbac3.admin.assignment.domain.vo.AssignmentFactsVO;
import top.egon.cola.platform.rbac3.admin.assignment.domain.vo.CardinalityVO;
import top.egon.cola.platform.rbac3.admin.runtime.repository.AuthorizationFenceRepository;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.AuthorizationFenceVO;
import top.egon.cola.platform.rbac3.admin.runtime.repository.AuthorizationMutationRepository;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.MutationRecordVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.AuthorizationMutationResultStatusEnum;

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
        var request = new RoleAssignmentDTO(
                "10001", "same-user", "same-user", "role-1", "DIRECT",
                now, now.plusSeconds(86400), "reason", "ticket", "MFA",
                false, 3L, "command-1", now);

        assertThatThrownBy(() -> facade.assign(request))
                .hasMessage("SELF_PRIVILEGE_ESCALATION_DENIED");
        org.mockito.Mockito.verify(management).authorize(
                org.mockito.ArgumentMatchers.any());
    }

    private AssignmentFactsVO facts() {
        return new AssignmentFactsVO(
                "root-1", "MEDIUM", false, "PUBLIC", 30,
                Set.of(), List.of(), List.of(),
                new CardinalityVO("TENANT", "10001", 10, 0));
    }

    private RoleAssignmentDTO request() {
        Instant now = Instant.parse("2026-07-30T08:00:00Z");
        return new RoleAssignmentDTO(
                "10001", "operator", "target", "role-1", "DIRECT",
                now, now.plusSeconds(86400), "reason", "ticket", "MFA",
                false, 3L, "command-1", now);
    }

    private AuthorizationMutationCoordinator coordinator() {
        return new AuthorizationMutationCoordinator(
                new AuthorizationMutationRepository() {
                    public void prepare(MutationRecordVO record) {
                    }

                    public void transition(
                            String mutationId,
                            AuthorizationMutationResultStatusEnum status,
                            String errorCode,
                            Instant now) {
                    }
                },
                new top.egon.cola.platform.rbac3.admin.runtime.service.AuthorizationFenceService(
                        new AuthorizationFenceRepository() {
                            public void put(AuthorizationFenceVO fence) {
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
