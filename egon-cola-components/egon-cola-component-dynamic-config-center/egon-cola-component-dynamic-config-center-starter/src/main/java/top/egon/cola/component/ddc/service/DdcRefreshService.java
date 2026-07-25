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

    private static final int MAX_ERROR_MESSAGE_LENGTH = 256;

    private final DdcLocalConfigRepository repository;

    private final DdcConfigApplierRegistry applierRegistry;

    private final DdcAdminClient adminClient;

    private final DdcLeaseSessionHolder sessionHolder;

    public DdcRefreshService(DdcLocalConfigRepository repository,
                             DdcConfigApplier applyFunction,
                             DdcAdminClient adminClient,
                             DdcLeaseSessionHolder sessionHolder) {
        this(repository, new DefaultDdcConfigApplierRegistry(applyFunction), adminClient, sessionHolder);
    }

    public DdcRefreshService(DdcLocalConfigRepository repository,
                             DdcConfigApplierRegistry applierRegistry,
                             DdcAdminClient adminClient,
                             DdcLeaseSessionHolder sessionHolder) {
        this.repository = repository;
        this.applierRegistry = applierRegistry;
        this.adminClient = adminClient;
        this.sessionHolder = sessionHolder;
    }

    public void applySnapshot(DdcConfigValue config) {
        if (config == null || config.getConfigKey() == null || config.getVersion() == null) {
            return;
        }
        repository.withConfigLock(config.getConfigKey(), () -> {
            String contentChecksum = DdcChecksum.content(config.getConfigValue());
            ConfigMetadata local = metadata(config.getConfigKey());
            VersionRelation relation = compare(local, config.getVersion(), contentChecksum);
            if (relation == VersionRelation.CHECKSUM_CONFLICT) {
                LOGGER.warn(
                        "DDC snapshot checksum conflict for configKey={} version={}",
                        config.getConfigKey(),
                        config.getVersion()
                );
            } else if (relation == VersionRelation.NEWER) {
                applyAndStore(
                        config.getConfigKey(),
                        config.getConfigValue(),
                        config.getVersion(),
                        contentChecksum,
                        local
                );
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
        ConfigMetadata local = metadata(message.getConfigKey());
        VersionRelation relation = compare(
                local,
                message.getTargetVersion(),
                message.getContentChecksum()
        );
        if (relation == VersionRelation.STALE) {
            return new AckOutcome(DdcAckStatus.IGNORED, local.version(), null);
        }
        if (relation == VersionRelation.SAME_CONTENT) {
            return new AckOutcome(DdcAckStatus.SUCCESS, local.version(), null);
        }
        if (relation == VersionRelation.CHECKSUM_CONFLICT) {
            return new AckOutcome(
                    DdcAckStatus.FAILED,
                    local.version(),
                    "DDC config checksum conflict"
            );
        }
        try {
            applyAndStore(
                    message.getConfigKey(),
                    message.getConfigValue(),
                    message.getTargetVersion(),
                    message.getContentChecksum(),
                    local
            );
            return new AckOutcome(DdcAckStatus.SUCCESS, message.getTargetVersion(), null);
        } catch (RuntimeException exception) {
            return new AckOutcome(DdcAckStatus.FAILED, local.version(), safeErrorMessage(exception));
        }
    }

    private ConfigMetadata metadata(String configKey) {
        return new ConfigMetadata(repository.version(configKey), repository.checksum(configKey));
    }

    private VersionRelation compare(ConfigMetadata local, long targetVersion, String targetChecksum) {
        if (local.version() == null || targetVersion > local.version()) {
            return VersionRelation.NEWER;
        }
        if (targetVersion < local.version()) {
            return VersionRelation.STALE;
        }
        return targetChecksum.equals(local.checksum())
                ? VersionRelation.SAME_CONTENT
                : VersionRelation.CHECKSUM_CONFLICT;
    }

    private void applyAndStore(String configKey,
                               String configValue,
                               long version,
                               String checksum,
                               ConfigMetadata previous) {
        try {
            applierRegistry.resolve(configKey).apply(configKey, configValue, version);
            repository.updateVersion(configKey, version);
            repository.updateChecksum(configKey, checksum);
        } catch (RuntimeException exception) {
            repository.restoreMetadata(configKey, previous.version(), previous.checksum());
            throw exception;
        }
    }

    private String safeErrorMessage(RuntimeException exception) {
        String prefix = "DDC config apply failed";
        String detail = exception.getMessage();
        String message = detail == null || detail.isBlank()
                ? prefix
                : prefix + ": " + detail.replaceAll("\\s+", " ").trim();
        return message.length() <= MAX_ERROR_MESSAGE_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
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

    private record ConfigMetadata(Long version, String checksum) {
    }

    private enum VersionRelation {
        NEWER,
        SAME_CONTENT,
        CHECKSUM_CONFLICT,
        STALE
    }
}
