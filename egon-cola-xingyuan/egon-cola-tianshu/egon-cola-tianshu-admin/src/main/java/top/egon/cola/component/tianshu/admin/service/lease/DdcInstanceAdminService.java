package top.egon.cola.component.tianshu.admin.service.lease;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.tianshu.admin.model.bo.DdcInstanceBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcInstanceResultConverter;
import top.egon.cola.component.tianshu.admin.model.enums.InstanceStatus;
import top.egon.cola.component.tianshu.admin.model.vo.DdcInstanceResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcInstanceRepository;
import top.egon.cola.component.tianshu.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.tianshu.model.config.DdcInstanceRegisterRequest;
import top.egon.cola.component.tianshu.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.tianshu.model.lease.DdcLeaseSession;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class DdcInstanceAdminService {

    private final DdcInstanceRepository instanceRepository;

    private final DdcConfigLeaseService configLeaseService;
    private final DdcInstanceResultConverter resultConverter;

    public DdcInstanceAdminService(DdcInstanceRepository instanceRepository,
                                   DdcConfigLeaseService configLeaseService,
                                   DdcInstanceResultConverter resultConverter) {
        this.instanceRepository = instanceRepository;
        this.configLeaseService = configLeaseService;
        this.resultConverter = resultConverter;
    }

    @Transactional
    public DdcLeaseSession register(DdcInstanceRegisterRequest request) {
        DdcConfigLeaseService.AdmittedRegistration admitted =
                configLeaseService.registerAdmitted(request);
        DdcLeaseSession session = admitted.session();
        DdcInstanceBO instance = instanceRepository.findByInstanceId(request.getInstanceId())
                .orElseGet(() -> newInstance(request));
        fillInstance(instance, request);
        instance.setLeaseId(session.leaseId());
        instance.setLeaseExpireAt(localTime(session.leaseExpireAt()));
        fillRegistration(instance, admitted.registration());
        instance.setStatus(InstanceStatus.ONLINE.name());
        instance.setLastHeartbeatAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());
        if (instance.getId() == null) instanceRepository.insert(instance);
        else instanceRepository.update(instance);
        return session;
    }

    @Transactional
    public DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request) {
        DdcConfigLeaseService.AdmittedHeartbeat admitted =
                configLeaseService.heartbeatAdmitted(request);
        DdcLeaseOperationResult result = admitted.result();
        if (result.renewed()) {
            instanceRepository.findByInstanceId(request.getInstanceId()).ifPresent(instance -> {
                if (!request.getLeaseId().equals(instance.getLeaseId())) {
                    return;
                }
                instance.setStatus(InstanceStatus.ONLINE.name());
                instance.setLastHeartbeatAt(LocalDateTime.now());
                instance.setLeaseExpireAt(localTime(result.leaseExpireAt()));
                fillRegistration(instance, admitted.registration());
                instance.setRuntimeMetadata(request.getMetadata());
                instance.setUpdatedAt(LocalDateTime.now());
                instanceRepository.update(instance);
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

    public List<DdcInstanceResult> list(
            String bizCode, String env, String appCode) {
        return resultConverter.toTargetList(instanceRepository.findByBizCodeAndEnvAndAppCode(
                bizCode, env, appCode));
    }

    public DdcPageResult<DdcInstanceResult> page(
            String bizCode,
            String env,
            String appCode,
            PageQuery pageQuery
    ) {
        DdcPageResult<DdcInstanceBO> page = instanceRepository.findByBizCodeAndEnvAndAppCode(
                bizCode, env, appCode, pageQuery);
        return DdcPageResult.<DdcInstanceResult>builder().records(resultConverter.toTargetList(page.getRecords()))
                .total(page.getTotal()).pageNo(page.getPageNo()).pageSize(page.getPageSize()).build();
    }

    private LocalDateTime localTime(Instant value) {
        return LocalDateTime.ofInstant(value, ZoneId.systemDefault());
    }

    private DdcInstanceBO newInstance(DdcInstanceRegisterRequest request) {
        DdcInstanceBO instance = new DdcInstanceBO();
        instance.setInstanceId(request.getInstanceId());
        instance.setCreatedAt(LocalDateTime.now());
        return instance;
    }

    private void fillInstance(DdcInstanceBO instance, DdcInstanceRegisterRequest request) {
        instance.setBizCode(request.getBizCode());
        instance.setAppCode(request.getAppCode());
        instance.setEnv(request.getEnv());
        instance.setHost(request.getHost());
        instance.setPort(request.getPort());
        instance.setPid(request.getPid());
        instance.setSdkVersion(request.getSdkVersion());
        instance.setRuntimeMetadata(request.getMetadata());
    }

    private void fillRegistration(
            DdcInstanceBO instance,
            top.egon.cola.component.tianshu.admin.security.registration.VerifiedDdcRegistrationIdentity registration
    ) {
        instance.setResourceServerId(registration.resourceServerId());
        instance.setResourceVersion(registration.resourceVersion());
        instance.setCredentialId(registration.credentialId());
        instance.setAdmissionExpiresAt(localTime(registration.expiresAt()));
    }
}
