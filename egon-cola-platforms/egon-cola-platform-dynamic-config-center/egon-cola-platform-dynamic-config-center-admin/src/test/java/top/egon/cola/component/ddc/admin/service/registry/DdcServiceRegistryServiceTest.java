package top.egon.cola.component.ddc.admin.service.registry;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.admin.repository.DdcServiceRegistryRedisRepository;
import top.egon.cola.component.ddc.admin.service.lease.DdcLeaseValidator;
import top.egon.cola.component.ddc.admin.service.metadata.DdcScopeGate;
import top.egon.cola.component.ddc.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.registry.model.DdcServiceKind;
import top.egon.cola.component.ddc.registry.model.DdcServiceKey;
import top.egon.cola.component.ddc.registry.model.DdcServiceRegistration;
import top.egon.cola.component.ddc.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.lease.DdcLeaseSession;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcServiceRegistryServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");

    @Test
    void everyRegistrationCreatesAndPersistsANewLease() {
        DdcServiceRegistryRedisRepository repository =
                mock(DdcServiceRegistryRedisRepository.class);
        AtomicInteger sequence = new AtomicInteger();
        DdcServiceRegistryService service = service(
                repository,
                () -> "lease-" + sequence.incrementAndGet()
        );

        DdcLeaseSession first = service.register(registration(Map.of("zone", "east")));
        DdcLeaseSession second = service.register(registration(Map.of("zone", "east")));

        assertThat(first.leaseId()).isEqualTo("lease-1");
        assertThat(second.leaseId()).isEqualTo("lease-2");
        assertThat(second.leaseId()).isNotEqualTo(first.leaseId());
        verify(repository, org.mockito.Mockito.times(2)).register(any());
    }

    @Test
    void rejectsReservedOrSensitiveMetadata() {
        assertThatThrownBy(() -> registration(Map.of("egon.rpc.weight", "100")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reserved");
        assertThatThrownBy(() -> registration(Map.of("database.password", "secret")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sensitive");
    }

    @Test
    void heartbeatAndDeregisterPreserveStrictLeaseResults() {
        DdcServiceRegistryRedisRepository repository =
                mock(DdcServiceRegistryRedisRepository.class);
        DdcServiceRegistryService service = service(repository, () -> "lease-1");
        DdcLeaseSession session = service.register(registration(Map.of()));
        when(repository.heartbeat(any(), any())).thenReturn(new DdcLeaseOperationResult(
                DdcLeaseOperationStatus.LEASE_MISMATCH,
                null
        ));
        when(repository.deregister(any(), any())).thenReturn(new DdcLeaseOperationResult(
                DdcLeaseOperationStatus.NOT_DELETED,
                null
        ));

        assertThat(service.heartbeat(service.leaseRequest(registration(Map.of()), session)))
                .extracting(DdcLeaseOperationResult::status)
                .isEqualTo(DdcLeaseOperationStatus.LEASE_MISMATCH);
        assertThat(service.deregister(service.leaseRequest(registration(Map.of()), session)))
                .extracting(DdcLeaseOperationResult::status)
                .isEqualTo(DdcLeaseOperationStatus.NOT_DELETED);
    }

    private DdcServiceRegistryService service(DdcServiceRegistryRedisRepository repository,
                                              java.util.function.Supplier<String> leaseIds) {
        return new DdcServiceRegistryService(
                repository,
                new DdcLeaseValidator(),
                mock(DdcScopeGate.class),
                Clock.fixed(NOW, ZoneOffset.UTC),
                leaseIds
        );
    }

    private DdcServiceRegistration registration(Map<String, String> metadata) {
        return new DdcServiceRegistration(
                "provider-1",
                new DdcServiceKey(
                        "pay-biz",
                        "dev",
                        "orders-app",
                        DdcServiceKind.RPC_PROVIDER,
                        "order.v1.OrderQueryService",
                        "default",
                        "1.0.0",
                        "grpc"
                ),
                "127.0.0.1",
                19090,
                false,
                metadata,
                30,
                10
        );
    }
}
