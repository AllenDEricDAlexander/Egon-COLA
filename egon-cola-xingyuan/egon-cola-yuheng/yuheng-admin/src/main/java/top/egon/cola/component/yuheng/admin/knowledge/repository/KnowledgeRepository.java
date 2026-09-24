package top.egon.cola.component.yuheng.admin.knowledge.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeBaseBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeChunkBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeDocumentBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeDocumentRevisionBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeJobBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobStatusEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobTypeEnum;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 中文说明：{@code KnowledgeRepository} 是知识库聚合（base / document / revision / chunk / job 五张表）的业务数据端口，
 * 只声明同域 Service 与作业执行器真正需要的具名读取与命令：按十进制字符串 id 的单行读取、成员可见的稳定分页当页与配对总数、
 * 首版插入、以 {@code expectedRevision} 为条件的乐观锁整行替换、活动版本切换、修订的暂存分块写入、
 * 意图（{@code kbId + actorId + type + idempotencyKey}）幂等回读，以及作业认领/心跳/终态写回这一组租约 CAS。
 * 它不继承任何泛型 CRUD，也不把 {@code KnowledgeBasePO}/{@code KnowledgeDocumentPO}/{@code KnowledgeDocumentRevisionPO}/
 * {@code KnowledgeChunkPO}/{@code KnowledgeJobPO} 行模型泄漏到签名上；id 一律是 {@code ^[1-9][0-9]{0,19}$} 的十进制字符串，
 * Long 与 String 的换算只发生在实现内部。
 * English summary: {@code KnowledgeRepository} is the business data port of the knowledge aggregate (the base, document,
 * revision, chunk and job tables), declaring only the named reads and commands the same-domain services and the job worker
 * actually use: a single-row read by decimal-string id, the page slice of the member-visible stable order plus its paired
 * total, the first-version insert, a full replace conditioned on {@code expectedRevision}, the active-revision switch, the
 * staged chunk write of one revision, the idempotent re-read by intent ({@code kbId + actorId + type + idempotencyKey}),
 * and the claim/heartbeat/terminal write-back lease CAS; it inherits no generic CRUD and never lets a
 * {@code KnowledgeBasePO}/{@code KnowledgeDocumentPO}/{@code KnowledgeDocumentRevisionPO}/{@code KnowledgeChunkPO}/
 * {@code KnowledgeJobPO} row model reach a signature. Ids are always {@code ^[1-9][0-9]{0,19}$} decimal strings and the
 * Long/String conversion stays inside the implementation.
 *
 * 用法 / Usage: 由 {@code MpKnowledgeRepository}（bean 名 {@code knowledgeRepository}）在调用方
 * {@code gatewayTransactionManager} 事务内以受守卫的 MP 读写实现。
 * 租户与操作者取自守卫上下文（MDC {@code tenantId}/{@code userId}）而不是入参，本端口没有任何自报租户的入口，
 * 跨租户行与软删行一律按不存在处理（读取返回空 {@code Optional}/空列表，由业务层如实转 404
 * {@code KNOWLEDGE_RESOURCE_NOT_FOUND}；越权身份由业务层转 403 {@code KNOWLEDGE_FORBIDDEN}，端口本身不做角色判断）。
 * 所有写方法的成功判定都是行CAS裁决：布尔 {@code false} 或 {@code 0} 表示影响 0 行，即期望修订、租约令牌或运行状态不再成立，
 * 它不是成功，调用方必须放弃发布并按 {@code 409 KNOWLEDGE_REVISION_CONFLICT}（携带库中现值）或“所有权已丢失”重投处理，
 * 绝不允许把 0 行包装成成功。作业三方法（{@link #claimNext(int)}/{@link #heartbeat(String, long, Instant)}/
 * {@link #finish(KnowledgeJobBO, long)}）对应具名 mapper 语句 {@code selectClaimable}/{@code claimJob}/
 * {@code heartbeatJob}/{@code finishJob}，{@link #activateRevision(String, String, String, long)} 对应
 * {@code activateRevision}；其余写入走 {@code Wrappers.lambdaQuery()/lambdaUpdate()} 与受守卫 DAO，绝不拼接原始 SQL。
 * / {@code MpKnowledgeRepository} (bean name {@code knowledgeRepository}) implements it through guarded MyBatis-Plus reads
 * and writes inside the caller's {@code gatewayTransactionManager} transaction. Tenancy and operator come from the guarded
 * context (MDC {@code tenantId}/{@code userId}) rather than an argument, so the port has no self-reported tenant at all; a
 * foreign-tenant or soft-deleted row reads as absent (an empty {@code Optional}/list that the owning service turns into
 * 404 {@code KNOWLEDGE_RESOURCE_NOT_FOUND}, while a forbidden actor becomes 403 {@code KNOWLEDGE_FORBIDDEN} — the port
 * itself makes no role decision). Every write is decided by a row CAS: {@code false} or {@code 0} means zero affected
 * rows, i.e. the expected revision, lease token or running state no longer holds, which is not success — the caller must
 * abandon publication and map it to {@code 409 KNOWLEDGE_REVISION_CONFLICT} carrying the stored revision, or to “ownership
 * lost” and re-poll, and fabricating success from a zero-row effect is forbidden. The three job methods map onto the named
 * statements {@code selectClaimable}/{@code claimJob}/{@code heartbeatJob}/{@code finishJob} and revision activation onto
 * {@code activateRevision}; the remaining writes use {@code Wrappers.lambdaQuery()/lambdaUpdate()} plus the guarded DAOs,
 * never concatenated SQL.
 */
@Validated
public interface KnowledgeRepository {

    /**
     * 中文说明：执行 findBase 操作；按主键在守卫租户的活跃知识库集合内定位至多一行，
     * 软删行与跨租户行一律视为不存在，成员可见性不在此判断（那是业务层的角色复核）。
     * English summary: Executes the findBase operation; locates at most one row by primary key inside the guarded tenant's
     * active knowledge-base set, treating a soft-deleted or foreign-tenant row as absent, and makes no membership decision
     * (that role check belongs to the owning service).
     *
     * 用法 / Usage: {@code knowledgeRepository.findBase(kbId)}；未命中返回 {@code Optional.empty()}，
     * 由调用方如实转 404，绝不返回零值占位载体。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @return 返回 知识库业务载体；returns the knowledge base carrier when present.
     */
    Optional<KnowledgeBaseBO> findBase(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId
    );

    /**
     * 中文说明：执行 listBasesOfActor 操作；读取该 actor 作为 owner 或成员可见的活跃知识库当页，
     * 次序固定 {@code create_time DESC, id DESC}（并列时间戳由主键稳定），不返回总数。
     * English summary: Executes the listBasesOfActor operation; reads the active knowledge bases visible to this actor as
     * owner or member in the fixed {@code create_time DESC, id DESC} order, where equal timestamps are stabilised by the
     * primary key, and reports no total.
     *
     * 用法 / Usage: {@code knowledgeRepository.listBasesOfActor(actorId, page, size)}；page 从 1 开始，
     * size 有效范围 1–100，空页返回 {@code []}，总数由 {@link #countBasesOfActor(String)} 同谓词配对给出。
     * @param actorId 参数 actor 稳定标识（varchar(128)）；parameter actor identifier as persisted (varchar(128)).
     * @param page 参数 页码，从 1 开始；parameter one-based page number.
     * @param size 参数 页大小，1–100；parameter effective page size.
     * @return 返回 当页知识库业务载体；returns the knowledge base carriers of that page.
     */
    List<KnowledgeBaseBO> listBasesOfActor(
            @NotBlank
            @Size(max = 128) String actorId,
            @Min(1) int page,
            @Min(1)
            @Max(100) int size
    );

    /**
     * 中文说明：执行 countBasesOfActor 操作；给出与 {@link #listBasesOfActor(String, int, int)} 完全同谓词的匹配总数，
     * 使分页响应携带真实 total 而不是当页条数。
     * English summary: Executes the countBasesOfActor operation; reports the matched total under exactly the same predicate as
     * {@link #listBasesOfActor(String, int, int)} so the page response carries a real total rather than the slice size.
     *
     * 用法 / Usage: {@code knowledgeRepository.countBasesOfActor(actorId)}；与当页查询同事务读取，口径一致。
     * @param actorId 参数 actor 稳定标识（varchar(128)）；parameter actor identifier as persisted (varchar(128)).
     * @return 返回 可见的活跃知识库总数；returns the number of visible active knowledge bases.
     */
    long countBasesOfActor(
            @NotBlank
            @Size(max = 128) String actorId
    );

    /**
     * 中文说明：执行 insertBase 操作；以权威 {@code revision = 1} 走受守卫插入首版知识库行，
     * 租户、审计列（{@code create_user_id}/{@code create_time}/{@code update_user_id}/{@code update_time}）与技术
     * {@code version} 全部由边界补齐，业务 {@code revision} 与 {@code embeddingSpaceId}/{@code dimensions} 的冻结值
     * 由入参载体携带（嵌入空间与维度在首次上传时定下，之后不得原地改变）；唯一键竞争按冲突如实抛出而不静默覆盖。
     * English summary: Executes the insertBase operation; performs a guarded insert of the first knowledge-base row at the
     * authoritative {@code revision = 1}, letting the boundary stamp tenant, audit columns
     * ({@code create_user_id}/{@code create_time}/{@code update_user_id}/{@code update_time}) and the technical
     * {@code version}, while the frozen business {@code revision}, {@code embeddingSpaceId} and {@code dimensions} travel in
     * the given carrier (the embedding space and dimensions are fixed at first upload and never changed in place); a
     * unique-key race surfaces honestly as a conflict instead of a silent overwrite.
     *
     * 用法 / Usage: {@code knowledgeRepository.insertBase(base)}；返回插入后的权威载体（同一实例携带服务端补齐的
     * id 与审计时刻）；调用方必须处于自己的写事务内，端口不开启事务。
     * @param base 参数 待插入的知识库载体；parameter the knowledge base carrier to insert.
     * @return 返回 已提交的知识库业务载体；returns the committed knowledge base carrier.
     */
    KnowledgeBaseBO insertBase(@Valid @NotNull KnowledgeBaseBO base);

    /**
     * 中文说明：执行 replaceBase 操作；等价于
     * {@code UPDATE gateway_knowledge_base SET ... WHERE id = :id AND tenant_id = :tenant AND revision = :expected AND
     * deleted_at IS NULL}：命中即把业务 {@code revision} 推进为 {@code expectedRevision + 1} 并回写审计列，
     * 返回 {@code true}；影响 0 行返回 {@code false}，表示期望修订已不成立（并发改写或跨租户/软删），
     * 这是失败而不是成功，调用方须以 {@code 409 KNOWLEDGE_REVISION_CONFLICT} 携带库中现值回应。
     * English summary: Executes the replaceBase operation, the equivalent of
     * {@code UPDATE gateway_knowledge_base SET ... WHERE id = :id AND tenant_id = :tenant AND revision = :expected AND
     * deleted_at IS NULL}: a hit advances the business {@code revision} to {@code expectedRevision + 1}, refreshes the audit
     * columns and returns {@code true}, while a zero-row effect returns {@code false} meaning the expected revision no
     * longer holds (a concurrent write, or a foreign-tenant/soft-deleted row) — that is failure, not success, and the caller
     * answers {@code 409 KNOWLEDGE_REVISION_CONFLICT} carrying the stored revision.
     *
     * 用法 / Usage: {@code knowledgeRepository.replaceBase(base, expectedRevision)}；入参载体携带期望 revision 与完整
     * 业务列（null 不能清空必填列）；members 的 jsonb 与 {@code egressPolicy} 等整行替换，角色判定在业务层先行完成。
     * @param base 参数 完整替换载体；parameter the full replacement carrier.
     * @param expectedRevision 参数 调用方期望的当前 revision，正整数；parameter caller-observed revision, a positive number.
     * @return 返回 CAS 是否命中；returns whether the compare-and-set hit.
     */
    boolean replaceBase(
            @Valid @NotNull KnowledgeBaseBO base,
            @Min(1) long expectedRevision
    );

    /**
     * 中文说明：执行 findDocument 操作；按 {@code kbId + documentId} 在守卫租户的活跃文档集合内定位至多一行，
     * 其他知识库下的同 id 文档与软删文档同样视为不存在，因此本方法是跨库越权的天然屏障。
     * English summary: Executes the findDocument operation; locates at most one active document by {@code kbId + documentId}
     * inside the guarded tenant, so the same identifier under another knowledge base and a soft-deleted document both read
     * as absent, which makes this method the natural barrier against cross-base access.
     *
     * 用法 / Usage: {@code knowledgeRepository.findDocument(kbId, documentId)}；未命中返回 {@code Optional.empty()}，
     * 由业务层转 404；返回载体携带 {@code activeRevisionId}/{@code latestJobId} 与当前 {@code revision}。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param documentId 参数 文档十进制字符串 id；parameter decimal-string document id.
     * @return 返回 文档业务载体；returns the document carrier when present.
     */
    Optional<KnowledgeDocumentBO> findDocument(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String documentId
    );

    /**
     * 中文说明：执行 listDocuments 操作；读取该知识库下活跃文档的当页，
     * 次序固定 {@code create_time DESC, id DESC}，只返回当页载体。
     * English summary: Executes the listDocuments operation; reads the active document page slice of this knowledge base in the
     * fixed {@code create_time DESC, id DESC} order and returns the slice only.
     *
     * 用法 / Usage: {@code knowledgeRepository.listDocuments(kbId, page, size)}；page 从 1 开始，size 1–100，
     * 空页返回 {@code []}，总数由 {@link #countDocuments(String)} 配对给出。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param page 参数 页码，从 1 开始；parameter one-based page number.
     * @param size 参数 页大小，1–100；parameter effective page size.
     * @return 返回 当页文档业务载体；returns the document carriers of that page.
     */
    List<KnowledgeDocumentBO> listDocuments(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @Min(1) int page,
            @Min(1)
            @Max(100) int size
    );

    /**
     * 中文说明：执行 countDocuments 操作；给出与 {@link #listDocuments(String, int, int)} 同谓词的活跃文档匹配总数。
     * English summary: Executes the countDocuments operation; reports the matched total of active documents under the same
     * predicate as {@link #listDocuments(String, int, int)}.
     *
     * 用法 / Usage: {@code knowledgeRepository.countDocuments(kbId)}。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @return 返回 活跃文档总数；returns the number of active documents.
     */
    long countDocuments(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId
    );

    /**
     * 中文说明：执行 insertDocument 操作；以权威 {@code revision = 1} 受守卫插入文档主行，
     * {@code activeRevisionId} 与 {@code latestJobId} 由同事务内先写的 revision/job 结果决定，
     * 租户与审计列由边界补齐；唯一键竞争如实抛出而不覆盖既有文档。
     * English summary: Executes the insertDocument operation; performs a guarded insert of the document row at the authoritative
     * {@code revision = 1}, where {@code activeRevisionId} and {@code latestJobId} are decided by the revision/job written
     * earlier in the same transaction, tenant and audit columns come from the boundary, and a unique-key race surfaces
     * honestly instead of overwriting an existing document.
     *
     * 用法 / Usage: {@code knowledgeRepository.insertDocument(document)}；上传路径必须与
     * {@link #insertRevision(KnowledgeDocumentRevisionBO)} 和 {@link #insertJob(KnowledgeJobBO)} 处于同一事务，
     * 三行一起提交或一起回滚，且事务内绝不发起模型调用。
     * @param document 参数 待插入的文档载体；parameter the document carrier to insert.
     * @return 返回 已提交的文档业务载体；returns the committed document carrier.
     */
    KnowledgeDocumentBO insertDocument(@Valid @NotNull KnowledgeDocumentBO document);

    /**
     * 中文说明：执行 updateDocument 操作；以 {@code id + tenant_id + kb_id + revision = expected + deleted_at IS NULL}
     * 为条件整行替换文档业务列并推进 revision（例如改写 {@code latestJobId}），
     * 影响 0 行返回 {@code false} —— 修订已被他人推进或行已不可见即为失败，调用方按 409 或“放弃本次写”处理；
     * 活动版本切换不走这里，必须使用 {@link #activateRevision(String, String, String, long)}，
     * 软删也不走这里，必须使用 {@link #softDeleteDocument(String, String, long)}。
     * English summary: Executes the updateDocument operation; replaces the document's business columns and advances the revision
     * under {@code id + tenant_id + kb_id + revision = expected + deleted_at IS NULL} (rewriting {@code latestJobId}, for
     * instance), returning {@code false} on a zero-row effect — an advanced revision or a row that is no longer visible
     * means failure, which the caller answers with 409 or by abandoning the write; switching the active revision never goes
     * through here but through {@link #activateRevision(String, String, String, long)}, and neither does a soft delete,
     * which belongs to {@link #softDeleteDocument(String, String, long)}.
     *
     * 用法 / Usage: {@code knowledgeRepository.updateDocument(document, expectedRevision)}；
     * 本方法永不写 {@code deleted_at}，软删走 {@link #softDeleteDocument(String, String, long)}，历史 revision 与 job 行保持可查。
     * @param document 参数 完整替换载体；parameter the full replacement carrier.
     * @param expectedRevision 参数 调用方期望的当前 revision，正整数；parameter caller-observed revision, a positive number.
     * @return 返回 CAS 是否命中；returns whether the compare-and-set hit.
     */
    boolean updateDocument(
            @Valid @NotNull KnowledgeDocumentBO document,
            @Min(1) long expectedRevision
    );

    /**
     * 中文说明：执行 softDeleteDocument 操作（API-017 的落笔处）；走 Starter 的带版本软删
     * （{@code EgonColaRepository#removeById(entity)} → {@code deleteVersionedById}），语句为
     * {@code SET deleted_at = (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'), version = version + 1}，条件为
     * {@code id + tenant_id + deleted_at IS NULL + version = 现值}，因此这是唯一能把 {@code deleted_at}
     * 写进库的文档路径：业务列一律不改，历史 revision、chunk 与 job 行全部留在册内可查。
     * 命中返回 {@code true}；行不可见、revision 已被他人推进或谓词不成立都返回 {@code false}，
     * 调用方据此抛 409，绝不把 0 行当成功。
     * English summary: Executes the softDeleteDocument operation, the write behind API-017: it goes through the starter's
     * versioned logic delete ({@code EgonColaRepository#removeById(entity)} → {@code deleteVersionedById}), whose statement
     * is {@code SET deleted_at = (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'), version = version + 1} under
     * {@code id + tenant_id + deleted_at IS NULL + version = observed}, making this the only document path able to write
     * {@code deleted_at}: no business column is touched, so revision, chunk and job history stay readable. A hit returns
     * {@code true}; an invisible row, an advanced revision or any unmet predicate returns {@code false}, which the caller
     * answers with 409 because a zero-row effect is never success.
     *
     * 用法 / Usage: {@code knowledgeRepository.softDeleteDocument(kbId, documentId, expectedRevision)}；
     * 期望 revision 必须是调用方刚读到的值，方法内部另做版本 CAS，因此并发删除只会有一方成功。
     * @param kbId 参数 文档所属知识库十进制字符串 id；parameter decimal-string knowledge base id owning the document.
     * @param documentId 参数 文档十进制字符串 id；parameter decimal-string document id.
     * @param expectedRevision 参数 调用方观察到的当前 revision，正整数；parameter caller-observed revision, a positive number.
     * @return 返回 软删是否命中；returns whether the soft delete hit the row.
     */
    boolean softDeleteDocument(
            @NotBlank String kbId,
            @NotBlank String documentId,
            @Min(1) long expectedRevision
    );

    /**
     * 中文说明：执行 activateRevision 操作；具名 mapper 语句 {@code KnowledgeDocumentDAO.activateRevision} 的端口，
     * 把文档的 {@code active_revision_id} 切到给定 revision，同时以
     * {@code id + tenant_id + kb_id + revision = expectedRevision + deleted_at IS NULL} 为条件推进 revision：
     * 命中即发布完成（发布是 CAS 恰一次），0 行返回 {@code false} 表示已有其他发布者抢先或文档被并发修改，
     * 此时旧活动 revision 必须原样保留，绝不出现半切换。
     * English summary: Executes the activateRevision operation, the port of the named statement
     * {@code KnowledgeDocumentDAO.activateRevision}: it switches the document's {@code active_revision_id} to the given
     * revision while advancing the revision under {@code id + tenant_id + kb_id + revision = expectedRevision +
     * deleted_at IS NULL}. A hit completes publication — publishing is CAS-exactly-once — and {@code false} on zero rows
     * means another publisher won first or the document changed concurrently, in which case the previous active revision
     * stays untouched and a half-switched state never exists.
     *
     * 用法 / Usage: {@code knowledgeRepository.activateRevision(kbId, documentId, activeRevisionId, expectedRevision)}；
     * 摄取策略只在 {@code stageChunks} 与向量全部校验通过后调用，且必须先经 {@link #heartbeat(String, long, Instant)}
     * 仍持有租约；一旦 {@code false}，调用方不得重试发布，而要把终态交给 {@link #finish(KnowledgeJobBO, long)} 判定。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param documentId 参数 文档十进制字符串 id；parameter decimal-string document id.
     * @param activeRevisionId 参数 待激活的 revision 十进制字符串 id；parameter decimal-string revision id to activate.
     * @param expectedRevision 参数 调用方观察到的文档 revision，正整数；parameter caller-observed document revision.
     * @return 返回 CAS 是否命中，{@code false} 即发布权已不在本方；returns whether the CAS hit, {@code false} meaning this
     *         side no longer owns publication.
     */
    boolean activateRevision(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String documentId,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String activeRevisionId,
            @Min(1) long expectedRevision
    );

    /**
     * 中文说明：执行 findRevision 操作；按 {@code kbId + revisionId} 定位活跃修订行，
     * 返回载体包含 {@code status}/{@code chunkCount}/{@code contentHash} 以及内部
     * {@code rawBytes}/{@code extractedText}（这些内容只用于服务端处理，绝不进入任何 VO 或日志）。
     * English summary: Executes the findRevision operation; locates the active revision row by {@code kbId + revisionId} and
     * returns a carrier holding {@code status}/{@code chunkCount}/{@code contentHash} plus the internal
     * {@code rawBytes}/{@code extractedText}, which exist for server-side processing only and never reach a VO or a log line.
     *
     * 用法 / Usage: {@code knowledgeRepository.findRevision(kbId, revisionId)}；未命中返回 {@code Optional.empty()}
     * 并由业务层转 404；跨知识库引用同一 revision 也读不到。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param revisionId 参数 修订十进制字符串 id；parameter decimal-string revision id.
     * @return 返回 修订业务载体；returns the revision carrier when present.
     */
    Optional<KnowledgeDocumentRevisionBO> findRevision(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String revisionId
    );

    /**
     * 中文说明：执行 listRevisions 操作；读取该文档的活跃修订当页，次序固定 {@code create_time DESC, id DESC}，
     * 分页只给当页（本方法不贡献总数，修订列表的原合同只要求有序集合）。
     * English summary: Executes the listRevisions operation; reads the active revision page slice of one document in the fixed
     * {@code create_time DESC, id DESC} order and returns the slice only (no total, since the original contract for the
     * revision list asks for an ordered collection).
     *
     * 用法 / Usage: {@code knowledgeRepository.listRevisions(kbId, documentId, page, size)}；空页返回 {@code []}；
     * 返回载体不携带原文字节，避免整页读取时把内容拉进内存。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param documentId 参数 文档十进制字符串 id；parameter decimal-string document id.
     * @param page 参数 页码，从 1 开始；parameter one-based page number.
     * @param size 参数 页大小，1–100；parameter effective page size.
     * @return 返回 当页修订业务载体；returns the revision carriers of that page.
     */
    List<KnowledgeDocumentRevisionBO> listRevisions(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String documentId,
            @Min(1) int page,
            @Min(1)
            @Max(100) int size
    );

    /**
     * 中文说明：执行 insertRevision 操作；受守卫插入 {@code STAGING} 修订行并以权威 {@code revision = 1} 落库，
     * 原始字节、{@code byteCount}、{@code contentHash}、嵌入空间与维度快照在同一次写入中固化，
     * 其中嵌入空间/维度必须等于知识库已冻结的值（首次上传后不可变，向量索引与查询口径由此一致）。
     * English summary: Executes the insertRevision operation; performs a guarded insert of the {@code STAGING} revision row at
     * the authoritative {@code revision = 1}, freezing the raw bytes, {@code byteCount}, {@code contentHash} and the
     * embedding space and dimension snapshot in the same write, where the embedding space and dimensions must equal the
     * knowledge base's frozen values (immutable after the first upload so that indexing and querying stay aligned).
     *
     * 用法 / Usage: {@code knowledgeRepository.insertRevision(revision)}；与 {@link #insertDocument(KnowledgeDocumentBO)}
     * 和摄取作业插入共用一个事务，插入后立即提交，任何模型调用都在事务之外。
     * @param revision 参数 待插入的修订载体；parameter the revision carrier to insert.
     * @return 返回 已提交的修订业务载体；returns the committed revision carrier.
     */
    KnowledgeDocumentRevisionBO insertRevision(@Valid @NotNull KnowledgeDocumentRevisionBO revision);

    /**
     * 中文说明：执行 updateRevision 操作；以 {@code id + tenant_id + kb_id + revision = expected + deleted_at IS NULL}
     * 为条件整行替换修订业务列（状态 {@code STAGING→READY/FAILED}、{@code extractedText}、{@code chunkCount} 等）
     * 并推进 revision；0 行返回 {@code false}，即并发推进或行不可见，属于失败而非成功。
     * English summary: Executes the updateRevision operation; replaces the revision's business columns (status
     * {@code STAGING→READY/FAILED}, {@code extractedText}, {@code chunkCount} and friends) and advances the revision under
     * {@code id + tenant_id + kb_id + revision = expected + deleted_at IS NULL}; {@code false} on zero rows means a
     * concurrent advance or an invisible row, which is failure rather than success.
     *
     * 用法 / Usage: {@code knowledgeRepository.updateRevision(revision, expectedRevision)}；
     * 重放同一 attempt 时必须先读现值再决定，不得凭假设的 revision 覆盖。
     * @param revision 参数 完整替换载体；parameter the full replacement carrier.
     * @param expectedRevision 参数 调用方期望的当前 revision，正整数；parameter caller-observed revision, a positive number.
     * @return 返回 CAS 是否命中；returns whether the compare-and-set hit.
     */
    boolean updateRevision(
            @Valid @NotNull KnowledgeDocumentRevisionBO revision,
            @Min(1) long expectedRevision
    );

    /**
     * 中文说明：执行 stageChunks 操作；为给定 {@code STAGING} revision 重写分块集合：先按
     * {@code revision_id} 清理该修订此前暂存的分块，再批量插入入参载体，返回受影响行数（0 表示本次没有落任何分块）。
     * 每个分块的 {@code embeddingSpaceId}/{@code dimensions} 必须等于知识库冻结值，向量必须来自 LOCAL 嵌入 alias、
     * 长度恰为 {@code dimensions}、全部有限且非全零——校验不通过要在写入之前如实失败，绝不写入部分向量或伪造空索引。
     * English summary: Executes the stageChunks operation; rewrites the chunk set of one {@code STAGING} revision by clearing
     * that revision's previously staged chunks per {@code revision_id} and batch-inserting the given carriers, returning the
     * affected row count (0 meaning nothing was staged). Every chunk's {@code embeddingSpaceId} and {@code dimensions} must
     * equal the knowledge base's frozen values and every vector must originate from the LOCAL embedding alias, be exactly
     * {@code dimensions} long, fully finite and not all zero — a violation fails honestly before the write rather than
     * leaving a partial index or a fabricated empty one.
     *
     * 用法 / Usage: {@code knowledgeRepository.stageChunks(kbId, revisionId, chunks)}；
     * 只在活动切换之前调用，失败时旧活动 revision 保持不动；重放同一作业是幂等的（清理后重写），
     * 云端嵌入永远不是兜底方案。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param revisionId 参数 修订十进制字符串 id；parameter decimal-string revision id.
     * @param chunks 参数 待暂存的分块载体列表；parameter the chunk carriers to stage.
     * @return 返回 写入的分块行数；returns the number of staged chunk rows.
     */
    int stageChunks(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String revisionId,
            @NotNull List<@Valid KnowledgeChunkBO> chunks
    );

    /**
     * 中文说明：执行 listChunksOfRevision 操作；按 {@code chunk_index} 升序读取该修订的全部暂存分块，
     * 次序稳定因此重放与校验可以逐条比对，向量只在服务端内存中短暂存在，绝不进入日志或响应体。
     * English summary: Executes the listChunksOfRevision operation; reads every staged chunk of one revision ordered ascending
     * by {@code chunk_index}, a stable order so a replay or a verification pass can compare entries one by one, while vectors
     * stay in server memory briefly and never reach a log line or a response body.
     *
     * 用法 / Usage: {@code knowledgeRepository.listChunksOfRevision(revisionId)}；无分块返回 {@code []}。
     * @param revisionId 参数 修订十进制字符串 id；parameter decimal-string revision id.
     * @return 返回 该修订的分块业务载体列表；returns the chunk carriers of that revision.
     */
    List<KnowledgeChunkBO> listChunksOfRevision(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String revisionId
    );

    /**
     * 中文说明：执行 findJob 操作；按主键定位守卫租户内的活跃作业行，返回载体包含租约三列
     * （{@code leaseOwner}/{@code leaseToken}/{@code leaseExpiresAt}）、{@code status}/{@code stage}/
     * {@code attempt}/{@code errorCode}/{@code result} 与 {@code retryOfJobId}。
     * English summary: Executes the findJob operation; locates the active job row by primary key inside the guarded tenant and
     * returns a carrier holding the three lease columns ({@code leaseOwner}, {@code leaseToken},
     * {@code leaseExpiresAt}) plus {@code status}/{@code stage}/{@code attempt}/{@code errorCode}/{@code result} and
     * {@code retryOfJobId}.
     *
     * 用法 / Usage: {@code knowledgeRepository.findJob(jobId)}；未命中（含跨租户与软删）返回
     * {@code Optional.empty()}，由 {@code KnowledgeJobService} 如实转 404。
     * @param jobId 参数 作业十进制字符串 id；parameter decimal-string job id.
     * @return 返回 作业业务载体；returns the job carrier when present.
     */
    Optional<KnowledgeJobBO> findJob(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String jobId
    );

    /**
     * 中文说明：执行 findJobByIntent 操作；按幂等意图 {@code kbId + actorId + type + idempotencyKey}
     * 回读既有作业，用于创建/重试的复用判定：命中且请求摘要一致即返回同一行（不新建），
     * 命中但摘要不一致由业务层转 {@code 409 KNOWLEDGE_IDEMPOTENCY_CONFLICT}，未命中返回空。
     * English summary: Executes the findJobByIntent operation; re-reads an existing job by the idempotency intent
     * {@code kbId + actorId + type + idempotencyKey} for the reuse decision of a create or retry: a hit with an equal request
     * digest returns the same row (no new job), a hit with a different digest becomes
     * {@code 409 KNOWLEDGE_IDEMPOTENCY_CONFLICT} in the business layer, and a miss returns empty.
     *
     * 用法 / Usage: {@code knowledgeRepository.findJobByIntent(kbId, actorId, type, idempotencyKey)}；
     * 该查询必须与后续插入共处唯一约束之下，竞争方在重读时命中同一行而不是产生第二个作业。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param actorId 参数 提交者 actor 稳定标识（varchar(128)）；parameter submitting actor identifier (varchar(128)).
     * @param type 参数 作业类型；parameter job type.
     * @param idempotencyKey 参数 客户端幂等键，至多 64 字符；parameter client idempotency key, at most 64 characters.
     * @return 返回 命中的作业业务载体；returns the matched job carrier when present.
     */
    Optional<KnowledgeJobBO> findJobByIntent(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @NotBlank
            @Size(max = 128) String actorId,
            @NotNull KnowledgeJobTypeEnum type,
            @NotBlank
            @Size(max = 64) String idempotencyKey
    );

    /**
     * 中文说明：执行 insertJob 操作；受守卫插入 {@code QUEUED} 作业行（{@code stage = QUEUED}、{@code attempt = 0}、
     * {@code nextAttemptAt} 不晚于当前时刻、{@code leaseToken = 0}、权威 {@code revision = 1}），
     * {@code payload}/{@code requestHash}/{@code resourceId}/{@code retryOfJobId} 由入参载体决定；
     * 幂等意图的唯一键冲突按冲突如实抛出，绝不静默产生第二个作业。
     * English summary: Executes the insertJob operation; performs a guarded insert of the {@code QUEUED} job row
     * ({@code stage = QUEUED}, {@code attempt = 0}, {@code nextAttemptAt} no later than now, {@code leaseToken = 0} and the
     * authoritative {@code revision = 1}) with {@code payload}, {@code requestHash}, {@code resourceId} and
     * {@code retryOfJobId} decided by the given carrier; a unique-key clash on the idempotency intent surfaces honestly as a
     * conflict rather than silently producing a second job.
     *
     * 用法 / Usage: {@code knowledgeRepository.insertJob(job)}；上传路径下与文档、revision 行同事务提交，
     * 提交之后才可能有 worker 认领。
     * @param job 参数 待插入的作业载体；parameter the job carrier to insert.
     * @return 返回 已提交的作业业务载体；returns the committed job carrier.
     */
    KnowledgeJobBO insertJob(@Valid @NotNull KnowledgeJobBO job);

    /**
     * 中文说明：执行 updateJob 操作；以 {@code id + tenant_id + revision = expected + deleted_at IS NULL}
     * 为条件整行替换作业业务列并推进 revision，用于排队/重试计划等非租约写回；
     * 0 行返回 {@code false}，表示修订已被推进，属于失败而不是成功。
     * English summary: Executes the updateJob operation; replaces the job's business columns and advances the revision under
     * {@code id + tenant_id + revision = expected + deleted_at IS NULL} for the non-lease write-backs of queueing and retry
     * planning; {@code false} on zero rows means the revision already moved, which is failure rather than success.
     *
     * 用法 / Usage: {@code knowledgeRepository.updateJob(job, expectedRevision)}；
     * 运行中作业的结果写回必须走 {@link #finish(KnowledgeJobBO, long)} 的租约 CAS，不得用本方法绕过所有权判定。
     * @param job 参数 完整替换载体；parameter the full replacement carrier.
     * @param expectedRevision 参数 调用方期望的当前 revision，正整数；parameter caller-observed revision, a positive number.
     * @return 返回 CAS 是否命中；returns whether the compare-and-set hit.
     */
    boolean updateJob(
            @Valid @NotNull KnowledgeJobBO job,
            @Min(1) long expectedRevision
    );

    /**
     * 中文说明：执行 listJobs 操作；读取该知识库下活跃作业的当页，{@code status} 为 {@code null} 时不加状态谓词，
     * 次序固定 {@code create_time DESC, id DESC}；只返回当页载体。
     * English summary: Executes the listJobs operation; reads the active job page slice of one knowledge base with no status
     * predicate when {@code status} is {@code null}, in the fixed {@code create_time DESC, id DESC} order, returning the
     * slice only.
     *
     * 用法 / Usage: {@code knowledgeRepository.listJobs(kbId, status, page, size)}；空页返回 {@code []}，
     * 总数由 {@link #countJobs(String, KnowledgeJobStatusEnum)} 以同一状态谓词配对给出。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param status 参数 可选状态过滤，{@code null} 表示不过滤；parameter optional status filter, {@code null} for all.
     * @param page 参数 页码，从 1 开始；parameter one-based page number.
     * @param size 参数 页大小，1–100；parameter effective page size.
     * @return 返回 当页作业业务载体；returns the job carriers of that page.
     */
    List<KnowledgeJobBO> listJobs(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            KnowledgeJobStatusEnum status,
            @Min(1) int page,
            @Min(1)
            @Max(100) int size
    );

    /**
     * 中文说明：执行 countJobs 操作；给出与 {@link #listJobs(String, KnowledgeJobStatusEnum, int, int)}
     * 完全同谓词（含可选状态过滤）的匹配总数。
     * English summary: Executes the countJobs operation; reports the matched total under exactly the same predicate as
     * {@link #listJobs(String, KnowledgeJobStatusEnum, int, int)}, including the optional status filter.
     *
     * 用法 / Usage: {@code knowledgeRepository.countJobs(kbId, status)}。
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param status 参数 可选状态过滤，{@code null} 表示不过滤；parameter optional status filter, {@code null} for all.
     * @return 返回 匹配的作业总数；returns the number of matched jobs.
     */
    long countJobs(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            KnowledgeJobStatusEnum status
    );

    /**
     * 中文说明：执行 claimNext 操作；具名语句 {@code selectClaimable} + {@code claimJob} 的端口，
     * 以 {@code SELECT ... FOR UPDATE SKIP LOCKED LIMIT slots} 取出可认领行（{@code QUEUED}/{@code RETRY_WAIT} 且
     * {@code next_attempt_at ≤ now}，或 {@code RUNNING} 但 {@code lease_expires_at ≤ now} 的失租约行），
     * 按 {@code next_attempt_at, id} 升序，并把每一行翻到 {@code RUNNING}、写入 {@code leaseOwner} 与
     * 单调递增的新 {@code leaseToken}（观察值 + 1）和 {@code leaseExpiresAt}；{@code slots} 至多返回这么多个作业，
     * 认领不成就返回空列表而不是伪造。返回的载体携带的 {@code leaseToken} 是该作业后续所有写回的唯一凭据。
     * English summary: Executes the claimNext operation, the port of the named statements {@code selectClaimable} plus
     * {@code claimJob}: it takes claimable rows with {@code SELECT ... FOR UPDATE SKIP LOCKED LIMIT slots} (a {@code QUEUED}
     * or {@code RETRY_WAIT} row whose {@code next_attempt_at ≤ now}, or a {@code RUNNING} row whose lease already expired),
     * ordered ascending by {@code next_attempt_at, id}, flips each to {@code RUNNING} and stores {@code leaseOwner}, the
     * monotonically advanced new {@code leaseToken} (observed value plus one) and {@code leaseExpiresAt}; it returns at most
     * {@code slots} jobs and an empty list when nothing is claimable rather than a fabrication. The {@code leaseToken} on
     * each returned carrier is the sole credential for every later write-back of that job.
     *
     * 用法 / Usage: {@code knowledgeRepository.claimNext(slots)}；worker 只在有空闲槽位时认领（并发 2、队列 32），
     * 并且必须已在守卫上下文中恢复该作业的可信租户与 actor——本方法不接受租户入参；租约时长固定 120 秒，
     * 重启使令牌单调递增，因此旧 worker 的迟到写回必然 0 行。
     * @param slots 参数 本次认领上限，非负；parameter claim bound for this round, non-negative.
     * @return 返回 已认领并持租约的作业载体列表；returns the claimed job carriers holding a lease.
     */
    List<KnowledgeJobBO> claimNext(
            @Min(0) int slots
    );

    /**
     * 中文说明：执行 heartbeat 操作；具名语句 {@code heartbeatJob} 的端口，以
     * {@code id + tenant_id + lease_token + status = 'RUNNING' + lease_expires_at > now} 为条件把
     * {@code leaseExpiresAt} 顺延；返回 {@code false} 意味着租约已丢失（超时被他人接管、令牌已推进或作业已终态），
     * 调用方必须立即停止后续工作并放弃发布——过期租约永远不能发布结果。
     * English summary: Executes the heartbeat operation, the port of the named statement {@code heartbeatJob}: it extends
     * {@code leaseExpiresAt} under {@code id + tenant_id + lease_token + status = 'RUNNING' + lease_expires_at > now}.
     * {@code false} means ownership is gone (the lease expired and another worker took over, the token advanced, or the job
     * reached a terminal state), so the caller must stop working immediately and abandon publication — a stale lease can
     * never publish.
     *
     * 用法 / Usage: {@code knowledgeRepository.heartbeat(jobId, leaseToken, leaseExpiresAt)}；
     * 心跳周期 30 秒对应 120 秒租约，长外部调用要在阶段边界补心跳；心跳不做业务写，也不延长 attempt 预算。
     * @param jobId 参数 作业十进制字符串 id；parameter decimal-string job id.
     * @param leaseToken 参数 认领时获得的租约令牌；parameter lease token acquired at claim time.
     * @param leaseExpiresAt 参数 顺延后的到期时刻；parameter the extended expiry instant.
     * @return 返回 所有权是否仍然成立；returns whether ownership still holds.
     */
    boolean heartbeat(
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String jobId,
            @Min(1) long leaseToken,
            @NotNull Instant leaseExpiresAt
    );

    /**
     * 中文说明：执行 finish 操作；具名语句 {@code finishJob} 的端口，把终态载体（{@code SUCCEEDED/FAILED/STALE/CANCELLED}
     * 与 {@code stage}、{@code errorCode}、{@code result}）以
     * {@code id + tenant_id + lease_token + status = 'RUNNING' + lease_expires_at > now} 为条件写回并推进 revision 与
     * 审计列；影响 0 行返回 {@code false}，即本次执行结果作废：所有权已丢失的作业不得留下状态，
     * 更不得据此触发活动版本切换。终态集合与 {@code attempt ≤ 3} 的取值约束由库 CHECK 与业务层共同保证。
     * English summary: Executes the finish operation, the port of the named statement {@code finishJob}: it writes the terminal
     * carrier ({@code SUCCEEDED/FAILED/STALE/CANCELLED} with {@code stage}, {@code errorCode} and {@code result}) back under
     * {@code id + tenant_id + lease_token + status = 'RUNNING' + lease_expires_at > now}, advancing the revision and the audit
     * columns. A zero-row effect returns {@code false}, i.e. this execution's outcome is void: a job whose ownership is gone
     * leaves no state behind and must certainly not drive an active-revision switch. The terminal vocabulary and the
     * {@code attempt ≤ 3} bound are jointly guaranteed by the database CHECK constraints and the business layer.
     *
     * 用法 / Usage: {@code knowledgeRepository.finish(job, leaseToken)}；
     * 发布（{@link #activateRevision(String, String, String, long)}）之后仍要以本方法收口作业状态，
     * 返回 {@code false} 时调用方丢弃本地结论并回到轮询，绝不重试覆盖新持有者的写入。
     * @param job 参数 携带终态的作业载体；parameter the job carrier holding the terminal state.
     * @param leaseToken 参数 认领时获得的租约令牌；parameter lease token acquired at claim time.
     * @return 返回 终态是否由本方写入；returns whether this side wrote the terminal state.
     */
    boolean finish(@Valid @NotNull KnowledgeJobBO job, @Min(1) long leaseToken);
}
