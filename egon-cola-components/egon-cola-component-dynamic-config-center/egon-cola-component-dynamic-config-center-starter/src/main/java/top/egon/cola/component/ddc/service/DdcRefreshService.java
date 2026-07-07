package top.egon.cola.component.ddc.service;

import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;
import top.egon.cola.component.ddc.model.enums.DdcAckStatus;
import top.egon.cola.component.ddc.repository.DdcLocalConfigRepository;

public class DdcRefreshService {

    private final DdcLocalConfigRepository repository;

    private final DdcConfigApplier applyFunction;

    private final DdcAdminClient adminClient;

    public DdcRefreshService(DdcLocalConfigRepository repository, DdcConfigApplier applyFunction, DdcAdminClient adminClient) {
        this.repository = repository;
        this.applyFunction = applyFunction;
        this.adminClient = adminClient;
    }

    public void refresh(DdcPublishMessage message) {
        Long localVersion = repository.version(message.getConfigKey());
        if (localVersion != null && message.getTargetVersion() <= localVersion) {
            adminClient.ack(ack(message, DdcAckStatus.IGNORED, localVersion, null));
            return;
        }
        try {
            applyFunction.apply(message.getConfigKey(), message.getConfigValue(), message.getTargetVersion());
            adminClient.ack(ack(message, DdcAckStatus.SUCCESS, message.getTargetVersion(), null));
        } catch (Exception e) {
            adminClient.ack(ack(message, DdcAckStatus.FAILED, localVersion, e.getMessage()));
        }
    }

    private DdcAckRequest ack(DdcPublishMessage message, DdcAckStatus status, Long currentVersion, String errorMessage) {
        DdcAckRequest request = new DdcAckRequest();
        request.setChangeId(message.getChangeId());
        request.setAppCode(message.getAppCode());
        request.setEnv(message.getEnv());
        request.setNamespace(message.getNamespace());
        request.setConfigKey(message.getConfigKey());
        request.setTargetVersion(message.getTargetVersion());
        request.setCurrentVersion(currentVersion);
        request.setStatus(status);
        request.setErrorMessage(errorMessage);
        request.setAckTime(System.currentTimeMillis());
        return request;
    }
}
