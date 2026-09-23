package top.egon.cola.component.yuheng.admin.wiki.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 中文说明：{@code WikiSourceDTO} 是 Wiki 证据列表的一层顶层载体，与 revision sources JSONB 元素一一映射，
 * 对应原 API-023–027 的 {@code documentRevisionId/chunkId/sourceHash} 三元组。
 * English summary: {@code WikiSourceDTO} is the top-level wiki source carrier mapped one-to-one to a revision
 * sources JSONB element instead of an untyped Map.
 *
 * 用法 / Usage: 由 {@code WikiDraftCommandDTO} 与 {@code WikiPageVO} 以 {@code @Valid} 逐个校验；
 * 来源必须属于任务冻结且当前可读的版本集合，发布时由 Service 复核 hash 是否仍有效。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class WikiSourceDTO {

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
}
