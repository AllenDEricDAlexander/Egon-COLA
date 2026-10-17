package top.egon.cola.component.tianshu.ai.workspace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.listener.MessageListener;
import top.egon.cola.component.tianshu.ai.properties.DdcAiWatchedResourceDTO;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcTenantProperties;
import top.egon.cola.component.tianshu.model.event.DdcAiPublicationEvent;
import top.egon.cola.component.tianshu.model.tenant.DdcTenantMessageDTO;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * 订阅 EVENT-103：只处理本租户、被监视且未固定版本的 Skill，并按事件版本号异步拉取。
 * / Subscribes to EVENT-103: handles only this tenant's watched, unpinned Skills and pulls the event's version
 * asynchronously.
 */
@Slf4j
public class DdcAiPublicationListener implements MessageListener<String> {

    private static final TypeReference<DdcTenantMessageDTO<DdcAiPublicationEvent>> ENVELOPE_TYPE =
            new TypeReference<>() { };

    private final ObjectMapper objectMapper;
    private final DdcTenantProperties tenantProperties;
    private final DdcAiSkillWorkspaceService workspace;
    private final Map<String, DdcAiWatchedResourceDTO> watched;
    private final Executor executor;

    public DdcAiPublicationListener(ObjectMapper objectMapper, DdcTenantProperties tenantProperties,
            DdcAiSkillWorkspaceService workspace, Map<String, DdcAiWatchedResourceDTO> watched, Executor executor) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.tenantProperties = Objects.requireNonNull(tenantProperties, "tenantProperties");
        this.workspace = Objects.requireNonNull(workspace, "workspace");
        this.watched = Map.copyOf(watched);
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public void onMessage(CharSequence channel, String json) {
        DdcTenantMessageDTO<DdcAiPublicationEvent> envelope;
        try {
            envelope = objectMapper.readValue(json, ENVELOPE_TYPE);
        } catch (JsonProcessingException | IllegalArgumentException malformed) {
            log.warn("Ignoring malformed AI publication message");
            return;
        }
        long tenantId = tenantProperties.requireTenantId();
        if (envelope == null || envelope.getPayload() == null || !envelope.belongsToTenant(tenantId)
                || !envelope.getPayload().hasValidIdentity()
                || !Long.valueOf(tenantId).equals(envelope.getPayload().getTenantId())) {
            log.debug("Ignoring AI publication outside tenant {}", tenantId);
            return;
        }
        DdcAiPublicationEvent event = envelope.getPayload();
        if (!"SKILL".equals(event.getKind()) || event.getVersionNo() == null) return;
        DdcAiWatchedResourceDTO resource = watched.get(event.getNamespaceCode() + "/" + event.getResourceKey());
        if (resource == null || resource.getPinnedVersionNo() != null
                || !resource.getResourceId().equals(event.getResourceId())) {
            return;
        }
        long versionNo = event.getVersionNo();
        try {
            executor.execute(() -> workspace.pull(resource, versionNo));
        } catch (RejectedExecutionException rejected) {
            log.warn("AI workspace pull for {} rejected; reconcile will catch up", resource.watchKey());
        }
    }
}
