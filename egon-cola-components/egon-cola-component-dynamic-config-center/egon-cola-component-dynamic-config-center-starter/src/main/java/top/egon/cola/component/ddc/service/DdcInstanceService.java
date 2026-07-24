package top.egon.cola.component.ddc.service;

import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.common.DdcException;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;
import top.egon.cola.component.ddc.model.vo.DdcInstanceIdentity;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

public class DdcInstanceService {

    private final DdcProperties properties;

    private final DdcAdminClient adminClient;

    private final DdcInstanceIdentity identity;

    private final DdcLeaseSessionHolder sessionHolder;

    public DdcInstanceService(DdcProperties properties,
                              DdcAdminClient adminClient,
                              DdcInstanceIdentity identity,
                              DdcLeaseSessionHolder sessionHolder) {
        this.properties = properties;
        this.adminClient = adminClient;
        this.identity = identity;
        this.sessionHolder = sessionHolder;
    }

    public DdcLeaseSession register() {
        DdcInstanceRegisterRequest request = new DdcInstanceRegisterRequest();
        fill(request);
        request.setLeaseSeconds(properties.getInstance().getLeaseSeconds());
        request.setHeartbeatIntervalSeconds(properties.getInstance().getHeartbeatIntervalSeconds());
        DdcLeaseSession session = adminClient.register(request);
        validate(session);
        sessionHolder.replace(session);
        return session;
    }

    public DdcLeaseOperationResult heartbeat(DdcLeaseSession session) {
        return adminClient.heartbeat(operationRequest(session));
    }

    public DdcLeaseOperationResult offline(DdcLeaseSession session) {
        return adminClient.offline(operationRequest(session));
    }

    public DdcInstanceIdentity identity() {
        return identity;
    }

    private DdcHeartbeatRequest operationRequest(DdcLeaseSession session) {
        DdcHeartbeatRequest request = new DdcHeartbeatRequest();
        request.setLeaseId(session.leaseId());
        fill(request);
        return request;
    }

    private void fill(DdcHeartbeatRequest request) {
        request.setInstanceId(identity.instanceId());
        request.setAppCode(identity.appCode());
        request.setEnv(identity.env());
        request.setNamespace(identity.namespace());
        request.setHost(identity.host());
        request.setPort(identity.port());
        request.setPid(identity.pid());
        request.setSdkVersion(identity.sdkVersion());
    }

    private void fill(DdcInstanceRegisterRequest request) {
        request.setInstanceId(identity.instanceId());
        request.setAppCode(identity.appCode());
        request.setEnv(identity.env());
        request.setNamespace(identity.namespace());
        request.setHost(identity.host());
        request.setPort(identity.port());
        request.setPid(identity.pid());
        request.setSdkVersion(identity.sdkVersion());
    }

    private void validate(DdcLeaseSession session) {
        if (session == null
                || !identity.instanceId().equals(session.instanceId())
                || session.leaseId() == null
                || session.leaseId().isBlank()
                || session.role() != DdcLeaseRole.CONFIG_CLIENT) {
            throw new DdcException("Admin returned an invalid DDC lease");
        }
    }
}
