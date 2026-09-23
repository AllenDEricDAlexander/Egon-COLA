package top.egon.cola.component.yuheng.admin.wiki.domain.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiPublicationStatusEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiReviewStatusEnum;

/**
 * 中文说明：{@code WikiTransitionResultVO} 是 INTERNAL-001 状态迁移的内部调用结果，
 * 不是额外 HTTP wrapper；外部 {@code WikiPageVO} 由同事务的 authoritative 状态映射。
 * English summary: {@code WikiTransitionResultVO} is the internal result of the INTERNAL-001 transition call,
 * never an extra HTTP wrapper; the outward page view is mapped from the same transaction.
 *
 * 用法 / Usage: 重复已生效事件时 changed=false 且两个 version 不增长；
 * 事务最终 rollback 时调用者不得把本结果交付客户端。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class WikiTransitionResultVO {

    /** 页面稳定 ID / page id. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String pageId;

    /** 参与迁移的版本 ID / revision id the transition applied to. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String revisionId;

    /** 迁移后的发布状态 / publication status after the transition. */
    @NotNull
    private WikiPublicationStatusEnum publicationStatus;

    /** 迁移后的审核状态 / review status after the transition. */
    @NotNull
    private WikiReviewStatusEnum reviewStatus;

    /** 发布状态 CAS 版本，正 long / publication CAS version, positive. */
    @NotNull
    @Min(1)
    private Long publicationVersion;

    /** 页面乐观版本，正 long / page optimistic revision, positive. */
    @NotNull
    @Min(1)
    private Long pageRevision;

    /** 本次调用是否真实改变状态；重复已生效事件为 false / whether this call actually changed state. */
    @NotNull
    private Boolean changed;
}
