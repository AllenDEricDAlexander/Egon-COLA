package top.egon.cola.component.tianshu.ai.workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.tianshu.ai.properties.DdcAiWatchedResourceDTO;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcTenantProperties;
import top.egon.cola.component.tianshu.model.event.DdcAiPublicationEvent;
import top.egon.cola.component.tianshu.model.tenant.DdcTenantMessageDTO;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DdcAiPublicationListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final DdcAiSkillWorkspaceService workspace = mock(DdcAiSkillWorkspaceService.class);
    private final DdcAiWatchedResourceDTO watched = DdcAiWorkspaceReconcileSchedulerTest.resource(null);
    private final DdcTenantProperties tenant = tenant(42L);
    private final DdcAiPublicationListener listener = new DdcAiPublicationListener(objectMapper, tenant, workspace,
            Map.of(watched.watchKey(), watched), Runnable::run);

    @Test
    void triggersPullByVersionNoForThisTenant() throws Exception {
        listener.onMessage("topic", json(42L, event(42L, "skill-x", "SKILL", 4L)));

        verify(workspace, times(1)).pull(eq(watched), eq(4L));
    }

    @Test
    void ignoresEnvelopeOfOtherTenantMalformedPayloadPromptsAndUnwatchedResources() throws Exception {
        listener.onMessage("topic", json(43L, event(43L, "skill-x", "SKILL", 4L)));
        listener.onMessage("topic", json(42L, event(43L, "skill-x", "SKILL", 4L)));
        listener.onMessage("topic", "{not json");
        listener.onMessage("topic", json(42L, event(42L, "skill-x", "PROMPT", 4L)));
        listener.onMessage("topic", json(42L, event(42L, "other-skill", "SKILL", 4L)));

        verify(workspace, never()).pull(any(), anyLong());
    }

    @Test
    void pinnedResourcesIgnoreEvents() throws Exception {
        DdcAiWatchedResourceDTO pinned = DdcAiWorkspaceReconcileSchedulerTest.resource(3L);
        DdcAiPublicationListener pinnedListener = new DdcAiPublicationListener(objectMapper, tenant, workspace,
                Map.of(pinned.watchKey(), pinned), Runnable::run);

        pinnedListener.onMessage("topic", json(42L, event(42L, "skill-x", "SKILL", 4L)));

        verify(workspace, never()).pull(any(), anyLong());
    }

    private String json(long envelopeTenant, DdcAiPublicationEvent event) throws Exception {
        return objectMapper.writeValueAsString(new DdcTenantMessageDTO<>(envelopeTenant, event));
    }

    private static DdcAiPublicationEvent event(long tenantId, String resourceKey, String kind, long versionNo) {
        return DdcAiPublicationEvent.builder().eventId("ai-pub-555").tenantId(tenantId).resourceId(7L).kind(kind)
                .bizCode("xingyuan").namespaceCode("ns-a").env("dev").appCode("order").resourceKey(resourceKey)
                .operation("PUBLISH").versionId(9L).versionNo(versionNo).contentSha256("a".repeat(64))
                .resourceRevision(8L).actor("user-1").occurredAt(Instant.parse("2026-10-17T05:00:00Z")).build();
    }

    private static DdcTenantProperties tenant(long tenantId) {
        DdcTenantProperties properties = new DdcTenantProperties();
        properties.setTenantId(tenantId);
        return properties;
    }
}
