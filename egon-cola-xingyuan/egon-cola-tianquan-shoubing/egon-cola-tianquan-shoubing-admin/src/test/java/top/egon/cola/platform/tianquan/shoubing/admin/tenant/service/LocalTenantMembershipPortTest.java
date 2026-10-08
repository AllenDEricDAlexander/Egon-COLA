package top.egon.cola.platform.tianquan.shoubing.admin.tenant.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantMembershipBO;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityUserStatus;
import top.egon.cola.platform.tianquan.shoubing.core.port.TenantMembershipPort;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalTenantMembershipPortTest {

    private static final Instant NOW =
            Instant.parse("2026-08-22T02:00:00Z");

    private final TenantMembershipService memberships =
            mock(TenantMembershipService.class);
    private final LocalTenantMembershipPort port =
            new LocalTenantMembershipPort(memberships);

    @Test
    void listsOnlyEffectiveActiveMembershipsUsingReducedContract() {
        when(memberships.listByIdentity("user-1")).thenReturn(List.of(
                profile(
                        "user-1",
                        "10001",
                        TenantMembershipPort.MembershipStatus.ACTIVE,
                        IdentityTenantMembershipBO.Status.ACTIVE
                ),
                profile(
                        "user-1",
                        "10002",
                        TenantMembershipPort.MembershipStatus.DISABLED,
                        IdentityTenantMembershipBO.Status.DISABLED
                )
        ));

        assertThat(port.list("user-1")).containsExactly(
                new TenantMembershipPort.TenantMembership(
                        "user-1",
                        "10001",
                        "Acme",
                        TenantMembershipPort.MembershipStatus.ACTIVE
                )
        );
    }

    @Test
    void resolvesDisabledStateAndReturnsNullOnlyForMissingPair() {
        when(memberships.resolve("user-1", "10001")).thenReturn(profile(
                "user-1",
                "10001",
                TenantMembershipPort.MembershipStatus.DISABLED,
                IdentityTenantMembershipBO.Status.ACTIVE
        ));
        when(memberships.resolve("user-1", "missing"))
                .thenThrow(new IllegalStateException("membership not found"));

        assertThat(port.resolve("user-1", "10001").status())
                .isEqualTo(TenantMembershipPort.MembershipStatus.DISABLED);
        assertThat(port.resolve("user-1", "missing")).isNull();
    }

    @Test
    void wrapsLocalFailuresWithoutNetworkFallback() {
        when(memberships.resolve("user-1", "10001"))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> port.resolve("user-1", "10001"))
                .isInstanceOf(TenantMembershipPort.TenantMembershipException.class)
                .hasMessage("tenant membership is unavailable");
    }

    private static TenantMembershipService.TenantMembershipProfile profile(
            String identitySub,
            String tenantId,
            TenantMembershipPort.MembershipStatus effectiveStatus,
            IdentityTenantMembershipBO.Status membershipStatus
    ) {
        return new TenantMembershipService.TenantMembershipProfile(
                identitySub,
                tenantId,
                "Acme",
                "Mario",
                IdentityTenantBO.Status.ACTIVE,
                IdentityUserStatus.ACTIVE,
                membershipStatus,
                effectiveStatus,
                0L,
                NOW
        );
    }
}
