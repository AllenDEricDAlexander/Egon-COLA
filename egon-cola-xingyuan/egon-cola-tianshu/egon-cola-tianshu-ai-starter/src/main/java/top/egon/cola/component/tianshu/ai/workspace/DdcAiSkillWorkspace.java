package top.egon.cola.component.tianshu.ai.workspace;

import java.nio.file.Path;
import java.util.Optional;

/**
 * 下游读取已落盘 Skill 的公共 API。 / Public API through which downstream code reads materialized Skills.
 */
public interface DdcAiSkillWorkspace {

    /** 当前版本目录（只读使用）。 / Current version directory (treat as read-only). */
    Optional<Path> currentDirectory(String namespaceCode, String resourceKey);

    /** 最近一次落盘的状态。 / State of the latest materialization. */
    Optional<DdcAiSkillWorkspaceStateDTO> state(String namespaceCode, String resourceKey);
}
