package top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.domain.dto.ReplaceCommandDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.domain.vo.CurrentStateVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.repository.ActivationTransaction;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.repository.ReselectionRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.service.RoleActivationFacade;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.dto.MutationWorkDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.vo.EventEnvelopeVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.RuntimeProjectionTargetRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.RuntimeProjectionTargetRepository.ProjectionTarget;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.RuntimePublicationRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.service.Rbac3RuntimeProjectionRecovery;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.state.domain.po.TenantAuthorizationStatePO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.state.repository.TenantAuthorizationStateRepository;
import top.egon.cola.platform.tianquan.jianshen.core.rule.Rbac3RuleViolation;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class Rbac3RuntimeProjectionRecoveryTest {

    private static final Instant NOW = Instant.parse("2026-09-06T00:00:00Z");
    private final RuntimeProjectionTargetRepository targets = mock(RuntimeProjectionTargetRepository.class);
    private final ActivationTransaction activeRoles = mock(ActivationTransaction.class);
    private final RoleActivationFacade activation = mock(RoleActivationFacade.class);
    private final ReselectionRepository reselection = mock(ReselectionRepository.class);
    private final RuntimePublicationRepository runtime = mock(RuntimePublicationRepository.class);
    private final TenantAuthorizationStateRepository state = mock(TenantAuthorizationStateRepository.class);
    private final ProjectionTarget user = new ProjectionTarget("9", "subject", 3L, true);
    private final Rbac3RuntimeProjectionRecovery recovery = new Rbac3RuntimeProjectionRecovery(
            targets, activeRoles, activation, reselection, runtime, state, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void setUp() {
        var tenant = new TenantAuthorizationStatePO(7L, "bootstrap", NOW);
        tenant.incrementPolicyVersion("actor", NOW);
        when(state.require(7L)).thenReturn(tenant);
        when(targets.find("7", "9")).thenReturn(Optional.of(user));
        when(activeRoles.current("7", "subject", "9", NOW))
                .thenReturn(new CurrentStateVO(Map.of("1", Set.of("10")), 3L, 0L, "unavailable", false));
    }

    @Test
    void userMutationRefreshesOnlyThePreviouslySelectedRoots() {
        recovery.project(userMutation());
        var command = ArgumentCaptor.forClass(ReplaceCommandDTO.class);
        verify(activation).refresh(command.capture());
        assertThat(command.getValue().requestedRoleIds()).containsExactly("10");
        assertThat(command.getValue().expectedAuthVersion()).isEqualTo(3L);
        verifyNoInteractions(reselection);
    }

    @Test
    void eventReplayUsesCurrentFactsAndKeysetPagesNotPayloadGrants() {
        when(targets.page("7", 0L, 200)).thenReturn(List.of(user));
        recovery.rebuild(event());
        recovery.rebuild(event());
        verify(activation, times(2)).refresh(any());
        verify(targets, times(2)).page("7", 9L, 200);
        verifyNoInteractions(reselection);
    }

    @Test
    void anEmptySelectionInvalidatesWithoutActivatingCandidates() {
        when(activeRoles.current("7", "subject", "9", NOW))
                .thenReturn(new CurrentStateVO(Map.of(), 3L, 0L, "unavailable", true));
        recovery.project(userMutation());
        verify(runtime).invalidate("7", "subject", "9", 3L, 1L);
        verifyNoInteractions(activation, reselection);
    }

    @Test
    void inactiveMembershipInvalidatesWithoutLoadingOrCreatingRoles() {
        when(targets.find("7", "9")).thenReturn(Optional.of(new ProjectionTarget("9", "subject", 4L, false)));
        recovery.project(userMutation());
        verify(runtime).invalidate("7", "subject", "9", 4L, 1L);
        verifyNoInteractions(activation, activeRoles, reselection);
    }

    @Test
    void revokedEligibilityRequiresReselectionAndInvalidatesTheOldUserVersion() {
        when(activation.refresh(any())).thenThrow(new Rbac3RuleViolation("ROLE_ACTIVATION_ASSIGNMENT_REQUIRED"));
        recovery.project(userMutation());
        verify(reselection).requireReselection("7", "9", 3L, NOW, "tianquan-jianshen-runtime-recovery");
        verify(runtime).invalidate("7", "subject", "9", 4L, 1L);
    }

    @Test
    void transientFailureRemainsRetryableAndDoesNotDiscardTheUserSelection() {
        when(activation.refresh(any())).thenThrow(new IllegalStateException("ddc unavailable"));
        assertThatThrownBy(() -> recovery.project(userMutation())).hasMessage("ddc unavailable");
        verifyNoInteractions(reselection, runtime);
    }

    @Test
    void failedUserDoesNotStarveLaterUsersAndTheWholeEventStillFails() {
        when(targets.page("7", 0L, 200)).thenReturn(List.of(user, new ProjectionTarget("12", "inactive", 6L, false)));
        when(activation.refresh(any())).thenThrow(new Rbac3RuleViolation("AUTH_PROPAGATION_PENDING"));
        assertThatThrownBy(() -> recovery.rebuild(event())).hasMessage("AUTH_PROPAGATION_PENDING");
        verify(runtime).invalidate("7", "inactive", "12", 6L, 1L);
        verify(targets).page("7", 12L, 200);
        verifyNoInteractions(reselection);
    }

    @Test
    void unsupportedScopeIsNotAcknowledgedAsSuccessfulWork() {
        assertThatThrownBy(() -> recovery.project(new MutationWorkDTO("mutation", "7", "UNKNOWN", "9", "COMMITTED")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(activation, targets, runtime);
    }

    private MutationWorkDTO userMutation() {
        return new MutationWorkDTO("mutation", "7", "USER", "9", "COMMITTED");
    }

    private EventEnvelopeVO event() {
        return new EventEnvelopeVO("event", "tianquan-jianshen.role.policy-changed.v1", 1, NOW, "7", "ROLE", "10", 0L,
                "trace", Map.of("roleIds", "unselected-role", "policyVersion", "0"));
    }
}
