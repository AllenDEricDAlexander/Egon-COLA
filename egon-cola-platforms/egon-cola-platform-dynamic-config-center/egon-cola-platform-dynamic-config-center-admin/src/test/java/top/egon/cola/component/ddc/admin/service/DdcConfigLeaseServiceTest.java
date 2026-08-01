package top.egon.cola.component.ddc.admin.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.repository.DdcConfigLeaseRedisRepository;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.dto.DdcPublishTarget;
import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DdcConfigLeaseServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");

    @Test
    void eachRegistrationReplacesTheLeaseAndInvalidatesTheOldIdentity() {
        DdcConfigLeaseRedisRepository repository = mock(DdcConfigLeaseRedisRepository.class);
        AtomicReference<String> currentLeaseId = new AtomicReference<>();
        doAnswer(invocation -> {
            DdcLeaseSession session = invocation.getArgument(1);
            currentLeaseId.set(session.leaseId());
            return null;
        }).when(repository).register(any(), any(), any());
        when(repository.heartbeat(any(), any())).thenAnswer(invocation -> {
            DdcHeartbeatRequest request = invocation.getArgument(0);
            return request.getLeaseId().equals(currentLeaseId.get())
                    ? new DdcLeaseOperationResult(DdcLeaseOperationStatus.RENEWED, NOW.plusSeconds(30))
                    : new DdcLeaseOperationResult(DdcLeaseOperationStatus.LEASE_MISMATCH, null);
        });
        DdcConfigLeaseService service = service(repository);

        DdcLeaseSession first = service.register(registerRequest());
        DdcLeaseSession second = service.register(registerRequest());

        assertThat(first.leaseId()).isNotEqualTo(second.leaseId());
        assertThat(service.heartbeat(heartbeatRequest(first.leaseId())).status())
                .isEqualTo(DdcLeaseOperationStatus.LEASE_MISMATCH);
        assertThat(service.heartbeat(heartbeatRequest(second.leaseId())).status())
                .isEqualTo(DdcLeaseOperationStatus.RENEWED);
    }

    @Test
    void heartbeatReturnsRepositoryExpiryAndDoesNotRecreateMissingLease() {
        DdcConfigLeaseRedisRepository repository = mock(DdcConfigLeaseRedisRepository.class);
        DdcHeartbeatRequest request = heartbeatRequest("lease-1");
        DdcLeaseOperationResult renewed =
                new DdcLeaseOperationResult(DdcLeaseOperationStatus.RENEWED, NOW.plusSeconds(30));
        when(repository.heartbeat(request, NOW)).thenReturn(renewed);
        DdcConfigLeaseService service = service(repository);

        assertThat(service.heartbeat(request)).isEqualTo(renewed);

        DdcHeartbeatRequest missing = heartbeatRequest("missing");
        when(repository.heartbeat(missing, NOW))
                .thenReturn(new DdcLeaseOperationResult(DdcLeaseOperationStatus.NOT_FOUND, null));
        assertThat(service.heartbeat(missing).status()).isEqualTo(DdcLeaseOperationStatus.NOT_FOUND);
        verify(repository, never()).register(any(), any(), any());
    }

    @Test
    void staleDeregisterLeavesTheCurrentLeaseIntact() {
        DdcConfigLeaseRedisRepository repository = mock(DdcConfigLeaseRedisRepository.class);
        DdcHeartbeatRequest oldRequest = heartbeatRequest("old-lease");
        when(repository.deregister(oldRequest))
                .thenReturn(new DdcLeaseOperationResult(DdcLeaseOperationStatus.NOT_DELETED, null));
        DdcConfigLeaseService service = service(repository);

        assertThat(service.deregister(oldRequest).status())
                .isEqualTo(DdcLeaseOperationStatus.NOT_DELETED);
        verify(repository).deregister(oldRequest);
    }

    @Test
    void invalidLeaseTimingFailsBeforeRedisAccess() {
        DdcConfigLeaseRedisRepository repository = mock(DdcConfigLeaseRedisRepository.class);
        DdcConfigLeaseService service = service(repository);
        DdcInstanceRegisterRequest request = registerRequest();
        request.setLeaseSeconds(4);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(DdcAdminException.class);

        request.setLeaseSeconds(30);
        request.setHeartbeatIntervalSeconds(30);
        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(DdcAdminException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void publishTargetsComeOnlyFromCurrentRedisLeases() {
        DdcConfigLeaseRedisRepository repository = mock(DdcConfigLeaseRedisRepository.class);
        when(repository.activeTargets("default", "dev", "demo", NOW))
                .thenReturn(List.of(new DdcPublishTarget("instance-1", "lease-1")));
        DdcConfigLeaseService service = service(repository);

        assertThat(service.activeTargets("default", "dev", "demo"))
                .containsExactly(new DdcPublishTarget("instance-1", "lease-1"));

        verify(repository).activeTargets("default", "dev", "demo", NOW);
    }

    private DdcConfigLeaseService service(DdcConfigLeaseRedisRepository repository) {
        AtomicInteger sequence = new AtomicInteger();
        return new DdcConfigLeaseService(
                repository,
                new DdcLeaseValidator(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> "lease-" + sequence.incrementAndGet()
        );
    }

    private DdcInstanceRegisterRequest registerRequest() {
        DdcInstanceRegisterRequest request = new DdcInstanceRegisterRequest();
        request.setInstanceId("instance-1");
        request.setBizCode("default");
        request.setAppCode("demo");
        request.setEnv("dev");
        request.setHost("127.0.0.1");
        request.setPort(8080);
        request.setPid("100");
        request.setSdkVersion("5.2.3");
        request.setLeaseSeconds(30);
        request.setHeartbeatIntervalSeconds(10);
        return request;
    }

    private DdcHeartbeatRequest heartbeatRequest(String leaseId) {
        DdcHeartbeatRequest request = new DdcHeartbeatRequest();
        request.setInstanceId("instance-1");
        request.setLeaseId(leaseId);
        request.setBizCode("default");
        request.setAppCode("demo");
        request.setEnv("dev");
        return request;
    }
}
