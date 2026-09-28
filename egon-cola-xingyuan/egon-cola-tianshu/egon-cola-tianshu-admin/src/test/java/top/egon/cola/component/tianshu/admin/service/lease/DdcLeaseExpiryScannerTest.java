package top.egon.cola.component.tianshu.admin.service.lease;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import top.egon.cola.component.tianshu.admin.model.bo.DdcInstanceBO;
import top.egon.cola.component.tianshu.admin.model.enums.InstanceStatus;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigLeaseRedisRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcInstanceRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcServiceRegistryRedisRepository;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantAdminProperties;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantBindingDTO;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextExecutor;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantRegistry;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcLeaseExpiryScannerTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:30Z");

    @Test
    void removesExpiredIndexAndMarksOnlyTheMatchingLeaseInBoundTenantContext() {
        DdcInstanceRepository instanceRepository = mock(DdcInstanceRepository.class);
        DdcConfigLeaseRedisRepository leaseRepository = mock(DdcConfigLeaseRedisRepository.class);
        DdcServiceRegistryRedisRepository registryRepository = mock(DdcServiceRegistryRedisRepository.class);
        DdcInstanceBO expired = expiredInstance();
        LocalDateTime databaseNow = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);
        when(instanceRepository.findByStatusAndLeaseExpireAtLessThanEqual(
                InstanceStatus.ONLINE.name(), databaseNow)).thenAnswer(invocation -> {
            assertThat(MDC.get("tenantId")).isEqualTo("101");
            assertThat(MDC.get("userId")).isEqualTo("system:lease-expiry-scanner");
            return List.of(expired);
        });
        when(leaseRepository.removeExpiredProjection(
                "default", "dev", "demo", "instance-1", "lease-1", NOW)).thenReturn(true);
        when(instanceRepository.markOfflineIfLeaseMatches(
                "instance-1", "lease-1", InstanceStatus.OFFLINE.name(), databaseNow)).thenReturn(1);
        DdcTenantAdminProperties tenantProperties = tenantProperties();
        DdcTenantRegistry registry = registry(tenantProperties);
        DdcLeaseExpiryScanner scanner = scanner(instanceRepository, leaseRepository,
                registryRepository, tenantProperties, registry);

        assertThat(scanner.scanExpired()).isEqualTo(1);

        verify(instanceRepository).markOfflineIfLeaseMatches(
                "instance-1", "lease-1", InstanceStatus.OFFLINE.name(), databaseNow);
        verify(registryRepository).expireKnown(NOW);
        assertThat(MDC.get("tenantId")).isNull();
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    void leavesProjectionOnlineWhenRedisContainsANewerLease() {
        DdcInstanceRepository instanceRepository = mock(DdcInstanceRepository.class);
        DdcConfigLeaseRedisRepository leaseRepository = mock(DdcConfigLeaseRedisRepository.class);
        DdcServiceRegistryRedisRepository registryRepository = mock(DdcServiceRegistryRedisRepository.class);
        when(instanceRepository.findByStatusAndLeaseExpireAtLessThanEqual(
                InstanceStatus.ONLINE.name(), LocalDateTime.ofInstant(NOW, ZoneOffset.UTC)))
                .thenReturn(List.of(expiredInstance()));
        when(leaseRepository.removeExpiredProjection(
                "default", "dev", "demo", "instance-1", "lease-1", NOW)).thenReturn(false);
        DdcTenantAdminProperties tenantProperties = tenantProperties();
        DdcLeaseExpiryScanner scanner = scanner(instanceRepository, leaseRepository,
                registryRepository, tenantProperties, registry(tenantProperties));

        assertThat(scanner.scanExpired()).isZero();

        verify(instanceRepository, never()).markOfflineIfLeaseMatches(
                any(), any(), any(), any());
    }

    private DdcLeaseExpiryScanner scanner(DdcInstanceRepository instances,
            DdcConfigLeaseRedisRepository leaseRepository,
            DdcServiceRegistryRedisRepository registryRepository,
            DdcTenantAdminProperties properties, DdcTenantRegistry registry) {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        return new DdcLeaseExpiryScanner(instances, leaseRepository, registryRepository, properties, registry,
                new DdcTenantContextExecutor(), transactionManager, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private DdcTenantAdminProperties tenantProperties() {
        DdcTenantAdminProperties properties = new DdcTenantAdminProperties();
        properties.setBindings(List.of(DdcTenantBindingDTO.builder().tenantId(101L)
                .identityTenantId("tenant-a").enabled(true).build()));
        return properties;
    }

    private DdcTenantRegistry registry(DdcTenantAdminProperties properties) {
        DdcTenantRegistry registry = mock(DdcTenantRegistry.class);
        when(registry.requireEnabledTenant(anyLong())).thenAnswer(invocation -> properties.getBindings().get(0));
        return registry;
    }

    private DdcInstanceBO expiredInstance() {
        return DdcInstanceBO.builder().instanceId("instance-1").leaseId("lease-1")
                .bizCode("default").appCode("demo").env("dev").status(InstanceStatus.ONLINE.name())
                .leaseExpireAt(LocalDateTime.ofInstant(NOW.minusSeconds(1), ZoneOffset.UTC)).build();
    }
}
