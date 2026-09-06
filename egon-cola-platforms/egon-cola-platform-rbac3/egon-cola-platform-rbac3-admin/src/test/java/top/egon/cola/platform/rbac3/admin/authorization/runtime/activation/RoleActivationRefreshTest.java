package top.egon.cola.platform.rbac3.admin.authorization.runtime.activation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.activation.domain.dto.ReplaceCommandDTO;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.activation.domain.vo.ActivationFactsVO;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.activation.domain.vo.ApplicationFactVO;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.activation.domain.vo.ResolvedActivationVO;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.activation.domain.vo.TransactionResultVO;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.activation.domain.vo.UserAuthorizationStateVO;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.activation.repository.ActivationTransaction;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.activation.repository.RoleActivationRuntimeRepository;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.activation.service.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.domain.vo.Rbac3RuntimePolicySnapshotVO;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.domain.vo.UserSnapshotProjectionVO;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.service.UserAuthorizationSnapshotProjector;
import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;
import top.egon.cola.platform.rbac3.core.activation.EligibleAssignmentFact;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RoleActivationRefreshTest {

    private static final Instant NOW = Instant.parse("2026-09-06T00:00:00Z");
    private final ActivationTransaction transaction = mock(ActivationTransaction.class);
    private final RoleActivationRuntimeRepository runtime = mock(RoleActivationRuntimeRepository.class);
    private final UserAuthorizationSnapshotProjector projector = mock(UserAuthorizationSnapshotProjector.class);
    private RoleActivationFacade facade;

    @BeforeEach
    void setUp() {
        var hierarchy = new RoleHierarchy(List.of(
                new RoleNode("10", "1", "ROOT", true, RoleNode.RiskLevel.LOW, false, null, 100),
                new RoleNode("11", "1", "CHILD", true, RoleNode.RiskLevel.LOW, false, null, 100)),
                List.of(new RoleEdge("10", "11")));
        var facts = new ActivationFactsVO("7", "9", hierarchy,
                List.of(new EligibleAssignmentFact("101", "9", "10", EligibleAssignmentFact.Status.ACTIVE,
                        NOW.minusSeconds(60), null)), List.of(),
                new AuthorizationRuleFacts(List.of(), List.of(), List.of(), List.of(), List.of()),
                3L, 8L, "directory:12", Map.of("1", new ApplicationFactVO("1", "finance", "Finance")),
                Map.of("10", "Root", "11", "Child"));
        when(transaction.replace(any(), any(), any())).thenAnswer(invocation -> {
            Function<UserAuthorizationStateVO, ResolvedActivationVO> resolve = invocation.getArgument(2);
            ResolvedActivationVO resolved = resolve.apply(null);
            return new TransactionResultVO(resolved, false, "repair", resolved.resolution().activeRoleSet().rootsByApplication(),
                    3L, 8L, resolved.resolution().snapshot().checksum(), NOW.plusSeconds(3600));
        });
        when(projector.project(any())).thenReturn(mock(UserSnapshotProjectionVO.class));
        facade = new RoleActivationFacade((tenant, user, now) -> facts, transaction, projector, runtime,
                () -> new Rbac3RuntimePolicySnapshotVO(32, Map.of()), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void refreshRepublishesTheSameSelectedRootWithoutChangingUserVersion() {
        var result = facade.refresh(command("10"));
        assertThat(result.changed()).isFalse();
        assertThat(result.authVersion()).isEqualTo(3L);
        assertThat(result.policyVersion()).isEqualTo(8L);
        verify(runtime).publish(any());
    }

    @Test
    void refreshCannotSilentlyReplaceAFormerRootWithItsNewAncestor() {
        assertThatThrownBy(() -> facade.refresh(command("11"))).hasMessage("ROLE_ACTIVATION_SET_INVALID");
        verifyNoInteractions(projector, runtime);
    }

    @Test
    void explicitUserActivationRetainsTheExistingRootNormalizationContract() {
        var result = facade.replace(command("11"));
        assertThat(result.authVersion()).isEqualTo(3L);
        verify(runtime).publish(any());
    }

    private ReplaceCommandDTO command(String root) {
        return new ReplaceCommandDTO("7", "subject", "9", List.of(root), 3L, "actor", "repair");
    }
}
