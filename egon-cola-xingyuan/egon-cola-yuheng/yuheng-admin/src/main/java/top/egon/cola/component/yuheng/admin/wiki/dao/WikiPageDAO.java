package top.egon.cola.component.yuheng.admin.wiki.dao;

import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.yuheng.admin.wiki.domain.po.WikiPagePO;

/**
 * 中文说明：{@code WikiPageDAO} 是 gateway_wiki_page 的受守卫访问接口：除 starter 生成的通用语句外，只声明
 * 两条无法用条件构造器表达的原语——带双指针期望值的 {@link #movePointers} 单语句 CAS，以及必须联结
 * 可见修订才能完成标题/标签检索的目录分页 {@link #selectCatalog} 与 {@link #countCatalog}。
 * CAS 的写入值全部取自 {@code et}（业务 revision 已是切换后的目标值），期望值另参数化进入谓词，
 * 因此并发切换不可能覆盖对方的指针；审计列的操作者取自持久边界的用户上下文，而不是行上残留的旧值。
 * English summary: {@code WikiPageDAO} is the guarded access interface for gateway_wiki_page, declaring beside the generated
 * generic statements the two primitives a condition wrapper cannot express: the {@link #movePointers} single-statement CAS
 * carrying both expected pointer values, and the catalog page {@link #selectCatalog} with its matching
 * {@link #countCatalog} that must join the visible revision to search titles and tags. Every written value comes from
 * {@code et} (its business revision already holding the post-swap target) while the expectations enter the predicate as
 * separate parameters, so a concurrent swap can never overwrite the other side's pointer; the audit operator comes from the
 * persistence boundary's user context rather than the stale value still stored on the row.
 *
 * 用法 / Usage: 只由 {@code MpWikiRepository} 通过 {@code @Qualifier("wikiPageDAO")} 注入调用，语句实现见
 * {@code resources/mybatis/mapper/wiki/WikiPageDAO.xml}。
 * Injected only by {@code MpWikiRepository}; the statements live in {@code resources/mybatis/mapper/wiki/WikiPageDAO.xml}.
 */
public interface WikiPageDAO extends EgonColaMapper<WikiPagePO> {

    /**
     * 中文说明：执行 movePointers 操作，以「页面业务 revision + 两个指针现值」为条件的单语句指针切换。
     * English summary: Executes the movePointers operation, the single-statement pointer swap conditioned on the page's
     * business revision plus both current pointer values.
     *
     * 用法 / Usage: {@code wikiPageDAO.movePointers(et, expectedRevision, expectedPublished, expectedDraft, actorUserId, now)}；
     * 返回受影响行数，{@code 1} 才算命中。
     * @param et 参数 已加载且目标指针与目标 revision 就位的行模型；parameter loaded row carrying the target pointers and revision.
     * @param expectedRevision 参数 期望的页面业务 revision 现值；parameter the expected current business revision.
     * @param expectedPublishedRevisionId 参数 期望的已发布指针现值，可为空；parameter the expected published pointer, nullable.
     * @param expectedDraftRevisionId 参数 期望的草稿指针现值，可为空；parameter the expected draft pointer, nullable.
     * @param actorUserId 参数 守卫上下文用户；parameter the guard-context user.
     * @param now 参数 审计时刻；parameter the audit instant.
     * @return 返回 受影响行数；returns the affected row count.
     */
    int movePointers(@Param("et") WikiPagePO et,
                     @Param("expectedRevision") long expectedRevision,
                     @Param("expectedPublishedRevisionId") Long expectedPublishedRevisionId,
                     @Param("expectedDraftRevisionId") Long expectedDraftRevisionId,
                     @Param("actorUserId") String actorUserId,
                     @Param("now") Instant now);

    /**
     * 中文说明：执行 selectCatalog 操作，读取目录当页，次序 {@code create_time DESC, id DESC} 与原合同一致。
     * English summary: Executes the selectCatalog operation, reading one catalog page in the contractual
     * {@code create_time DESC, id DESC} order.
     *
     * 用法 / Usage: {@code wikiPageDAO.selectCatalog(kbId, includeDraft, searchPattern, tag, offset, limit)}；
     * 与 {@link #countCatalog} 共用同一 {@code catalogScope} 片段。
     * @param kbId 参数 知识库主键；parameter knowledge base key.
     * @param includeDraft 参数 草稿是否可见（调用方已复核 EDITOR 角色）；parameter whether drafts are visible, EDITOR already proven.
     * @param searchPattern 参数 已转义的检索模式，可为空；parameter escaped search pattern, nullable.
     * @param tag 参数 精确标签，可为空；parameter exact tag, nullable.
     * @param offset 参数 行偏移；parameter row offset.
     * @param limit 参数 行上限；parameter row ceiling.
     * @return 返回 当页行模型；returns the rows of that page.
     */
    List<WikiPagePO> selectCatalog(@Param("kbId") long kbId,
                                   @Param("includeDraft") boolean includeDraft,
                                   @Param("searchPattern") String searchPattern,
                                   @Param("tag") String tag,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    /**
     * 中文说明：执行 countCatalog 操作，与 {@link #selectCatalog} 逐字同谓词地统计总数。
     * English summary: Executes the countCatalog operation, counting under a predicate worded identically to
     * {@link #selectCatalog}'s.
     *
     * 用法 / Usage: {@code wikiPageDAO.countCatalog(kbId, includeDraft, searchPattern, tag)}。
     * @param kbId 参数 知识库主键；parameter knowledge base key.
     * @param includeDraft 参数 草稿是否可见；parameter whether drafts are visible.
     * @param searchPattern 参数 已转义的检索模式，可为空；parameter escaped search pattern, nullable.
     * @param tag 参数 精确标签，可为空；parameter exact tag, nullable.
     * @return 返回 总数；returns the total.
     */
    long countCatalog(@Param("kbId") long kbId,
                      @Param("includeDraft") boolean includeDraft,
                      @Param("searchPattern") String searchPattern,
                      @Param("tag") String tag);
}
