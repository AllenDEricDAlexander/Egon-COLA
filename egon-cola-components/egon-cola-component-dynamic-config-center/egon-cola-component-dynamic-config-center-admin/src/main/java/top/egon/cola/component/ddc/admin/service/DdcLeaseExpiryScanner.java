package top.egon.cola.component.ddc.admin.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.ddc.admin.model.enums.InstanceStatus;
import top.egon.cola.component.ddc.admin.repository.DdcConfigLeaseRedisRepository;
import top.egon.cola.component.ddc.admin.repository.DdcInstanceRepository;
import top.egon.cola.component.ddc.admin.repository.DdcServiceRegistryRedisRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

public class DdcLeaseExpiryScanner {

    private final DdcInstanceRepository instanceRepository;

    private final DdcConfigLeaseRedisRepository leaseRepository;

    private final DdcServiceRegistryRedisRepository registryRepository;

    private final Clock clock;

    public DdcLeaseExpiryScanner(DdcInstanceRepository instanceRepository,
                                 DdcConfigLeaseRedisRepository leaseRepository) {
        this(instanceRepository, leaseRepository, null, Clock.systemUTC());
    }

    public DdcLeaseExpiryScanner(DdcInstanceRepository instanceRepository,
                                 DdcConfigLeaseRedisRepository leaseRepository,
                                 DdcServiceRegistryRedisRepository registryRepository) {
        this(instanceRepository, leaseRepository, registryRepository, Clock.systemUTC());
    }

    DdcLeaseExpiryScanner(DdcInstanceRepository instanceRepository,
                          DdcConfigLeaseRedisRepository leaseRepository,
                          Clock clock) {
        this(instanceRepository, leaseRepository, null, clock);
    }

    DdcLeaseExpiryScanner(DdcInstanceRepository instanceRepository,
                          DdcConfigLeaseRedisRepository leaseRepository,
                          DdcServiceRegistryRedisRepository registryRepository,
                          Clock clock) {
        this.instanceRepository = instanceRepository;
        this.leaseRepository = leaseRepository;
        this.registryRepository = registryRepository;
        this.clock = clock;
    }

    @Transactional
    @Scheduled(
            fixedDelayString = "${egon.cola.component.ddc.admin.lease.scan-interval-millis:5000}",
            initialDelayString = "${egon.cola.component.ddc.admin.lease.scan-interval-millis:5000}"
    )
    public int scanExpired() {
        Instant now = clock.instant();
        LocalDateTime databaseNow = LocalDateTime.ofInstant(now, clock.getZone());
        int configExpired = instanceRepository.findByStatusAndLeaseExpireAtLessThanEqual(
                        InstanceStatus.ONLINE.name(),
                        databaseNow
                ).stream()
                .filter(instance -> leaseRepository.removeExpiredProjection(
                        instance.getAppCode(),
                        instance.getEnv(),
                        instance.getNamespace(),
                        instance.getInstanceId(),
                        instance.getLeaseId(),
                        now
                ))
                .mapToInt(instance -> instanceRepository.markOfflineIfLeaseMatches(
                        instance.getInstanceId(),
                        instance.getLeaseId(),
                        InstanceStatus.OFFLINE.name(),
                        databaseNow
                ))
                .sum();
        int serviceExpired = registryRepository == null ? 0 : registryRepository.expireKnown(now);
        return configExpired + serviceExpired;
    }
}
