package top.egon.cola.component.yuheng.admin.wiki.dao;

import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.yuheng.admin.wiki.domain.po.WikiRevisionPO;

/**
 * 中文说明：{@code WikiRevisionDAO} 是 gateway_wiki_revision 的受守卫访问接口，除 starter 生成的通用语句外只声明
 * 三条原语：{@link #transitionRevision} 以「状态版本 + 起止状态」为条件整列覆盖生命周期列，
 * {@link #supersedePrevious} 让页面当前的已发布行让位并保留其发布与评审痕迹，
 * {@link #selectPublishedRevisions} 按当前发布态读取建图与内链闭合所需的修订。
 * 两条 CAS 都绝不触碰内容列与两个父级定位键——内容是插入时冻结的事实，只有状态会移动；
 * {@code publication_version} 只在状态迁移时推进，因此它同时是这条语句的期望值与写入值。
 * English summary: {@code WikiRevisionDAO} is the guarded access interface for gateway_wiki_revision, declaring beside the
 * generated generic statements three primitives only: {@link #transitionRevision} overwrite-replaces the lifecycle columns
 * under 「the status version plus the from-statuses」, {@link #supersedePrevious} moves the page's current published row aside
 * while keeping its publication and review trace, and {@link #selectPublishedRevisions} reads the rows currently published
 * for the graph and the inline-link closure. Neither compare-and-set touches a content column or either parent locator —
 * content is the fact frozen at insert time and only status moves — and {@code publication_version} advances on a status
 * transition only, which is why it serves as that statement's expected value and its written value at once.
 *
 * 用法 / Usage: 只由 {@code MpWikiRepository} 通过 {@code @Qualifier("wikiRevisionDAO")} 注入调用，语句实现见
 * {@code resources/mybatis/mapper/wiki/WikiRevisionDAO.xml}。
 * Injected only by {@code MpWikiRepository}; the statements live in {@code resources/mybatis/mapper/wiki/WikiRevisionDAO.xml}.
 */
public interface WikiRevisionDAO extends EgonColaMapper<WikiRevisionPO> {

    /**
     * 中文说明：执行 transitionRevision 操作，状态机唯一的状态写入语句。
     * English summary: Executes the transitionRevision operation, the state machine's single status-writing statement.
     *
     * 用法 / Usage: {@code wikiRevisionDAO.transitionRevision(et, expectedPublicationVersion, fromPublicationStatus,
     * fromReviewStatus, actorUserId, now)}；{@code 1} 才算命中。
     * @param et 参数 已加载且目标生命周期列就位的行模型；parameter loaded row carrying the target lifecycle columns.
     * @param expectedPublicationVersion 参数 期望的状态版本现值；parameter the expected current status version.
     * @param fromPublicationStatus 参数 期望的发布状态起点；parameter the expected publication from-state.
     * @param fromReviewStatus 参数 期望的评审状态起点；parameter the expected review from-state.
     * @param actorUserId 参数 守卫上下文用户；parameter the guard-context user.
     * @param now 参数 审计时刻；parameter the audit instant.
     * @return 返回 受影响行数；returns the affected row count.
     */
    int transitionRevision(@Param("et") WikiRevisionPO et,
                           @Param("expectedPublicationVersion") long expectedPublicationVersion,
                           @Param("fromPublicationStatus") String fromPublicationStatus,
                           @Param("fromReviewStatus") String fromReviewStatus,
                           @Param("actorUserId") String actorUserId,
                           @Param("now") Instant now);

    /**
     * 中文说明：执行 supersedePrevious 操作，把当前已发布行让位；部分唯一索引 {@code uq_wiki_revision_published}
     * 规定一页只有一个已发布行，所以新版进入 PUBLISHED 之前必须先把旧版置 SUPERSEDED。
     * English summary: Executes the supersedePrevious operation, moving the current published row aside; the partial unique index
     * {@code uq_wiki_revision_published} allows one published row per page, so the old row must become SUPERSEDED before the
     * new one may enter PUBLISHED.
     *
     * 用法 / Usage: {@code wikiRevisionDAO.supersedePrevious(et, newRevisionId, expectedPublicationVersion, actorUserId, now)}。
     * @param et 参数 已加载的当前已发布行模型；parameter the loaded current published row.
     * @param newRevisionId 参数 接替它的新修订主键，谓词保证同一行不会自己让自己让位；parameter the replacing key, kept out of self-supersession.
     * @param expectedPublicationVersion 参数 旧行的状态版本现值；parameter the old row's status version.
     * @param actorUserId 参数 守卫上下文用户；parameter the guard-context user.
     * @param now 参数 审计时刻；parameter the audit instant.
     * @return 返回 受影响行数；returns the affected row count.
     */
    int supersedePrevious(@Param("et") WikiRevisionPO et,
                          @Param("newRevisionId") long newRevisionId,
                          @Param("expectedPublicationVersion") long expectedPublicationVersion,
                          @Param("actorUserId") String actorUserId,
                          @Param("now") Instant now);

    /**
     * 中文说明：执行 selectPublishedRevisions 操作，读取本库当前处于 PUBLISHED 的修订，供图与内链闭合。
     * English summary: Executes the selectPublishedRevisions operation, reading this base's currently PUBLISHED revisions for the
     * graph and the inline-link closure.
     *
     * 用法 / Usage: {@code wikiRevisionDAO.selectPublishedRevisions(kbId, limit)}。
     * @param kbId 参数 知识库主键；parameter knowledge base key.
     * @param limit 参数 行上限；parameter row ceiling.
     * @return 返回 已发布行模型；returns the published rows.
     */
    List<WikiRevisionPO> selectPublishedRevisions(@Param("kbId") long kbId,
                                                  @Param("limit") int limit);
}
