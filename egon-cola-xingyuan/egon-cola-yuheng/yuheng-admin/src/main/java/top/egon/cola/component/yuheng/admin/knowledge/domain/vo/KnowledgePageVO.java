package top.egon.cola.component.yuheng.admin.knowledge.domain.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 中文说明：{@code KnowledgePageVO} 是原 §9 分页读接口的泛型页载体，直接输出
 * {@code items/page/size/total}，不额外增加 code/data wrapper。
 * English summary: {@code KnowledgePageVO} is the generic page carrier of the §9 paged read endpoints and adds no
 * code/data wrapper around the items.
 *
 * 用法 / Usage: items 空结果为 {@code []} 而非 null；page 从 1 开始，size 有效范围 1–100，
 * total 是本次查询匹配总数而非跨页快照；页项目类型由调用方（模型/KB/文档/任务/Wiki 列表）指定。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgePageVO<T> {

    /** 当前页项目，空结果为空数组 / items of the current page, empty array when none. */
    @NotNull
    private List<T> items;

    /** 页码，从 1 开始 / one-based page number. */
    @Min(1)
    private int page;

    /** 有效页大小 1–100 / effective page size, 1–100. */
    @Min(1)
    @Max(100)
    private int size;

    /** 本次查询匹配总数，非跨页快照 / matched total of this query, not a cross-page snapshot. */
    @Min(0)
    private long total;

    /**
     * 中文说明：返回 items 的不可变快照副本；未设置时投影为 {@code []} 而非 null。
     * English summary: Returns an unmodifiable copy of the items, or an empty list so the wire value is never null.
     */
    public List<T> getItems() {
        return items == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(items));
    }

    /**
     * 中文说明：写入时复制 items，避免异步序列化期间被外部修改。
     * English summary: Defensively copies the incoming items.
     */
    public KnowledgePageVO<T> setItems(List<T> items) {
        this.items = items == null ? null : new ArrayList<>(items);
        return this;
    }
}
