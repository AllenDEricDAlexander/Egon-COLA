package top.egon.cola.component.tianshu.admin.service.lease;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.tianshu.admin.model.bo.DdcInstanceBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcInstanceResultConverter;
import top.egon.cola.component.tianshu.admin.model.vo.DdcInstanceResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcInstanceRepository;
import top.egon.cola.component.tianshu.model.config.DdcInstanceRegisterRequest;
import top.egon.cola.component.tianshu.model.lease.DdcLeaseRole;
import top.egon.cola.component.tianshu.model.lease.DdcLeaseSession;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static top.egon.cola.component.tianshu.admin.security.registration.DdcRegistrationTestFixture.identity;

@ResourceLock("java.util.TimeZone.default")
class DdcInstanceAdminServiceTest {

    @Test
    void pagesPersistentInstancesNewestFirst() {
        DdcInstanceRepository repository = mock(DdcInstanceRepository.class);
        DdcConfigLeaseService leaseService = mock(DdcConfigLeaseService.class);
        DdcInstanceResultConverter converter = mock(DdcInstanceResultConverter.class);
        DdcInstanceBO instance = DdcInstanceBO.builder().publicId("instance-1").instanceId("engine-1").build();
        DdcInstanceResult result = DdcInstanceResult.builder().publicId("instance-1").instanceId("engine-1").build();
        when(repository.findByBizCodeAndEnvAndAppCode(
                eq("infra"), eq("prod"), eq("yuheng"), any(PageQuery.class)))
                .thenReturn(DdcPageResult.<DdcInstanceBO>builder()
                        .records(List.of(instance)).total(1).pageNo(1).pageSize(10).build());
        when(converter.toTargetList(List.of(instance))).thenReturn(List.of(result));
        DdcInstanceAdminService service = new DdcInstanceAdminService(
                repository, leaseService, converter);

        var page = service.page(
                "infra", "prod", "yuheng", new PageQuery(1, 10));

        assertThat(page.getRecords()).containsExactly(result);
        var pageQuery = org.mockito.ArgumentCaptor.forClass(PageQuery.class);
        verify(repository).findByBizCodeAndEnvAndAppCode(
                eq("infra"), eq("prod"), eq("yuheng"), pageQuery.capture());
        assertThat(pageQuery.getValue().pageNo()).isEqualTo(1);
        assertThat(pageQuery.getValue().pageSize()).isEqualTo(10);
    }

    @Test
    void storesLeaseExpiryInTheDatabaseLocalTimeConvention() {
        TimeZone previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        try {
            DdcInstanceRepository repository = mock(
                    DdcInstanceRepository.class
            );
            DdcInstanceResultConverter converter = mock(DdcInstanceResultConverter.class);
            DdcConfigLeaseService leaseService = mock(
                    DdcConfigLeaseService.class
            );
            DdcInstanceRegisterRequest request = request();
            Instant expiry = Instant.parse("2026-07-27T08:30:00Z");
            DdcLeaseSession session = new DdcLeaseSession(
                            request.getInstanceId(),
                            "lease-1",
                            DdcLeaseRole.CONFIG_CLIENT,
                            30,
                            10,
                            expiry.minusSeconds(30),
                            expiry
                    );
            when(leaseService.registerAdmitted(request)).thenReturn(
                    new DdcConfigLeaseService.AdmittedRegistration(
                            session,
                            identity(expiry.plusSeconds(10))
                    )
            );
            when(repository.findByInstanceId(request.getInstanceId()))
                    .thenReturn(Optional.empty());
            when(repository.insert(any())).thenAnswer(invocation ->
                    invocation.getArgument(0));
            DdcInstanceAdminService service = new DdcInstanceAdminService(
                    repository,
                    leaseService,
                    converter);

            service.register(request);

            var entity = org.mockito.ArgumentCaptor.forClass(
                    DdcInstanceBO.class
            );
            verify(repository).insert(entity.capture());
            assertThat(entity.getValue().getLeaseExpireAt()).isEqualTo(
                    LocalDateTime.ofInstant(expiry, ZoneId.systemDefault())
            );
            assertThat(entity.getValue().getResourceServerId())
                    .isEqualTo("resource-1");
            assertThat(entity.getValue().getResourceVersion()).isEqualTo(7L);
            assertThat(entity.getValue().getCredentialId())
                    .isEqualTo("credential-1");
            assertThat(entity.getValue().getAdmissionExpiresAt()).isEqualTo(
                    LocalDateTime.ofInstant(
                            expiry.plusSeconds(10),
                            ZoneId.systemDefault()
                    )
            );
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    private DdcInstanceRegisterRequest request() {
        DdcInstanceRegisterRequest request = new DdcInstanceRegisterRequest();
        request.setInstanceId("engine-1");
        request.setBizCode("default");
        request.setAppCode("yuheng-biz-gateway-default");
        request.setEnv("test");
        request.setNamespace("yuheng-live");
        request.setHost("127.0.0.1");
        request.setPid("123");
        request.setSdkVersion("5.2.3");
        request.setLeaseSeconds(30);
        request.setHeartbeatIntervalSeconds(10);
        request.setRegistrationToken("test-registration-token");
        return request;
    }
}
