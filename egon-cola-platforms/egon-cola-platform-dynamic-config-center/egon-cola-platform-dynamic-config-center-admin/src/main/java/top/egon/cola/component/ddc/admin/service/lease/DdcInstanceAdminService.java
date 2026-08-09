package top.egon.cola.component.ddc.admin.service.lease;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.model.entity.DdcInstanceEntity;
import top.egon.cola.component.ddc.admin.model.enums.InstanceStatus;
import top.egon.cola.component.ddc.admin.repository.DdcInstanceRepository;
import top.egon.cola.component.ddc.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.config.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class DdcInstanceAdminService {

    private final DdcInstanceRepository instanceRepository;

    private final DdcConfigLeaseService configLeaseService;

    public DdcInstanceAdminService(DdcInstanceRepository instanceRepository,
                                   DdcConfigLeaseService configLeaseService) {
        this.instanceRepository = instanceRepository;
        this.configLeaseService = configLeaseService;
    }

    @Transactional
    public DdcLeaseSession register(DdcInstanceRegisterRequest request) {
        DdcLeaseSession session = configLeaseService.register(request);
        DdcInstanceEntity instance = instanceRepository.findByInstanceId(request.getInstanceId())
                .orElseGet(() -> newInstance(request));
        fillInstance(instance, request);
        instance.setLeaseId(session.leaseId());
        instance.setLeaseExpireAt(localTime(session.leaseExpireAt()));
        instance.setStatus(InstanceStatus.ONLINE.name());
        instance.setLastHeartbeatAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());
        instanceRepository.save(instance);
        return session;
    }

    @Transactional
    public DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request) {
        DdcLeaseOperationResult result = configLeaseService.heartbeat(request);
        if (result.renewed()) {
            instanceRepository.findByInstanceId(request.getInstanceId()).ifPresent(instance -> {
                if (!request.getLeaseId().equals(instance.getLeaseId())) {
                    return;
                }
                instance.setStatus(InstanceStatus.ONLINE.name());
                instance.setLastHeartbeatAt(LocalDateTime.now());
                instance.setLeaseExpireAt(localTime(result.leaseExpireAt()));
                instance.setRuntimeMetadata(request.getMetadata());
                instance.setUpdatedAt(LocalDateTime.now());
                instanceRepository.save(instance);
            });
        }
        return result;
    }

    @Transactional
    public DdcLeaseOperationResult offline(DdcHeartbeatRequest request) {
        DdcLeaseOperationResult result = configLeaseService.deregister(request);
        if (result.deleted()) {
            instanceRepository.markOfflineIfLeaseMatches(
                    request.getInstanceId(),
                    request.getLeaseId(),
                    InstanceStatus.OFFLINE.name(),
                    LocalDateTime.now()
            );
        }
        return result;
    }

    public List<DdcInstanceEntity> list(
            String bizCode, String env, String appCode) {
        return instanceRepository.findByBizCodeAndEnvAndAppCode(
                bizCode, env, appCode);
    }

    private LocalDateTime localTime(Instant value) {
        return LocalDateTime.ofInstant(value, ZoneId.systemDefault());
    }

    private DdcInstanceEntity newInstance(DdcInstanceRegisterRequest request) {
        DdcInstanceEntity instance = new DdcInstanceEntity();
        instance.setId(UuidV7.simpleString());
        instance.setInstanceId(request.getInstanceId());
        instance.setCreatedAt(LocalDateTime.now());
        return instance;
    }

    private void fillInstance(DdcInstanceEntity instance, DdcInstanceRegisterRequest request) {
        instance.setBizCode(request.getBizCode());
        instance.setAppCode(request.getAppCode());
        instance.setEnv(request.getEnv());
        instance.setHost(request.getHost());
        instance.setPort(request.getPort());
        instance.setPid(request.getPid());
        instance.setSdkVersion(request.getSdkVersion());
        instance.setRuntimeMetadata(request.getMetadata());
    }
}
