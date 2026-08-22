package top.egon.cola.platform.idp.admin.tenant.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.idp.admin.identity.repo.IdentityUserDirectory;
import top.egon.cola.platform.idp.admin.tenant.domain.pojo.IdentityTenantEntity;
import top.egon.cola.platform.idp.admin.tenant.domain.pojo.IdentityTenantMembershipEntity;
import top.egon.cola.platform.idp.admin.tenant.repo.IdentityTenantMembershipRepository;
import top.egon.cola.platform.idp.admin.tenant.repo.IdentityTenantRepository;
import top.egon.cola.platform.idp.admin.tenant.service.TenantMembershipService;
import top.egon.cola.platform.idp.core.identity.IdentityUser;
import top.egon.cola.platform.idp.core.identity.IdentityUserStatus;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantMembershipServiceImplTest {

    private static final Instant NOW =
            Instant.parse("2026-08-22T02:00:00Z");

    private final IdentityTenantRepository tenants =
            mock(IdentityTenantRepository.class);
    private final IdentityTenantMembershipRepository memberships =
            mock(IdentityTenantMembershipRepository.class);
    private final IdentityUserDirectory users = mock(IdentityUserDirectory.class);
    private final LongIdGenerator ids = () -> 20001L;
    private final IdentityTenantEntity tenant = IdentityTenantEntity.create(
            "10001",
            "acme",
            "Acme",
            "{}",
            "operator-1",
            NOW
    );
    private final IdentityUser user = new IdentityUser(
            "user-1",
            "mario",
            "mario",
            "Mario",
            IdentityUserStatus.ACTIVE,
            0,
            null,
            null,
            0L
    );
    private TenantMembershipServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TenantMembershipServiceImpl(
                tenants,
                memberships,
                users,
                ids,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        when(tenants.findById("10001")).thenReturn(Optional.of(tenant));
        when(users.list()).thenReturn(List.of(user));
        when(memberships.save(any(IdentityTenantMembershipEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void upsertsMembershipAndResolvesEffectiveStatusFromAllThreeAuthorities() {
        when(memberships.findByTenantIdAndIdentitySub("10001", "user-1"))
                .thenReturn(Optional.empty());
        TenantMembershipService.MembershipView created = service.upsert(
                new TenantMembershipService.UpsertMembershipCommand(
                        "10001",
                        "user-1",
                        IdentityTenantMembershipEntity.Status.ACTIVE,
                        null,
                        "operator-2"
                )
        );

        assertThat(created.identitySub()).isEqualTo("user-1");
        assertThat(created.status()).isEqualTo(
                IdentityTenantMembershipEntity.Status.ACTIVE
        );
        assertThat(created.version()).isZero();
        verify(memberships).save(any(IdentityTenantMembershipEntity.class));

        IdentityTenantMembershipEntity membership =
                IdentityTenantMembershipEntity.create(
                        "membership-1",
                        "10001",
                        "user-1",
                        IdentityTenantMembershipEntity.Status.ACTIVE,
                        "operator-2",
                        NOW
                );
        when(memberships.findByTenantIdAndIdentitySub("10001", "user-1"))
                .thenReturn(Optional.of(membership));

        TenantMembershipService.TenantMembershipProfile profile =
                service.resolve("user-1", "10001");
        assertThat(profile.tenantStatus()).isEqualTo(
                IdentityTenantEntity.Status.INITIALIZING
        );
        assertThat(profile.identityStatus()).isEqualTo(
                IdentityUserStatus.ACTIVE
        );
        assertThat(profile.membershipStatus()).isEqualTo(
                IdentityTenantMembershipEntity.Status.ACTIVE
        );
        assertThat(profile.effectiveStatus()).isEqualTo(
                TenantMembershipPort.MembershipStatus.DISABLED
        );

        tenant.update(
                "Acme",
                "{}",
                IdentityTenantEntity.Status.ACTIVE,
                0L,
                "operator-3",
                NOW
        );
        profile = service.resolve("user-1", "10001");
        assertThat(profile.effectiveStatus()).isEqualTo(
                TenantMembershipPort.MembershipStatus.ACTIVE
        );
    }

    @Test
    void rejectsUnknownIdentityAndClosedTenantActivation() {
        when(memberships.findByTenantIdAndIdentitySub("10001", "missing"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.upsert(
                new TenantMembershipService.UpsertMembershipCommand(
                        "10001",
                        "missing",
                        IdentityTenantMembershipEntity.Status.ACTIVE,
                        null,
                        "operator-2"
                )
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("identity user not found");

        tenant.update(
                "Acme",
                "{}",
                IdentityTenantEntity.Status.ACTIVE,
                0L,
                "operator-3",
                NOW
        );
        tenant.update(
                "Acme",
                "{}",
                IdentityTenantEntity.Status.CLOSED,
                1L,
                "operator-3",
                NOW
        );
        when(memberships.findByTenantIdAndIdentitySub("10001", "user-1"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.upsert(
                new TenantMembershipService.UpsertMembershipCommand(
                        "10001",
                        "user-1",
                        IdentityTenantMembershipEntity.Status.ACTIVE,
                        null,
                        "operator-2"
                )
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("closed tenant cannot activate membership");
    }
}
