package top.egon.cola.platform.rbac3.admin.simulation;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.iam.authorizationstate.domain.po.TenantAuthorizationStatePO;
import top.egon.cola.platform.rbac3.admin.iam.authorizationstate.repository.TenantAuthorizationStateRepository;
import top.egon.cola.platform.rbac3.admin.iam.role.service.RoleFacade;
import top.egon.cola.platform.rbac3.admin.simulation.repository.jdbc.PostgresqlRoleImpactRepository;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import top.egon.cola.platform.rbac3.admin.iam.role.domain.vo.RoleImpactVO;

class PostgresqlRoleImpactSourceTest {

    @Test
    void bindsImpactEvidenceToCurrentTenantPolicyVersion() {
        RoleFacade roles = mock(RoleFacade.class);
        TenantAuthorizationStateRepository stateStore = mock(
                TenantAuthorizationStateRepository.class);
        TenantAuthorizationStatePO state = new TenantAuthorizationStatePO(
                17L, "bootstrap", Instant.parse("2026-07-30T12:00:00Z"));
        state.incrementPolicyVersion(
                "operator", Instant.parse("2026-07-30T12:01:00Z"));
        RoleImpactVO impact = new RoleImpactVO(
                "31", List.of("20"), List.of("20", "31"),
                "HIGH", 7, List.of());
        when(stateStore.requireForUpdate(17L)).thenReturn(state);
        when(roles.impact("17", "31")).thenReturn(impact);

        var snapshot = new PostgresqlRoleImpactRepository(
                roles, stateStore)
                .load("17", "31");

        assertThat(snapshot.impact()).isEqualTo(impact);
        assertThat(snapshot.policyVersion()).isEqualTo(1);
        assertThat(snapshot.evidenceChecksum()).startsWith("sha256:");
    }
}
