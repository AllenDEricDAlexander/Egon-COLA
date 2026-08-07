package top.egon.cola.component.ddc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.ddc.client.DdcAdminClient;
import top.egon.cola.component.ddc.common.DdcChecksum;
import top.egon.cola.component.ddc.environment.DdcYamlPropertySourceLoader;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;
import top.egon.cola.component.ddc.model.dto.DdcPublishTarget;
import top.egon.cola.component.ddc.model.enums.DdcAckStatus;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.refresh.DdcYamlConfigApplier;
import top.egon.cola.component.ddc.repository.DdcLocalConfigRepository;

import java.util.List;

public class DdcRefreshService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DdcRefreshService.class);

    private static final int MAX_ERROR_MESSAGE_LENGTH = 256;

    private static final String RESOURCE_NAME =
            DdcYamlConfigApplier.RESOURCE_NAME;

    private static final String VALUE_TYPE =
            DdcYamlPropertySourceLoader.VALUE_TYPE;

    private final DdcLocalConfigRepository repository;

    private final DdcYamlConfigApplier yamlConfigApplier;

    private final AckSubmitter ackSubmitter;

    private final DdcLeaseSessionHolder sessionHolder;

    public DdcRefreshService(DdcLocalConfigRepository repository,
                             DdcYamlConfigApplier yamlConfigApplier,
                             DdcAdminClient adminClient,
                             DdcLeaseSessionHolder sessionHolder) {
        this(
                repository,
                yamlConfigApplier,
                directAck(adminClient),
                sessionHolder
        );
    }

    public DdcRefreshService(DdcLocalConfigRepository repository,
                             DdcYamlConfigApplier yamlConfigApplier,
                             DdcAckDelivery ackDelivery,
                             DdcLeaseSessionHolder sessionHolder) {
        this(
                repository,
                yamlConfigApplier,
                ackDelivery::submit,
                sessionHolder
        );
    }

    private DdcRefreshService(DdcLocalConfigRepository repository,
                              DdcYamlConfigApplier yamlConfigApplier,
                              AckSubmitter ackSubmitter,
                              DdcLeaseSessionHolder sessionHolder) {
        this.repository = repository;
        this.yamlConfigApplier = yamlConfigApplier;
        this.ackSubmitter = ackSubmitter;
        this.sessionHolder = sessionHolder;
        seedConfigDataMetadata();
    }

    public void applySnapshots(List<DdcConfigValue> configs) {
        if (configs == null || configs.isEmpty()) {
            return;
        }
        if (configs.size() != 1) {
            throw new IllegalArgumentException(
                    "DDC scope must contain exactly one application.yml"
            );
        }
        applySnapshot(configs.getFirst());
    }

    public void applySnapshot(DdcConfigValue config) {
        requireYamlResource(config);
        repository.withConfigLock(RESOURCE_NAME, () -> {
            String contentChecksum =
                    DdcChecksum.content(config.getConfigValue());
            ConfigMetadata local = metadata();
            VersionRelation relation = compare(
                    local,
                    config.getVersion(),
                    contentChecksum
            );
            if (relation == VersionRelation.CHECKSUM_CONFLICT) {
                LOGGER.warn(
                        "DDC snapshot checksum conflict for resource={} version={}",
                        RESOURCE_NAME,
                        config.getVersion()
                );
            } else if (relation == VersionRelation.NEWER) {
                applyAndStore(
                        config.getConfigValue(),
                        config.getVersion(),
                        contentChecksum,
                        null,
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

        AckOutcome outcome = repository.withConfigLock(
                RESOURCE_NAME,
                () -> apply(message)
        );
        try {
            if (!ackSubmitter.submit(ack(message, session, outcome))) {
                LOGGER.warn(
                        "DDC ACK delivery rejected for changeId={} instanceId={}",
                        message.getChangeId(),
                        session.instanceId()
                );
            }
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "DDC ACK delivery failed for changeId={} instanceId={}",
                    message.getChangeId(),
                    session.instanceId()
            );
        }
    }

    private void requireYamlResource(DdcConfigValue config) {
        if (config == null
                || !RESOURCE_NAME.equals(config.getConfigKey())
                || !VALUE_TYPE.equals(config.getValueType())
                || config.getVersion() == null
                || config.getVersion() <= 0) {
            throw new IllegalArgumentException(
                    "DDC scope must contain only application.yml with YAML type"
            );
        }
    }

    private boolean isValid(DdcPublishMessage message) {
        return message != null
                && hasText(message.getChangeId())
                && RESOURCE_NAME.equals(message.getConfigKey())
                && VALUE_TYPE.equals(message.getValueType())
                && message.getTargetVersion() != null
                && message.getTargetVersion() > 0
                && hasText(message.getContentChecksum())
                && message.getContentChecksum().equals(
                        DdcChecksum.content(message.getConfigValue())
                );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private AckOutcome apply(DdcPublishMessage message) {
        ConfigMetadata local = metadata();
        VersionRelation relation = compare(
                local,
                message.getTargetVersion(),
                message.getContentChecksum()
        );
        if (relation == VersionRelation.STALE) {
            return new AckOutcome(
                    DdcAckStatus.IGNORED,
                    local.version(),
                    null
            );
        }
        if (relation == VersionRelation.SAME_CONTENT) {
            return new AckOutcome(
                    DdcAckStatus.SUCCESS,
                    local.version(),
                    null
            );
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
                    message.getConfigValue(),
                    message.getTargetVersion(),
                    message.getContentChecksum(),
                    message.getChangeId(),
                    local
            );
            return new AckOutcome(
                    DdcAckStatus.SUCCESS,
                    message.getTargetVersion(),
                    null
            );
        } catch (RuntimeException exception) {
            return new AckOutcome(
                    DdcAckStatus.FAILED,
                    local.version(),
                    safeErrorMessage(exception)
            );
        }
    }

    private ConfigMetadata metadata() {
        return new ConfigMetadata(
                repository.version(RESOURCE_NAME),
                repository.checksum(RESOURCE_NAME)
        );
    }

    private VersionRelation compare(ConfigMetadata local,
                                    long targetVersion,
                                    String targetChecksum) {
        if (local.version() == null
                || targetVersion > local.version()) {
            return VersionRelation.NEWER;
        }
        if (targetVersion < local.version()) {
            return VersionRelation.STALE;
        }
        return targetChecksum.equals(local.checksum())
                ? VersionRelation.SAME_CONTENT
                : VersionRelation.CHECKSUM_CONFLICT;
    }

    private void applyAndStore(String content,
                               long version,
                               String checksum,
                               String changeId,
                               ConfigMetadata previous) {
        try {
            yamlConfigApplier.apply(content, version, changeId);
            repository.updateVersion(RESOURCE_NAME, version);
            repository.updateChecksum(RESOURCE_NAME, checksum);
        } catch (RuntimeException exception) {
            repository.restoreMetadata(
                    RESOURCE_NAME,
                    previous.version(),
                    previous.checksum()
            );
            throw exception;
        }
    }

    private void seedConfigDataMetadata() {
        if (repository.version(RESOURCE_NAME) != null) {
            return;
        }
        var snapshot = yamlConfigApplier.currentSnapshot();
        repository.updateVersion(RESOURCE_NAME, snapshot.version());
        repository.updateChecksum(RESOURCE_NAME, snapshot.checksum());
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

    private boolean isTarget(DdcPublishMessage message,
                             DdcLeaseSession session) {
        DdcPublishTarget current = new DdcPublishTarget(
                session.instanceId(),
                session.leaseId()
        );
        return message.getTargets().contains(current);
    }

    private DdcAckRequest ack(DdcPublishMessage message,
                              DdcLeaseSession session,
                              AckOutcome outcome) {
        DdcAckRequest request = new DdcAckRequest();
        request.setChangeId(message.getChangeId());
        request.setInstanceId(session.instanceId());
        request.setLeaseId(session.leaseId());
        request.setBizCode(message.getBizCode());
        request.setAppCode(message.getAppCode());
        request.setEnv(message.getEnv());
        request.setConfigKey(RESOURCE_NAME);
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

    private static AckSubmitter directAck(DdcAdminClient adminClient) {
        if (adminClient == null) {
            throw new IllegalArgumentException(
                    "adminClient must not be null"
            );
        }
        return request -> {
            adminClient.ack(request);
            return true;
        };
    }

    @FunctionalInterface
    private interface AckSubmitter {

        boolean submit(DdcAckRequest request);
    }

    private enum VersionRelation {
        NEWER,
        SAME_CONTENT,
        CHECKSUM_CONFLICT,
        STALE
    }
}
