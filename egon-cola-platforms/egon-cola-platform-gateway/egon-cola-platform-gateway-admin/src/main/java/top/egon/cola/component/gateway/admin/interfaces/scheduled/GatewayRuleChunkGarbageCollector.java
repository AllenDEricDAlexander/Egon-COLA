package top.egon.cola.component.gateway.admin.interfaces.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.egon.cola.component.ddc.management.DdcManagementClient;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigDeleteRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigQuery;
import top.egon.cola.component.gateway.admin.application.release.GatewayReleasePublicationStore;
import top.egon.cola.component.gateway.admin.config.GatewayAdminProperties;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
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

    private final AtomicLong deleted = new AtomicLong();

    private final AtomicLong failed = new AtomicLong();

    @Autowired
    public GatewayRuleChunkGarbageCollector(
            GatewayReleasePublicationStore journal,
            ObjectProvider<DdcManagementClient> client,
            GatewayAdminProperties properties) {
        this(
                journal,
                client.getIfAvailable(),
                properties,
                Clock.systemUTC()
        );
    }

    GatewayRuleChunkGarbageCollector(
            GatewayReleasePublicationStore journal,
            DdcManagementClient client,
            GatewayAdminProperties properties,
            Clock clock) {
        this.journal = Objects.requireNonNull(journal, "journal");
        this.client = client;
        this.properties = Objects.requireNonNull(properties, "properties");
        this.clock = Objects.requireNonNull(clock, "clock");
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
        try {
            client.delete(new DdcManagementConfigDeleteRequest(
                    properties.getDdc().getTargetBizCode(),
                    candidate.env(),
                    properties.getDdc().getTargetAppCode(),
                    candidate.configKey(),
                    candidate.targetVersion(),
                    "gateway_rule_chunk_gc",
                    "expired Gateway release chunk " + candidate.releaseId()
            ));
        } catch (RuntimeException failure) {
            if (!alreadyDeleted(candidate)) {
                recordFailure(candidate, failure);
                return;
            }
        }
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
            return client.findConfig(new DdcManagementConfigQuery(
                    properties.getDdc().getTargetBizCode(),
                    candidate.env(),
                    properties.getDdc().getTargetAppCode(),
                    candidate.configKey()
            )).map(config -> config.deleted()).orElse(true);
        } catch (RuntimeException unavailable) {
            return false;
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
        Duration retention = properties.getRuleChunk().getRetention();
        if (retention == null || retention.isZero() || retention.isNegative()) {
            throw new IllegalArgumentException(
                    "gateway rule chunk retention must be positive"
            );
        }
        return retention;
    }
}
