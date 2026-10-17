package top.egon.cola.component.tianshu.ai.authoring;

import java.time.Instant;
import java.util.Objects;

/**
 * 上行创作状态文件（.tianshu-authoring-state.json）的不可变值对象。
 * / Immutable value object of the authoring state file (.tianshu-authoring-state.json).
 */
public record DdcAiAuthoringStateDTO(String namespaceCode, String resourceKey, Long resourceId, Long baseRevision,
        String lastUploadedSha256, Status status, String lastError, Instant updatedAt) {

    public DdcAiAuthoringStateDTO {
        Objects.requireNonNull(namespaceCode, "namespaceCode");
        Objects.requireNonNull(resourceKey, "resourceKey");
        Objects.requireNonNull(resourceId, "resourceId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** 状态机：CONFLICT 需人工 resync，ERROR 下次轮询重试。 / CONFLICT needs a manual resync; ERROR retries. */
    public enum Status { IDLE, UPLOADED, PUBLISHED, CONFLICT, ERROR }

    public DdcAiAuthoringStateDTO withStatus(Status next, String error, Instant at) {
        return new DdcAiAuthoringStateDTO(namespaceCode, resourceKey, resourceId, baseRevision, lastUploadedSha256,
                next, error, at);
    }

    public DdcAiAuthoringStateDTO withUpload(Long revision, String sha, Status next, Instant at) {
        return new DdcAiAuthoringStateDTO(namespaceCode, resourceKey, resourceId, revision, sha, next, null, at);
    }
}
