package top.egon.cola.component.tianshu.ai.authoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.tianshu.ai.authoring.DdcAiAuthoringStateDTO.Status;
import top.egon.cola.component.tianshu.ai.observability.DdcAiStarterMetrics;
import top.egon.cola.component.tianshu.ai.properties.DdcAiAuthoringProperties;
import top.egon.cola.component.tianshu.ai.properties.DdcAiWatchedResourceDTO;
import top.egon.cola.component.tianshu.ai.workspace.DdcAiSkillMaterializer;
import top.egon.cola.component.tianshu.api.client.DdcAiSyncClient;
import top.egon.cola.component.tianshu.error.DdcException;
import top.egon.cola.component.tianshu.model.ai.DdcAiPublicationReceiptDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiResourceQuery;
import top.egon.cola.component.tianshu.model.ai.DdcAiRevisionDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiSkillDraftUploadDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiSkillPublishDTO;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 上行创作：轮询目录摘要，去抖后以状态文件中的修订号 CAS 上传草稿，可选继续发布。修订冲突进入 CONFLICT，
 * 直到调用 resync 或删除状态文件。
 * / Upstream authoring: polls directory digests, uploads the draft after a debounce with the state-file revision as
 * CAS, and optionally publishes it. A revision conflict enters CONFLICT until resync is called or the state file is
 * deleted.
 */
@Slf4j
public class DdcAiSkillAuthoringWatcher {

    static final String STATE_FILE = ".tianshu-authoring-state.json";
    static final String UPLOAD = "tianshu.ai.authoring.upload";
    static final String REVISION_CONFLICT = "TIANSHU_AI_REVISION_CONFLICT";

    private final DdcAiSyncClient syncClient;
    private final DdcAiSkillTreeReader reader;
    private final DdcAiAuthoringProperties properties;
    private final ObjectMapper objectMapper;
    private final DdcAiStarterMetrics metrics;
    private final Clock clock;
    private final ScheduledExecutorService scheduler;
    private final Map<String, String> lastSeen = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastChange = new ConcurrentHashMap<>();
    private volatile ScheduledFuture<?> task;

    public DdcAiSkillAuthoringWatcher(DdcAiSyncClient syncClient, DdcAiSkillTreeReader reader,
            DdcAiAuthoringProperties properties, ObjectMapper objectMapper, DdcAiStarterMetrics metrics, Clock clock,
            ScheduledExecutorService scheduler) {
        this.syncClient = Objects.requireNonNull(syncClient, "syncClient");
        this.reader = Objects.requireNonNull(reader, "reader");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    public void start() {
        long interval = Math.max(1L, properties.getPollInterval().toMillis());
        task = scheduler.scheduleWithFixedDelay(this::pollOnce, interval, interval, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        ScheduledFuture<?> current = task;
        if (current != null) current.cancel(false);
    }

    /** 处理全部资源一轮。 / Processes every resource once. */
    public void pollOnce() {
        for (DdcAiWatchedResourceDTO resource : properties.getResources()) {
            try {
                poll(resource);
            } catch (IOException | RuntimeException failure) {
                log.warn("AI authoring poll failed for {}: {}", resource.watchKey(), failure.getMessage());
            }
        }
    }

    /** 清除冲突/状态，下次轮询以服务端最新修订为基线。 / Clears the state so the next poll rebases on the server. */
    public void resync(DdcAiWatchedResourceDTO resource) throws IOException {
        Files.deleteIfExists(directory(resource).resolve(STATE_FILE));
        lastSeen.remove(resource.watchKey());
        lastChange.remove(resource.watchKey());
        log.info("AI authoring state reset for {}", resource.watchKey());
    }

    /** 当前状态（无状态文件时为 IDLE）。 / Current state; IDLE without a state file. */
    public DdcAiAuthoringStateDTO state(DdcAiWatchedResourceDTO resource) throws IOException {
        Path file = directory(resource).resolve(STATE_FILE);
        try {
            return objectMapper.readValue(Files.readString(file, StandardCharsets.UTF_8),
                    DdcAiAuthoringStateDTO.class);
        } catch (NoSuchFileException missing) {
            return new DdcAiAuthoringStateDTO(resource.getNamespaceCode(), resource.getResourceKey(),
                    resource.getResourceId(), null, null, Status.IDLE, null, clock.instant());
        } catch (com.fasterxml.jackson.core.JsonProcessingException corrupt) {
            log.warn("Unreadable AI authoring state for {}; treating as IDLE", resource.watchKey());
            return new DdcAiAuthoringStateDTO(resource.getNamespaceCode(), resource.getResourceKey(),
                    resource.getResourceId(), null, null, Status.IDLE, null, clock.instant());
        }
    }

    private void poll(DdcAiWatchedResourceDTO resource) throws IOException {
        DdcAiAuthoringStateDTO state = state(resource);
        if (state.status() == Status.CONFLICT) return;
        DdcAiSkillTreeSnapshotDTO snapshot = reader.read(directory(resource));
        Instant now = clock.instant();
        if (snapshot.sha256().equals(state.lastUploadedSha256())) {
            if (resource.isAutoPublish() && state.status() == Status.UPLOADED) publish(resource, state, snapshot, now);
            return;
        }
        String key = resource.watchKey();
        if (!snapshot.sha256().equals(lastSeen.get(key))) {
            lastSeen.put(key, snapshot.sha256());
            lastChange.put(key, now);
            return;
        }
        if (Duration.between(lastChange.getOrDefault(key, now), now).compareTo(properties.getDebounce()) < 0) return;
        upload(resource, state, snapshot, now);
    }

    private void upload(DdcAiWatchedResourceDTO resource, DdcAiAuthoringStateDTO state,
            DdcAiSkillTreeSnapshotDTO snapshot, Instant now) throws IOException {
        try {
            Long base = state.baseRevision() != null ? state.baseRevision()
                    : syncClient.getVersionHead(query(resource)).getResourceRevision();
            DdcAiRevisionDTO revision = syncClient.replaceSkillDraft(DdcAiSkillDraftUploadDTO.builder()
                    .namespaceCode(resource.getNamespaceCode()).resourceKey(resource.getResourceKey())
                    .resourceId(resource.getResourceId()).expectedRevision(base)
                    .displayName(snapshot.displayName() == null ? resource.getResourceKey() : snapshot.displayName())
                    .description("").files(snapshot.nodes()).build());
            DdcAiAuthoringStateDTO uploaded = state.withUpload(revision.getResourceRevision(), snapshot.sha256(),
                    Status.UPLOADED, now);
            save(resource, uploaded);
            record(resource, "UPLOADED");
            log.info("AI_AUTHORING_UPLOADED resourceKey={} revision={}", resource.getResourceKey(),
                    revision.getResourceRevision());
            if (resource.isAutoPublish()) publish(resource, uploaded, snapshot, now);
        } catch (DdcException failure) {
            fail(resource, state, failure.getStatus(), now);
        } catch (RuntimeException failure) {
            fail(resource, state, failure.getClass().getSimpleName(), now);
        }
    }

    private void publish(DdcAiWatchedResourceDTO resource, DdcAiAuthoringStateDTO uploaded,
            DdcAiSkillTreeSnapshotDTO snapshot, Instant now) throws IOException {
        try {
            DdcAiPublicationReceiptDTO receipt = syncClient.publishSkillDraft(DdcAiSkillPublishDTO.builder()
                    .namespaceCode(resource.getNamespaceCode()).resourceKey(resource.getResourceKey())
                    .resourceId(resource.getResourceId()).expectedRevision(uploaded.baseRevision())
                    .requestId(requestId(resource, snapshot.sha256())).changeNote(resource.getChangeNote()).build());
            save(resource, uploaded.withUpload(receipt.getResourceRevision(), snapshot.sha256(), Status.PUBLISHED, now));
            record(resource, "PUBLISHED");
            log.info("AI_AUTHORING_PUBLISHED resourceKey={} versionNo={}", resource.getResourceKey(),
                    receipt.getTargetVersionNo());
        } catch (DdcException failure) {
            if (REVISION_CONFLICT.equals(failure.getStatus())) {
                fail(resource, uploaded, failure.getStatus(), now);
            } else {
                save(resource, uploaded.withStatus(Status.UPLOADED, failure.getStatus(), now));
                record(resource, "PUBLISH_ERROR");
            }
        } catch (RuntimeException failure) {
            save(resource, uploaded.withStatus(Status.UPLOADED, failure.getClass().getSimpleName(), now));
            record(resource, "PUBLISH_ERROR");
        }
    }

    private void fail(DdcAiWatchedResourceDTO resource, DdcAiAuthoringStateDTO state, String error, Instant now)
            throws IOException {
        if (REVISION_CONFLICT.equals(error)) {
            save(resource, state.withStatus(Status.CONFLICT, error, now));
            record(resource, "CONFLICT");
            log.error("AI authoring conflict for {}; pull the remote version, then call resync", resource.watchKey());
            return;
        }
        save(resource, state.withStatus(Status.ERROR, error, now));
        record(resource, "ERROR");
    }

    Path directory(DdcAiWatchedResourceDTO resource) {
        String relative = resource.getDirectory() == null || resource.getDirectory().isBlank()
                ? resource.getResourceKey() : resource.getDirectory();
        return DdcAiSkillMaterializer.safeResolve(properties.getRoot(), relative);
    }

    private void save(DdcAiWatchedResourceDTO resource, DdcAiAuthoringStateDTO state) throws IOException {
        Path file = directory(resource).resolve(STATE_FILE);
        Path temp = file.resolveSibling(STATE_FILE + ".tmp-" + UUID.randomUUID());
        try {
            Files.writeString(temp, objectMapper.writeValueAsString(state), StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private void record(DdcAiWatchedResourceDTO resource, String result) {
        metrics.increment(UPLOAD, 1, "result", result, "resourceKey", resource.getResourceKey());
    }

    private static DdcAiResourceQuery query(DdcAiWatchedResourceDTO resource) {
        return new DdcAiResourceQuery(resource.getNamespaceCode(), resource.getResourceKey(), null,
                resource.getResourceId());
    }

    /** 由内容摘要派生的稳定请求 ID，重试同一内容可幂等重放。 / Content-derived request ID for idempotent retries. */
    static String requestId(DdcAiWatchedResourceDTO resource, String sha256) {
        return "auth-" + sha256.substring(0, 32) + "-" + resource.getResourceId();
    }
}
