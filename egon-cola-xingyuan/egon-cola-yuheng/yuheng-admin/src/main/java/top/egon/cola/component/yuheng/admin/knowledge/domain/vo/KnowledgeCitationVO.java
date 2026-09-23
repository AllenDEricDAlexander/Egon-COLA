package top.egon.cola.component.yuheng.admin.knowledge.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 中文说明：{@code KnowledgeCitationVO} 是原 API-022 引用条目的投影，只携带授权节选与冻结来源标识；
 * chunk 的内部 metadata、向量、tenant 与删除时间不在此暴露。
 * English summary: {@code KnowledgeCitationVO} exposes only the authorized excerpt and frozen source identity;
 * chunk metadata, vectors, tenant and deletion columns never reach it.
 *
 * 用法 / Usage: pageId 仅在 WIKI 来源下非 null，资料来源保持 null；
 * excerpt 最多 1000 字符；score 是检索排序分数，不是真实性置信概率。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgeCitationVO {

    /** 冻结资料版本 ID / frozen document revision id. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String documentRevisionId;

    /** 稳定分块 ID，不使用查询列表下标 / stable chunk id, never a result-list index. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String chunkId;

    /** 冻结来源 hash，用于 stale 检查 / frozen source hash used for staleness checks. */
    @NotBlank
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String sourceHash;

    /** 资料稳定 ID / document id. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String documentId;

    /** 原始文件显示名，1–255，不作为路径 / original display file name, never a path. */
    @NotBlank
    @Size(max = 255)
    private String fileName;

    /** Wiki 页面稳定 ID，资料来源为 null / wiki page id, null for document-only citations. */
    @Schema(nullable = true)
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String pageId;

    /** 授权证据节选，最多 1000 字符 / authorized excerpt, at most 1000 characters. */
    @NotBlank
    @Size(max = 1_000)
    private String excerpt;

    /** 检索排序分数，非真实性置信概率 / retrieval ranking score, not a truth probability. */
    @NotNull
    private Double score;
}
