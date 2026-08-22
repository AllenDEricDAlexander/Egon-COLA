package top.egon.cola.component.ddc.test;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.egon.cola.component.ddc.admin.repository.DdcServiceRegistryRedisRepository;
import top.egon.cola.component.ddc.admin.security.registration.DdcRegistrationAuthenticationException;
import top.egon.cola.component.ddc.admin.security.registration.DdcRegistrationCredentialVerifier;
import top.egon.cola.component.ddc.admin.security.registration.VerifiedDdcRegistrationIdentity;
import top.egon.cola.component.ddc.admin.service.lease.DdcLeaseValidator;
import top.egon.cola.component.ddc.admin.service.metadata.DdcScopeGate;
import top.egon.cola.component.ddc.admin.service.registry.DdcServiceRegistryService;
import top.egon.cola.component.ddc.error.DdcErrorStatus;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 验收 DDC 对精确业务域、应用、环境和实例 Ticket 的生命周期约束。
 * Accepts DDC lifecycle constraints for exact business, application, environment, and instance Tickets.
 */
class DdcResourceAdmissionLifecycleTest {

    private static final Instant EXPIRES_AT = Instant.now().plusSeconds(300);

    @Test
    void admitsEveryInstanceOfTheRegisteredTripleAndRejectsAnotherApplication() {
        DdcServiceRegistryRedisRepository repository =
                mock(DdcServiceRegistryRedisRepository.class);
        MutableVerifier verifier = new MutableVerifier();
        DdcServiceRegistryService service = service(repository, verifier);

        service.register(registration("idp-1", "idp"));
        service.register(registration("idp-2", "idp"));

        ArgumentCaptor<DdcServiceInstance> instances =
                ArgumentCaptor.forClass(DdcServiceInstance.class);
        verify(repository, org.mockito.Mockito.times(2))
                .register(instances.capture(), any());
        assertThat(instances.getAllValues())
                .extracting(DdcServiceInstance::instanceId)
                .containsExactly("idp-1", "idp-2");
        assertThatThrownBy(() -> service.register(
                registration("forged-rbac3-1", "rbac3")))
                .isInstanceOf(DdcRegistrationAuthenticationException.class);
    }

    @Test
    void idpUnavailabilityAllowsNeitherNewLeaseNorHeartbeatExtension() {
        DdcServiceRegistryRedisRepository repository =
                mock(DdcServiceRegistryRedisRepository.class);
        MutableVerifier verifier = new MutableVerifier();
        DdcServiceRegistryService service = service(repository, verifier);
        DdcServiceRegistration admitted = registration("idp-1", "idp");
        var lease = service.register(admitted);
        verifier.available.set(false);

        assertThat(lease.leaseExpireAt()).isBeforeOrEqualTo(EXPIRES_AT);
        assertThatThrownBy(() -> service.register(
                registration("idp-2", "idp")))
                .isInstanceOf(DdcRegistrationAuthenticationException.class);
        DdcServiceLeaseRequest heartbeat = new DdcServiceLeaseRequest();
        heartbeat.setServiceKey(admitted.serviceKey());
        heartbeat.setInstanceId(admitted.instanceId());
        heartbeat.setLeaseId(lease.leaseId());
        heartbeat.setRegistrationToken(admitted.registrationToken());
        assertThatThrownBy(() -> service.heartbeat(heartbeat))
                .isInstanceOf(DdcRegistrationAuthenticationException.class);
        verify(repository, never()).heartbeat(any(), any(), any());
    }

    private DdcServiceRegistryService service(
            DdcServiceRegistryRedisRepository repository,
            DdcRegistrationCredentialVerifier verifier
    ) {
        return new DdcServiceRegistryService(
                repository,
                new DdcLeaseValidator(),
                mock(DdcScopeGate.class),
                verifier);
    }

    private DdcServiceRegistration registration(
            String instanceId,
            String appCode
    ) {
        return new DdcServiceRegistration(
                instanceId,
                new DdcServiceKey(
                        "permission", "prod", appCode,
                        DdcServiceKind.HTTP_PROVIDER, appCode + "-admin",
                        "default", "5.3.3", "http"),
                "10.0.0.8", 8080, false, Map.of(), 30, 10,
                "token:" + instanceId);
    }

    private static final class MutableVerifier
            implements DdcRegistrationCredentialVerifier {

        private final AtomicBoolean available = new AtomicBoolean(true);

        @Override
        public VerifiedDdcRegistrationIdentity verify(
                String token,
                String bizCode,
                String appCode,
                String env,
                String instanceId
        ) {
            if (!available.get()) {
                throw new DdcRegistrationAuthenticationException(
                        DdcErrorStatus.RESOURCE_ADMISSION_INVALID);
            }
            if (!"permission".equals(bizCode)
                    || !"idp".equals(appCode)
                    || !"prod".equals(env)
                    || !("token:" + instanceId).equals(token)) {
                throw new DdcRegistrationAuthenticationException(
                        DdcErrorStatus.RESOURCE_ADMISSION_BINDING_MISMATCH);
            }
            return new VerifiedDdcRegistrationIdentity(
                    "app-id",
                    "client-id",
                    "permission-idp-prod",
                    "https://api.egon.internal/prod/permission/idp",
                    7L, bizCode, appCode, env, instanceId, "idp-key-1",
                    "token-1", EXPIRES_AT.minusSeconds(60), EXPIRES_AT,
                    java.util.Set.of("ddc:registration:write"));
        }
    }
}
