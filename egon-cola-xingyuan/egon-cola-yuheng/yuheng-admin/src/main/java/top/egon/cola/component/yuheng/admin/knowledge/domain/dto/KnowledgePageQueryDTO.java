package top.egon.cola.component.yuheng.admin.knowledge.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
 * 中文说明：{@code KnowledgePageQueryDTO} 是知识库、资料、作业三类分页读接口（API-008/014/020）共用的
 * 查询条件载体，只承载 {@code page/size/status/search}；它不承载租户、KB ID、actorId 与排序键，
 * 这些分别来自守卫上下文、路径与固定 SQL 顺序，因此调用方无法注入任意排序或跨租户条件。
 * English summary: {@code KnowledgePageQueryDTO} is the shared query carrier for the paged reads of bases, documents
 * and jobs (API-008/014/020) holding only {@code page/size/status/search}; tenant, kb id, actor and sort keys are
 * absent because they come from the guarded context, the path and the fixed SQL order, so a caller can never inject
 * an arbitrary sort or a cross-tenant predicate.
 *
 * 用法 / Usage: 由 Controller 以 {@code @Valid @ModelAttribute} 绑定，字段是基本类型 int，因此缺失的
 * query 参数必须先由绑定层落到 {@code page=1,size=20} 的默认值，再交由注解守住范围（size 上限 100 与
 * {@code KnowledgePageVO} 一致）；{@code status} 只在作业与资料列表上有意义，取值是两份持久枚举词汇的并集
 * （作业 QUEUED/RUNNING/RETRY_WAIT/SUCCEEDED/FAILED/STALE/CANCELLED 与修订 STAGING/READY/FAILED），
 * 不适用时省略而不是传空串；{@code search} 是名称或文件名的有界子串匹配，绝不进入 SQL 片段。
 * Bound with {@code @Valid @ModelAttribute}; because page and size are primitive ints the binder must default them to
 * {@code page=1,size=20} before these bounds (the size ceiling 100 matches {@code KnowledgePageVO}) apply, status is
 * meaningful only for the job and document lists and takes the union of the two persisted vocabularies while being
 * omitted rather than emptied elsewhere, and search stays a bounded substring match that never becomes SQL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgePageQueryDTO {

    /** 页码，从 1 开始 / one-based page number. */
    @Min(1)
    private int page;

    /** 页大小，1–100，与 {@code KnowledgePageVO} 同界 / page size, 1–100, the same bound as {@code KnowledgePageVO}. */
    @Min(1)
    @Max(100)
    private int size;

    /** 状态过滤，作业与修订两份词汇的并集，不适用时省略 / optional status filter over the union of the job and revision vocabularies, omitted when not applicable. */
    @Schema(nullable = true)
    @Pattern(regexp = "^(QUEUED|RUNNING|RETRY_WAIT|SUCCEEDED|FAILED|STALE|CANCELLED|STAGING|READY)$")
    private String status;

    /** 名称/文件名子串查询，0–128 字符，省略即不过滤 / optional name or file-name substring, 0–128 characters, omitted means no filter. */
    @Schema(nullable = true)
    @Size(max = 128)
    private String search;
}
