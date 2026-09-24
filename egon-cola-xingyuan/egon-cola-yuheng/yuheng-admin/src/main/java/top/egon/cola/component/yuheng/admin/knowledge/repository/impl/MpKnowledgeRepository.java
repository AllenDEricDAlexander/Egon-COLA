package top.egon.cola.component.yuheng.admin.knowledge.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.yuheng.admin.config.properties.KnowledgeProperties;
import top.egon.cola.component.yuheng.admin.knowledge.converter.KnowledgeBasePersistenceConverter;
import top.egon.cola.component.yuheng.admin.knowledge.converter.KnowledgeChunkPersistenceConverter;
import top.egon.cola.component.yuheng.admin.knowledge.converter.KnowledgeDocumentPersistenceConverter;
import top.egon.cola.component.yuheng.admin.knowledge.converter.KnowledgeDocumentRevisionPersistenceConverter;
import top.egon.cola.component.yuheng.admin.knowledge.converter.KnowledgeJobPersistenceConverter;
import top.egon.cola.component.yuheng.admin.knowledge.dao.KnowledgeDocumentDAO;
import top.egon.cola.component.yuheng.admin.knowledge.dao.KnowledgeJobDAO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeBaseBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeChunkBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeDocumentBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeDocumentRevisionBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeJobBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobStatusEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobTypeEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.po.KnowledgeBasePO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.po.KnowledgeChunkPO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.po.KnowledgeDocumentPO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.po.KnowledgeDocumentRevisionPO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.po.KnowledgeJobPO;
import top.egon.cola.component.yuheng.admin.knowledge.repository.KnowledgeRepository;
import top.egon.cola.component.yuheng.admin.knowledge.repository.mp.KnowledgeBasePersistenceRepository;
import top.egon.cola.component.yuheng.admin.knowledge.repository.mp.KnowledgeChunkPersistenceRepository;
import top.egon.cola.component.yuheng.admin.knowledge.repository.mp.KnowledgeDocumentPersistenceRepository;
import top.egon.cola.component.yuheng.admin.knowledge.repository.mp.KnowledgeDocumentRevisionPersistenceRepository;
import top.egon.cola.component.yuheng.admin.knowledge.repository.mp.KnowledgeJobPersistenceRepository;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminIdempotencyConflictException;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;

/**
 * 中文说明：{@code MpKnowledgeRepository} 是知识库聚合五张表（{@code gateway_knowledge_base}、
 * {@code gateway_knowledge_document}、{@code gateway_knowledge_revision}、{@code gateway_knowledge_chunk}、
 * {@code gateway_knowledge_job}）的 MyBatis-Plus 门面存储，实现业务端口 {@code KnowledgeRepository}：
 * 它把十进制字符串 id 换成受守卫的按主键/按父资源读取，把乐观锁写换成「先读活跃行 → 比对业务
 * {@code revision} → {@code updateById} 或具名 CAS 语句 → 以影响行数裁决」，把租约会话换成
 * {@code selectClaimable}/{@code claimJob}/{@code heartbeatJob}/{@code finishJob} 四条具名语句，
 * 把活动版本切换换成 {@code activateRevision} 单语句 CAS。行模型
 * （{@code KnowledgeBasePO}/{@code KnowledgeDocumentPO}/{@code KnowledgeDocumentRevisionPO}/
 * {@code KnowledgeChunkPO}/{@code KnowledgeJobPO}）与列/类型换算只存在于本类内部，一律经五个 MapStruct
 * 持久转换器完成，绝不泄漏到签名上。
 * English summary: {@code MpKnowledgeRepository} is the MyBatis-Plus facade store over the five tables of the knowledge
 * aggregate ({@code gateway_knowledge_base}, {@code gateway_knowledge_document}, {@code gateway_knowledge_revision},
 * {@code gateway_knowledge_chunk} and {@code gateway_knowledge_job}) implementing the business port
 * {@code KnowledgeRepository}: decimal-string ids become guarded by-primary-key / by-parent reads, optimistic writes become
 * "load the active row, compare the business {@code revision}, then {@code updateById} or the named CAS statement and let the
 * affected row count decide", the lease session becomes the four named statements {@code selectClaimable}/{@code claimJob}/
 * {@code heartbeatJob}/{@code finishJob}, and the active-revision switch becomes the single-statement {@code activateRevision}
 * CAS. The row models and every column/type conversion live only inside this class, always through the five MapStruct
 * persistence converters, and never reach a signature.
 *
 * 用法 / Usage: 以 bean 名 {@code knowledgeRepository} 注入；租户与操作者取自守卫上下文（MDC
 * {@code tenantId}/{@code userId}），本类没有任何租户入参，跨租户行与软删行一律读不到并按不存在处理。
 * 每条语句都经过该表的受守卫 {@code EgonColaRepository} 边界，因此同租户过滤、仅活跃行
 * （{@code deleted_at IS NULL}）与技术 {@code version} 乐观锁是结构性保证而非本类代码。事务边界：
 * 只有 {@link #stageChunks(String, String, List)} 与短只读分页快照自行声明 {@code @Transactional}
 * （分页快照用 {@code REPEATABLE_READ}，与调用方已有事务合并为同一快照）；上传三写
 * （{@code insertDocument} → {@code insertRevision} → {@code insertJob}）以及
 * {@link #claimNext(int)} 的「锁定读取 + 逐行 CAS」都必须在调用方 {@code gatewayTransactionManager}
 * 事务内执行，端口本身不为其扩事务，模型调用绝不在任何事务与行锁之内。写入裁决只认影响行数：
 * 布尔 {@code false} 或 {@code 0} 表示期望修订、租约令牌、运行状态或可见性已不成立，它不是成功，
 * 调用方按 {@code 409 KNOWLEDGE_REVISION_CONFLICT}（现值由业务层回读）或「所有权已丢失」如实处理；
 * 唯一键竞争只在事务已中止时如实抛出冲突而不回读既有行（回读需要新事务，那是业务层的幂等复用职责）。
 * / Use it through the bean name {@code knowledgeRepository}; tenancy and operator come from the guarded context (MDC
 * {@code tenantId}/{@code userId}) so no method takes a tenant argument and a foreign-tenant or soft-deleted row reads as
 * absent. Every statement crosses the table's guarded {@code EgonColaRepository} boundary, making same-tenant filtering,
 * active-only rows ({@code deleted_at IS NULL}) and the technical {@code version} lock structural rather than hand-written.
 * Transaction boundaries: only {@link #stageChunks(String, String, List)} and the short read-only page snapshots declare
 * {@code @Transactional} (the page snapshots use {@code REPEATABLE_READ} and join the caller's transaction into one snapshot);
 * the upload trio ({@code insertDocument} → {@code insertRevision} → {@code insertJob}) and {@link #claimNext(int)}'s
 * "locking read plus per-row CAS" must run inside the caller's {@code gatewayTransactionManager} transaction, which this port
 * never widens, and a model call never sits inside a transaction or a row lock. Writes are decided solely by the affected row
 * count: {@code false} or {@code 0} means the expected revision, lease token, running state or visibility no longer holds,
 * which is not success, and a unique-key race surfaces honestly as a conflict because the transaction is already aborted (a
 * reload would need a new transaction, which is the caller's idempotent-reuse duty).
 */
@Slf4j
@Repository("knowledgeRepository")
@RequiredArgsConstructor
@Validated
public class MpKnowledgeRepository implements KnowledgeRepository {

    /** 中文说明：新建行的权威业务 revision，创建即 1（投影合同要求正整数 revision）。 English summary: the authoritative revision of a fresh row, one, because the projection contract requires a positive revision. */
    private static final long FIRST_REVISION = 1L;

    /** 中文说明：创建意图的乐观版本哨兵，与命令载体的 {@code expectedRevision = 0} 同义，也是竞争失败时回报的现值哨兵。 English summary: the create-intent revision sentinel, equal to the command's {@code expectedRevision = 0}, and the value reported back on a lost race. */
    private static final long CREATE_REVISION = 0L;

    /** 中文说明：分页与批量扫描的单页上限，与端口 {@code @Max(100)} 口径一致并落在受守卫边界的 maxPageSize 之内。 English summary: the page ceiling shared with the port's {@code @Max(100)} and inside the guarded boundary's maximum page size. */
    private static final int SCAN_PAGE_SIZE = 100;

    /** 中文说明：一次暂存写入的分块上限，Spec 的「每批 ≤ 64」批次界限。 English summary: the chunk bound of one staging write, the Spec's "each batch ≤ 64" limit. */
    private static final int STAGE_BATCH_MAX = 64;

    /** 中文说明：本 JVM 的 worker 实例身份，写入 {@code lease_owner}（varchar(128)）；只含进程与主机标识，不含密钥与租户数据。 English summary: this JVM's worker identity written into {@code lease_owner} (varchar(128)); process and host only, never a secret or tenant datum. */
    private static final String WORKER_IDENTITY = workerIdentity();

    @Qualifier("knowledgeBasePersistenceRepository")
    private final KnowledgeBasePersistenceRepository basePersistenceRepository;

    @Qualifier("knowledgeDocumentPersistenceRepository")
    private final KnowledgeDocumentPersistenceRepository documentPersistenceRepository;

    @Qualifier("knowledgeDocumentRevisionPersistenceRepository")
    private final KnowledgeDocumentRevisionPersistenceRepository revisionPersistenceRepository;

    @Qualifier("knowledgeChunkPersistenceRepository")
    private final KnowledgeChunkPersistenceRepository chunkPersistenceRepository;

    @Qualifier("knowledgeJobPersistenceRepository")
    private final KnowledgeJobPersistenceRepository jobPersistenceRepository;

    @Qualifier("knowledgeBasePersistenceConverter")
    private final KnowledgeBasePersistenceConverter basePersistenceConverter;

    @Qualifier("knowledgeDocumentPersistenceConverter")
    private final KnowledgeDocumentPersistenceConverter documentPersistenceConverter;

    @Qualifier("knowledgeDocumentRevisionPersistenceConverter")
    private final KnowledgeDocumentRevisionPersistenceConverter revisionPersistenceConverter;

    @Qualifier("knowledgeChunkPersistenceConverter")
    private final KnowledgeChunkPersistenceConverter chunkPersistenceConverter;

    @Qualifier("knowledgeJobPersistenceConverter")
    private final KnowledgeJobPersistenceConverter jobPersistenceConverter;

    @Qualifier("knowledgeDocumentDAO")
    private final KnowledgeDocumentDAO knowledgeDocumentDAO;

    @Qualifier("knowledgeJobDAO")
    private final KnowledgeJobDAO knowledgeJobDAO;

    /** 中文说明：租约时长来自 {@code yuheng.knowledge.lease}（缺省 120 秒），认领写回时用它算到期时刻；
     *  心跳周期 30 秒是 worker 侧的节奏，不在本类。 English summary: the lease length comes from {@code yuheng.knowledge.lease} (two minutes by default) and decides the expiry stamped at claim; the 30-second heartbeat cadence belongs to the worker, not this class. */
    @Qualifier(KnowledgeProperties.BEAN_NAME)
    private final KnowledgeProperties knowledgeProperties;

    /**
     * 中文说明：执行 findBase 操作；按主键走受守卫的活跃读取，软删与跨租户行返回空。
     * English summary: Executes the findBase operation; a guarded active read by primary key, a soft-deleted or
     * foreign-tenant row yielding empty.
     *
     * 用法 / Usage: {@code knowledgeRepository.findBase(kbId)}；未命中由业务层转 404。
     * @param kbId 知识库十进制字符串 id；decimal-string knowledge base id.
     * @return 知识库业务载体；the knowledge base carrier when present.
     */
    @Override
    public Optional<KnowledgeBaseBO> findBase(String kbId) {
        return Optional.ofNullable(basePersistenceRepository.getById(idOf(kbId)))
                .map(basePersistenceConverter::toBusiness);
    }

    /**
     * 中文说明：执行 listBasesOfActor 操作；在守卫租户的活跃知识库内读取该 actor 作为 owner 或 members 成员可见的
     * 当页，次序固定 {@code create_time DESC, id DESC}；成员谓词是 {@code members @> [{actorId: …}]} 的 jsonb 包含，
     * 由 DDL 的 {@code ix_knowledge_base_members}（GIN jsonb_path_ops）支撑，探测值以绑定参数传入而非拼接。
     * English summary: Executes the listBasesOfActor operation; the visible page of knowledge bases owned or membered by this
     * actor inside the guarded tenant, ordered {@code create_time DESC, id DESC}; membership is the
     * {@code members @> [{actorId: …}]} jsonb containment served by the DDL's {@code ix_knowledge_base_members}
     * (GIN jsonb_path_ops) index, with the probe arriving as a bound parameter rather than concatenated text.
     *
     * 用法 / Usage: {@code knowledgeRepository.listBasesOfActor(actorId, page, size)}；与
     * {@link #countBasesOfActor(String)} 同谓词配对，空页返回 {@code []}。
     * @param actorId actor 稳定标识；actor identifier as persisted.
     * @param page 页码，从 1 开始；one-based page number.
     * @param size 页大小；effective page size.
     * @return 当页知识库业务载体；the knowledge base carriers of that page.
     */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<KnowledgeBaseBO> listBasesOfActor(
            String actorId,
            int page,
            int size) {
        return basePersistenceConverter.toBusinessList(
                basePersistenceRepository.list(
                        new Page<KnowledgeBasePO>(page, bounded(size), false),
                        visibleBases(actorId)
                )
        );
    }

    /**
     * 中文说明：执行 countBasesOfActor 操作；与当页读取完全同谓词的受守卫活跃计数，使分页响应携带真实总数。
     * English summary: Executes the countBasesOfActor operation; the guarded active count under exactly the page's predicate so
     * the response carries a real total.
     *
     * 用法 / Usage: {@code knowledgeRepository.countBasesOfActor(actorId)}。
     * @param actorId actor 稳定标识；actor identifier as persisted.
     * @return 可见的活跃知识库总数；the number of visible active knowledge bases.
     */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public long countBasesOfActor(String actorId) {
        return basePersistenceRepository.count(visibleBases(actorId));
    }

    /**
     * 中文说明：执行 insertBase 操作；受守卫插入首版知识库行（技术 id、租户、审计与 {@code version} 由边界补齐，
     * 业务 {@code revision} 固定为权威 1），插入 0 行与唯一键竞争都按冲突如实抛出——竞争事务已被约束中止，
     * 因此不回读既有行；成功时把权威 id、revision 与审计时刻回写进入参载体。
     * English summary: Executes the insertBase operation; a guarded insert of the first knowledge-base row (the boundary stamps
     * technical id, tenant, audit and {@code version} while the business {@code revision} is pinned to the authoritative one),
     * with a zero-row insert and a unique-key race both surfacing honestly as conflicts — a race arrives with the transaction
     * already aborted by the constraint, so no row is re-read — and on success the authoritative id, revision and audit
     * instants being written back into the carrier.
     *
     * 用法 / Usage: {@code knowledgeRepository.insertBase(base)}；在调用方写事务内执行；幂等复用由业务层先查
     * 意图记录决定，本方法绝不静默覆盖。
     * @param base 待插入的知识库载体；the knowledge base carrier to insert.
     * @return 已提交的知识库业务载体；the committed knowledge base carrier.
     */
    @Override
    public KnowledgeBaseBO insertBase(KnowledgeBaseBO base) {
        KnowledgeBasePO row = basePersistenceConverter.newRow(base);
        row.setId(null);
        row.setRevision(FIRST_REVISION);
        try {
            requireSaved(basePersistenceRepository.save(row), "gateway_knowledge_base", "YUHENG_ADMIN_KNOWLEDGE_BASE_CREATE_FAILED");
        } catch (DuplicateKeyException raced) {
            log.warn("YUHENG_ADMIN_KNOWLEDGE_BASE_CREATE_RACED owner={}", base.getOwnerActorId(), raced);
            throw new GatewayAdminRevisionConflictException(CREATE_REVISION);
        }
        return authoritative(base, row);
    }

    /**
     * 中文说明：执行 replaceBase 操作；按主键读活跃行，库中业务 revision 与期望值不符即返回 {@code false}
     * （失败而非成功），一致时以 {@code applyBusiness} 整行覆盖可写业务列并以 {@code expectedRevision + 1}
     * 走受守卫 {@code updateById} CAS，影响 0 行同样返回 {@code false}。
     * English summary: Executes the replaceBase operation; it loads the active row by primary key, returns {@code false} when the
     * stored business revision differs from the expectation (failure, not success), and on a match overwrite-replaces the
     * writable columns via {@code applyBusiness} and performs the guarded {@code updateById} CAS at
     * {@code expectedRevision + 1}, again returning {@code false} on a zero-row effect.
     *
     * 用法 / Usage: {@code knowledgeRepository.replaceBase(base, expectedRevision)}；{@code false} 时业务层回读现值并以
     * 409 回应；{@code id} 无法解析或行已不可见也返回 {@code false} 而不是伪造。
     * @param base 完整替换载体；the full replacement carrier.
     * @param expectedRevision 调用方期望的当前 revision；the caller-observed revision.
     * @return CAS 是否命中；whether the compare-and-set hit.
     */
    @Override
    public boolean replaceBase(
            KnowledgeBaseBO base,
            long expectedRevision) {
        KnowledgeBasePO row = basePersistenceRepository.getById(idOf(base.getId()));
        if (row == null || revisionOf(row.getRevision()) != expectedRevision) {
            log.info("YUHENG_ADMIN_KNOWLEDGE_BASE_CAS_MISSED kbId={} expected={}", base.getId(), expectedRevision);
            return false;
        }
        basePersistenceConverter.applyBusiness(base, row);
        row.setRevision(expectedRevision + 1);
        if (!basePersistenceRepository.updateById(row)) {
            log.info("YUHENG_ADMIN_KNOWLEDGE_BASE_CAS_LOST kbId={}", base.getId());
            return false;
        }
        authoritative(base, row);
        return true;
    }

    /**
     * 中文说明：执行 findDocument 操作；按主键读取后复核 {@code kb_id} 归属，其他知识库下的同 id 文档与软删行
     * 都视为不存在，因此本方法是跨库越权的天然屏障。
     * English summary: Executes the findDocument operation; the row is read by primary key and its {@code kb_id} ownership
     * re-checked, so the same identifier under another knowledge base and a soft-deleted row both read as absent, which makes
     * this method the natural cross-base barrier.
     *
     * 用法 / Usage: {@code knowledgeRepository.findDocument(kbId, documentId)}；未命中返回空并由业务层转 404。
     * @param kbId 知识库十进制字符串 id；decimal-string knowledge base id.
     * @param documentId 文档十进制字符串 id；decimal-string document id.
     * @return 文档业务载体；the document carrier when present.
     */
    @Override
    public Optional<KnowledgeDocumentBO> findDocument(
            String kbId,
            String documentId) {
        KnowledgeDocumentPO row = documentPersistenceRepository.getById(idOf(documentId));
        if (row == null || !Objects.equals(row.getKbId(), idOf(kbId))) {
            return Optional.empty();
        }
        return Optional.of(documentPersistenceConverter.toBusiness(row));
    }

    /**
     * 中文说明：执行 listDocuments 操作；受守卫读取该知识库下活跃文档当页，次序固定 {@code create_time DESC, id DESC}。
     * English summary: Executes the listDocuments operation; the guarded page of active documents of one base under the fixed
     * {@code create_time DESC, id DESC} order.
     *
     * 用法 / Usage: {@code knowledgeRepository.listDocuments(kbId, page, size)}；总数由 {@link #countDocuments(String)} 配对。
     * @param kbId 知识库十进制字符串 id；decimal-string knowledge base id.
     * @param page 页码，从 1 开始；one-based page number.
     * @param size 页大小；effective page size.
     * @return 当页文档业务载体；the document carriers of that page.
     */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<KnowledgeDocumentBO> listDocuments(
            String kbId,
            int page,
            int size) {
        return documentPersistenceConverter.toBusinessList(
                documentPersistenceRepository.list(
                        new Page<KnowledgeDocumentPO>(page, bounded(size), false),
                        documentsOf(kbId)
                )
        );
    }

    /**
     * 中文说明：执行 countDocuments 操作；与文档当页完全同谓词的受守卫活跃计数。
     * English summary: Executes the countDocuments operation; the guarded active count under the page's own predicate.
     *
     * 用法 / Usage: {@code knowledgeRepository.countDocuments(kbId)}。
     * @param kbId 知识库十进制字符串 id；decimal-string knowledge base id.
     * @return 活跃文档总数；the number of active documents.
     */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public long countDocuments(String kbId) {
        return documentPersistenceRepository.count(documentsOf(kbId));
    }

    /**
     * 中文说明：执行 insertDocument 操作；受守卫插入文档主行，业务 {@code revision} 固定权威 1。
     * 上传路径的顺序不变式在此把守：{@code active_revision_id} 指向尚未插入的 revision，因此文档行必须先以
     * {@code activeRevisionId = null} 插入，revision 与 job 随后写入，活动指针由 {@code activateRevision} 的
     * CAS 在后续语句里指向已存在的行——延迟复合外键 {@code fk_knowledge_document_active_revision} 在提交时校验，
     * 任何「先写指针再写行」的顺序都会让事务在提交处失败。
     * English summary: Executes the insertDocument operation; a guarded insert of the document row at the authoritative business
     * {@code revision} of one. The upload ordering invariant is held here: since {@code active_revision_id} points at a
     * revision that does not exist yet, the document row is inserted first with {@code activeRevisionId = null}, the revision
     * and the job follow, and the pointer is moved onto an existing row later by the {@code activateRevision} CAS — the
     * deferring composite FK {@code fk_knowledge_document_active_revision} is validated at commit, so writing the pointer
     * before its row would fail the transaction exactly there.
     *
     * 用法 / Usage: {@code knowledgeRepository.insertDocument(document)}；必须与 {@link #insertRevision} 和
     * {@link #insertJob} 处于调用方同一事务，三行一起提交且事务内不发起模型调用。
     * @param document 待插入的文档载体；the document carrier to insert.
     * @return 已提交的文档业务载体；the committed document carrier.
     */
    @Override
    public KnowledgeDocumentBO insertDocument(KnowledgeDocumentBO document) {
        KnowledgeDocumentPO row = documentPersistenceConverter.newRow(document);
        row.setId(null);
        row.setRevision(FIRST_REVISION);
        try {
            requireSaved(documentPersistenceRepository.save(row), "gateway_knowledge_document", "YUHENG_ADMIN_KNOWLEDGE_DOCUMENT_CREATE_FAILED");
        } catch (DuplicateKeyException raced) {
            log.warn("YUHENG_ADMIN_KNOWLEDGE_DOCUMENT_CREATE_RACED kbId={}", document.getKbId(), raced);
            throw new GatewayAdminRevisionConflictException(CREATE_REVISION);
        }
        return authoritative(document, row);
    }

    /**
     * 中文说明：执行 updateDocument 操作；按主键读活跃行并复核 {@code kb_id} 归属与业务 revision，随后整行覆盖
     * 可写业务列（例如改写 {@code latest_job_id}）并以 {@code expectedRevision + 1} 走 {@code updateById} CAS；
     * 行不可见、跨库、revision 不符或影响 0 行一律返回 {@code false}。活动版本切换不走这里。
     * English summary: Executes the updateDocument operation; the active row is loaded by primary key, its {@code kb_id}
     * ownership and business revision re-checked, the writable columns replaced (a {@code latest_job_id} rewrite, for
     * instance) and the {@code updateById} CAS performed at {@code expectedRevision + 1}; an invisible row, a cross-base
     * mismatch, a revision mismatch or a zero-row effect all return {@code false}. Switching the active revision never goes
     * through here.
     *
     * 用法 / Usage: {@code knowledgeRepository.updateDocument(document, expectedRevision)}；软删由业务层经
     * {@code KnowledgeDocumentPersistenceRepository#removeById} 完成，本端口只覆盖业务列。
     * @param document 完整替换载体；the full replacement carrier.
     * @param expectedRevision 调用方期望的当前 revision；the caller-observed revision.
     * @return CAS 是否命中；whether the compare-and-set hit.
     */
    @Override
    public boolean updateDocument(
            KnowledgeDocumentBO document,
            long expectedRevision) {
        KnowledgeDocumentPO row = documentPersistenceRepository.getById(idOf(document.getId()));
        if (row == null || !Objects.equals(row.getKbId(), idOf(document.getKbId()))
                || revisionOf(row.getRevision()) != expectedRevision) {
            log.info("YUHENG_ADMIN_KNOWLEDGE_DOCUMENT_CAS_MISSED documentId={} expected={}", document.getId(), expectedRevision);
            return false;
        }
        documentPersistenceConverter.applyBusiness(document, row);
        row.setRevision(expectedRevision + 1);
        if (!documentPersistenceRepository.updateById(row)) {
            log.info("YUHENG_ADMIN_KNOWLEDGE_DOCUMENT_CAS_LOST documentId={}", document.getId());
            return false;
        }
        authoritative(document, row);
        return true;
    }

    /**
     * 中文说明：执行 softDeleteDocument 操作；先按主键读活跃行并核对 {@code kb_id} 与期望 revision
     * （任一不成立即 {@code false}，绝不跨库删别人的行），再交给 Starter 的带版本逻辑删除
     * {@code removeById(entity) → deleteVersionedById}：那条语句只写 {@code deleted_at} 与 {@code version}，
     * 因此这是文档行唯一的下架路径，历史 revision、chunk 与 job 一律留在册内。
     * English summary: Executes the softDeleteDocument operation: the active row is loaded by primary key and checked for its
     * {@code kb_id} and the expected revision (either break yielding {@code false}, so no foreign row can be withdrawn),
     * then handed to the starter's versioned logic delete {@code removeById(entity) → deleteVersionedById}. That statement
     * writes only {@code deleted_at} and {@code version}, which makes it the document row's single takedown path while its
     * revision, chunk and job history stay readable.
     *
     * 用法 / Usage: {@code knowledgeRepository.softDeleteDocument(kbId, documentId, expectedRevision)}；
     * 0 行由调用方转 409，本方法不抛异常也不重试。
     * @param kbId 参数 文档所属知识库十进制字符串 id；parameter decimal-string knowledge base id owning the document.
     * @param documentId 参数 文档十进制字符串 id；parameter decimal-string document id.
     * @param expectedRevision 参数 调用方观察到的当前 revision；parameter the caller-observed revision.
     * @return 返回 软删是否命中；returns whether the soft delete hit the row.
     */
    @Override
    public boolean softDeleteDocument(
            String kbId,
            String documentId,
            long expectedRevision) {
        KnowledgeDocumentPO row = documentPersistenceRepository.getById(idOf(documentId));
        if (row == null || !Objects.equals(row.getKbId(), idOf(kbId))
                || revisionOf(row.getRevision()) != expectedRevision) {
            log.info("YUHENG_ADMIN_KNOWLEDGE_DOCUMENT_SOFT_DELETE_MISSED documentId={} expected={}",
                    documentId, expectedRevision);
            return false;
        }
        if (!documentPersistenceRepository.removeById(row)) {
            log.info("YUHENG_ADMIN_KNOWLEDGE_DOCUMENT_SOFT_DELETE_CAS_LOST documentId={}", documentId);
            return false;
        }
        log.info("YUHENG_ADMIN_KNOWLEDGE_DOCUMENT_SOFT_DELETED kbId={} documentId={} revision={}",
                kbId, documentId, expectedRevision);
        return true;
    }

    /**
     * 中文说明：执行 activateRevision 操作；具名语句 {@code KnowledgeDocumentDAO.activateRevision} 的端口。
     * 先读活跃文档行并复核 {@code kb_id} 归属与业务 revision，再把刚读到的行作为 {@code et} 交给该语句：
     * 单语句 CAS 同时写 {@code active_revision_id}、推进业务 {@code revision} 与技术 {@code version} 并盖审计列，
     * 命中即发布完成（发布恰一次），0 行返回 {@code false} 且旧活动 revision 原样保留。
     * English summary: Executes the activateRevision operation, the port of the named
     * {@code KnowledgeDocumentDAO.activateRevision} statement. The active document row is read first and its {@code kb_id}
     * ownership plus business revision re-checked, then handed to the statement as {@code et}: the single-statement CAS writes
     * {@code active_revision_id} while advancing the business {@code revision}, the technical {@code version} and the audit
     * columns, so a hit completes publication exactly once and a zero-row effect returns {@code false} with the previous active
     * revision untouched.
     *
     * 用法 / Usage: {@code knowledgeRepository.activateRevision(kbId, documentId, activeRevisionId, expectedRevision)}；
     * 只在 {@link #stageChunks(String, String, List)} 与向量校验全部通过、租约仍由 {@link #heartbeat(String, long, Instant)}
     * 持有时调用一次；返回 {@code false} 时不得重试覆盖新持有者，而要把终态交给 {@link #finish(KnowledgeJobBO, long)}。
     * @param kbId 知识库十进制字符串 id；decimal-string knowledge base id.
     * @param documentId 文档十进制字符串 id；decimal-string document id.
     * @param activeRevisionId 待激活的 revision 十进制字符串 id；decimal-string revision id to activate.
     * @param expectedRevision 调用方观察到的文档 revision；the caller-observed document revision.
     * @return CAS 是否命中，{@code false} 即发布权已不在本方；whether the CAS hit, {@code false} meaning publication is no longer ours.
     */
    @Override
    public boolean activateRevision(
            String kbId,
            String documentId,
            String activeRevisionId,
            long expectedRevision) {
        long revisionKey = idOf(activeRevisionId);
        KnowledgeDocumentPO row = documentPersistenceRepository.getById(idOf(documentId));
        if (row == null || !Objects.equals(row.getKbId(), idOf(kbId))
                || revisionOf(row.getRevision()) != expectedRevision) {
            log.info("YUHENG_ADMIN_KNOWLEDGE_PUBLISH_PRECONDITION_MISSED documentId={} expected={}", documentId, expectedRevision);
            return false;
        }
        Instant now = now();
        int published = knowledgeDocumentDAO.activateRevision(row, revisionKey, expectedRevision, now);
        if (published != 1) {
            log.info("YUHENG_ADMIN_KNOWLEDGE_PUBLISH_CAS_LOST documentId={} revisionId={}", documentId, activeRevisionId);
            return false;
        }
        log.info("YUHENG_ADMIN_KNOWLEDGE_PUBLISHED documentId={} revisionId={} revision={}->{}",
                documentId, activeRevisionId, expectedRevision, expectedRevision + 1);
        return true;
    }

    /**
     * 中文说明：执行 findRevision 操作；按主键读取后复核 {@code kb_id} 归属，跨库引用同一 revision 也读不到；
     * 返回载体携带内部 {@code rawBytes}/{@code extractedText}，二者只供服务端处理，绝不进入日志或响应。
     * English summary: Executes the findRevision operation; the row is read by primary key and its {@code kb_id} ownership
     * re-checked, so a cross-base reference reads as absent; the carrier carries the internal {@code rawBytes} and
     * {@code extractedText}, which exist for server-side processing only and never enter a log line or a response.
     *
     * 用法 / Usage: {@code knowledgeRepository.findRevision(kbId, revisionId)}；未命中返回空并由业务层转 404。
     * @param kbId 知识库十进制字符串 id；decimal-string knowledge base id.
     * @param revisionId 修订十进制字符串 id；decimal-string revision id.
     * @return 修订业务载体；the revision carrier when present.
     */
    @Override
    public Optional<KnowledgeDocumentRevisionBO> findRevision(
            String kbId,
            String revisionId) {
        KnowledgeDocumentRevisionPO row = revisionPersistenceRepository.getById(idOf(revisionId));
        if (row == null || !Objects.equals(row.getKbId(), idOf(kbId))) {
            return Optional.empty();
        }
        return Optional.of(revisionPersistenceConverter.toBusiness(row));
    }

    /**
     * 中文说明：执行 listRevisions 操作；受守卫读取该文档的活跃修订当页，次序固定 {@code create_time DESC, id DESC}，
     * 与文档/知识库双谓词共同限定作用域。
     * English summary: Executes the listRevisions operation; the guarded page of one document's active revisions in the fixed
     * {@code create_time DESC, id DESC} order under both the document and the knowledge-base predicate.
     *
     * 用法 / Usage: {@code knowledgeRepository.listRevisions(kbId, documentId, page, size)}；空页返回 {@code []}。
     * @param kbId 知识库十进制字符串 id；decimal-string knowledge base id.
     * @param documentId 文档十进制字符串 id；decimal-string document id.
     * @param page 页码，从 1 开始；one-based page number.
     * @param size 页大小；effective page size.
     * @return 当页修订业务载体；the revision carriers of that page.
     */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<KnowledgeDocumentRevisionBO> listRevisions(
            String kbId,
            String documentId,
            int page,
            int size) {
        return revisionPersistenceConverter.toBusinessList(
                revisionPersistenceRepository.list(
                        new Page<KnowledgeDocumentRevisionPO>(page, bounded(size), false),
                        Wrappers.<KnowledgeDocumentRevisionPO>lambdaQuery()
                                .eq(KnowledgeDocumentRevisionPO::getKbId, idOf(kbId))
                                .eq(KnowledgeDocumentRevisionPO::getDocumentId, idOf(documentId))
                                .orderByDesc(KnowledgeDocumentRevisionPO::getCreateTime)
                                .orderByDesc(KnowledgeDocumentRevisionPO::getId)
                )
        );
    }

    /**
     * 中文说明：执行 insertRevision 操作；受守卫插入 {@code STAGING} 修订行，业务 {@code revision} 固定权威 1，
     * 原始字节、{@code byteCount}、{@code contentHash} 与嵌入空间/维度快照在同一次写入中固化；插入 0 行与
     * 唯一键竞争都按冲突如实抛出而不回读。
     * English summary: Executes the insertRevision operation; a guarded insert of the {@code STAGING} revision row at the
     * authoritative business {@code revision} of one, freezing the raw bytes, {@code byteCount}, {@code contentHash} and the
     * embedding-space and dimension snapshot in the same write, with a zero-row insert and a unique-key race both surfacing
     * honestly as conflicts rather than a re-read.
     *
     * 用法 / Usage: {@code knowledgeRepository.insertRevision(revision)}；与文档、作业行共用调用方的一个事务，
     * 提交之后才可能有 worker 认领。
     * @param revision 待插入的修订载体；the revision carrier to insert.
     * @return 已提交的修订业务载体；the committed revision carrier.
     */
    @Override
    public KnowledgeDocumentRevisionBO insertRevision(KnowledgeDocumentRevisionBO revision) {
        KnowledgeDocumentRevisionPO row = revisionPersistenceConverter.newRow(revision);
        row.setId(null);
        row.setRevision(FIRST_REVISION);
        try {
            requireSaved(revisionPersistenceRepository.save(row), "gateway_knowledge_revision", "YUHENG_ADMIN_KNOWLEDGE_REVISION_CREATE_FAILED");
        } catch (DuplicateKeyException raced) {
            log.warn("YUHENG_ADMIN_KNOWLEDGE_REVISION_CREATE_RACED documentId={}", revision.getDocumentId(), raced);
            throw new GatewayAdminRevisionConflictException(CREATE_REVISION);
        }
        return authoritative(revision, row);
    }

    /**
     * 中文说明：执行 updateRevision 操作；按主键读活跃行并复核 {@code kb_id} 归属与业务 revision，整行覆盖状态、
     * 抽取文本、分块计数等可写列后以 {@code expectedRevision + 1} 走 {@code updateById} CAS；
     * 任何不成立或 0 行都返回 {@code false}，即失败而非成功。
     * English summary: Executes the updateRevision operation; the active row is loaded by primary key with its {@code kb_id}
     * ownership and business revision re-checked, the writable columns (status, extracted text, chunk count and friends)
     * replaced and the {@code updateById} CAS performed at {@code expectedRevision + 1}; any unmet precondition or zero-row
     * effect returns {@code false}, which is failure rather than success.
     *
     * 用法 / Usage: {@code knowledgeRepository.updateRevision(revision, expectedRevision)}；重放同一 attempt 前先读现值，
     * 不得凭假设的 revision 覆盖。
     * @param revision 完整替换载体；the full replacement carrier.
     * @param expectedRevision 调用方期望的当前 revision；the caller-observed revision.
     * @return CAS 是否命中；whether the compare-and-set hit.
     */
    @Override
    public boolean updateRevision(
            KnowledgeDocumentRevisionBO revision,
            long expectedRevision) {
        KnowledgeDocumentRevisionPO row = revisionPersistenceRepository.getById(idOf(revision.getId()));
        if (row == null || !Objects.equals(row.getKbId(), idOf(revision.getKbId()))
                || revisionOf(row.getRevision()) != expectedRevision) {
            log.info("YUHENG_ADMIN_KNOWLEDGE_REVISION_CAS_MISSED revisionId={} expected={}", revision.getId(), expectedRevision);
            return false;
        }
        revisionPersistenceConverter.applyBusiness(revision, row);
        row.setRevision(expectedRevision + 1);
        if (!revisionPersistenceRepository.updateById(row)) {
            log.info("YUHENG_ADMIN_KNOWLEDGE_REVISION_CAS_LOST revisionId={}", revision.getId());
            return false;
        }
        authoritative(revision, row);
        return true;
    }

    /**
     * 中文说明：执行 stageChunks 操作；为一个 {@code STAGING} 修订重写分块集合，全部动作处在一个事务里：
     * 先复核知识库与修订都在守卫租户内活跃且归属一致，再逐条校验分块意图（归属、批次上限、冻结的嵌入空间与
     * 维度、向量长度/有限性/非零范数、{@code chunkIndex} 非负且不重复），随后按 {@code revision_id} 用受守卫的
     * {@code removeById} 软删该修订此前暂存的分块，最后 {@code saveBatch} 批量插入并返回真正写入的行数。
     * 校验失败一律在写入之前抛出，绝不留下部分向量；软删与插入的任一 0 行裁决都按冲突抛出并回滚整个事务，
     * 因此旧活动修订与已索引集合从不被半改写。
     * English summary: Executes the stageChunks operation; it rewrites one {@code STAGING} revision's chunk set inside a single
     * transaction: the base and the revision are first re-checked as active inside the guarded tenant and mutually consistent,
     * every chunk intent is then validated (ownership, batch bound, the frozen embedding space and dimensions, vector length,
     * finiteness and non-zero norm, plus a non-negative and duplicate-free {@code chunkIndex}), the revision's previously
     * staged chunks are soft-deleted per {@code revision_id} through the guarded {@code removeById}, and {@code saveBatch}
     * finally inserts the new rows whose count is returned. A validation failure always precedes the write so a partial vector
     * set never survives, and any zero-row verdict in the delete or the insert raises a conflict that rolls the whole
     * transaction back, so neither the old active revision nor an indexed set is ever half-rewritten.
     *
     * 用法 / Usage: {@code knowledgeRepository.stageChunks(kbId, revisionId, chunks)}；只在活动切换之前调用，
     * 重放同一作业是幂等的（清理后重写）；分块上限 64，云端嵌入永远不是兜底方案。
     * @param kbId 知识库十进制字符串 id；decimal-string knowledge base id.
     * @param revisionId 修订十进制字符串 id；decimal-string revision id.
     * @param chunks 待暂存的分块载体列表；the chunk carriers to stage.
     * @return 写入的分块行数；the number of staged chunk rows.
     */
    @Override
    @Transactional
    public int stageChunks(
            String kbId,
            String revisionId,
            List<KnowledgeChunkBO> chunks) {
        long baseKey = idOf(kbId);
        long revisionKey = idOf(revisionId);
        KnowledgeBasePO base = basePersistenceRepository.getById(baseKey);
        if (base == null) {
            throw notFound("gateway_knowledge_base", kbId);
        }
        KnowledgeDocumentRevisionPO revision = revisionPersistenceRepository.getById(revisionKey);
        if (revision == null || !Objects.equals(revision.getKbId(), baseKey)) {
            throw notFound("gateway_knowledge_revision", revisionId);
        }
        if (chunks.size() > STAGE_BATCH_MAX) {
            throw validation("staged chunk batch exceeds the bound of " + STAGE_BATCH_MAX);
        }
        List<KnowledgeChunkPO> rows = new ArrayList<>(chunks.size());
        Set<Integer> indexes = new HashSet<>(chunks.size());
        for (KnowledgeChunkBO chunk : chunks) {
            rows.add(stagedChunkRow(chunk, base, baseKey, revisionKey, indexes));
        }
        int cleared = 0;
        for (KnowledgeChunkPO stale : chunkPersistenceRepository.list(
                Wrappers.<KnowledgeChunkPO>lambdaQuery().eq(KnowledgeChunkPO::getRevisionId, revisionKey))) {
            if (!chunkPersistenceRepository.removeById(stale)) {
                log.info("YUHENG_ADMIN_KNOWLEDGE_CHUNK_STAGE_CAS_LOST revisionId={} chunkId={}", revisionId, stale.getId());
                throw new GatewayAdminRevisionConflictException(revisionOf(stale.getRevision()));
            }
            cleared++;
        }
        if (rows.isEmpty()) {
            log.info("YUHENG_ADMIN_KNOWLEDGE_CHUNKS_STAGED revisionId={} staged=0 cleared={}", revisionId, cleared);
            return 0;
        }
        requireSaved(chunkPersistenceRepository.saveBatch(rows), "gateway_knowledge_chunk", "YUHENG_ADMIN_KNOWLEDGE_CHUNK_STAGE_FAILED");
        log.info("YUHENG_ADMIN_KNOWLEDGE_CHUNKS_STAGED revisionId={} staged={} cleared={}", revisionId, rows.size(), cleared);
        return rows.size();
    }

    /**
     * 中文说明：执行 listChunksOfRevision 操作；按 {@code chunk_index} 升序读取该修订的全部活跃分块，
     * 稳定次序使重放与逐条校验可比对；向量只在服务端内存短暂存在。
     * English summary: Executes the listChunksOfRevision operation; every active chunk of one revision ascending by
     * {@code chunk_index}, a stable order so a replay or a verification pass can compare entries one by one, while vectors
     * stay in server memory only.
     *
     * 用法 / Usage: {@code knowledgeRepository.listChunksOfRevision(revisionId)}；无分块返回 {@code []}。
     * @param revisionId 修订十进制字符串 id；decimal-string revision id.
     * @return 该修订的分块业务载体列表；the chunk carriers of that revision.
     */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<KnowledgeChunkBO> listChunksOfRevision(String revisionId) {
        return chunkPersistenceConverter.toBusinessList(
                chunkPersistenceRepository.list(
                        Wrappers.<KnowledgeChunkPO>lambdaQuery()
                                .eq(KnowledgeChunkPO::getRevisionId, idOf(revisionId))
                                .orderByAsc(KnowledgeChunkPO::getChunkIndex)
                )
        );
    }

    /**
     * 中文说明：执行 findJob 操作；按主键走受守卫的活跃读取，返回载体携带租约三列与状态/阶段/尝试/错误码/结果。
     * English summary: Executes the findJob operation; a guarded active read by primary key whose carrier holds the three lease
     * columns plus the stage, attempt, error code and result.
     *
     * 用法 / Usage: {@code knowledgeRepository.findJob(jobId)}；未命中（含跨租户与软删）返回空并由业务层转 404。
     * @param jobId 作业十进制字符串 id；decimal-string job id.
     * @return 作业业务载体；the job carrier when present.
     */
    @Override
    public Optional<KnowledgeJobBO> findJob(String jobId) {
        return Optional.ofNullable(jobPersistenceRepository.getById(idOf(jobId)))
                .map(jobPersistenceConverter::toBusiness);
    }

    /**
     * 中文说明：执行 findJobByIntent 操作；按幂等意图 {@code kb_id + actor_id + type + idempotency_key} 回读既有作业，
     * 是创建/重试复用判定的唯一入口；命中同一行而不新建，摘要是否一致由业务层判定。
     * English summary: Executes the findJobByIntent operation; an existing job re-read by the idempotency intent
     * {@code kb_id + actor_id + type + idempotency_key}, the single entry point of the reuse decision for create and retry:
     * the same row comes back instead of a new job and whether the digest matches is the caller's judgement.
     *
     * 用法 / Usage: {@code knowledgeRepository.findJobByIntent(kbId, actorId, type, idempotencyKey)}；
     * 与后续插入共处唯一约束，竞争方在重读时命中同一行。
     * @param kbId 知识库十进制字符串 id；decimal-string knowledge base id.
     * @param actorId 提交者 actor 稳定标识；submitting actor identifier.
     * @param type 作业类型；job type.
     * @param idempotencyKey 客户端幂等键；client idempotency key.
     * @return 命中的作业业务载体；the matched job carrier when present.
     */
    @Override
    public Optional<KnowledgeJobBO> findJobByIntent(
            String kbId,
            String actorId,
            KnowledgeJobTypeEnum type,
            String idempotencyKey) {
        return jobPersistenceRepository.list(
                Wrappers.<KnowledgeJobPO>lambdaQuery()
                        .eq(KnowledgeJobPO::getKbId, idOf(kbId))
                        .eq(KnowledgeJobPO::getActorId, actorId)
                        .eq(KnowledgeJobPO::getType, type.wireValue())
                        .eq(KnowledgeJobPO::getIdempotencyKey, idempotencyKey)
                        .orderByDesc(KnowledgeJobPO::getCreateTime)
                        .orderByDesc(KnowledgeJobPO::getId)
        ).stream().findFirst().map(jobPersistenceConverter::toBusiness);
    }

    /**
     * 中文说明：执行 insertJob 操作；受守卫插入 {@code QUEUED} 作业行，业务 {@code revision} 固定权威 1，
     * 意图身份与 {@code payload}/{@code requestHash}/{@code retryOfJobId} 全部由载体决定；
     * 幂等意图的唯一键竞争如实抛出 {@code GatewayAdminIdempotencyConflictException} 而不静默产生第二个作业，
     * 因为该事务已被约束违例中止、无法在同一事务内回读 winner。
     * English summary: Executes the insertJob operation; a guarded insert of the {@code QUEUED} job row at the authoritative
     * business {@code revision} of one, every part of the intent identity and {@code payload}/{@code requestHash}/
     * {@code retryOfJobId} decided by the carrier, with a unique-key race on the idempotency intent surfacing as a
     * {@code GatewayAdminIdempotencyConflictException} rather than a silently second job, since the transaction is already
     * aborted by the constraint and cannot re-read the winner inside itself.
     *
     * 用法 / Usage: {@code knowledgeRepository.insertJob(job)}；调用方必须先 {@link #findJobByIntent} 复用，
     * 上传路径下与文档、revision 行同事务提交。
     * @param job 待插入的作业载体；the job carrier to insert.
     * @return 已提交的作业业务载体；the committed job carrier.
     */
    @Override
    public KnowledgeJobBO insertJob(KnowledgeJobBO job) {
        KnowledgeJobPO row = jobPersistenceConverter.newRow(job);
        row.setId(null);
        row.setRevision(FIRST_REVISION);
        try {
            requireSaved(jobPersistenceRepository.save(row), "gateway_knowledge_job", "YUHENG_ADMIN_KNOWLEDGE_JOB_CREATE_FAILED");
        } catch (DuplicateKeyException raced) {
            log.warn("YUHENG_ADMIN_KNOWLEDGE_JOB_INTENT_RACED kbId={} type={}", job.getKbId(), job.getType(), raced);
            throw new GatewayAdminIdempotencyConflictException();
        }
        return authoritative(job, row);
    }

    /**
     * 中文说明：执行 updateJob 操作；按主键读活跃行并比对业务 revision，整行覆盖排队/重试计划等非租约列后以
     * {@code expectedRevision + 1} 走 {@code updateById} CAS；不成立或 0 行返回 {@code false}。
     * 运行中作业的结果写回绝不走这里，必须经 {@link #finish(KnowledgeJobBO, long)} 的租约令牌 CAS。
     * English summary: Executes the updateJob operation; the active row is loaded by primary key and its business revision
     * compared, the non-lease columns of queueing and retry planning are replaced and the {@code updateById} CAS performed at
     * {@code expectedRevision + 1}, with an unmet precondition or a zero-row effect returning {@code false}. The result of a
     * running job never travels here but through {@link #finish(KnowledgeJobBO, long)}'s lease-token CAS.
     *
     * 用法 / Usage: {@code knowledgeRepository.updateJob(job, expectedRevision)}；重试后继行的插入用
     * {@link #insertJob(KnowledgeJobBO)}。
     * @param job 完整替换载体；the full replacement carrier.
     * @param expectedRevision 调用方期望的当前 revision；the caller-observed revision.
     * @return CAS 是否命中；whether the compare-and-set hit.
     */
    @Override
    public boolean updateJob(
            KnowledgeJobBO job,
            long expectedRevision) {
        KnowledgeJobPO row = jobPersistenceRepository.getById(idOf(job.getId()));
        if (row == null || revisionOf(row.getRevision()) != expectedRevision) {
            log.info("YUHENG_ADMIN_KNOWLEDGE_JOB_CAS_MISSED jobId={} expected={}", job.getId(), expectedRevision);
            return false;
        }
        jobPersistenceConverter.applyBusiness(job, row);
        row.setRevision(expectedRevision + 1);
        if (!jobPersistenceRepository.updateById(row)) {
            log.info("YUHENG_ADMIN_KNOWLEDGE_JOB_CAS_LOST jobId={}", job.getId());
            return false;
        }
        authoritative(job, row);
        return true;
    }

    /**
     * 中文说明：执行 listJobs 操作；受守卫读取该知识库下活跃作业当页，{@code status} 为空时不加状态谓词，
     * 次序固定 {@code create_time DESC, id DESC}。
     * English summary: Executes the listJobs operation; the guarded page of one base's active jobs with no status predicate when
     * {@code status} is null, ordered {@code create_time DESC, id DESC}.
     *
     * 用法 / Usage: {@code knowledgeRepository.listJobs(kbId, status, page, size)}；总数由
     * {@link #countJobs(String, KnowledgeJobStatusEnum)} 以同一谓词配对。
     * @param kbId 知识库十进制字符串 id；decimal-string knowledge base id.
     * @param status 可选状态过滤；optional status filter.
     * @param page 页码，从 1 开始；one-based page number.
     * @param size 页大小；effective page size.
     * @return 当页作业业务载体；the job carriers of that page.
     */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<KnowledgeJobBO> listJobs(
            String kbId,
            KnowledgeJobStatusEnum status,
            int page,
            int size) {
        return jobPersistenceConverter.toBusinessList(
                jobPersistenceRepository.list(
                        new Page<KnowledgeJobPO>(page, bounded(size), false),
                        jobsOf(kbId, status)
                )
        );
    }

    /**
     * 中文说明：执行 countJobs 操作；与作业当页完全同谓词（含可选状态过滤）的受守卫活跃计数。
     * English summary: Executes the countJobs operation; the guarded active count under the job page's own predicate, the
     * optional status filter included.
     *
     * 用法 / Usage: {@code knowledgeRepository.countJobs(kbId, status)}。
     * @param kbId 知识库十进制字符串 id；decimal-string knowledge base id.
     * @param status 可选状态过滤；optional status filter.
     * @return 匹配的作业总数；the number of matched jobs.
     */
    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public long countJobs(
            String kbId,
            KnowledgeJobStatusEnum status) {
        return jobPersistenceRepository.count(jobsOf(kbId, status));
    }

    /**
     * 中文说明：执行 claimNext 操作；具名语句 {@code selectClaimable} + {@code claimJob} 的端口。
     * 先以 {@code FOR UPDATE SKIP LOCKED LIMIT slots} 锁定本轮候选（{@code next_attempt_at, id} 升序），
     * 再逐行发起带观察态复核的认领 CAS：新 {@code lease_token} 取「观察值 + 1」使重启单调递增，租约时长取
     * {@code yuheng.knowledge.lease}，
     * {@code lease_owner} 为本 JVM 实例身份；只有返回 1 的行才进入结果，因此认领不到就是少一行，
     * 绝不伪造。返回的载体携带刚写入的租约三列与推进后的 revision。
     * English summary: Executes the claimNext operation, the port of {@code selectClaimable} plus {@code claimJob}: the round's
     * candidates are first locked with {@code FOR UPDATE SKIP LOCKED LIMIT slots} ascending by
     * {@code next_attempt_at, id}, then each row meets its own claim CAS re-asserting the observed state — the new
     * {@code lease_token} being "observed plus one" so a restart advances it monotonically, the lease taken from
     * {@code yuheng.knowledge.lease} and
     * {@code lease_owner} this JVM's instance identity. Only rows answering one enter the result, so an unclaimed row is simply
     * one row fewer and never a fabrication, and every returned carrier holds the freshly written lease triple plus the
     * advanced revision.
     *
     * 用法 / Usage: {@code knowledgeRepository.claimNext(slots)}；<b>必须</b>在调用方 {@code gatewayTransactionManager}
     * 事务内执行，行锁才在认领写回提交前一直持有；worker 只在有空闲槽位时认领，并且必须已在守卫上下文中恢复
     * 可信租户与 actor——本方法没有租户入参。
     * @param slots 本次认领上限；claim bound for this round.
     * @return 已认领并持租约的作业载体列表；the claimed job carriers holding a lease.
     */
    @Override
    public List<KnowledgeJobBO> claimNext(int slots) {
        if (slots <= 0) {
            return List.of();
        }
        Instant now = now();
        List<KnowledgeJobPO> candidates = knowledgeJobDAO.selectClaimable(now, slots);
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<KnowledgeJobBO> claimed = new ArrayList<>(candidates.size());
        for (KnowledgeJobPO row : candidates) {
            long token = leaseTokenOf(row) + 1;
            Instant expiresAt = now.plus(knowledgeProperties.getLease());
            if (knowledgeJobDAO.claimJob(row, WORKER_IDENTITY, token, expiresAt, now) != 1) {
                log.info("YUHENG_ADMIN_KNOWLEDGE_JOB_CLAIM_LOST jobId={} observedToken={}", row.getId(), row.getLeaseToken());
                continue;
            }
            row.setStatus(KnowledgeJobStatusEnum.RUNNING.wireValue());
            row.setLeaseOwner(WORKER_IDENTITY);
            row.setLeaseToken(token);
            row.setLeaseExpiresAt(expiresAt);
            row.setRevision(revisionOf(row.getRevision()) + 1);
            claimed.add(jobPersistenceConverter.toBusiness(row));
        }
        log.info("YUHENG_ADMIN_KNOWLEDGE_JOB_CLAIMED candidates={} claimed={} worker={}", candidates.size(), claimed.size(), WORKER_IDENTITY);
        return claimed;
    }

    /**
     * 中文说明：执行 heartbeat 操作；具名语句 {@code heartbeatJob} 的端口：按主键读活跃行，令牌或运行态在读取处
     * 已不成立即返回 {@code false}，否则把刚读到的行交给 CAS 顺延 {@code lease_expires_at}；
     * 影响 0 行同样是 {@code false}——所有权已丢失，调用方必须停止工作并放弃发布。心跳不推进业务 revision，
     * 也不改 {@code stage}/{@code attempt}。
     * English summary: Executes the heartbeat operation, the port of {@code heartbeatJob}: the active row is read by primary key,
     * a token or running state that already fails at load time returning {@code false}, otherwise the freshly read row is
     * handed to the CAS extending {@code lease_expires_at}. A zero-row effect is likewise {@code false} — ownership is gone
     * and the caller must stop working and abandon publication. A heartbeat advances neither the business revision nor
     * {@code stage}/{@code attempt}.
     *
     * 用法 / Usage: {@code knowledgeRepository.heartbeat(jobId, leaseToken, leaseExpiresAt)}；30 秒周期对应 120 秒租约，
     * 长外部调用要在阶段边界补心跳；{@code false} 之后不得再发布任何结果。
     * @param jobId 作业十进制字符串 id；decimal-string job id.
     * @param leaseToken 认领时获得的租约令牌；lease token acquired at claim time.
     * @param leaseExpiresAt 顺延后的到期时刻；the extended expiry instant.
     * @return 所有权是否仍然成立；whether ownership still holds.
     */
    @Override
    public boolean heartbeat(
            String jobId,
            long leaseToken,
            Instant leaseExpiresAt) {
        KnowledgeJobPO row = jobPersistenceRepository.getById(idOf(jobId));
        if (row == null || row.getLeaseToken() == null || row.getLeaseToken() != leaseToken
                || !KnowledgeJobStatusEnum.RUNNING.wireValue().equals(row.getStatus())) {
            log.info("YUHENG_ADMIN_KNOWLEDGE_JOB_HEARTBEAT_NO_LEASE jobId={} token={}", jobId, leaseToken);
            return false;
        }
        boolean held = knowledgeJobDAO.heartbeatJob(row, leaseToken, leaseExpiresAt, now()) == 1;
        if (!held) {
            log.info("YUHENG_ADMIN_KNOWLEDGE_JOB_HEARTBEAT_CAS_LOST jobId={} token={}", jobId, leaseToken);
        }
        return held;
    }

    /**
     * 中文说明：执行 finish 操作；具名语句 {@code finishJob} 的端口：按主键读活跃行后，只把载体给出的终态五列
     * （{@code status}/{@code stage}/{@code attempt}/{@code nextAttemptAt}/{@code errorCode}/{@code result}）覆写到该行上，
     * 令租约三列与技术 {@code version} 保持观察值，再交语句以 {@code lease_token + status='RUNNING' +
     * lease_expires_at > now} 为条件写回；0 行返回 {@code false}，即本次执行结果作废，过期租约不留下任何状态。
     * English summary: Executes the finish operation, the port of {@code finishJob}: after the active row is read by primary key,
     * only the terminal columns the carrier offers ({@code status}/{@code stage}/{@code attempt}/{@code nextAttemptAt}/
     * {@code errorCode}/{@code result}) are copied onto that row so the three lease columns and the technical {@code version}
     * keep their observed values, and the statement then writes them back under {@code lease_token + status='RUNNING' +
     * lease_expires_at > now}. A zero-row effect returns {@code false}, voiding this execution's outcome: a lapsed lease leaves
     * no state behind.
     *
     * 用法 / Usage: {@code knowledgeRepository.finish(job, leaseToken)}；发布之后仍以本方法收口作业状态，
     * 返回 {@code false} 时调用方丢弃本地结论回到轮询，绝不重试覆盖新持有者的写入。
     * @param job 携带终态的作业载体；the job carrier holding the terminal state.
     * @param leaseToken 认领时获得的租约令牌；lease token acquired at claim time.
     * @return 终态是否由本方写入；whether this side wrote the terminal state.
     */
    @Override
    public boolean finish(
            KnowledgeJobBO job,
            long leaseToken) {
        KnowledgeJobPO row = jobPersistenceRepository.getById(idOf(job.getId()));
        if (row == null) {
            log.info("YUHENG_ADMIN_KNOWLEDGE_JOB_FINISH_ABSENT jobId={}", job.getId());
            return false;
        }
        if (job.getStatus() == null || job.getStage() == null) {
            throw validation("a terminal job write-back requires both status and stage");
        }
        row.setStatus(job.getStatus().wireValue());
        row.setStage(job.getStage().wireValue());
        row.setAttempt(job.getAttempt() == null ? row.getAttempt() : job.getAttempt());
        row.setNextAttemptAt(job.getNextAttemptAt() == null ? row.getNextAttemptAt() : job.getNextAttemptAt());
        row.setErrorCode(job.getErrorCode());
        row.setResult(job.getResult());
        boolean written = knowledgeJobDAO.finishJob(row, leaseToken, now()) == 1;
        log.info("YUHENG_ADMIN_KNOWLEDGE_JOB_FINISHED jobId={} status={} written={}", job.getId(), job.getStatus(), written);
        return written;
    }

    /**
     * 中文说明：把一个待暂存分块校验并渲染为待插入行：归属必须等于给定的 {@code kbId}/{@code revisionId}，
     * {@code chunkIndex} 必须非负且不重复，嵌入空间与维度必须等于知识库已冻结的值，向量必须恰为
     * {@code dimensions} 长、全部有限且非全零；违规在写入之前按 422 如实抛出。
     * English summary: Validates one chunk to be staged and renders it as the row to insert: ownership must equal the given
     * {@code kbId}/{@code revisionId}, {@code chunkIndex} must be non-negative and duplicate-free, the embedding space and
     * dimensions must equal the knowledge base's frozen values, and the vector must be exactly {@code dimensions} long, fully
     * finite and not all zero; a violation raises 422 before any write.
     * @param chunk 参数 分块载体；parameter the chunk carrier.
     * @param base 参数 已冻结嵌入空间与维度的知识库行；parameter the base row freezing space and dimensions.
     * @param baseKey 参数 知识库主键；parameter knowledge base key.
     * @param revisionKey 参数 修订主键；parameter revision key.
     * @param indexes 参数 已见 {@code chunkIndex} 集合，本方法登记新值；parameter the seen index set, extended by this call.
     * @return 返回 待插入分块行；returns the chunk row to insert.
     */
    private KnowledgeChunkPO stagedChunkRow(
            KnowledgeChunkBO chunk,
            KnowledgeBasePO base,
            long baseKey,
            long revisionKey,
            Set<Integer> indexes) {
        if (!Objects.equals(idOf(chunk.getKbId()), baseKey) || !Objects.equals(idOf(chunk.getRevisionId()), revisionKey)) {
            throw validation("a staged chunk must belong to the given knowledge base and revision");
        }
        Integer position = chunk.getChunkIndex();
        if (position == null || position < 0 || !indexes.add(position)) {
            throw validation("every staged chunk needs a distinct non-negative chunk index");
        }
        Integer dimensions = base.getDimensions();
        if (dimensions == null || !Objects.equals(chunk.getDimensions(), dimensions)
                || !Objects.equals(chunk.getEmbeddingSpaceId(), base.getEmbeddingSpaceId())) {
            throw validation("a staged chunk must carry the knowledge base's frozen embedding space and dimensions");
        }
        float[] vector = chunk.getEmbedding();
        if (vector == null || vector.length != dimensions) {
            throw validation("an embedding vector must be exactly the frozen number of dimensions long");
        }
        boolean nonZero = false;
        for (float value : vector) {
            if (!Float.isFinite(value)) {
                throw validation("an embedding vector must be finite in every component");
            }
            nonZero |= value != 0f;
        }
        if (!nonZero) {
            throw validation("an embedding vector must not be all zero");
        }
        KnowledgeChunkPO row = chunkPersistenceConverter.newRow(chunk);
        row.setId(null);
        row.setRevision(FIRST_REVISION);
        return row;
    }

    /**
     * 中文说明：知识库成员可见谓词：owner 等值，或 {@code members} jsonb 包含该 actor 的成员条目；
     * 探测文档由 Jackson 节点工厂构造后作为绑定参数传入（不拼接字面量），租户与活跃谓词由受守卫边界追加。
     * English summary: The membership predicate of a knowledge base: the owner equals this actor, or the {@code members} jsonb
     * column contains a member entry for them; the probe document is built by the Jackson node factory and arrives as a bound
     * parameter (never concatenated text) while the boundary contributes the tenant and active-row predicates.
     * @param actorId 参数 actor 稳定标识；parameter actor identifier as persisted.
     * @return 返回 可见知识库查询；returns the query over visible knowledge bases.
     */
    private static LambdaQueryWrapper<KnowledgeBasePO> visibleBases(String actorId) {
        ObjectNode entry = JsonNodeFactory.instance.objectNode().put("actorId", actorId);
        ArrayNode probe = JsonNodeFactory.instance.arrayNode().add(entry);
        return Wrappers.<KnowledgeBasePO>lambdaQuery()
                .and(scope -> scope.eq(KnowledgeBasePO::getOwnerActorId, actorId)
                        .or()
                        .apply("members @> CAST({0} AS jsonb)", probe.toString()))
                .orderByDesc(KnowledgeBasePO::getCreateTime)
                .orderByDesc(KnowledgeBasePO::getId);
    }

    /**
     * 中文说明：某知识库下活跃文档的稳定分页谓词。
     * English summary: The stable paging predicate over one base's active documents.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @return 返回 文档查询；returns the document query.
     */
    private LambdaQueryWrapper<KnowledgeDocumentPO> documentsOf(String kbId) {
        return Wrappers.<KnowledgeDocumentPO>lambdaQuery()
                .eq(KnowledgeDocumentPO::getKbId, idOf(kbId))
                .orderByDesc(KnowledgeDocumentPO::getCreateTime)
                .orderByDesc(KnowledgeDocumentPO::getId);
    }

    /**
     * 中文说明：某知识库下活跃作业的稳定分页谓词，{@code status} 为空时不加状态条件。
     * English summary: The stable paging predicate over one base's active jobs, with no status condition when {@code status}
     * is null.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param status 参数 可选状态过滤；parameter optional status filter.
     * @return 返回 作业查询；returns the job query.
     */
    private LambdaQueryWrapper<KnowledgeJobPO> jobsOf(
            String kbId,
            KnowledgeJobStatusEnum status) {
        return Wrappers.<KnowledgeJobPO>lambdaQuery()
                .eq(KnowledgeJobPO::getKbId, idOf(kbId))
                .eq(status != null, KnowledgeJobPO::getStatus, status == null ? null : status.wireValue())
                .orderByDesc(KnowledgeJobPO::getCreateTime)
                .orderByDesc(KnowledgeJobPO::getId);
    }

    /**
     * 中文说明：把端口上的十进制字符串 id 换算为 {@code long} 主键，是本类唯一的 id 换算入口：
     * 空值、非数字、非正数或超出 19 位一律按 {@code 422 KNOWLEDGE_VALIDATION_FAILED} 如实抛出，
     * 绝不静默降级为 0 或按前缀猜测。
     * English summary: Converts a port-side decimal-string id into the {@code long} primary key and is this class's single
     * conversion entry point: blank, non-numeric, non-positive or over-long input raises {@code 422
     * KNOWLEDGE_VALIDATION_FAILED} honestly, never degrading to zero nor guessing from a prefix.
     * @param value 参数 十进制字符串 id；parameter decimal-string identifier.
     * @return 返回 主键；returns the primary key.
     */
    private static long idOf(String value) {
        if (value == null || value.isBlank() || value.length() > 20) {
            throw validation("an identifier must be a decimal string of 1 to 20 digits");
        }
        long parsed;
        try {
            parsed = Long.parseLong(value);
        } catch (NumberFormatException malformed) {
            throw validation("an identifier must be a decimal string of 1 to 20 digits");
        }
        if (parsed <= 0) {
            throw validation("an identifier must be a positive decimal string");
        }
        return parsed;
    }

    /**
     * 中文说明：读取可空 bigint 业务 revision，缺失按 0（未盖章行）处理。
     * English summary: Reads a nullable bigint business revision, an absent value being zero (an unstamped row).
     * @param revision 参数 列值；parameter column value.
     * @return 返回 业务 revision；returns the business revision.
     */
    private static long revisionOf(Long revision) {
        return revision == null ? CREATE_REVISION : revision;
    }

    /**
     * 中文说明：读取认领前的租约令牌，缺失按 0 处理，因此首次认领得到的令牌是 1。
     * English summary: Reads the lease token observed before a claim, an absent value being zero so the first claim receives
     * token one.
     * @param row 参数 观察到的作业行；parameter the observed job row.
     * @return 返回 观察到的令牌；returns the observed token.
     */
    private static long leaseTokenOf(KnowledgeJobPO row) {
        return row.getLeaseToken() == null ? CREATE_REVISION : row.getLeaseToken();
    }

    /**
     * 中文说明：把页大小收敛到扫描上界，端口注解已给出 1–100，此处只是防御性再夹一次。
     * English summary: Clamps a page size to the scan bound, the port's annotations already enforcing 1–100 so this is a
     * defensive second clamp.
     * @param size 参数 请求页大小；parameter requested page size.
     * @return 返回 生效页大小；returns the effective page size.
     */
    private static int bounded(int size) {
        return Math.min(Math.max(size, 1), SCAN_PAGE_SIZE);
    }

    /** 中文说明：本轮语句的时刻基准；认领/心跳/终态的可认领性与租约有效性都以数据库此刻为准。 English summary: This round's clock for the statements; claimability and lease validity are judged against the database's now. */
    private static Instant now() {
        return Instant.now();
    }

    /**
     * 中文说明：构造本 JVM 的 worker 实例身份（进程标识 + 主机名，截断至 128 字符），写入 {@code lease_owner}；
     * 不含密钥、租户与作业内容。
     * English summary: Builds this JVM's worker identity (process plus host, truncated to 128 characters) written into
     * {@code lease_owner}; never a secret, a tenant or job content.
     * @return 返回 worker 身份；returns the worker identity.
     */
    private static String workerIdentity() {
        String runtime = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        String host;
        try {
            host = java.net.InetAddress.getLocalHost().getHostName();
        } catch (java.io.IOException unavailable) {
            host = "unknown-host";
        }
        String identity = "yuheng-admin:" + host + ":" + runtime;
        return identity.length() <= 128 ? identity : identity.substring(0, 128);
    }

    /**
     * 中文说明：受守卫写入的行数裁决：{@code false}（影响 0 行）按内部错误抛出并回滚调用方事务，
     * 绝不把「什么都没写」当作创建成功。
     * English summary: The row-count verdict of a guarded write: {@code false} (zero affected rows) raises an internal error that
     * rolls back the caller's transaction instead of treating "nothing written" as a successful create.
     * @param written 参数 受守卫边界的写入结论；parameter the boundary's write verdict.
     * @param table 参数 逻辑表名（仅日志与错误细节）；parameter logical table (log and detail only).
     * @param code 参数 稳定机读码；parameter stable machine code.
     */
    private static void requireSaved(
            boolean written,
            String table,
            String code) {
        if (!written) {
            log.error("{} table={}", code, table);
            throw new IllegalStateException(code);
        }
    }

    /**
     * 中文说明：构造 {@code 422 KNOWLEDGE_VALIDATION_FAILED}，用于 id 形态与暂存分块契约违规。
     * English summary: Builds the {@code 422 KNOWLEDGE_VALIDATION_FAILED} raised for identifier shape and staged-chunk contract
     * violations.
     * @param detail 参数 稳定可读说明（不含正文与向量）；parameter a stable readable detail (no content, no vectors).
     * @return 返回 待抛出的异常；returns the exception to throw.
     */
    private static CommonException validation(String detail) {
        return new CommonException(422, "KNOWLEDGE_VALIDATION_FAILED", detail);
    }

    /**
     * 中文说明：构造 {@code 404 KNOWLEDGE_RESOURCE_NOT_FOUND}，用于写入路径上缺失的父资源（知识库/修订）——
     * 缺失的归属关系不能靠返回 0 伪装成成功。
     * English summary: Builds the {@code 404 KNOWLEDGE_RESOURCE_NOT_FOUND} of a missing parent (base or revision) on a write
     * path, an unestablished ownership never being allowed to masquerade as success through a zero return.
     * @param table 参数 逻辑表名；parameter logical table.
     * @param id 参数 十进制字符串 id；parameter decimal-string identifier.
     * @return 返回 待抛出的异常；returns the exception to throw.
     */
    private static CommonException notFound(
            String table,
            String id) {
        return new CommonException(404, "KNOWLEDGE_RESOURCE_NOT_FOUND", table + " " + id + " is absent in this tenant");
    }

    /**
     * 中文说明：把仓储侧权威的技术与业务值回写进入参载体并返回同一对象：只同步不透明主键、权威
     * {@code revision} 与审计时刻，业务列以载体为准。
     * English summary: Copies the repository-authoritative technical and business values back into the carrier and returns that
     * same object, syncing only the opaque identifier, the authoritative {@code revision} and the audit instants while the
     * business columns stay as the carrier holds them.
     * @param carrier 参数 入参载体；parameter the incoming carrier.
     * @param row 参数 仓储侧行；parameter the repository-side row.
     * @return 返回 权威载体；returns the authoritative carrier.
     */
    private static KnowledgeBaseBO authoritative(
            KnowledgeBaseBO carrier,
            KnowledgeBasePO row) {
        carrier.setId(row.getId() == null ? null : Long.toString(row.getId()));
        carrier.setRevision(revisionOf(row.getRevision()));
        carrier.setCreatedAt(row.getCreateTime());
        carrier.setUpdatedAt(row.getUpdateTime());
        log.debug("gateway_knowledge_base {} resolved to authoritative revision {}", carrier.getId(), row.getRevision());
        return carrier;
    }

    /**
     * 中文说明：文档侧的权威值回写，语义与知识库侧一致。
     * English summary: The document-side authoritative write-back, with the knowledge-base side's semantics.
     * @param carrier 参数 入参载体；parameter the incoming carrier.
     * @param row 参数 仓储侧行；parameter the repository-side row.
     * @return 返回 权威载体；returns the authoritative carrier.
     */
    private static KnowledgeDocumentBO authoritative(
            KnowledgeDocumentBO carrier,
            KnowledgeDocumentPO row) {
        carrier.setId(row.getId() == null ? null : Long.toString(row.getId()));
        carrier.setRevision(revisionOf(row.getRevision()));
        carrier.setCreatedAt(row.getCreateTime());
        carrier.setUpdatedAt(row.getUpdateTime());
        return carrier;
    }

    /**
     * 中文说明：修订侧的权威值回写；只同步 id、revision 与审计时刻，原始字节与抽取文本不回写也不落日志。
     * English summary: The revision-side authoritative write-back, syncing only id, revision and audit instants while the raw
     * bytes and extracted text travel neither back into the carrier nor into a log line.
     * @param carrier 参数 入参载体；parameter the incoming carrier.
     * @param row 参数 仓储侧行；parameter the repository-side row.
     * @return 返回 权威载体；returns the authoritative carrier.
     */
    private static KnowledgeDocumentRevisionBO authoritative(
            KnowledgeDocumentRevisionBO carrier,
            KnowledgeDocumentRevisionPO row) {
        carrier.setId(row.getId() == null ? null : Long.toString(row.getId()));
        carrier.setRevision(revisionOf(row.getRevision()));
        carrier.setCreatedAt(row.getCreateTime());
        carrier.setUpdatedAt(row.getUpdateTime());
        return carrier;
    }

    /**
     * 中文说明：作业侧的权威值回写；租约三列以库中为准，因此认领后的行也经本方法把令牌带给调用方。
     * English summary: The job-side authoritative write-back, the three lease columns being authoritative in the database, so a
     * claimed row hands its token to the caller through this method too.
     * @param carrier 参数 入参载体；parameter the incoming carrier.
     * @param row 参数 仓储侧行；parameter the repository-side row.
     * @return 返回 权威载体；returns the authoritative carrier.
     */
    private static KnowledgeJobBO authoritative(
            KnowledgeJobBO carrier,
            KnowledgeJobPO row) {
        carrier.setId(row.getId() == null ? null : Long.toString(row.getId()));
        carrier.setRevision(revisionOf(row.getRevision()));
        carrier.setLeaseToken(row.getLeaseToken());
        carrier.setLeaseOwner(row.getLeaseOwner());
        carrier.setLeaseExpiresAt(row.getLeaseExpiresAt());
        carrier.setCreatedAt(row.getCreateTime());
        carrier.setUpdatedAt(row.getUpdateTime());
        return carrier;
    }
}
