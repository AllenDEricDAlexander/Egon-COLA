package top.egon.cola.component.ddc.admin.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.model.entity.DdcInstanceEntity;
import top.egon.cola.component.ddc.admin.model.enums.InstanceStatus;
import top.egon.cola.component.ddc.admin.repository.DdcInstanceRepository;
import top.egon.cola.component.ddc.admin.repository.DdcRedisRepository;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DdcInstanceAdminService {

    private final DdcInstanceRepository instanceRepository;

    private final ObjectProvider<DdcRedisRepository> redisRepositoryProvider;

    public DdcInstanceAdminService(DdcInstanceRepository instanceRepository,
                                   ObjectProvider<DdcRedisRepository> redisRepositoryProvider) {
        this.instanceRepository = instanceRepository;
        this.redisRepositoryProvider = redisRepositoryProvider;
    }

    @Transactional
    public DdcInstanceEntity register(DdcHeartbeatRequest request) {
        DdcInstanceEntity instance = instanceRepository.findByInstanceId(request.getInstanceId())
                .orElseGet(() -> newInstance(request));
        fillInstance(instance, request);
        instance.setStatus(InstanceStatus.ONLINE.name());
        instance.setLastHeartbeatAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());
        DdcInstanceEntity saved = instanceRepository.save(instance);
        redisRepositoryProvider.ifAvailable(repository -> repository.writeInstanceHeartbeat(request));
        return saved;
    }

    @Transactional
    public DdcInstanceEntity heartbeat(DdcHeartbeatRequest request) {
        return register(request);
    }

    @Transactional
    public void offline(DdcHeartbeatRequest request) {
        instanceRepository.findByInstanceId(request.getInstanceId()).ifPresent(instance -> {
            instance.setStatus(InstanceStatus.OFFLINE.name());
            instance.setUpdatedAt(LocalDateTime.now());
            instanceRepository.save(instance);
        });
        redisRepositoryProvider.ifAvailable(repository -> repository.removeInstance(
                request.getAppCode(), request.getEnv(), request.getNamespace(), request.getInstanceId()));
    }

    public List<DdcInstanceEntity> list(String appCode, String env, String namespace) {
        return instanceRepository.findByAppCodeAndEnvAndNamespace(appCode, env, namespace);
    }

    private DdcInstanceEntity newInstance(DdcHeartbeatRequest request) {
        DdcInstanceEntity instance = new DdcInstanceEntity();
        instance.setId(UuidV7.simpleString());
        instance.setInstanceId(request.getInstanceId());
        instance.setCreatedAt(LocalDateTime.now());
        return instance;
    }

    private void fillInstance(DdcInstanceEntity instance, DdcHeartbeatRequest request) {
        instance.setAppCode(request.getAppCode());
        instance.setEnv(request.getEnv());
        instance.setNamespace(request.getNamespace());
        instance.setHost(request.getHost());
        instance.setPort(request.getPort());
        instance.setPid(request.getPid());
        instance.setSdkVersion(request.getSdkVersion());
    }
}
