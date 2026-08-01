package top.egon.cola.component.ddc.admin.service;

import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.repository.DdcConfigLeaseRedisRepository;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.dto.DdcPublishTarget;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;
import top.egon.cola.component.ddc.model.vo.DdcInstanceIdentity;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

public class DdcConfigLeaseService {

    private final DdcConfigLeaseRedisRepository repository;

    private final DdcLeaseValidator validator;

    private final Clock clock;

    private final Supplier<String> leaseIdSupplier;

    public DdcConfigLeaseService(DdcConfigLeaseRedisRepository repository,
                                 DdcLeaseValidator validator) {
        this(repository, validator, Clock.systemUTC(), UuidV7::simpleString);
    }

    DdcConfigLeaseService(DdcConfigLeaseRedisRepository repository,
                          DdcLeaseValidator validator,
                          Clock clock,
                          Supplier<String> leaseIdSupplier) {
        this.repository = repository;
        this.validator = validator;
        this.clock = clock;
        this.leaseIdSupplier = leaseIdSupplier;
    }

    public DdcLeaseSession register(DdcInstanceRegisterRequest request) {
        validator.validateRegistration(request);
        Instant registeredAt = clock.instant();
        DdcLeaseSession session = new DdcLeaseSession(
                request.getInstanceId(),
                leaseIdSupplier.get(),
                DdcLeaseRole.CONFIG_CLIENT,
                request.getLeaseSeconds(),
                request.getHeartbeatIntervalSeconds(),
                registeredAt,
                registeredAt.plusSeconds(request.getLeaseSeconds())
        );
        repository.register(identity(request), session, registeredAt);
        return session;
    }

    public DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request) {
        validator.validateOperation(request);
        return repository.heartbeat(request, clock.instant());
    }

    public DdcLeaseOperationResult deregister(DdcHeartbeatRequest request) {
        validator.validateOperation(request);
        return repository.deregister(request);
    }

    public List<DdcPublishTarget> activeTargets(String appCode,
                                                String env,
                                                String namespace) {
        return repository.activeTargets(appCode, env, namespace, clock.instant());
    }

    public boolean areActiveTargets(String appCode,
                                    String env,
                                    String namespace,
                                    List<DdcPublishTarget> targets) {
        Instant now = clock.instant();
        return targets != null
                && !targets.isEmpty()
                && targets.stream().allMatch(target -> repository.isActiveTarget(
                        appCode,
                        env,
                        namespace,
                        target,
                        now
                ));
    }

    private DdcInstanceIdentity identity(DdcInstanceRegisterRequest request) {
        return new DdcInstanceIdentity(
                request.getInstanceId(),
                request.getBizCode(),
                request.getAppCode(),
                request.getEnv(),
                request.getHost(),
                request.getPort(),
                request.getPid(),
                request.getSdkVersion()
        );
    }
}
