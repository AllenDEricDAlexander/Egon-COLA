package top.egon.cola.component.ddc.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.model.entity.DdcInstanceEntity;
import top.egon.cola.component.ddc.admin.model.enums.InstanceStatus;
import top.egon.cola.component.ddc.admin.repository.DdcInstanceRepository;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
        instance.setLeaseExpireAt(LocalDateTime.ofInstant(session.leaseExpireAt(), ZoneOffset.UTC));
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
                instance.setLeaseExpireAt(LocalDateTime.ofInstant(result.leaseExpireAt(), ZoneOffset.UTC));
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

    public List<DdcInstanceEntity> list(String appCode, String env, String namespace) {
        return instanceRepository.findByAppCodeAndEnvAndNamespace(appCode, env, namespace);
    }

    private DdcInstanceEntity newInstance(DdcInstanceRegisterRequest request) {
        DdcInstanceEntity instance = new DdcInstanceEntity();
        instance.setId(UuidV7.simpleString());
        instance.setInstanceId(request.getInstanceId());
        instance.setCreatedAt(LocalDateTime.now());
        return instance;
    }

    private void fillInstance(DdcInstanceEntity instance, DdcInstanceRegisterRequest request) {
        instance.setAppCode(request.getAppCode());
        instance.setEnv(request.getEnv());
        instance.setNamespace(request.getNamespace());
        instance.setHost(request.getHost());
        instance.setPort(request.getPort());
        instance.setPid(request.getPid());
        instance.setSdkVersion(request.getSdkVersion());
    }
}
