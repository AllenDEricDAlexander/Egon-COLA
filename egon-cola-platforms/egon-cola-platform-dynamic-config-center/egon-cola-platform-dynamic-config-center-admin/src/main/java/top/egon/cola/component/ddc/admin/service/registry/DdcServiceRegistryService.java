package top.egon.cola.component.ddc.admin.service.registry;

import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.repository.DdcServiceRegistryRedisRepository;
import top.egon.cola.component.ddc.admin.security.admission.DdcAdmissionClaims;
import top.egon.cola.component.ddc.admin.security.admission.DdcAdmissionVerifier;
import top.egon.cola.component.ddc.admin.service.lease.DdcLeaseValidator;
import top.egon.cola.component.ddc.admin.service.metadata.DdcScopeGate;
import top.egon.cola.component.ddc.model.registry.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;

import java.time.Clock;
import java.time.Instant;
import java.util.function.Supplier;

public class DdcServiceRegistryService {

    private final DdcServiceRegistryRedisRepository repository;

    private final DdcLeaseValidator leaseValidator;

    private final DdcScopeGate scopeGate;

    private final DdcAdmissionVerifier admissionVerifier;

    private final Clock clock;

    private final Supplier<String> leaseIdSupplier;

    public DdcServiceRegistryService(DdcServiceRegistryRedisRepository repository,
                                     DdcLeaseValidator leaseValidator,
                                     DdcScopeGate scopeGate,
                                     DdcAdmissionVerifier admissionVerifier) {
        this(
                repository,
                leaseValidator,
                scopeGate,
                admissionVerifier,
                Clock.systemUTC(),
                UuidV7::simpleString
        );
    }

    DdcServiceRegistryService(DdcServiceRegistryRedisRepository repository,
                              DdcLeaseValidator leaseValidator,
                              DdcScopeGate scopeGate,
                              DdcAdmissionVerifier admissionVerifier,
                              Clock clock,
                              Supplier<String> leaseIdSupplier) {
        this.repository = repository;
        this.leaseValidator = leaseValidator;
        this.scopeGate = scopeGate;
        this.admissionVerifier = admissionVerifier;
        this.clock = clock;
        this.leaseIdSupplier = leaseIdSupplier;
    }

    public DdcLeaseSession register(DdcServiceRegistration registration) {
        validateRegistration(registration);
        DdcServiceKey serviceKey = registration.serviceKey();
        scopeGate.assertPhysicalEnabled(
                serviceKey.bizCode(),
                serviceKey.appCode(),
                serviceKey.env()
        );
        DdcAdmissionClaims admission = admissionVerifier.verify(
                registration.admissionTicket(),
                serviceKey.bizCode(),
                serviceKey.appCode(),
                serviceKey.env(),
                registration.instanceId()
        );
        Instant now = clock.instant();
        DdcLeaseSession session = new DdcLeaseSession(
                registration.instanceId(),
                leaseIdSupplier.get(),
                registration.serviceKey().serviceKind().leaseRole(),
                registration.leaseSeconds(),
                registration.heartbeatIntervalSeconds(),
                now,
                leaseValidator.capLeaseExpireAt(
                        now,
                        registration.leaseSeconds(),
                        admission
                )
        );
        repository.register(new DdcServiceInstance(
                registration.instanceId(),
                session.leaseId(),
                registration.serviceKey(),
                registration.host(),
                registration.port(),
                registration.secure(),
                registration.metadata(),
                registration.leaseSeconds(),
                registration.heartbeatIntervalSeconds(),
                now,
                now,
                session.leaseExpireAt(),
                "ONLINE",
                0L,
                admission.resourceServerId(),
                admission.resourceVersion(),
                admission.credentialId(),
                admission.expiresAt()
        ));
        return session;
    }

    public DdcLeaseOperationResult heartbeat(DdcServiceLeaseRequest request) {
        validateOperation(request);
        DdcServiceKey serviceKey = request.getServiceKey();
        DdcAdmissionClaims admission = admissionVerifier.verify(
                request.getAdmissionTicket(),
                serviceKey.bizCode(),
                serviceKey.appCode(),
                serviceKey.env(),
                request.getInstanceId()
        );
        return repository.heartbeat(request, admission, clock.instant());
    }

    public DdcLeaseOperationResult deregister(DdcServiceLeaseRequest request) {
        validateOperation(request);
        return repository.deregister(request, clock.instant());
    }

    public DdcServiceSnapshot getInstances(DdcServiceKey serviceKey) {
        if (serviceKey == null) {
            throw new DdcAdminException("serviceKey is required");
        }
        return repository.getInstances(serviceKey, clock.instant());
    }

    public DdcServiceCatalogSnapshot getServiceKeys(DdcServiceQuery query) {
        if (query == null) {
            throw new DdcAdminException("service query is required");
        }
        return repository.getServiceKeys(query, clock.instant());
    }

    DdcServiceLeaseRequest leaseRequest(DdcServiceRegistration registration,
                                        DdcLeaseSession session) {
        DdcServiceLeaseRequest request = new DdcServiceLeaseRequest();
        request.setServiceKey(registration.serviceKey());
        request.setInstanceId(registration.instanceId());
        request.setLeaseId(session.leaseId());
        request.setAdmissionTicket(registration.admissionTicket());
        return request;
    }

    private void validateRegistration(DdcServiceRegistration registration) {
        if (registration == null) {
            throw new DdcAdminException("service registration is required");
        }
        leaseValidator.validateServiceRegistration(registration);
    }

    private void validateOperation(DdcServiceLeaseRequest request) {
        if (request == null
                || request.getServiceKey() == null
                || blank(request.getInstanceId())
                || blank(request.getLeaseId())) {
            throw new DdcAdminException("complete service lease identity is required");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
