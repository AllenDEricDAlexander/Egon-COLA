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

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class DdcRefreshService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DdcRefreshService.class);

    private static final int MAX_ERROR_MESSAGE_LENGTH = 256;

    private final DdcLocalConfigRepository repository;

    private final DdcConfigApplierRegistry applierRegistry;

    private final AckSubmitter ackSubmitter;

    private final DdcLeaseSessionHolder sessionHolder;

    public DdcRefreshService(DdcLocalConfigRepository repository,
                             DdcConfigApplier applyFunction,
                             DdcAdminClient adminClient,
                             DdcLeaseSessionHolder sessionHolder) {
        this(
                repository,
                new DefaultDdcConfigApplierRegistry(applyFunction),
                directAck(adminClient),
                sessionHolder
        );
    }

    public DdcRefreshService(DdcLocalConfigRepository repository,
                             DdcConfigApplierRegistry applierRegistry,
                             DdcAdminClient adminClient,
                             DdcLeaseSessionHolder sessionHolder) {
        this(repository, applierRegistry, directAck(adminClient), sessionHolder);
    }

    public DdcRefreshService(DdcLocalConfigRepository repository,
                             DdcConfigApplierRegistry applierRegistry,
                             DdcAckDelivery ackDelivery,
                             DdcLeaseSessionHolder sessionHolder) {
        this(repository, applierRegistry, ackDelivery::submit, sessionHolder);
    }

    private DdcRefreshService(DdcLocalConfigRepository repository,
                              DdcConfigApplierRegistry applierRegistry,
                              AckSubmitter ackSubmitter,
                              DdcLeaseSessionHolder sessionHolder) {
        this.repository = repository;
        this.applierRegistry = applierRegistry;
        this.ackSubmitter = ackSubmitter;
        this.sessionHolder = sessionHolder;
    }

    public void applySnapshots(List<DdcConfigValue> configs) {
        if (configs == null || configs.isEmpty()) {
            return;
        }
        configs.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparingInt(this::priority)
                        .thenComparing(
                                DdcConfigValue::getConfigKey,
                                Comparator.nullsLast(String::compareTo)
                        ))
                .forEach(this::applySnapshot);
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

    private int priority(DdcConfigValue config) {
        String configKey = config.getConfigKey();
        if (configKey == null || configKey.isBlank()) {
            return Integer.MAX_VALUE;
        }
        return applierRegistry.resolve(configKey).priority();
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

    private static AckSubmitter directAck(DdcAdminClient adminClient) {
        if (adminClient == null) {
            throw new IllegalArgumentException("adminClient must not be null");
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
