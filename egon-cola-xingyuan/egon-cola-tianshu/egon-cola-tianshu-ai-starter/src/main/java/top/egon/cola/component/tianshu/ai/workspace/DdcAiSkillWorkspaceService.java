package top.egon.cola.component.tianshu.ai.workspace;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.tianshu.ai.observability.DdcAiStarterMetrics;
import top.egon.cola.component.tianshu.ai.properties.DdcAiWatchedResourceDTO;
import top.egon.cola.component.tianshu.ai.properties.DdcAiWorkspaceProperties;
import top.egon.cola.component.tianshu.api.client.DdcAiSyncClient;
import top.egon.cola.component.tianshu.api.client.DdcSkillClient;
import top.egon.cola.component.tianshu.error.DdcClientTransportException;
import top.egon.cola.component.tianshu.model.ai.DdcAiResourceQuery;
import top.egon.cola.component.tianshu.model.ai.DdcAiVersionHeadDTO;
import top.egon.cola.component.tianshu.model.ai.DdcSkillResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 拉取编排：按版本号拉取并落盘、按发布头对账；每资源串行，失败只记录并等待下次对账。
 * / Pull orchestration: pulls and materializes by version number and reconciles against the published head;
 * serialized per resource, and failures are only recorded until the next reconcile.
 */
@Slf4j
public class DdcAiSkillWorkspaceService implements DdcAiSkillWorkspace {

    static final String REFRESH = "tianshu.ai.workspace.refresh";

    private final DdcSkillClient skillClient;
    private final DdcAiSyncClient syncClient;
    private final DdcAiSkillMaterializer materializer;
    private final DdcAiWorkspaceProperties properties;
    private final DdcAiStarterMetrics metrics;
    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final Set<String> warnedPermanent = ConcurrentHashMap.newKeySet();

    public DdcAiSkillWorkspaceService(DdcSkillClient skillClient, DdcAiSyncClient syncClient,
            DdcAiSkillMaterializer materializer, DdcAiWorkspaceProperties properties, DdcAiStarterMetrics metrics) {
        this.skillClient = Objects.requireNonNull(skillClient, "skillClient");
        this.syncClient = Objects.requireNonNull(syncClient, "syncClient");
        this.materializer = Objects.requireNonNull(materializer, "materializer");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
    }

    @Override
    public Optional<Path> currentDirectory(String namespaceCode, String resourceKey) {
        return materializer.currentDirectory(properties.getRoot(), namespaceCode, resourceKey);
    }

    @Override
    public Optional<DdcAiSkillWorkspaceStateDTO> state(String namespaceCode, String resourceKey) {
        return materializer.readState(properties.getRoot(), namespaceCode, resourceKey);
    }

    /** 拉取指定版本；本地已是该版本时跳过。 / Pulls an exact version; skipped when already local. */
    public boolean pull(DdcAiWatchedResourceDTO resource, long versionNo) {
        ReentrantLock lock = lockFor(resource);
        lock.lock();
        try {
            if (state(resource.getNamespaceCode(), resource.getResourceKey())
                    .map(current -> Long.valueOf(versionNo).equals(current.versionNo())).orElse(false)) {
                return false;
            }
            DdcSkillResult skill = skillClient.get(new DdcAiResourceQuery(resource.getNamespaceCode(),
                    resource.getResourceKey(), versionNo, resource.getResourceId()));
            if (skill == null || !Long.valueOf(versionNo).equals(skill.getVersionNo())
                    || !resource.getResourceId().equals(skill.getResourceId())) {
                throw new IllegalStateException("AI_WORKSPACE_PULL_IDENTITY_MISMATCH");
            }
            materializer.materialize(properties.getRoot(), resource.getNamespaceCode(), resource.getResourceKey(),
                    skill);
            record(resource, "PULLED");
            return true;
        } catch (IOException failure) {
            log.warn("AI workspace write failed for {}: {}", resource.watchKey(), failure.getClass().getSimpleName());
            record(resource, "IO_ERROR");
            return false;
        } catch (RuntimeException failure) {
            handle(resource, failure);
            return false;
        } finally {
            lock.unlock();
        }
    }

    /** 与发布头对账；固定版本只拉取一次。 / Reconciles with the head; a pinned version is pulled once. */
    public void reconcile(DdcAiWatchedResourceDTO resource) {
        ReentrantLock lock = lockFor(resource);
        lock.lock();
        try {
            Optional<DdcAiSkillWorkspaceStateDTO> state = state(resource.getNamespaceCode(),
                    resource.getResourceKey());
            if (resource.getPinnedVersionNo() != null) {
                if (!state.map(current -> resource.getPinnedVersionNo().equals(current.versionNo())).orElse(false)) {
                    pull(resource, resource.getPinnedVersionNo());
                }
                return;
            }
            DdcAiVersionHeadDTO head = syncClient.getVersionHead(new DdcAiResourceQuery(
                    resource.getNamespaceCode(), resource.getResourceKey(), null, resource.getResourceId()));
            if (head == null || !Boolean.TRUE.equals(head.getPublished()) || head.getVersionNo() == null) return;
            if (state.map(current -> current.matches(head)).orElse(false)) return;
            pull(resource, head.getVersionNo());
        } catch (RuntimeException failure) {
            handle(resource, failure);
        } finally {
            lock.unlock();
        }
    }

    private void handle(DdcAiWatchedResourceDTO resource, RuntimeException failure) {
        if (failure instanceof DdcClientTransportException transport && transport.retryable()) {
            log.warn("AI workspace refresh deferred for {}: transport unavailable", resource.watchKey());
            record(resource, "TRANSPORT");
            return;
        }
        if (warnedPermanent.add(resource.watchKey())) {
            log.warn("AI workspace refresh failed for {}; retrying each reconcile: {}", resource.watchKey(),
                    failure.getMessage());
        }
        record(resource, "ERROR");
    }

    private void record(DdcAiWatchedResourceDTO resource, String result) {
        metrics.increment(REFRESH, 1, "result", result, "resourceKey", resource.getResourceKey());
        if ("PULLED".equals(result)) warnedPermanent.remove(resource.watchKey());
    }

    private ReentrantLock lockFor(DdcAiWatchedResourceDTO resource) {
        return locks.computeIfAbsent(resource.watchKey(), key -> new ReentrantLock());
    }
}
