package top.egon.cola.component.ddc.admin.service;

import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.repository.DdcServiceRegistryRedisRepository;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.dto.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

import java.time.Clock;
import java.time.Instant;
import java.util.function.Supplier;

public class DdcServiceRegistryService {

    private final DdcServiceRegistryRedisRepository repository;

    private final DdcLeaseValidator leaseValidator;

    private final Clock clock;

    private final Supplier<String> leaseIdSupplier;

    public DdcServiceRegistryService(DdcServiceRegistryRedisRepository repository,
                                     DdcLeaseValidator leaseValidator) {
        this(repository, leaseValidator, Clock.systemUTC(), UuidV7::simpleString);
    }

    DdcServiceRegistryService(DdcServiceRegistryRedisRepository repository,
                              DdcLeaseValidator leaseValidator,
                              Clock clock,
                              Supplier<String> leaseIdSupplier) {
        this.repository = repository;
        this.leaseValidator = leaseValidator;
        this.clock = clock;
        this.leaseIdSupplier = leaseIdSupplier;
    }

    public DdcLeaseSession register(DdcServiceRegistration registration) {
        validateRegistration(registration);
        Instant now = clock.instant();
        DdcLeaseSession session = new DdcLeaseSession(
                registration.instanceId(),
                leaseIdSupplier.get(),
                registration.serviceKey().serviceKind().leaseRole(),
                registration.leaseSeconds(),
                registration.heartbeatIntervalSeconds(),
                now,
                now.plusSeconds(registration.leaseSeconds())
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
                0L
        ));
        return session;
    }

    public DdcLeaseOperationResult heartbeat(DdcServiceLeaseRequest request) {
        validateOperation(request);
        return repository.heartbeat(request, clock.instant());
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
        request.setEnv(registration.serviceKey().env());
        request.setNamespace(registration.serviceKey().namespace());
        request.setServiceKind(registration.serviceKey().serviceKind());
        request.setServiceKey(registration.serviceKey());
        request.setInstanceId(registration.instanceId());
        request.setLeaseId(session.leaseId());
        return request;
    }

    private void validateRegistration(DdcServiceRegistration registration) {
        if (registration == null) {
            throw new DdcAdminException("service registration is required");
        }
        DdcInstanceRegisterRequest request = new DdcInstanceRegisterRequest();
        request.setInstanceId(registration.instanceId());
        request.setAppCode(registration.serviceKey().serviceName());
        request.setEnv(registration.serviceKey().env());
        request.setNamespace(registration.serviceKey().namespace());
        request.setHost(registration.host());
        request.setPort(registration.port());
        request.setLeaseSeconds(registration.leaseSeconds());
        request.setHeartbeatIntervalSeconds(registration.heartbeatIntervalSeconds());
        leaseValidator.validateRegistration(request);
    }

    private void validateOperation(DdcServiceLeaseRequest request) {
        if (request == null
                || request.getServiceKey() == null
                || request.getServiceKind() == null
                || blank(request.getInstanceId())
                || blank(request.getLeaseId())
                || blank(request.getEnv())
                || blank(request.getNamespace())) {
            throw new DdcAdminException("complete service lease identity is required");
        }
        DdcServiceKey key = request.getServiceKey();
        if (!request.getEnv().equals(key.env())
                || !request.getNamespace().equals(key.namespace())
                || request.getServiceKind() != key.serviceKind()) {
            throw new DdcAdminException("service lease identity does not match serviceKey");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
