package top.egon.cola.component.ddc.admin.service.lease;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.admin.repository.DdcConfigLeaseRedisRepository;
import top.egon.cola.component.ddc.admin.repository.DdcInstanceRepository;
import top.egon.cola.component.ddc.admin.repository.DdcServiceRegistryRedisRepository;
import top.egon.cola.component.ddc.model.management.DdcResourceAdmissionRevocationRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
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
                        "permission-idp-prod", "permission", "idp", "prod", 7L
                );
        when(configLeases.revokeResourceAdmission(
                "permission-idp-prod", "permission", "prod", "idp", 7L
        )).thenReturn(2, 0);
        when(providerLeases.revokeResourceAdmission(
                "permission-idp-prod", "permission", "prod", "idp", 7L
        )).thenReturn(3, 0);
        when(instances.markResourceAdmissionOffline(
                "permission-idp-prod", "permission", "prod", "idp", 7L,
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

        var first = service.revoke(request);
        var replay = service.revoke(request);

        assertThat(first.configLeaseCount()).isEqualTo(2);
        assertThat(first.providerLeaseCount()).isEqualTo(3);
        assertThat(first.persistedInstanceCount()).isEqualTo(2);
        assertThat(replay.configLeaseCount()).isZero();
        assertThat(replay.providerLeaseCount()).isZero();
        assertThat(replay.persistedInstanceCount()).isZero();
        verify(configLeases, org.mockito.Mockito.times(2))
                .revokeResourceAdmission(
                        "permission-idp-prod", "permission", "prod", "idp", 7L
                );
    }
}
