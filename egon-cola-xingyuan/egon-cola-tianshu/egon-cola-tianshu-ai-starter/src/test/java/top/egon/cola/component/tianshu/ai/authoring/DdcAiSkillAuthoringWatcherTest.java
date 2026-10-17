package top.egon.cola.component.tianshu.ai.authoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.tianshu.ai.authoring.DdcAiAuthoringStateDTO.Status;
import top.egon.cola.component.tianshu.ai.observability.DdcAiStarterMetrics;
import top.egon.cola.component.tianshu.ai.properties.DdcAiAuthoringProperties;
import top.egon.cola.component.tianshu.ai.properties.DdcAiWatchedResourceDTO;
import top.egon.cola.component.tianshu.api.client.DdcAiSyncClient;
import top.egon.cola.component.tianshu.error.DdcClientTransportException;
import top.egon.cola.component.tianshu.error.DdcException;
import top.egon.cola.component.tianshu.model.ai.DdcAiPublicationReceiptDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiRevisionDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiVersionHeadDTO;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcAiSkillAuthoringWatcherTest {

    private final DdcAiSyncClient syncClient = mock(DdcAiSyncClient.class);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-17T08:00:00Z"));

    @Test
    void uploadsWhenTreeHashChangesAfterDebounce(@TempDir Path root) throws IOException {
        DdcAiWatchedResourceDTO resource = resource(false);
        DdcAiSkillTreeReaderTest.write(root.resolve("skill-x"), "SKILL.md", "# Refund");
        when(syncClient.getVersionHead(any())).thenReturn(head(9L));
        when(syncClient.replaceSkillDraft(any())).thenReturn(revision(10L));
        DdcAiSkillAuthoringWatcher watcher = watcher(root, resource);

        watcher.pollOnce();
        verify(syncClient, never()).replaceSkillDraft(any());
        clock.advance(Duration.ofSeconds(3));
        watcher.pollOnce();
        watcher.pollOnce();

        verify(syncClient, times(1)).replaceSkillDraft(argThat(upload -> upload.getExpectedRevision() == 9L
                && "Refund".equals(upload.getDisplayName()) && upload.getFiles().size() == 2
                && "".equals(upload.getFiles().get(0).getPath())));
        DdcAiAuthoringStateDTO state = watcher.state(resource);
        assertThat(state.status()).isEqualTo(Status.UPLOADED);
        assertThat(state.baseRevision()).isEqualTo(10L);
        verify(syncClient, never()).publishSkillDraft(any());
    }

    @Test
    void debouncesRapidEdits(@TempDir Path root) throws IOException {
        DdcAiWatchedResourceDTO resource = resource(false);
        Path dir = root.resolve("skill-x");
        DdcAiSkillAuthoringWatcher watcher = watcher(root, resource);

        DdcAiSkillTreeReaderTest.write(dir, "SKILL.md", "# v1");
        watcher.pollOnce();
        clock.advance(Duration.ofSeconds(1));
        DdcAiSkillTreeReaderTest.write(dir, "SKILL.md", "# v2");
        watcher.pollOnce();
        clock.advance(Duration.ofSeconds(1));
        watcher.pollOnce();

        verify(syncClient, never()).replaceSkillDraft(any());
    }

    @Test
    void publishesWhenAutoPublishConfiguredWithAContentDerivedRequestId(@TempDir Path root) throws IOException {
        DdcAiWatchedResourceDTO resource = resource(true);
        DdcAiSkillTreeReaderTest.write(root.resolve("skill-x"), "SKILL.md", "# Refund");
        when(syncClient.getVersionHead(any())).thenReturn(head(9L));
        when(syncClient.replaceSkillDraft(any())).thenReturn(revision(10L));
        when(syncClient.publishSkillDraft(any()))
                .thenThrow(new DdcClientTransportException("UNAVAILABLE", true))
                .thenReturn(DdcAiPublicationReceiptDTO.builder().publicationId(5L).resourceId(7L).targetVersionId(70L)
                        .targetVersionNo(4L).resourceRevision(11L).contentSha256("a".repeat(64)).build());
        DdcAiSkillAuthoringWatcher watcher = watcher(root, resource);

        watcher.pollOnce();
        clock.advance(Duration.ofSeconds(3));
        watcher.pollOnce();
        assertThat(watcher.state(resource).status()).isEqualTo(Status.UPLOADED);
        watcher.pollOnce();

        verify(syncClient, times(1)).replaceSkillDraft(any());
        verify(syncClient, times(2)).publishSkillDraft(argThat(publish -> publish.getExpectedRevision() == 10L
                && publish.getRequestId().matches("^auth-[0-9a-f]{32}-7$")
                && "from authoring".equals(publish.getChangeNote())));
        assertThat(watcher.state(resource).status()).isEqualTo(Status.PUBLISHED);
        assertThat(watcher.state(resource).baseRevision()).isEqualTo(11L);
    }

    @Test
    void conflictStopsUntilResync(@TempDir Path root) throws IOException {
        DdcAiWatchedResourceDTO resource = resource(false);
        Path dir = root.resolve("skill-x");
        DdcAiSkillTreeReaderTest.write(dir, "SKILL.md", "# a");
        when(syncClient.getVersionHead(any())).thenReturn(head(9L));
        when(syncClient.replaceSkillDraft(any()))
                .thenThrow(new DdcException(56303, "TIANSHU_AI_REVISION_CONFLICT", "stale"))
                .thenReturn(revision(13L));
        DdcAiSkillAuthoringWatcher watcher = watcher(root, resource);

        watcher.pollOnce();
        clock.advance(Duration.ofSeconds(3));
        watcher.pollOnce();
        assertThat(watcher.state(resource).status()).isEqualTo(Status.CONFLICT);

        DdcAiSkillTreeReaderTest.write(dir, "SKILL.md", "# b");
        for (int round = 0; round < 3; round++) {
            clock.advance(Duration.ofSeconds(3));
            watcher.pollOnce();
        }
        verify(syncClient, times(1)).replaceSkillDraft(any());

        when(syncClient.getVersionHead(any())).thenReturn(head(12L));
        watcher.resync(resource);
        watcher.pollOnce();
        clock.advance(Duration.ofSeconds(3));
        watcher.pollOnce();

        verify(syncClient, times(2)).replaceSkillDraft(any());
        verify(syncClient).replaceSkillDraft(argThat(upload -> upload.getExpectedRevision() == 12L));
        assertThat(watcher.state(resource).status()).isEqualTo(Status.UPLOADED);
        assertThat(watcher.state(resource).baseRevision()).isEqualTo(13L);
    }

    private DdcAiSkillAuthoringWatcher watcher(Path root, DdcAiWatchedResourceDTO resource) {
        DdcAiAuthoringProperties properties = DdcAiAuthoringProperties.builder().enabled(true).root(root)
                .debounce(Duration.ofSeconds(2)).pollInterval(Duration.ofSeconds(5)).resources(List.of(resource))
                .build();
        return new DdcAiSkillAuthoringWatcher(syncClient, new DdcAiSkillTreeReader(), properties,
                new ObjectMapper().registerModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS),
                DdcAiStarterMetrics.NOOP, clock, Executors.newSingleThreadScheduledExecutor());
    }

    private static DdcAiWatchedResourceDTO resource(boolean autoPublish) {
        return DdcAiWatchedResourceDTO.builder().namespaceCode("ns-a").resourceKey("skill-x").resourceId(7L)
                .autoPublish(autoPublish).changeNote("from authoring").build();
    }

    private static DdcAiVersionHeadDTO head(long revision) {
        return DdcAiVersionHeadDTO.builder().resourceId(7L).resourceKey("skill-x").resourceRevision(revision)
                .published(false).build();
    }

    private static DdcAiRevisionDTO revision(long revision) {
        return DdcAiRevisionDTO.builder().resourceId(7L).resourceRevision(revision).draftSha256("b".repeat(64))
                .draftMatchesCurrent(false).build();
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
