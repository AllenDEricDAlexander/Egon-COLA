package top.egon.cola.component.tianshu.ai.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.tianshu.ai.observability.DdcAiStarterMetrics;
import top.egon.cola.component.tianshu.ai.properties.DdcAiWatchedResourceDTO;
import top.egon.cola.component.tianshu.ai.properties.DdcAiWorkspaceProperties;
import top.egon.cola.component.tianshu.api.client.DdcAiSyncClient;
import top.egon.cola.component.tianshu.api.client.DdcSkillClient;
import top.egon.cola.component.tianshu.error.DdcClientTransportException;
import top.egon.cola.component.tianshu.model.ai.DdcAiVersionHeadDTO;

import java.nio.file.Path;
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

class DdcAiWorkspaceReconcileSchedulerTest {

    private final DdcSkillClient skillClient = mock(DdcSkillClient.class);
    private final DdcAiSyncClient syncClient = mock(DdcAiSyncClient.class);

    @Test
    void pullsWhenHeadDiffersAndIsNoopWhenHeadMatches(@TempDir Path root) {
        DdcAiWatchedResourceDTO resource = resource(null);
        DdcAiSkillWorkspaceService workspace = workspace(root, resource);
        when(syncClient.getVersionHead(any())).thenReturn(head(2L, "a"));
        when(skillClient.get(any())).thenReturn(DdcAiSkillMaterializerTest.skill(2L, "a", "SKILL.md", "# v2"));
        DdcAiWorkspaceReconcileScheduler scheduler = scheduler(workspace, root, resource);

        assertThat(scheduler.reconcileOnce()).isEqualTo(1);
        assertThat(scheduler.reconcileOnce()).isEqualTo(1);

        verify(skillClient, times(1)).get(argThat(query -> query.versionNo() == 2L && query.resourceId() == 7L));
        assertThat(workspace.currentDirectory("ns-a", "skill-x")).isPresent();
    }

    @Test
    void unpublishedHeadAndTransportFailuresLeaveTheWorkspaceUntouched(@TempDir Path root) {
        DdcAiWatchedResourceDTO resource = resource(null);
        DdcAiSkillWorkspaceService workspace = workspace(root, resource);
        when(syncClient.getVersionHead(any()))
                .thenReturn(DdcAiVersionHeadDTO.builder().resourceId(7L).resourceKey("skill-x").resourceRevision(1L)
                        .published(false).build())
                .thenThrow(new DdcClientTransportException("UNAVAILABLE", true));
        DdcAiWorkspaceReconcileScheduler scheduler = scheduler(workspace, root, resource);

        scheduler.reconcileOnce();
        scheduler.reconcileOnce();

        verify(skillClient, never()).get(any());
        assertThat(workspace.currentDirectory("ns-a", "skill-x")).isEmpty();
    }

    @Test
    void pinnedVersionNeverSwitches(@TempDir Path root) {
        DdcAiWatchedResourceDTO pinned = resource(3L);
        DdcAiSkillWorkspaceService workspace = workspace(root, pinned);
        when(skillClient.get(any())).thenReturn(DdcAiSkillMaterializerTest.skill(3L, "b", "SKILL.md", "# v3"));
        when(syncClient.getVersionHead(any())).thenReturn(head(9L, "c"));
        DdcAiWorkspaceReconcileScheduler scheduler = scheduler(workspace, root, pinned);

        scheduler.reconcileOnce();
        scheduler.reconcileOnce();

        verify(syncClient, never()).getVersionHead(any());
        verify(skillClient, times(1)).get(argThat(query -> query.versionNo() == 3L));
        assertThat(workspace.state("ns-a", "skill-x").orElseThrow().versionNo()).isEqualTo(3L);
    }

    @Test
    void oneFailingResourceDoesNotStopTheOthers(@TempDir Path root) {
        DdcAiWatchedResourceDTO broken = resource(null).toBuilder().resourceKey("broken").build();
        DdcAiWatchedResourceDTO healthy = resource(null);
        DdcAiWorkspaceProperties properties = DdcAiWorkspaceProperties.builder().enabled(true).root(root)
                .resources(List.of(broken, healthy)).build();
        DdcAiSkillWorkspaceService workspace = new DdcAiSkillWorkspaceService(skillClient, syncClient,
                DdcAiSkillMaterializerTest.materializer(), properties, DdcAiStarterMetrics.NOOP);
        when(syncClient.getVersionHead(argThat(query -> query != null && "broken".equals(query.resourceKey()))))
                .thenThrow(new IllegalStateException("boom"));
        when(syncClient.getVersionHead(argThat(query -> query != null && "skill-x".equals(query.resourceKey()))))
                .thenReturn(head(2L, "a"));
        when(skillClient.get(any())).thenReturn(DdcAiSkillMaterializerTest.skill(2L, "a", "SKILL.md", "# v2"));

        new DdcAiWorkspaceReconcileScheduler(workspace, properties, Executors.newSingleThreadScheduledExecutor())
                .reconcileOnce();

        assertThat(workspace.currentDirectory("ns-a", "skill-x")).isPresent();
    }

    private DdcAiSkillWorkspaceService workspace(Path root, DdcAiWatchedResourceDTO resource) {
        DdcAiWorkspaceProperties properties = DdcAiWorkspaceProperties.builder().enabled(true).root(root)
                .resources(List.of(resource)).build();
        return new DdcAiSkillWorkspaceService(skillClient, syncClient, DdcAiSkillMaterializerTest.materializer(),
                properties, DdcAiStarterMetrics.NOOP);
    }

    private static DdcAiWorkspaceReconcileScheduler scheduler(DdcAiSkillWorkspaceService workspace, Path root,
            DdcAiWatchedResourceDTO resource) {
        return new DdcAiWorkspaceReconcileScheduler(workspace, DdcAiWorkspaceProperties.builder().enabled(true)
                .root(root).resources(List.of(resource)).build(), Executors.newSingleThreadScheduledExecutor());
    }

    static DdcAiWatchedResourceDTO resource(Long pinnedVersionNo) {
        return DdcAiWatchedResourceDTO.builder().namespaceCode("ns-a").resourceKey("skill-x").resourceId(7L)
                .pinnedVersionNo(pinnedVersionNo).build();
    }

    private static DdcAiVersionHeadDTO head(long versionNo, String shaChar) {
        return DdcAiVersionHeadDTO.builder().resourceId(7L).resourceKey("skill-x").versionId(100L + versionNo)
                .versionNo(versionNo).contentSha256(shaChar.repeat(64)).resourceRevision(5L).published(true).build();
    }
}
