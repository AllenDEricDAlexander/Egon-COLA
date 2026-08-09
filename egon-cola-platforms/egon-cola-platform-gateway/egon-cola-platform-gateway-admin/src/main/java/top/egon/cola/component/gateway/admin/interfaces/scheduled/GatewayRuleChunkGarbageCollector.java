package top.egon.cola.component.gateway.admin.interfaces.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.ddc.model.management.DdcManagementConfig;
import top.egon.cola.component.ddc.model.management.DdcManagementConfigQuery;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishResult;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishStatus;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishTask;
import top.egon.cola.component.gateway.admin.application.release.GatewayReleasePublicationStore;
import top.egon.cola.component.gateway.admin.config.GatewayAdminProperties;
import top.egon.cola.component.gateway.admin.rule.GatewayDdcYamlDocument;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class GatewayRuleChunkGarbageCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            GatewayRuleChunkGarbageCollector.class
    );

    private final GatewayReleasePublicationStore journal;

    private final DdcManagementClient client;

    private final GatewayAdminProperties properties;

    private final Clock clock;

    private final Duration publishTimeout;

    private final GatewayDdcYamlDocument yamlDocument;

    private final AtomicLong deleted = new AtomicLong();

    private final AtomicLong failed = new AtomicLong();

    @Autowired
    public GatewayRuleChunkGarbageCollector(
            GatewayReleasePublicationStore journal,
            ObjectProvider<DdcManagementClient> client,
            GatewayAdminProperties properties,
            @Value("${gateway.admin.ddc.publish-timeout:PT30S}")
            Duration publishTimeout) {
        this(
                journal,
                client.getIfAvailable(),
                properties,
                Clock.systemUTC(),
                publishTimeout
        );
    }

    GatewayRuleChunkGarbageCollector(
            GatewayReleasePublicationStore journal,
            DdcManagementClient client,
            GatewayAdminProperties properties,
            Clock clock,
            Duration publishTimeout) {
        this.journal = Objects.requireNonNull(journal, "journal");
        this.client = client;
        this.properties = Objects.requireNonNull(properties, "properties");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.publishTimeout = positive(
                publishTimeout,
                "Gateway DDC publish timeout"
        );
        this.yamlDocument = new GatewayDdcYamlDocument();
        retention();
    }

    @Scheduled(
            fixedDelayString =
                    "${gateway.admin.rule-chunk.cleanup-delay:PT1H}"
    )
    public void collect() {
        collectOnce();
    }

    public void collectOnce() {
        if (client == null) {
            return;
        }
        journal.findChunkCleanupCandidates(
                clock.instant().minus(retention())
        ).forEach(this::delete);
    }

    public long deletedCount() {
        return deleted.get();
    }

    public long failedCount() {
        return failed.get();
    }

    private void delete(
            GatewayReleasePublicationStore.ChunkCleanupCandidate candidate) {
        String changeId = UuidV7.string();
        try {
            DdcManagementConfig config = current(candidate).orElse(null);
            if (config == null || config.deleted()) {
                markCleaned(candidate);
                return;
            }
            validate(config);
            GatewayDdcYamlDocument.Removal removal =
                    yamlDocument.removeLeaf(
                            config.content(),
                            candidate.configKey()
                    );
            if (!removal.removed()) {
                markCleaned(candidate);
                return;
            }
            DdcManagementPublishResult result = client.publish(
                    new DdcManagementPublishRequest(
                            properties.getDdc().getTargetBizCode(),
                            candidate.env(),
                            properties.getDdc().getTargetAppCode(),
                            config.resourceName(),
                            removal.content(),
                            config.format(),
                            config.version(),
                            changeId,
                            publishTimeout.toMillis(),
                            "gateway_rule_chunk_gc"
                    )
            );
            if (result.status() != DdcManagementPublishStatus.SUCCESS) {
                recordFailure(candidate, new IllegalStateException(
                        "Gateway rule chunk cleanup publish did not succeed"
                ));
                return;
            }
        } catch (RuntimeException failure) {
            Optional<DdcManagementPublishStatus> recovered =
                    publishStatus(changeId);
            if (recovered.isPresent()
                    && recovered.get()
                    != DdcManagementPublishStatus.SUCCESS) {
                recordFailure(candidate, failure);
                return;
            }
            if (recovered.isEmpty() && !alreadyDeleted(candidate)) {
                recordFailure(candidate, failure);
                return;
            }
        }
        markCleaned(candidate);
    }

    private void markCleaned(
            GatewayReleasePublicationStore.ChunkCleanupCandidate candidate) {
        try {
            journal.markChunkCleaned(
                    candidate.changeId(),
                    clock.instant()
            );
            deleted.incrementAndGet();
        } catch (RuntimeException failure) {
            recordFailure(candidate, failure);
        }
    }

    private boolean alreadyDeleted(
            GatewayReleasePublicationStore.ChunkCleanupCandidate candidate) {
        try {
            return current(candidate)
                    .map(config -> config.deleted()
                            || yamlDocument.leafValue(
                                    config.content(),
                                    candidate.configKey()
                            ).isEmpty())
                    .orElse(true);
        } catch (RuntimeException unavailable) {
            return false;
        }
    }

    private Optional<DdcManagementConfig> current(
            GatewayReleasePublicationStore.ChunkCleanupCandidate candidate) {
        return client.findConfig(new DdcManagementConfigQuery(
                properties.getDdc().getTargetBizCode(),
                candidate.env(),
                properties.getDdc().getTargetAppCode()
        ));
    }

    private Optional<DdcManagementPublishStatus> publishStatus(
            String changeId) {
        try {
            DdcManagementPublishTask task = client.getPublishTask(changeId);
            return task == null
                    ? Optional.empty()
                    : Optional.of(task.status());
        } catch (RuntimeException unavailable) {
            return Optional.empty();
        }
    }

    private void validate(DdcManagementConfig config) {
        if (config.version() == null || config.version() < 0
                || !config.enabled()
                || !GatewayDdcYamlDocument.RESOURCE_NAME.equals(
                config.resourceName())
                || !GatewayDdcYamlDocument.FORMAT.equals(config.format())) {
            throw new IllegalStateException(
                    "Gateway rule cleanup requires an enabled "
                            + "application.yml/YAML document"
            );
        }
    }

    private void recordFailure(
            GatewayReleasePublicationStore.ChunkCleanupCandidate candidate,
            RuntimeException failure) {
        failed.incrementAndGet();
        LOGGER.warn(
                "Gateway rule chunk cleanup failed releaseId={} "
                        + "configKey={} targetVersion={} cause={}",
                candidate.releaseId(),
                candidate.configKey(),
                candidate.targetVersion(),
                failure.getClass().getSimpleName()
        );
    }

    private Duration retention() {
        return positive(
                properties.getRuleChunk().getRetention(),
                "gateway rule chunk retention"
        );
    }

    private Duration positive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
