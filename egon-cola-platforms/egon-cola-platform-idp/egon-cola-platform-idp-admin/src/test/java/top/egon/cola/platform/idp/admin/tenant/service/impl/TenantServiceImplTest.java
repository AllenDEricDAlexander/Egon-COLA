package top.egon.cola.platform.idp.admin.tenant.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.idp.admin.tenant.domain.pojo.IdentityTenantEntity;
import top.egon.cola.platform.idp.admin.tenant.repo.IdentityTenantRepository;
import top.egon.cola.platform.idp.admin.tenant.service.TenantService;

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

class TenantServiceImplTest {

    private static final Instant NOW =
            Instant.parse("2026-08-22T02:00:00Z");

    private final IdentityTenantRepository tenants =
            mock(IdentityTenantRepository.class);
    private final LongIdGenerator ids = () -> 10001L;
    private TenantServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TenantServiceImpl(
                tenants,
                ids,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsDecimalInitializingTenantAndNormalizesSettings() {
        when(tenants.existsByTenantCodeIgnoreCase("acme")).thenReturn(false);
        when(tenants.save(any(IdentityTenantEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TenantService.TenantView created = service.create(
                new TenantService.CreateTenantCommand(
                        " ACME ",
                        " Acme ",
                        "{\"region\":\"cn\"}",
                        "operator-1"
                )
        );

        assertThat(created.tenantId()).isEqualTo("10001");
        assertThat(created.tenantCode()).isEqualTo("acme");
        assertThat(created.tenantName()).isEqualTo("Acme");
        assertThat(created.status())
                .isEqualTo(IdentityTenantEntity.Status.INITIALIZING);
        assertThat(created.version()).isZero();
        assertThat(created.settings()).isEqualTo("{\"region\":\"cn\"}");
        verify(tenants).save(any(IdentityTenantEntity.class));
    }

    @Test
    void updatesOnlyOncePerExpectedVersionAndEnforcesClosedTerminalState() {
        IdentityTenantEntity tenant = IdentityTenantEntity.create(
                "10001",
                "acme",
                "Acme",
                "{}",
                "operator-1",
                NOW
        );
        when(tenants.findByIdForUpdate("10001"))
                .thenReturn(Optional.of(tenant));
        when(tenants.save(any(IdentityTenantEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TenantService.TenantView active = service.update(
                "10001",
                new TenantService.UpdateTenantCommand(
                        0L,
                        null,
                        null,
                        IdentityTenantEntity.Status.ACTIVE,
                        "operator-2"
                )
        );
        assertThat(active.status()).isEqualTo(
                IdentityTenantEntity.Status.ACTIVE
        );
        assertThat(active.version()).isEqualTo(1L);

        assertThatThrownBy(() -> service.update(
                "10001",
                new TenantService.UpdateTenantCommand(
                        0L,
                        null,
                        null,
                        IdentityTenantEntity.Status.SUSPENDED,
                        "operator-3"
                )
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("tenant version conflict");

        tenant.update(
                "Acme",
                "{}",
                IdentityTenantEntity.Status.CLOSED,
                1L,
                "operator-4",
                NOW
        );
        assertThatThrownBy(() -> tenant.update(
                "Acme",
                "{}",
                IdentityTenantEntity.Status.ACTIVE,
                2L,
                "operator-5",
                NOW
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("closed tenant cannot change status");
    }

    @Test
    void rejectsDuplicateCodeBeforeAllocatingOrWriting() {
        when(tenants.existsByTenantCodeIgnoreCase("acme")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new TenantService.CreateTenantCommand(
                        "acme",
                        "Acme",
                        "{}",
                        "operator-1"
                )
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("tenant code already exists");
    }
}
