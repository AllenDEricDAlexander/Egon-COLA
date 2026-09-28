package top.egon.cola.component.tianshu.admin.service.lease;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.tianshu.admin.model.enums.InstanceStatus;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigLeaseRedisRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcInstanceRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcServiceRegistryRedisRepository;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantAdminProperties;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantBindingDTO;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextDTO;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextExecutor;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantRegistry;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
public class DdcLeaseExpiryScanner {

    private final DdcInstanceRepository instanceRepository;
    private final DdcConfigLeaseRedisRepository leaseRepository;
    private final DdcServiceRegistryRedisRepository registryRepository;
    private final DdcTenantAdminProperties tenantProperties;
    private final DdcTenantRegistry tenantRegistry;
    private final DdcTenantContextExecutor tenantContextExecutor;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public DdcLeaseExpiryScanner(DdcInstanceRepository instanceRepository,
            DdcConfigLeaseRedisRepository leaseRepository,
            DdcServiceRegistryRedisRepository registryRepository,
            DdcTenantAdminProperties tenantProperties, DdcTenantRegistry tenantRegistry,
            DdcTenantContextExecutor tenantContextExecutor, PlatformTransactionManager transactionManager) {
        this(instanceRepository, leaseRepository, registryRepository, tenantProperties, tenantRegistry,
                tenantContextExecutor, transactionManager, Clock.systemUTC());
    }

    DdcLeaseExpiryScanner(DdcInstanceRepository instanceRepository,
            DdcConfigLeaseRedisRepository leaseRepository,
            DdcServiceRegistryRedisRepository registryRepository,
            DdcTenantAdminProperties tenantProperties, DdcTenantRegistry tenantRegistry,
            DdcTenantContextExecutor tenantContextExecutor, PlatformTransactionManager transactionManager,
            Clock clock) {
        this.instanceRepository = instanceRepository;
        this.leaseRepository = leaseRepository;
        this.registryRepository = registryRepository;
        this.tenantProperties = tenantProperties;
        this.tenantRegistry = tenantRegistry;
        this.tenantContextExecutor = tenantContextExecutor;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString = "${egon.cola.component.tianshu.admin.lease.scan-interval-millis:5000}",
            initialDelayString = "${egon.cola.component.tianshu.admin.lease.scan-interval-millis:5000}"
    )
    public int scanExpired() {
        int total = 0;
        for (DdcTenantBindingDTO configured : tenantProperties.getBindings()) {
            if (configured == null || !configured.isEnabled()) continue;
            Long tenantId = configured.getTenantId();
            try {
                DdcTenantBindingDTO registered = tenantRegistry.requireEnabledTenant(tenantId);
                if (!registered.isEnabled()
                        || !registered.getIdentityTenantId().equals(configured.getIdentityTenantId())) {
                    throw new IllegalStateException("TENANT_BINDING_CHANGED_DURING_SCAN");
                }
                DdcTenantContextDTO context = new DdcTenantContextDTO(
                        registered.getTenantId(), registered.getIdentityTenantId(), "system:lease-expiry-scanner");
                Integer expired = tenantContextExecutor.call(context, () -> transactionTemplate.execute(
                        status -> scanCurrentTenant(clock.instant())));
                total += expired == null ? 0 : expired;
            } catch (RuntimeException failure) {
                log.error("Lease expiry scan failed for tenantId={}", tenantId, failure);
            }
        }
        return total;
    }

    private int scanCurrentTenant(Instant now) {
        LocalDateTime databaseNow = LocalDateTime.ofInstant(now, clock.getZone());
        int configExpired = instanceRepository.findByStatusAndLeaseExpireAtLessThanEqual(
                        InstanceStatus.ONLINE.name(), databaseNow).stream()
                .filter(instance -> leaseRepository.removeExpiredProjection(
                        instance.getBizCode(), instance.getEnv(), instance.getAppCode(),
                        instance.getInstanceId(), instance.getLeaseId(), now))
                .mapToInt(instance -> instanceRepository.markOfflineIfLeaseMatches(
                        instance.getInstanceId(), instance.getLeaseId(),
                        InstanceStatus.OFFLINE.name(), databaseNow))
                .sum();
        int serviceExpired = registryRepository == null ? 0 : registryRepository.expireKnown(now);
        return configExpired + serviceExpired;
    }
}
