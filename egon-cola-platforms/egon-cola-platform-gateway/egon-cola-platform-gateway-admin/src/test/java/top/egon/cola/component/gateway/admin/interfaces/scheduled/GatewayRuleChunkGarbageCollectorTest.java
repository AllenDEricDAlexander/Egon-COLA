package top.egon.cola.component.gateway.admin.interfaces.scheduled;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.management.DdcManagementClient;
import top.egon.cola.component.ddc.management.model.DdcManagementConfig;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigDeleteRequest;
import top.egon.cola.component.gateway.admin.application.release.GatewayReleasePublicationStore;
import top.egon.cola.component.gateway.admin.config.GatewayAdminProperties;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayRuleChunkGarbageCollectorTest {

    private static final Instant NOW =
            Instant.parse("2026-07-26T08:00:00Z");

    @Test
    void deletesOnlyJournalCandidatesOlderThanRetention() {
        GatewayReleasePublicationStore journal =
                mock(GatewayReleasePublicationStore.class);
        DdcManagementClient client = mock(DdcManagementClient.class);
        GatewayReleasePublicationStore.ChunkCleanupCandidate candidate =
                candidate("change-1", "release-old", "chunk.old.0", 3L);
        when(journal.findChunkCleanupCandidates(
                NOW.minus(Duration.ofHours(24))
        )).thenReturn(List.of(candidate));
        GatewayRuleChunkGarbageCollector collector = collector(
                journal,
                client
        );

        collector.collectOnce();

        verify(client).delete(new DdcManagementConfigDeleteRequest(
                "gateway-engine-orders",
                "test",
                "default",
                "chunk.old.0",
                3L,
                "gateway_rule_chunk_gc",
                "expired Gateway release chunk release-old"
        ));
        verify(journal).markChunkCleaned("change-1", NOW);
        assertThat(collector.deletedCount()).isEqualTo(1);
        assertThat(collector.failedCount()).isZero();
    }

    @Test
    void failedCasDeleteIsRetriedOnALaterRun() {
        GatewayReleasePublicationStore journal =
                mock(GatewayReleasePublicationStore.class);
        DdcManagementClient client = mock(DdcManagementClient.class);
        GatewayReleasePublicationStore.ChunkCleanupCandidate candidate =
                candidate("change-2", "release-old", "chunk.old.1", 4L);
        when(journal.findChunkCleanupCandidates(any()))
                .thenReturn(List.of(candidate));
        doThrow(new IllegalStateException("version changed"))
                .when(client)
                .delete(any());
        when(client.findConfig(any())).thenReturn(Optional.of(
                new DdcManagementConfig(
                        "gateway-engine-orders",
                        "test",
                        "default",
                        "chunk.old.1",
                        "value",
                        "STRING",
                        5L,
                        true,
                        false,
                        NOW
                )
        ));
        GatewayRuleChunkGarbageCollector collector = collector(
                journal,
                client
        );

        collector.collectOnce();

        verify(journal, never()).markChunkCleaned(any(), any());
        assertThat(collector.failedCount()).isEqualTo(1);
    }

    @Test
    void lostDeleteResponseConvergesWhenExactGetIsAbsent() {
        GatewayReleasePublicationStore journal =
                mock(GatewayReleasePublicationStore.class);
        DdcManagementClient client = mock(DdcManagementClient.class);
        GatewayReleasePublicationStore.ChunkCleanupCandidate candidate =
                candidate("change-3", "release-old", "chunk.old.2", 5L);
        when(journal.findChunkCleanupCandidates(any()))
                .thenReturn(List.of(candidate));
        doThrow(new IllegalStateException("response lost"))
                .when(client)
                .delete(any());
        when(client.findConfig(any())).thenReturn(Optional.empty());
        GatewayRuleChunkGarbageCollector collector = collector(
                journal,
                client
        );

        collector.collectOnce();

        verify(journal).markChunkCleaned("change-3", NOW);
        assertThat(collector.deletedCount()).isEqualTo(1);
        assertThat(collector.failedCount()).isZero();
    }

    private GatewayRuleChunkGarbageCollector collector(
            GatewayReleasePublicationStore journal,
            DdcManagementClient client) {
        GatewayAdminProperties properties = new GatewayAdminProperties();
        properties.getRuleChunk().setRetention(Duration.ofHours(24));
        return new GatewayRuleChunkGarbageCollector(
                journal,
                client,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private GatewayReleasePublicationStore.ChunkCleanupCandidate candidate(
            String changeId,
            String releaseId,
            String configKey,
            long targetVersion) {
        return new GatewayReleasePublicationStore.ChunkCleanupCandidate(
                changeId,
                releaseId,
                "gateway-engine-orders",
                "test",
                "default",
                configKey,
                targetVersion
        );
    }
}
