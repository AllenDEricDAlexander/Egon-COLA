package top.egon.cola.component.ddc.admin.service.lease;

import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.repository.DdcConfigLeaseRedisRepository;
import top.egon.cola.component.ddc.admin.security.admission.DdcAdmissionClaims;
import top.egon.cola.component.ddc.admin.security.admission.DdcAdmissionVerifier;
import top.egon.cola.component.ddc.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.config.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.config.DdcPublishTarget;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.instance.DdcInstanceIdentity;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

public class DdcConfigLeaseService {

    private final DdcConfigLeaseRedisRepository repository;

    private final DdcLeaseValidator validator;

    private final DdcAdmissionVerifier admissionVerifier;

    private final Clock clock;

    private final Supplier<String> leaseIdSupplier;

    public DdcConfigLeaseService(DdcConfigLeaseRedisRepository repository,
                                 DdcLeaseValidator validator,
                                 DdcAdmissionVerifier admissionVerifier) {
        this(
                repository,
                validator,
                admissionVerifier,
                Clock.systemUTC(),
                UuidV7::simpleString
        );
    }

    DdcConfigLeaseService(DdcConfigLeaseRedisRepository repository,
                          DdcLeaseValidator validator,
                          DdcAdmissionVerifier admissionVerifier,
                          Clock clock,
                          Supplier<String> leaseIdSupplier) {
        this.repository = repository;
        this.validator = validator;
        this.admissionVerifier = admissionVerifier;
        this.clock = clock;
        this.leaseIdSupplier = leaseIdSupplier;
    }

    public DdcLeaseSession register(DdcInstanceRegisterRequest request) {
        return registerAdmitted(request).session();
    }

    AdmittedRegistration registerAdmitted(DdcInstanceRegisterRequest request) {
        validator.validateRegistration(request);
        DdcAdmissionClaims admission = admissionVerifier.verify(
                request.getAdmissionTicket(),
                request.getBizCode(),
                request.getAppCode(),
                request.getEnv(),
                request.getInstanceId()
        );
        Instant registeredAt = clock.instant();
        DdcLeaseSession session = new DdcLeaseSession(
                request.getInstanceId(),
                leaseIdSupplier.get(),
                DdcLeaseRole.CONFIG_CLIENT,
                request.getLeaseSeconds(),
                request.getHeartbeatIntervalSeconds(),
                registeredAt,
                validator.capLeaseExpireAt(
                        registeredAt,
                        request.getLeaseSeconds(),
                        admission
                )
        );
        repository.register(identity(request), session, registeredAt, admission);
        return new AdmittedRegistration(session, admission);
    }

    public DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request) {
        return heartbeatAdmitted(request).result();
    }

    AdmittedHeartbeat heartbeatAdmitted(DdcHeartbeatRequest request) {
        validator.validateOperation(request);
        DdcAdmissionClaims admission = admissionVerifier.verify(
                request.getAdmissionTicket(),
                request.getBizCode(),
                request.getAppCode(),
                request.getEnv(),
                request.getInstanceId()
        );
        DdcLeaseOperationResult result = repository.heartbeat(
                request,
                admission,
                clock.instant()
        );
        return new AdmittedHeartbeat(result, admission);
    }

    public DdcLeaseOperationResult deregister(DdcHeartbeatRequest request) {
        validator.validateOperation(request);
        return repository.deregister(request);
    }

    public List<DdcPublishTarget> activeTargets(
            String bizCode, String env, String appCode) {
        return repository.activeTargets(bizCode, env, appCode, clock.instant());
    }

    public boolean areActiveTargets(String bizCode,
                                    String env,
                                    String appCode,
                                    List<DdcPublishTarget> targets) {
        Instant now = clock.instant();
        return targets != null
                && !targets.isEmpty()
                && targets.stream().allMatch(target -> repository.isActiveTarget(
                        bizCode,
                        env,
                        appCode,
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

    record AdmittedRegistration(
            DdcLeaseSession session,
            DdcAdmissionClaims admission
    ) {
    }

    record AdmittedHeartbeat(
            DdcLeaseOperationResult result,
            DdcAdmissionClaims admission
    ) {
    }
}
