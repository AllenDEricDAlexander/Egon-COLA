package top.egon.cola.component.ddc.admin.service.lease;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import top.egon.cola.component.ddc.admin.model.entity.DdcInstanceEntity;
import top.egon.cola.component.ddc.admin.repository.DdcInstanceRepository;
import top.egon.cola.component.ddc.model.config.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ResourceLock("java.util.TimeZone.default")
class DdcInstanceAdminServiceTest {

    @Test
    void storesLeaseExpiryInTheDatabaseLocalTimeConvention() {
        TimeZone previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        try {
            DdcInstanceRepository repository = mock(
                    DdcInstanceRepository.class
            );
            DdcConfigLeaseService leaseService = mock(
                    DdcConfigLeaseService.class
            );
            DdcInstanceRegisterRequest request = request();
            Instant expiry = Instant.parse("2026-07-27T08:30:00Z");
            when(leaseService.register(request)).thenReturn(
                    new DdcLeaseSession(
                            request.getInstanceId(),
                            "lease-1",
                            DdcLeaseRole.CONFIG_CLIENT,
                            30,
                            10,
                            expiry.minusSeconds(30),
                            expiry
                    )
            );
            when(repository.findByInstanceId(request.getInstanceId()))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(invocation ->
                    invocation.getArgument(0));
            DdcInstanceAdminService service = new DdcInstanceAdminService(
                    repository,
                    leaseService
            );

            service.register(request);

            var entity = org.mockito.ArgumentCaptor.forClass(
                    DdcInstanceEntity.class
            );
            verify(repository).save(entity.capture());
            assertThat(entity.getValue().getLeaseExpireAt()).isEqualTo(
                    LocalDateTime.ofInstant(expiry, ZoneId.systemDefault())
            );
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    private DdcInstanceRegisterRequest request() {
        DdcInstanceRegisterRequest request = new DdcInstanceRegisterRequest();
        request.setInstanceId("engine-1");
        request.setAppCode("gateway-engine-default");
        request.setEnv("test");
        request.setNamespace("gateway-live");
        request.setHost("127.0.0.1");
        request.setPid("123");
        request.setSdkVersion("5.2.3");
        return request;
    }
}
