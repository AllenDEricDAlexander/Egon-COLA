package top.egon.cola.component.yuheng.admin.wiki.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 中文说明：{@code WikiPageQueryDTO} 是 API-023 目录分页的查询载体，只有原合同声明的四个可选参数：
 * 标题或 slug 的子串 {@code search}（至多 128 字符）、标签精确匹配 {@code tag}（一到三十二字符）、
 * 是否连带草稿的 {@code includeDraft}（只有 EDITOR 及以上可用，角色判定在业务侧完成，本载体不表达权限），
 * 外加 {@code page}/{@code size} 两个分页列。Wiki 的 {@code size} 上界是 20，比知识列表的 100 更严，
 * 因为每行还要带正文摘要；这里没有租户、没有排序字段，也没有任意 SQL 片段。
 * English summary: {@code WikiPageQueryDTO} is the API-023 catalog query carrier with only the four optional parameters the
 * contract declares: the {@code search} substring over title or slug (at most 128 characters), the exact {@code tag} match
 * (one to thirty-two characters), {@code includeDraft} (EDITOR or above, decided in the business layer since this carrier
 * expresses no permission) and the two paging columns. Wiki caps {@code size} at 20 — tighter than the knowledge list's 100 —
 * because each row also carries a body summary; there is no tenant, no ordering field and no SQL fragment here.
 *
 * 用法 / Usage: 由 {@code WikiController#listWikiPages} 补齐 {@code page=1, size=20, includeDraft=false} 默认值后装配，
 * 经 {@code WikiService#listPages} 交给 {@code WikiRepository#listPages} 与 {@code countPages}（两者共用同一谓词）；
 * {@code crossChecks()} 承担「草稿视图必须与角色相配」之外的通用值域复核。
 * Assembled by the controller with its defaults and handed to the paging and counting port methods that share one predicate;
 * {@code crossChecks()} carries the remaining value-domain review.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class WikiPageQueryDTO {

    @Min(1)
    private int page;

    /**
     * 中文说明：页大小上限是 20 而非 §9.2 通用规则的 100——API-023 与目录项共享完整的 {@code WikiPageVO}
     * （含 markdown，正文合同上限 131072 字节），Spec §9.2.23 因此把本接口的页大小另行收紧以限制单次响应正文总量。
     * English summary: The ceiling is 20 rather than §9.2's generic 100 because API-023 shares the full {@code WikiPageVO}
     * (markdown included, capped at 131072 bytes) with its catalog rows, so Spec §9.2.23 tightens this endpoint's page size to
     * bound how much body text one answer may carry.
     */
    @Min(1)
    @Max(20)
    private int size;

    @Schema(nullable = true)
    @Size(max = 128)
    private String search;

    @Schema(nullable = true)
    @Size(min = 1, max = 32)
    private String tag;

    private boolean includeDraft;

    /**
     * 中文说明：值域交叉复核：分页缺省必须补齐（{@code page} 与 {@code size} 是原始 {@code int}，0 只能来自未补齐的调用方），
     * 空白检索词按未提供处理而不是「匹配空串」，避免把 {@code search=} 当成全表过滤条件。
     * English summary: Cross-checks the value domain: the primitive paging columns must have been defaulted (zero only comes
     * from a caller that skipped defaults) and a blank search term counts as absent rather than matching everything, so
     * {@code search=} never becomes a filter over the whole table.
     * @return 返回 值域是否成立；whether the value domain holds.
     */
    @AssertTrue(message = "wiki paging must be defaulted and a blank search term must be dropped")
    @Schema(hidden = true)
    @JsonIgnore
    public boolean isCrossChecked() {
        return page >= 1 && size >= 1 && (search == null || !search.isBlank());
    }
}
