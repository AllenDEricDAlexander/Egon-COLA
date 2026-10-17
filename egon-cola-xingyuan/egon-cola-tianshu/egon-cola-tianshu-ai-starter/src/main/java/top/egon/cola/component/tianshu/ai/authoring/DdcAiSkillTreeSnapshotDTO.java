package top.egon.cola.component.tianshu.ai.authoring;

import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;

import java.util.List;
import java.util.Objects;

/**
 * 创作目录的不可变快照：完整节点树、稳定摘要与派生的显示名。
 * / Immutable snapshot of an authoring directory: the full node tree, a stable digest and the derived display name.
 */
public record DdcAiSkillTreeSnapshotDTO(List<DdcAiNodeDTO> nodes, String sha256, String displayName) {

    public DdcAiSkillTreeSnapshotDTO {
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
        Objects.requireNonNull(sha256, "sha256");
    }
}
