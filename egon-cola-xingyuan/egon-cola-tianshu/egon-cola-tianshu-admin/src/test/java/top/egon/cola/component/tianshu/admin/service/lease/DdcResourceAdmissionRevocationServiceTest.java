package top.egon.cola.component.tianshu.admin.service.lease;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigLeaseRedisRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcInstanceRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcServiceRegistryRedisRepository;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextDTO;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextExecutor;
import top.egon.cola.component.tianshu.model.management.DdcResourceAdmissionRevocationRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcResourceAdmissionRevocationServiceTest {

    @Test
    void revokesOnlyExactTripleAndIsIdempotentForReplay() {
        DdcConfigLeaseRedisRepository configLeases =
                mock(DdcConfigLeaseRedisRepository.class);
        DdcServiceRegistryRedisRepository providerLeases =
                mock(DdcServiceRegistryRedisRepository.class);
        DdcInstanceRepository instances = mock(DdcInstanceRepository.class);
        DdcResourceAdmissionRevocationRequest request =
                new DdcResourceAdmissionRevocationRequest(
                        "permission-tianquan-shoubing-prod", "permission", "tianquan-shoubing", "prod", 7L
                );
        when(configLeases.revokeResourceAdmission(
                "permission-tianquan-shoubing-prod", "permission", "prod", "tianquan-shoubing", 7L
        )).thenReturn(2, 0);
        when(providerLeases.revokeResourceAdmission(
                "permission-tianquan-shoubing-prod", "permission", "prod", "tianquan-shoubing", 7L
        )).thenReturn(3, 0);
        when(instances.markResourceAdmissionOffline(
                "permission-tianquan-shoubing-prod", "permission", "prod", "tianquan-shoubing", 7L,
                Instant.parse("2026-08-10T00:00:00Z")
        )).thenReturn(2, 0);
        DdcResourceAdmissionRevocationService service =
                new DdcResourceAdmissionRevocationService(
                        configLeases,
                        providerLeases,
                        instances,
                        Clock.fixed(
                                Instant.parse("2026-08-10T00:00:00Z"),
                                ZoneOffset.UTC
                        )
                );

        DdcTenantContextExecutor contextExecutor = new DdcTenantContextExecutor();
        DdcTenantContextDTO tenant = new DdcTenantContextDTO(101L, "tenant-a", "service:test");
        var results = contextExecutor.call(tenant, () -> java.util.List.of(
                service.revoke(request), service.revoke(request)));
        var first = results.get(0);
        var replay = results.get(1);

        assertThat(first.configLeaseCount()).isEqualTo(2);
        assertThat(first.providerLeaseCount()).isEqualTo(3);
        assertThat(first.persistedInstanceCount()).isEqualTo(2);
        assertThat(replay.configLeaseCount()).isZero();
        assertThat(replay.providerLeaseCount()).isZero();
        assertThat(replay.persistedInstanceCount()).isZero();
        verify(configLeases, org.mockito.Mockito.times(2))
                .revokeResourceAdmission(
                        "permission-tianquan-shoubing-prod", "permission", "prod", "tianquan-shoubing", 7L
                );
    }

    @Test
    void rejectsMissingTrustedAuditContextBeforeRevokingRedisLeases() {
        DdcConfigLeaseRedisRepository configLeases = mock(DdcConfigLeaseRedisRepository.class);
        DdcServiceRegistryRedisRepository providerLeases = mock(DdcServiceRegistryRedisRepository.class);
        DdcInstanceRepository instances = mock(DdcInstanceRepository.class);
        DdcResourceAdmissionRevocationRequest request = new DdcResourceAdmissionRevocationRequest(
                "resource-1", "biz", "env", "app", 1L);
        DdcResourceAdmissionRevocationService service = new DdcResourceAdmissionRevocationService(
                configLeases, providerLeases, instances, Clock.systemUTC());

        assertThatThrownBy(() -> service.revoke(request)).isInstanceOf(IllegalStateException.class)
                .hasMessage("TENANT_CONTEXT_MISSING");

        verifyNoInteractions(configLeases, providerLeases, instances);
    }
}
