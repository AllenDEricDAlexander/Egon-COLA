package top.egon.cola.platform.tianquan.jianshen.admin.config.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import top.egon.cola.component.tianshu.http.registration.DdcHttpRegistrationRuntime;
import top.egon.cola.component.tianshu.http.registration.DdcHttpRegistrationState;
import top.egon.cola.component.tianshu.model.lease.DdcLeaseRole;
import top.egon.cola.component.tianshu.model.lease.DdcLeaseSession;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.vo.DefinitionStatusVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.vo.FenceMutationStatusVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.vo.FlywayStatusVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.vo.OperationalStatusVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.vo.OutboxStatusVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.vo.ProviderLeaseStatusVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.vo.RedisProjectionStatusVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.vo.RuntimeStatusVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.tianshu.DdcConfigClientStatusRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.service.GatewayDdcRuntimeStatusService;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.service.Rbac3OperationalRuntimeStatusService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Rbac3IndependentProviderStatusTest {

    private static final Instant NOW = Instant.parse("2026-09-06T03:00:00Z");

    @Test
    void reportsTheActualHttpLeaseWhenLegacyGatewayReportingIsDisabled() {
        DdcHttpRegistrationRuntime provider = mock(DdcHttpRegistrationRuntime.class);
        when(provider.state()).thenReturn(DdcHttpRegistrationState.REGISTERED);
        when(provider.instanceId()).thenReturn("tianquan-jianshen-local-1");
        when(provider.lease()).thenReturn(Optional.of(new DdcLeaseSession(
                "tianquan-jianshen-local-1", "lease-1", DdcLeaseRole.HTTP_PROVIDER, 30, 10,
                NOW, NOW.plusSeconds(30))));

        RuntimeStatusVO status = status(new StaticListableBeanFactory(Map.of("httpRuntime", provider)));

        assertThat(status.providerLease()).isEqualTo(new ProviderLeaseStatusVO(
                "REGISTERED", "tianquan-jianshen-local-1", NOW.plusSeconds(30)));
        assertThat(status.definition()).isEqualTo(new DefinitionStatusVO(
                "UNKNOWN", null, List.of("CONTROL_PLANE_DISABLED")));
        assertThat(status.gatewayRelease().status()).isEqualTo("UNKNOWN");
        assertThat(status.checkedAt()).isEqualTo(NOW);
    }

    @Test
    void doesNotInventALeaseWhenTheHttpRuntimeIsAbsent() {
        assertThat(status(new StaticListableBeanFactory()).providerLease())
                .isEqualTo(new ProviderLeaseStatusVO("STOPPED", null, null));
    }

    private RuntimeStatusVO status(StaticListableBeanFactory beans) {
        Rbac3OperationalRuntimeStatusService operational = mock(Rbac3OperationalRuntimeStatusService.class);
        when(operational.status()).thenReturn(new OperationalStatusVO(
                new FlywayStatusVO("UP_TO_DATE", "UP_TO_DATE"),
                new RedisProjectionStatusVO("HEALTHY", 0),
                new FenceMutationStatusVO("HEALTHY", 0, 0, 0),
                new OutboxStatusVO("HEALTHY", 0, 0)));
        return new Rbac3PlatformIntegrationConfiguration().rbac3ControlPlaneRuntimeStatusPort(
                beans.getBeanProvider(GatewayDdcRuntimeStatusService.class),
                beans.getBeanProvider(DdcHttpRegistrationRuntime.class),
                beans.getBeanProvider(DdcConfigClientStatusRepository.class),
                operational, Clock.fixed(NOW, ZoneOffset.UTC)).status();
    }
}
