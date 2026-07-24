package top.egon.cola.component.ddc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.common.DdcChecksum;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;
import top.egon.cola.component.ddc.model.dto.DdcPublishTarget;
import top.egon.cola.component.ddc.model.enums.DdcAckStatus;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.repository.DdcLocalConfigRepository;

public class DdcRefreshService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DdcRefreshService.class);

    private final DdcLocalConfigRepository repository;

    private final DdcConfigApplier applyFunction;

    private final DdcAdminClient adminClient;

    private final DdcLeaseSessionHolder sessionHolder;

    public DdcRefreshService(DdcLocalConfigRepository repository,
                             DdcConfigApplier applyFunction,
                             DdcAdminClient adminClient,
                             DdcLeaseSessionHolder sessionHolder) {
        this.repository = repository;
        this.applyFunction = applyFunction;
        this.adminClient = adminClient;
        this.sessionHolder = sessionHolder;
    }

    public void applySnapshot(DdcConfigValue config) {
        if (config == null || config.getConfigKey() == null || config.getVersion() == null) {
            return;
        }
        repository.withConfigLock(config.getConfigKey(), () -> {
            Long localVersion = repository.version(config.getConfigKey());
            if (localVersion == null || config.getVersion() > localVersion) {
                applyFunction.apply(config.getConfigKey(), config.getConfigValue(), config.getVersion());
                repository.updateVersion(config.getConfigKey(), config.getVersion());
                repository.updateChecksum(config.getConfigKey(), DdcChecksum.content(config.getConfigValue()));
            }
            return null;
        });
    }

    public void refresh(DdcPublishMessage message) {
        if (!isValid(message)) {
            return;
        }
        DdcLeaseSession session = sessionHolder.current().orElse(null);
        if (session == null || !isTarget(message, session)) {
            return;
        }

        AckOutcome outcome = repository.withConfigLock(message.getConfigKey(), () -> apply(message));
        try {
            adminClient.ack(ack(message, session, outcome));
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "DDC ACK delivery failed for changeId={} instanceId={}",
                    message.getChangeId(),
                    session.instanceId()
            );
        }
    }

    private boolean isValid(DdcPublishMessage message) {
        return message != null
                && hasText(message.getChangeId())
                && hasText(message.getConfigKey())
                && message.getTargetVersion() != null
                && hasText(message.getContentChecksum())
                && message.getContentChecksum().equals(DdcChecksum.content(message.getConfigValue()));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private AckOutcome apply(DdcPublishMessage message) {
        Long localVersion = repository.version(message.getConfigKey());
        String localChecksum = repository.checksum(message.getConfigKey());
        if (localVersion != null && message.getTargetVersion() < localVersion) {
            return new AckOutcome(DdcAckStatus.IGNORED, localVersion, null);
        }
        if (localVersion != null
                && message.getTargetVersion().equals(localVersion)
                && message.getContentChecksum().equals(localChecksum)) {
            return new AckOutcome(DdcAckStatus.SUCCESS, localVersion, null);
        }
        try {
            applyFunction.apply(message.getConfigKey(), message.getConfigValue(), message.getTargetVersion());
            repository.updateVersion(message.getConfigKey(), message.getTargetVersion());
            repository.updateChecksum(message.getConfigKey(), message.getContentChecksum());
            return new AckOutcome(DdcAckStatus.SUCCESS, message.getTargetVersion(), null);
        } catch (RuntimeException exception) {
            return new AckOutcome(DdcAckStatus.FAILED, localVersion, exception.getMessage());
        }
    }

    private boolean isTarget(DdcPublishMessage message, DdcLeaseSession session) {
        if (message.getTargets() == null) {
            return false;
        }
        DdcPublishTarget current = new DdcPublishTarget(session.instanceId(), session.leaseId());
        return message.getTargets().contains(current);
    }

    private DdcAckRequest ack(DdcPublishMessage message,
                              DdcLeaseSession session,
                              AckOutcome outcome) {
        DdcAckRequest request = new DdcAckRequest();
        request.setChangeId(message.getChangeId());
        request.setInstanceId(session.instanceId());
        request.setLeaseId(session.leaseId());
        request.setAppCode(message.getAppCode());
        request.setEnv(message.getEnv());
        request.setNamespace(message.getNamespace());
        request.setConfigKey(message.getConfigKey());
        request.setTargetVersion(message.getTargetVersion());
        request.setCurrentVersion(outcome.currentVersion());
        request.setContentChecksum(message.getContentChecksum());
        request.setStatus(outcome.status());
        request.setErrorMessage(outcome.errorMessage());
        request.setAckTime(System.currentTimeMillis());
        return request;
    }

    private record AckOutcome(
            DdcAckStatus status,
            Long currentVersion,
            String errorMessage
    ) {
    }
}
