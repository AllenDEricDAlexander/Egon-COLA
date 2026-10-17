package top.egon.cola.component.tianshu.ai.workspace;

import top.egon.cola.component.tianshu.model.ai.DdcAiVersionHeadDTO;

import java.time.Instant;
import java.util.Objects;

/**
 * 工作区 state.json 不可变值对象：最近落盘的版本与摘要。
 * / Immutable value object of the workspace state.json: the last materialized version and digest.
 */
public record DdcAiSkillWorkspaceStateDTO(String namespaceCode, String resourceKey, Long resourceId, Long versionNo,
        String contentSha256, Instant updatedAt) {

    public DdcAiSkillWorkspaceStateDTO {
        Objects.requireNonNull(namespaceCode, "namespaceCode");
        Objects.requireNonNull(resourceKey, "resourceKey");
        Objects.requireNonNull(resourceId, "resourceId");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** 本地是否已是该发布头。 / Whether the local copy already equals the published head. */
    public boolean matches(DdcAiVersionHeadDTO head) {
        return head != null && head.getVersionNo() != null && head.getVersionNo().equals(versionNo)
                && Objects.equals(head.getContentSha256(), contentSha256);
    }
}
