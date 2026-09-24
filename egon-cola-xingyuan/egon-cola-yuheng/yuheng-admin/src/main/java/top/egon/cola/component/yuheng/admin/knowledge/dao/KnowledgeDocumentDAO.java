package top.egon.cola.component.yuheng.admin.knowledge.dao;

import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.yuheng.admin.knowledge.domain.po.KnowledgeDocumentPO;

import java.time.Instant;

/**
 * 知识文档表的 MyBatis-Plus Mapper，复用 EgonColaMapper 的租户内活跃读取与版本化软删除。
 * MyBatis-Plus mapper for knowledge documents; inherits tenant-scoped active reads and versioned soft-delete.
 * 用法 / Usage: 仅声明 Spec §11.2 访问路径所需的具名类型化查询，通用 CRUD 一律经对应持久化仓储调用。
 * 除继承的通用方法外，本接口只承载 Step 12 的原子发布语句 {@code activateRevision}：活动版本切换必须是
 * 「带 expectedRevision 的单语句 CAS」，两步读改写在并发发布者之间会留下半切换状态，故只能落在具名 SQL 上。
 * 写入实体参数一律命名 {@code et}，否则 {@code EgonColaOriginalSqlGuardInterceptor} 的 {@code WRITE_ID_MISMATCH}
 * 与 MP 的租户/审计盖章都会失配。/ Beyond the inherited generic methods this interface carries only Step 12's
 * {@code activateRevision}: switching the active revision has to be a single-statement CAS on the expected revision, since a
 * two-step read-modify-write would leave a half-switched document between concurrent publishers, so it belongs in named SQL.
 * A write statement's entity parameter is always named {@code et}, otherwise both the starter's {@code WRITE_ID_MISMATCH}
 * check and MyBatis-Plus' tenant/audit stamping would miss.
 */
public interface KnowledgeDocumentDAO extends EgonColaMapper<KnowledgeDocumentPO> {

    /**
     * 中文说明：以 {@code id + tenant_id + kb_id + revision = expectedRevision + version = et.version +
     * deleted_at IS NULL} 为条件把文档的 {@code active_revision_id} 切到给定 revision，并在同一条语句里推进业务
     * {@code revision}、技术 {@code version} 与审计两列；命中即发布完成（发布是 CAS 恰一次），0 行表示已有其他
     * 发布者抢先或文档被并发修改，此时旧活动 revision 原样保留，绝不出现半切换。技术 {@code version} 谓词与
     * {@code update_user_id}/{@code update_time} 的 SET 都只能经 {@code et} 绑定——业务 {@code revision =
     * expectedRevision} 是另一列，替代不了它。
     * English summary: Switches the document's {@code active_revision_id} to the given revision under
     * {@code id + tenant_id + kb_id + revision = expectedRevision + version = et.version + deleted_at IS NULL}, advancing the
     * business {@code revision}, the technical {@code version} and the audit pair in that same statement. A hit completes
     * publication — publishing is CAS-exactly-once — while zero rows mean another publisher won first or the document changed
     * concurrently, in which case the previous active revision stays untouched and a half-switched state never exists. The
     * technical {@code version} predicate and the {@code update_user_id}/{@code update_time} assignments can only be bound through
     * {@code et}; the business {@code revision = expectedRevision} predicate is a different column and cannot substitute.
     *
     * 用法 / Usage: 只由 {@code MpKnowledgeRepository.activateRevision(String, String, String, long)} 调用，
     * 摄取策略只在 {@code stageChunks} 与向量校验全部通过、且租约仍由 {@code heartbeatJob} 持有时调用一次；
     * 返回 0 时调用方不得重试覆盖新持有者的写入，而要把终态交给 {@code finishJob} 判定。{@code et} 必须是刚读到的
     * 活跃文档行（其 {@code kbId} 亦提供跨库防护），{@code now} 与写入 {@code et.updateTime} 的是同一时钟读数，
     * 保留该形参只为与 §2 固定签名一致。
     * Called only by {@code MpKnowledgeRepository.activateRevision(String, String, String, long)}: an ingestion strategy
     * invokes it once, after {@code stageChunks} and full vector validation and while {@code heartbeatJob} still holds the
     * lease; on zero rows it must not retry over the new holder's write but hand the terminal state to {@code finishJob}.
     * {@code et} must be the freshly loaded active document row (its {@code kbId} supplies the cross-base guard too), and
     * {@code now} is the same clock reading written into {@code et.updateTime}; the parameter survives only to keep the §2
     * pinned signature.
     * @param et 刚读到的活跃文档行，提供主键、租户、所属知识库、技术版本与审计盖章；the freshly loaded active document row, supplying key, tenant, owning base, technical version and audit stamping.
     * @param activeRevisionId 待激活的 revision 主键；the revision identifier to activate.
     * @param expectedRevision 调用方观察到的文档业务 revision；the caller-observed business revision of the document.
     * @param now 审计时刻基准（同时写入 {@code et.updateTime}）；the audit clock, also written into {@code et.updateTime}.
     * @return 1 表示发布由本方完成，0 表示发布权已不在本方；one when this side published, zero meaning publication is no longer ours.
     */
    int activateRevision(@Param("et") KnowledgeDocumentPO et,
                         @Param("activeRevisionId") long activeRevisionId,
                         @Param("expectedRevision") long expectedRevision,
                         @Param("now") Instant now);
}
