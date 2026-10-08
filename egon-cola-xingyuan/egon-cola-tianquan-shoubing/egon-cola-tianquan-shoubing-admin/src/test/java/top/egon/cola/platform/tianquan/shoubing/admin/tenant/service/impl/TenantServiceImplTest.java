package top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.impl;

import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repo.IdentityDirectoryContext;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.repo.IdentityTenantRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.TenantService;
import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantServiceImplTest {

    @BeforeAll
    static void bindTheProcessWideEngine() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
    }

    private static final Instant NOW =
            Instant.parse("2026-08-22T02:00:00Z");

    private final IdentityTenantRepository tenants =
            mock(IdentityTenantRepository.class);
    private final IdentityDirectoryContext directoryContext =
            new IdentityDirectoryContext(9_001L, "tenantId");
    private TenantServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TenantServiceImpl(
                tenants,
                directoryContext,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsDecimalInitializingTenantAndNormalizesSettings() {
        when(tenants.existsByTenantCodeIgnoreCase("acme")).thenReturn(false);
        when(tenants.save(any(IdentityTenantBO.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TenantService.TenantView created = service.create(
                new TenantService.CreateTenantCommand(
                        " ACME ",
                        " Acme ",
                        "{\"region\":\"cn\"}",
                        "operator-1"
                )
        );

        assertThat(created.tenantId()).matches("\\d+");
        assertThat(Long.parseLong(created.tenantId())).isPositive();
        assertThat(created.tenantCode()).isEqualTo("acme");
        assertThat(created.tenantName()).isEqualTo("Acme");
        assertThat(created.status())
                .isEqualTo(IdentityTenantBO.Status.INITIALIZING);
        assertThat(created.version()).isZero();
        assertThat(created.settings()).isEqualTo("{\"region\":\"cn\"}");
        verify(tenants).save(any(IdentityTenantBO.class));
    }

    @Test
    void updatesOnlyOncePerExpectedVersionAndEnforcesClosedTerminalState() {
        IdentityTenantBO tenant = IdentityTenantBO.create(
                "10001",
                "acme",
                "Acme",
                "{}",
                "operator-1",
                NOW
        );
        when(tenants.findByIdForUpdate("10001"))
                .thenReturn(Optional.of(tenant));
        when(tenants.save(any(IdentityTenantBO.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TenantService.TenantView active = service.update(
                "10001",
                new TenantService.UpdateTenantCommand(
                        0L,
                        null,
                        null,
                        IdentityTenantBO.Status.ACTIVE,
                        "operator-2"
                )
        );
        assertThat(active.status()).isEqualTo(
                IdentityTenantBO.Status.ACTIVE
        );
        assertThat(active.version()).isEqualTo(1L);

        assertThatThrownBy(() -> service.update(
                "10001",
                new TenantService.UpdateTenantCommand(
                        0L,
                        null,
                        null,
                        IdentityTenantBO.Status.SUSPENDED,
                        "operator-3"
                )
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("tenant version conflict");

        tenant.update(
                "Acme",
                "{}",
                IdentityTenantBO.Status.CLOSED,
                1L,
                "operator-4",
                NOW
        );
        assertThatThrownBy(() -> tenant.update(
                "Acme",
                "{}",
                IdentityTenantBO.Status.ACTIVE,
                2L,
                "operator-5",
                NOW
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("closed tenant cannot change status");
    }

    @Test
    void refusesToAllocateTheReservedPlatformPartitionAsABusinessTenant() {
        when(tenants.existsByTenantCodeIgnoreCase("acme")).thenReturn(false);
        try (var ids = mockStatic(SnowflakeIdGenerator.class)) {
            ids.when(SnowflakeIdGenerator::nextLongId).thenReturn(9_001L);

            assertThatThrownBy(() -> service.create(
                    new TenantService.CreateTenantCommand(
                            "acme", "Acme", "{}", "operator-1"
                    )
            )).isInstanceOf(IllegalStateException.class)
                    .hasMessage("tenant id is reserved for platform storage");
        }
        verify(tenants, never()).save(any(IdentityTenantBO.class));
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
