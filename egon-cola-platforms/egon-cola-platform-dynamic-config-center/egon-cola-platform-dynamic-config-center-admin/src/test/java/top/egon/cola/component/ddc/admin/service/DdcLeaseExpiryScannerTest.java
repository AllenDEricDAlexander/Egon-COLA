package top.egon.cola.component.ddc.admin.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.admin.model.entity.DdcInstanceEntity;
import top.egon.cola.component.ddc.admin.model.enums.InstanceStatus;
import top.egon.cola.component.ddc.admin.repository.DdcConfigLeaseRedisRepository;
import top.egon.cola.component.ddc.admin.repository.DdcInstanceRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcLeaseExpiryScannerTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:30Z");

    @Test
    void removesExpiredIndexAndMarksOnlyTheMatchingLeaseOffline() {
        DdcInstanceRepository instanceRepository = mock(DdcInstanceRepository.class);
        DdcConfigLeaseRedisRepository leaseRepository = mock(DdcConfigLeaseRedisRepository.class);
        DdcInstanceEntity expired = expiredInstance();
        when(instanceRepository.findByStatusAndLeaseExpireAtLessThanEqual(
                InstanceStatus.ONLINE.name(),
                LocalDateTime.ofInstant(NOW, ZoneOffset.UTC)
        )).thenReturn(List.of(expired));
        when(leaseRepository.removeExpiredProjection(
                "default", "dev", "demo", "instance-1", "lease-1", NOW
        )).thenReturn(true);
        when(instanceRepository.markOfflineIfLeaseMatches(
                "instance-1",
                "lease-1",
                InstanceStatus.OFFLINE.name(),
                LocalDateTime.ofInstant(NOW, ZoneOffset.UTC)
        )).thenReturn(1);
        DdcLeaseExpiryScanner scanner = scanner(instanceRepository, leaseRepository);

        assertThat(scanner.scanExpired()).isEqualTo(1);

        verify(instanceRepository).markOfflineIfLeaseMatches(
                "instance-1",
                "lease-1",
                InstanceStatus.OFFLINE.name(),
                LocalDateTime.ofInstant(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void leavesProjectionOnlineWhenRedisContainsANewerLease() {
        DdcInstanceRepository instanceRepository = mock(DdcInstanceRepository.class);
        DdcConfigLeaseRedisRepository leaseRepository = mock(DdcConfigLeaseRedisRepository.class);
        DdcInstanceEntity expired = expiredInstance();
        when(instanceRepository.findByStatusAndLeaseExpireAtLessThanEqual(
                InstanceStatus.ONLINE.name(),
                LocalDateTime.ofInstant(NOW, ZoneOffset.UTC)
        )).thenReturn(List.of(expired));
        when(leaseRepository.removeExpiredProjection(
                "default", "dev", "demo", "instance-1", "lease-1", NOW
        )).thenReturn(false);
        DdcLeaseExpiryScanner scanner = scanner(instanceRepository, leaseRepository);

        assertThat(scanner.scanExpired()).isZero();

        verify(instanceRepository, never()).markOfflineIfLeaseMatches(
                "instance-1",
                "lease-1",
                InstanceStatus.OFFLINE.name(),
                LocalDateTime.ofInstant(NOW, ZoneOffset.UTC)
        );
    }

    private DdcLeaseExpiryScanner scanner(DdcInstanceRepository instanceRepository,
                                          DdcConfigLeaseRedisRepository leaseRepository) {
        return new DdcLeaseExpiryScanner(
                instanceRepository,
                leaseRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private DdcInstanceEntity expiredInstance() {
        DdcInstanceEntity instance = new DdcInstanceEntity();
        instance.setInstanceId("instance-1");
        instance.setLeaseId("lease-1");
        instance.setBizCode("default");
        instance.setAppCode("demo");
        instance.setEnv("dev");
        instance.setStatus(InstanceStatus.ONLINE.name());
        instance.setLeaseExpireAt(LocalDateTime.ofInstant(NOW.minusSeconds(1), ZoneOffset.UTC));
        return instance;
    }
}
