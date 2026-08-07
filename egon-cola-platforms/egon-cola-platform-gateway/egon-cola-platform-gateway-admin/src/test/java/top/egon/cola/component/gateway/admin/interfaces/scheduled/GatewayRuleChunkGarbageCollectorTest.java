package top.egon.cola.component.gateway.admin.interfaces.scheduled;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.egon.cola.component.ddc.management.DdcManagementClient;
import top.egon.cola.component.ddc.management.model.DdcManagementConfig;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishResult;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishStatus;
import top.egon.cola.component.gateway.admin.application.release.GatewayReleasePublicationStore;
import top.egon.cola.component.gateway.admin.config.GatewayAdminProperties;
import top.egon.cola.component.gateway.admin.rule.GatewayDdcYamlDocument;

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
    void publishesYamlWithoutOnlyTheExpiredChunk() {
        GatewayReleasePublicationStore journal =
                mock(GatewayReleasePublicationStore.class);
        DdcManagementClient client = mock(DdcManagementClient.class);
        GatewayReleasePublicationStore.ChunkCleanupCandidate candidate =
                candidate(
                        "change-1",
                        "release-old",
                        "gateway.rules.chunk.release-old.0",
                        3L
                );
        when(journal.findChunkCleanupCandidates(
                NOW.minus(Duration.ofHours(24))
        )).thenReturn(List.of(candidate));
        when(client.findConfig(any())).thenReturn(Optional.of(config(
                yaml().putLeaf(
                        yaml().putLeaf(
                                null,
                                GatewayDdcYamlDocument.ACTIVE_CONFIG_KEY,
                                "activation"
                        ),
                        candidate.configKey(),
                        "chunk"
                ),
                7L
        )));
        when(client.publish(any())).thenAnswer(invocation -> {
            DdcManagementPublishRequest request = invocation.getArgument(0);
            return result(request, DdcManagementPublishStatus.SUCCESS);
        });
        GatewayRuleChunkGarbageCollector collector = collector(
                journal,
                client
        );

        collector.collectOnce();

        ArgumentCaptor<DdcManagementPublishRequest> captor =
                ArgumentCaptor.forClass(DdcManagementPublishRequest.class);
        verify(client).publish(captor.capture());
        assertThat(captor.getValue().expectedVersion()).isEqualTo(7L);
        assertThat(yaml().leafValue(
                captor.getValue().configValue(),
                candidate.configKey()
        )).isEmpty();
        assertThat(yaml().leafValue(
                captor.getValue().configValue(),
                GatewayDdcYamlDocument.ACTIVE_CONFIG_KEY
        )).contains("activation");
        verify(journal).markChunkCleaned("change-1", NOW);
        assertThat(collector.deletedCount()).isEqualTo(1);
        assertThat(collector.failedCount()).isZero();
    }

    @Test
    void failedYamlPublishIsRetriedOnALaterRun() {
        GatewayReleasePublicationStore journal =
                mock(GatewayReleasePublicationStore.class);
        DdcManagementClient client = mock(DdcManagementClient.class);
        GatewayReleasePublicationStore.ChunkCleanupCandidate candidate =
                candidate(
                        "change-2",
                        "release-old",
                        "gateway.rules.chunk.release-old.1",
                        4L
                );
        when(journal.findChunkCleanupCandidates(any()))
                .thenReturn(List.of(candidate));
        when(client.publish(any())).thenAnswer(invocation -> result(
                invocation.getArgument(0),
                DdcManagementPublishStatus.FAILED
        ));
        when(client.findConfig(any())).thenReturn(Optional.of(
                config(documentWithChunk(candidate.configKey()), 5L)
        ));
        GatewayRuleChunkGarbageCollector collector = collector(
                journal,
                client
        );

        collector.collectOnce();

        verify(client).publish(any());
        verify(journal, never()).markChunkCleaned(any(), any());
        assertThat(collector.failedCount()).isEqualTo(1);
    }

    @Test
    void lostPublishResponseConvergesWhenTheLeafIsAlreadyAbsent() {
        GatewayReleasePublicationStore journal =
                mock(GatewayReleasePublicationStore.class);
        DdcManagementClient client = mock(DdcManagementClient.class);
        GatewayReleasePublicationStore.ChunkCleanupCandidate candidate =
                candidate(
                        "change-3",
                        "release-old",
                        "gateway.rules.chunk.release-old.2",
                        5L
                );
        when(journal.findChunkCleanupCandidates(any()))
                .thenReturn(List.of(candidate));
        doThrow(new IllegalStateException("response lost"))
                .when(client).publish(any());
        when(client.findConfig(any())).thenReturn(
                Optional.of(config(
                        documentWithChunk(candidate.configKey()),
                        5L
                )),
                Optional.of(config(
                        yaml().putLeaf(
                                "feature:\n  enabled: true\n",
                                GatewayDdcYamlDocument.ACTIVE_CONFIG_KEY,
                                "activation"
                        ),
                        6L
                ))
        );
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
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofSeconds(30)
        );
    }

    private DdcManagementConfig config(String content, long version) {
        return new DdcManagementConfig(
                "infra",
                "test",
                "ge",
                "application.yml",
                content,
                "YAML",
                version,
                true,
                false,
                NOW
        );
    }

    private DdcManagementPublishResult result(
            DdcManagementPublishRequest request,
            DdcManagementPublishStatus status) {
        return new DdcManagementPublishResult(
                request.changeId(),
                status,
                request.expectedVersion() + 1,
                "checksum",
                1,
                List.of(),
                null,
                NOW,
                NOW,
                NOW
        );
    }

    private GatewayDdcYamlDocument yaml() {
        return new GatewayDdcYamlDocument();
    }

    private String documentWithChunk(String configKey) {
        return yaml().putLeaf(
                yaml().putLeaf(
                        null,
                        GatewayDdcYamlDocument.ACTIVE_CONFIG_KEY,
                        "activation"
                ),
                configKey,
                "chunk"
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
