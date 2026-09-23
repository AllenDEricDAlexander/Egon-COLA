package top.egon.cola.component.yuheng.admin.wiki.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.yuheng.admin.shared.domain.validation.ExecuteGroup;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiTransitionEventEnum;
import top.egon.cola.component.yuheng.admin.wiki.validation.ValidWikiTransition;

/**
 * 中文说明：{@code WikiTransitionCommandDTO} 是 INTERNAL-001 Wiki 状态迁移的 CQE Command 载体，
 * 字段合同为 pageId/revisionId/event/expectedPageRevision/expectedPublicationVersion/reasonCode。
 * English summary: {@code WikiTransitionCommandDTO} is the CQE command carrier of INTERNAL-001 wiki state
 * transitions with the exact page/revision/event/two version fields and a safe reason code.
 *
 * 用法 / Usage: 整组约束按 §10.3.1 归入 ExecuteGroup，由内部入口以 {@code @Validated(ExecuteGroup.class)} 触发；
 * 状态值、审核人与策略快照一律服务端派生，本载体不提供任何 caller 覆写审核状态或 reviewer 的字段。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@ValidWikiTransition(groups = ExecuteGroup.class)
public class WikiTransitionCommandDTO {

    /** 页面稳定 ID，正十进制 1–20 / page id, positive decimal of 1–20 digits. */
    @NotBlank(groups = ExecuteGroup.class)
    @Pattern(regexp = "^[1-9][0-9]{0,19}$", groups = ExecuteGroup.class)
    private String pageId;

    /** 参与迁移的版本 ID，正十进制 1–20 / revision id taking part in the transition. */
    @NotBlank(groups = ExecuteGroup.class)
    @Pattern(regexp = "^[1-9][0-9]{0,19}$", groups = ExecuteGroup.class)
    private String revisionId;

    /** §7.3.7 迁移事件枚举，必填 / declared transition event, required. */
    @NotNull(groups = ExecuteGroup.class)
    private WikiTransitionEventEnum event;

    /** 页面乐观版本，≥1 / page optimistic revision, at least 1. */
    @NotNull(groups = ExecuteGroup.class)
    @Min(value = 1, groups = ExecuteGroup.class)
    private Long expectedPageRevision;

    /** 发布状态 CAS 版本，≥1 / publication CAS version, at least 1. */
    @NotNull(groups = ExecuteGroup.class)
    @Min(value = 1, groups = ExecuteGroup.class)
    private Long expectedPublicationVersion;

    /** 1–64 大写安全原因码 / 1–64 uppercase safe reason code. */
    @NotBlank(groups = ExecuteGroup.class)
    @Size(max = 64, groups = ExecuteGroup.class)
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{0,63}$", groups = ExecuteGroup.class)
    private String reasonCode;
}
