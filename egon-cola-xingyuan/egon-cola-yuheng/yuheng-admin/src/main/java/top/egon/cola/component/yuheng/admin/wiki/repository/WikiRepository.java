package top.egon.cola.component.yuheng.admin.wiki.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Optional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.wiki.domain.bo.WikiPageBO;
import top.egon.cola.component.yuheng.admin.wiki.domain.bo.WikiRevisionBO;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiPageQueryDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiPublicationStatusEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiReviewStatusEnum;

/**
 * 中文说明：{@code WikiRepository} 是 Wiki 两面（页面与修订）唯一的类型化访问端口：读侧按十进制字符串 id 取活跃行并
 * 复核跨库归属，写侧只接受整行业务载体，绝不暴露 MyBatis-Plus 行模型、{@code LambdaQueryWrapper} 或通用 CRUD 方法。
 * 两类 CAS 是本端口的核心：{@link #movePagePointers} 以「页面业务 revision + 两个指针现值」为条件切换指针，
 * {@link #transitionRevision} 以「revision 行的状态版本 + 起止状态」为条件迁移状态，两者都只有一行命中才算成功，
 * 零行返回 {@code false} 由业务侧换算成 409；{@link #supersedePrevious} 把旧已发布行置 SUPERSEDED 而保留其发布痕迹。
 * 目录分页 {@link #listPages} 与 {@link #countPages} 必须共用同一谓词（含 slug 与标题的检索、标签精确匹配、草稿可见性），
 * 否则当页与总数会互相矛盾。
 * English summary: {@code WikiRepository} is the only typed access port for both Wiki sides: reads fetch active rows by decimal
 * id and re-check base ownership, writes accept whole business carriers only, and no MyBatis-Plus row model, wrapper or generic
 * CRUD method is ever exposed. Two compare-and-sets are its core: {@link #movePagePointers} swaps pointers under
 * 「the page business revision plus both current pointers」 and {@link #transitionRevision} migrates status under
 * 「the revision row's status version plus its from-statuses」; exactly one affected row is success and zero rows answer
 * {@code false} so the business layer can raise a 409, while {@link #supersedePrevious} marks the previously published row
 * SUPERSEDED without disturbing its publication trace. {@link #listPages} and {@link #countPages} must share one predicate
 * (slug-or-title search, exact tag, draft visibility) or the page and the total would contradict each other.
 *
 * 用法 / Usage: 只由 {@code WikiLifecycleServiceImpl} 与 {@code MpWikiRepository} 之间的业务侧调用；
 * 实现把租户、技术乐观锁与软删谓词留在受守卫边界，审计写入由业务侧在同一事务内完成。
 * Reached only by the business side; the implementation keeps tenant, technical lock and soft-delete predicates behind the
 * guarded boundary while the audit row is written by the caller in the same transaction.
 */
@Validated
public interface WikiRepository {

    /**
     * 中文说明：按主键读取活跃页面，读不到（含跨租户与软删）返回空。
     * English summary: Reads an active page by key, empty when unreadable (foreign tenant or soft-deleted included).
     * @param pageId 参数 页面十进制字符串 id；parameter decimal-string page id.
     * @return 返回 页面载体或空；returns the page carrier or empty.
     */
    Optional<WikiPageBO> findPage(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String pageId);

    /**
     * 中文说明：按库内唯一 slug 读取活跃页面，是生成作业判断「新建还是替换草稿」的入口。
     * English summary: Reads the active page behind a base-unique slug, how a generation job decides create-versus-replace.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param slug 参数 库内唯一短链名；parameter the base-unique slug.
     * @return 返回 页面载体或空；returns the page carrier or empty.
     */
    Optional<WikiPageBO> findPageBySlug(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @NotBlank
            @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$") String slug);

    /**
     * 中文说明：按原合同顺序读取页面目录当页（{@code createdAt DESC, id DESC}），检索、标签与草稿可见性谓词
     * 与 {@link #countPages} 完全一致。
     * English summary: Reads one catalog page in the contract order ({@code createdAt DESC, id DESC}) under exactly the
     * predicate {@link #countPages} uses.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param query 参数 已补齐默认值与值域复核的查询载体；parameter the defaulted, value-checked query carrier.
     * @return 返回 当页页面载体；returns the page carriers of that page.
     */
    List<WikiPageBO> listPages(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @Valid @NotNull WikiPageQueryDTO query);

    /**
     * 中文说明：与当页同谓词地统计页面总数。
     * English summary: Counts the pages under the same predicate as the page itself.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param query 参数 查询载体；parameter the query carrier.
     * @return 返回 总数；returns the total.
     */
    long countPages(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @Valid @NotNull WikiPageQueryDTO query);

    /**
     * 中文说明：插入一个两个指针皆空的页面行，主键与业务 revision 由持久边界回填进返回载体；
     * slug 库内唯一由部分唯一索引兜底，冲突按重复键向上抛出。
     * English summary: Inserts a page row with both pointers empty, the key and business revision written back into the
     * returned carrier; the base-unique slug is enforced by the partial unique index and a clash surfaces as a duplicate key.
     * @param page 参数 待插入页面载体；parameter the page carrier to insert.
     * @return 返回 带权威主键与 revision 的同一载体；returns that carrier with its authoritative key and revision.
     */
    WikiPageBO insertPage(@Valid @NotNull WikiPageBO page);

    /**
     * 中文说明：以「页面业务 revision 与两个指针现值」为条件整行切换指针，零行返回 {@code false}；
     * 期望值参与谓词，因此并发切换不会把另一方的指针覆盖掉。
     * English summary: Swaps the pointers under 「the page's business revision plus both current pointer values」, zero rows
     * answering {@code false}; the expected values sit in the predicate so a concurrent swap is never overwritten.
     * @param page 参数 目标指针已就位的页面载体，其 {@code revision} 已是切换后的值；parameter carrier holding the target pointers with the post-swap revision.
     * @param expectedPageRevision 参数 调用方观察到的页面 revision；parameter the caller-observed page revision.
     * @param expectedPublishedRevisionId 参数 期望的已发布指针现值，可为空；parameter the expected published pointer, nullable.
     * @param expectedDraftRevisionId 参数 期望的草稿指针现值，可为空；parameter the expected draft pointer, nullable.
     * @return 返回 CAS 是否命中；whether the compare-and-set hit.
     */
    boolean movePagePointers(
            @Valid @NotNull WikiPageBO page,
            @Min(1) long expectedPageRevision,
            String expectedPublishedRevisionId,
            String expectedDraftRevisionId);

    /**
     * 中文说明：按主键读取活跃修订并复核所属知识库，跨库读不到按空返回，避免用存在性泄漏。
     * English summary: Reads an active revision by key and re-checks its base, a cross-base read being empty so existence never leaks.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param revisionId 参数 修订十进制字符串 id；parameter decimal-string revision id.
     * @return 返回 修订载体或空；returns the revision carrier or empty.
     */
    Optional<WikiRevisionBO> findRevision(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String revisionId);

    /**
     * 中文说明：批量读取本库的活跃修订，供目录、图与来源复核一次取齐，缺失的 id 如实少返回而不补位。
     * English summary: Batch-reads this base's active revisions for the catalog, the graph and the source re-check, a missing id
     * simply shortening the result.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param revisionIds 参数 待读修订 id 列表；parameter the revision ids to read.
     * @return 返回 读到的修订载体；returns the revision carriers found.
     */
    List<WikiRevisionBO> listRevisions(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @NotEmpty List<@Pattern(regexp = "^[1-9][0-9]{0,19}$") String> revisionIds);

    /**
     * 中文说明：插入一个 DRAFT 修订行；内容列在此落定，之后只能由状态 CAS 改写状态列。
     * English summary: Inserts a DRAFT revision row; the content columns settle here and only the status columns move afterwards.
     * @param revision 参数 待插入修订载体；parameter the revision carrier to insert.
     * @return 返回 带权威主键与 revision 的同一载体；returns that carrier with its authoritative key and revision.
     */
    WikiRevisionBO insertRevision(@Valid @NotNull WikiRevisionBO revision);

    /**
     * 中文说明：以「状态版本 + 起止状态」为条件迁移修订状态并覆盖状态列，零行返回 {@code false}；
     * 内容列与 {@code pageId}/{@code kbId} 由实现的受守卫谓词固定，不参与写入。
     * English summary: Migrates the revision status and rewrites the status columns under 「the status version plus the from-statuses」,
     * zero rows answering {@code false}; the content columns and the two pointers stay out of the write and are pinned by the
     * guarded predicate instead.
     * @param revision 参数 目标状态已就位的修订载体，其 {@code publicationVersion} 已是迁移后的值；parameter carrier holding the target status with the post-migration version.
     * @param expectedPublicationVersion 参数 调用方观察到的状态版本；parameter the caller-observed status version.
     * @param fromPublicationStatus 参数 期望的发布状态起点；parameter the expected publication from-state.
     * @param fromReviewStatus 参数 期望的评审状态起点；parameter the expected review from-state.
     * @return 返回 CAS 是否命中；whether the compare-and-set hit.
     */
    boolean transitionRevision(
            @Valid @NotNull WikiRevisionBO revision,
            @Min(1) long expectedPublicationVersion,
            @NotNull WikiPublicationStatusEnum fromPublicationStatus,
            @NotNull WikiReviewStatusEnum fromReviewStatus);

    /**
     * 中文说明：把页面当前的已发布修订置 SUPERSEDED（保留其发布与评审痕迹），同一 CAS 复核它仍是 PUBLISHED
     * 且状态版本未被并发改动；旧行绝不复活，也不会被改写成草稿。
     * English summary: Marks the page's current published revision SUPERSEDED while keeping its publication and review trace, the
     * same compare-and-set re-checking it is still PUBLISHED at the expected status version; the old row never revives and is
     * never rewritten into a draft.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param publishedRevisionId 参数 待旧化的已发布修订 id；parameter the published revision to supersede.
     * @param newRevisionId 参数 接替它的新修订 id，仅入审计与谓词校验；parameter the replacing revision id.
     * @param expectedPublicationVersion 参数 旧行的状态版本现值；parameter the old row's status version.
     * @return 返回 CAS 是否命中；whether the compare-and-set hit.
     */
    boolean supersedePrevious(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String publishedRevisionId,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String newRevisionId,
            @Min(1) long expectedPublicationVersion);

    /**
     * 中文说明：读取本库当前的全部已发布修订，用于图的节点与内链闭合；上限由调用方按合同裁剪。
     * English summary: Reads this base's currently published revisions for the graph's nodes and link closure, the caller bounding
     * how many come back.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param limit 参数 返回上限，正整数；parameter the positive ceiling.
     * @return 返回 已发布修订载体；returns the published revision carriers.
     */
    List<WikiRevisionBO> listPublishedRevisions(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @Min(1) int limit);
}
