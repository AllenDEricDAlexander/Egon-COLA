package top.egon.cola.platform.tianquan.shoubing.admin.support.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.pojo.IdentityTenantEntity;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.pojo.IdentityTenantMembershipEntity;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.TenantMembershipService;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.TenantService;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityUser;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityUserStatus;
import top.egon.cola.platform.tianquan.shoubing.core.port.IdentityUserStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IdpDevelopmentTenantBootstrapTest {

    private final TenantService tenants = mock(TenantService.class);
    private final TenantMembershipService memberships = mock(TenantMembershipService.class);
    private final IdentityUserStore users = mock(IdentityUserStore.class);
    private final IdpDevelopmentTenantBootstrap bootstrap =
            new IdpDevelopmentTenantBootstrap(tenants, memberships, users);

    @Test
    void createsFreshTenantsThroughIdpServicesAndResolvesOnlyNumericIds() {
        List<TenantService.TenantView> stored = new ArrayList<>();
        when(tenants.list()).thenAnswer(invocation -> List.copyOf(stored));
        when(tenants.create(any())).thenAnswer(invocation -> {
            TenantService.CreateTenantCommand command = invocation.getArgument(0);
            TenantService.TenantView tenant = tenant(Integer.toString(1001 + stored.size()),
                    command.tenantCode(), IdentityTenantEntity.Status.INITIALIZING);
            stored.add(tenant);
            return tenant;
        });

        assertThat(bootstrap.resolveTenantIds(Set.of("default"))).containsExactly("1001");
        assertThat(bootstrap.resolveTenantIds(Set.of("1002"))).containsExactly("1002");
        verify(tenants, times(2)).create(any());
        verify(tenants, times(2)).update(anyString(), argThat(command ->
                command.status() == IdentityTenantEntity.Status.ACTIVE && command.expectedVersion() == 0));
        verifyNoInteractions(memberships, users);
    }

    @Test
    void preservesExistingTenantStateAndRejectsUnknownGrantTargets() {
        when(tenants.list()).thenReturn(List.of(
                tenant("1001", "default", IdentityTenantEntity.Status.SUSPENDED),
                tenant("1002", "tenant-b", IdentityTenantEntity.Status.ACTIVE)));

        assertThat(bootstrap.resolveTenantIds(Set.of("default"))).containsExactly("1001");
        assertThatThrownBy(() -> bootstrap.resolveTenantIds(Set.of("unknown")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("does not exist");
        verify(tenants, never()).create(any());
        verify(tenants, never()).update(any(), any());
    }

    @Test
    void addsMissingAdminMembershipWithoutReactivatingExistingDisabledMembership() {
        when(users.findByNormalizedUsername("alice")).thenReturn(Optional.of(new IdentityUser(
                "9001", "alice", "alice", "Alice", IdentityUserStatus.ACTIVE,
                0, null, null, 0L)));
        when(tenants.list()).thenReturn(List.of(
                tenant("1001", "default", IdentityTenantEntity.Status.ACTIVE),
                tenant("1002", "tenant-b", IdentityTenantEntity.Status.ACTIVE),
                tenant("1003", "unrelated", IdentityTenantEntity.Status.ACTIVE)));
        when(memberships.listByIdentity("9001")).thenReturn(List.of(
                new TenantMembershipService.TenantMembershipProfile("9001", "1001", "Default",
                        "Alice", IdentityTenantEntity.Status.ACTIVE, IdentityUserStatus.ACTIVE,
                        IdentityTenantMembershipEntity.Status.DISABLED, null, 2, Instant.EPOCH)));

        bootstrap.initializeAdministratorMemberships();

        verify(memberships).upsert(argThat(command -> command.tenantId().equals("1002")
                && command.identitySub().equals("9001") && command.expectedVersion() == null
                && command.status() == IdentityTenantMembershipEntity.Status.ACTIVE));
        verify(memberships, times(1)).upsert(any());
    }

    @Test
    void doesNotCreateAnAdministratorAndRequiresExplicitLocalOptIn() {
        when(users.findByNormalizedUsername("alice")).thenReturn(Optional.empty());
        bootstrap.initializeAdministratorMemberships();
        verifyNoInteractions(tenants, memberships);
        assertThat(IdpDevelopmentTenantBootstrap.class.getAnnotation(Profile.class).value())
                .containsExactly("local");
        ConditionalOnProperty condition = IdpDevelopmentTenantBootstrap.class
                .getAnnotation(ConditionalOnProperty.class);
        assertThat(condition.prefix()).isEqualTo("egon.tianquan-shoubing.development-bootstrap");
        assertThat(condition.havingValue()).isEqualTo("true");
        assertThat(condition.matchIfMissing()).isFalse();
    }

    private TenantService.TenantView tenant(String id, String code, IdentityTenantEntity.Status status) {
        return new TenantService.TenantView(id, code, code, status, "{}", 0, Instant.EPOCH, Instant.EPOCH);
    }
}
