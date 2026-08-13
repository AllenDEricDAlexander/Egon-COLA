package top.egon.cola.platform.rbac3.admin.simulation;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.tenant.domain.po.TenantPO;
import top.egon.cola.platform.rbac3.admin.role.service.RoleFacade;
import top.egon.cola.platform.rbac3.admin.simulation.repository.jdbc.PostgresqlRoleImpactRepository;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import top.egon.cola.platform.rbac3.admin.role.domain.vo.RoleImpactVO;

class PostgresqlRoleImpactSourceTest {

    @Test
    void bindsImpactEvidenceToCurrentTenantPolicyVersion() {
        EntityManager entityManager = mock(EntityManager.class);
        RoleFacade roles = mock(RoleFacade.class);
        TenantPO tenant = new TenantPO(
                17L, "tenant-17", "Tenant 17", "bootstrap",
                Instant.parse("2026-07-30T12:00:00Z"));
        tenant.incrementPolicyVersion("operator", Instant.parse("2026-07-30T12:01:00Z"));
        RoleImpactVO impact = new RoleImpactVO(
                "31", List.of("20"), List.of("20", "31"),
                "HIGH", 7, List.of());
        when(entityManager.find(TenantPO.class, 17L)).thenReturn(tenant);
        when(roles.impact("17", "31")).thenReturn(impact);

        var snapshot = new PostgresqlRoleImpactRepository(entityManager, roles)
                .load("17", "31");

        assertThat(snapshot.impact()).isEqualTo(impact);
        assertThat(snapshot.policyVersion()).isEqualTo(1);
        assertThat(snapshot.evidenceChecksum()).startsWith("sha256:");
    }
}
