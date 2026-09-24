package top.egon.cola.component.yuheng.admin.knowledge.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeAnswerCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeBaseCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeMemberDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeMembersCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgePageQueryDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeReindexCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeUploadCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeAnswerVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeBaseVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeDocumentRevisionVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeDocumentVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeJobVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgePageVO;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;

import java.util.List;

/**
 * 中文说明：{@code KnowledgeService} 是知识库管理面 API-008–018 与 API-022 的业务合同，
 * 覆盖知识库的创建/分页/读取/完整替换、成员读取与替换、文档分页/上传/修订读取/删除、重投作业创建以及接地问答入口。
 * 每个方法都以已验证的 {@link AdminActor} 作为第一个参数，权限判定（OWNER 取 {@code ownerActorId} 或 members 中角色为
 * OWNER 的成员，EDITOR/READER 取自 {@code members}，创建要求 {@code KB_CREATE} 作用域或角色）全部落在本层，
 * 仓储端口不做角色判断；租户与审计操作者仍由守卫上下文决定，任何方法都不接受调用方自报租户。
 * 返回值只有既有 VO/分页载体，绝不泄漏 PO，也不携带原文字节、提取文本、提示词或向量。
 * English summary: {@code KnowledgeService} is the business contract behind API-008–018 and API-022 of the knowledge
 * management plane: knowledge-base create, page, read and full replace, member read and replace, document page, upload,
 * revision read and delete, reindex job creation and the grounded answer entry point. Every method takes the verified
 * {@link AdminActor} first and performs the authorization here (OWNER is {@code ownerActorId} or a member whose role is
 * OWNER, EDITOR/READER come from {@code members}, and creation requires the {@code KB_CREATE} scope or role) because the
 * repository port makes no role decision; tenancy and the audit operator stay in the guarded context and no method accepts a
 * caller-reported tenant. Only the existing VO and page carriers leave this layer — never a PO, raw bytes, extracted text,
 * prompts or vectors.
 *
 * 用法 / Usage: 由 {@code KnowledgeController}、{@code KnowledgeDocumentController} 以限定名
 * （bean {@code knowledgeServiceImpl}）注入并按原路径与 operationId 调用；实现类持有写事务与限定协作者，
 * 接口只做声明式校验（约束在未声明分组的载体上走 default 组，创建与替换共用 {@code KnowledgeBaseCommandDTO}，
 * 其 {@code expectedRevision} 在替换语义下必填由实现按分组/业务复核）。错误契约固定为：身份或角色不满足
 * {@code 403 KNOWLEDGE_FORBIDDEN}；本租户不可见的行（含跨租户与软删）{@code 404 KNOWLEDGE_RESOURCE_NOT_FOUND}；
 * 修订不匹配 {@code 409 KNOWLEDGE_REVISION_CONFLICT}（保留 {@code GatewayAdminRevisionConflictException} 携带的现值
 * {@code currentRevision}）；幂等意图复用冲突 {@code 409 KNOWLEDGE_IDEMPOTENCY_CONFLICT}；
 * 字段或状态不成立 {@code 422 KNOWLEDGE_VALIDATION_FAILED}；本地嵌入或模型别名不可用
 * {@code 503 KNOWLEDGE_MODEL_UNAVAILABLE}。0 行写入永远不是成功，事务失败即整体回滚并保留旧活动版本。
 * / Inject it by qualifier ({@code knowledgeServiceImpl}) into the controllers and call it through the original paths and
 * operationIds; the implementation owns the write transaction and its qualified collaborators while the interface carries
 * only declarative validation in the default groups (create and replace share {@code KnowledgeBaseCommandDTO}, whose
 * {@code expectedRevision} is required by replace semantics and re-checked by the implementation). The error contract is
 * fixed: an unmet identity or role raises {@code 403 KNOWLEDGE_FORBIDDEN}; a row invisible to this tenant (foreign tenant or
 * soft-deleted) raises {@code 404 KNOWLEDGE_RESOURCE_NOT_FOUND}; a revision mismatch raises
 * {@code 409 KNOWLEDGE_REVISION_CONFLICT} keeping the stored {@code currentRevision} from
 * {@code GatewayAdminRevisionConflictException}; a diverged idempotency intent raises
 * {@code 409 KNOWLEDGE_IDEMPOTENCY_CONFLICT}; a field or state violation raises {@code 422 KNOWLEDGE_VALIDATION_FAILED};
 * and an unavailable local embedding alias raises {@code 503 KNOWLEDGE_MODEL_UNAVAILABLE}. A zero-row effect is never
 * success and a failed transaction rolls back entirely leaving the previous active revision untouched.
 */
@Validated
public interface KnowledgeService {

    /**
     * 中文说明：API-009 创建知识库：要求 actor 具备 {@code KB_CREATE} 作用域/角色，否则 403；
     * {@code ownerActorId} 由服务端从 actor 派生、成员表由命令初始化管理员集合，嵌入空间与维度不在命令里出现而由服务端
     * 按 chat/embedding alias 解析后冻结；{@code idempotencyKey} 命中同一意图且请求摘要一致时返回既有知识库，
     * 摘要不同按 409 幂等冲突拒绝。
     * English summary: API-009 creates a knowledge base and requires the {@code KB_CREATE} scope or role on the actor, else
     * 403; {@code ownerActorId} is derived server-side from the actor, the member set is seeded from the command, the
     * embedding space and dimensions never appear in the command but are resolved from the aliases and frozen by the server;
     * an {@code idempotencyKey} matching the same intent with an equal digest returns the existing knowledge base while a
     * different digest is rejected as a 409 idempotency conflict.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.createBase(actor, command, idempotencyKey)}；
     * 新建以权威 {@code revision = 1} 落库并由控制器返回 201；创建本身不调用任何模型。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param command 参数 创建命令；parameter the create command.
     * @param idempotencyKey 参数 可选 {@code Idempotency-Key} 请求头值，至多 64 字符；parameter optional
     *                       {@code Idempotency-Key} header value, at most 64 characters.
     * @return 返回 已提交的知识库投影；returns the committed knowledge base projection.
     */
    KnowledgeBaseVO createBase(
            @NotNull AdminActor actor,
            @Valid @NotNull KnowledgeBaseCommandDTO command,
            @Size(max = 64) String idempotencyKey
    );

    /**
     * 中文说明：API-008 分页读取该 actor 可见（owner 或成员）的知识库，返回 {@code items/page/size/total} 裸 JSON，
     * 次序固定 {@code create_time DESC, id DESC}，不追加 code/data wrapper，也不投影成员之外的敏感列。
     * English summary: API-008 pages the knowledge bases visible to this actor as owner or member and answers with the bare
     * {@code items/page/size/total} JSON in the fixed {@code create_time DESC, id DESC} order, without a code/data wrapper
     * and without projecting sensitive columns beyond membership.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.listBases(actor, query)}；page 从 1 开始、size 1–100，
     * 空页返回 {@code []} 而非 null，total 与当页同谓词。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param query 参数 分页查询载体；parameter the page query carrier.
     * @return 返回 知识库分页投影；returns the paged knowledge base projection.
     */
    KnowledgePageVO<KnowledgeBaseVO> listBases(
            @NotNull AdminActor actor,
            @Valid @NotNull KnowledgePageQueryDTO query
    );

    /**
     * 中文说明：API-010 读取单个知识库：先按 READER 及以上角色（owner 或 members）判定可见性，
     * 不可见即 403 而不是 404，避免把存在性泄漏给无权限身份；本租户确实不存在该行时为 404。
     * English summary: API-010 reads one knowledge base after a visibility decision at READER or above (owner or member); an
     * invisible one becomes 403 rather than 404 so existence never leaks to an unauthorized actor, while a genuinely absent
     * row inside this tenant is 404.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.getBase(actor, kbId)}；返回体携带服务端当前 {@code revision}，
     * 供后续替换作为期望值。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @return 返回 知识库投影；returns the knowledge base projection.
     */
    KnowledgeBaseVO getBase(
            @NotNull AdminActor actor,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId
    );

    /**
     * 中文说明：API-011 完整替换知识库定义：仅 OWNER 可为，要求 {@code command.expectedRevision} 等于库中现值，
     * 不一致即 409 并携带现值；已冻结的 {@code embeddingSpaceId}/{@code dimensions} 不得原地改变，
     * chat/embedding alias 变更必须在写入前复核其仍存在且 embedding 侧仍是 LOCAL 部署，
     * 否则以 422 拒绝而不是静默放宽或转云。写回以 CAS 推进 revision，0 行不得伪装成功。
     * English summary: API-011 fully replaces a knowledge base definition, OWNER only, requiring
     * {@code command.expectedRevision} to equal the stored one (otherwise 409 carrying that value); the frozen
     * {@code embeddingSpaceId} and {@code dimensions} cannot change in place, and a chat or embedding alias change must be
     * re-validated before the write — the alias still exists and the embedding side is still a LOCAL deployment — otherwise
     * it is a 422 field failure rather than a silent widening or a cloud fallback. The write-back advances the revision by
     * CAS and a zero-row effect may not be dressed up as success.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.replaceBase(actor, kbId, command)}；
     * 返回体是服务端推进后的权威投影，控制器据此回 200。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param command 参数 完整替换命令；parameter the full replace command.
     * @return 返回 已提交的知识库投影；returns the committed knowledge base projection.
     */
    KnowledgeBaseVO replaceBase(
            @NotNull AdminActor actor,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @Valid @NotNull KnowledgeBaseCommandDTO command
    );

    /**
     * 中文说明：API-012 读取成员列表：Spec §9.2 将本操作定为 OWNER（owner 审核内容权限），输出顺序稳定，
     * 每位成员只有 {@code actorId} 与 typed {@code role}，不携带任何知识库内容。
     * English summary: API-012 reads the member list, which Spec §9.2 reserves for OWNER, in a stable order, projecting only each member's
     * {@code actorId} and typed {@code role} and no knowledge base content.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.listMembers(actor, kbId)}；无附加成员时返回空列表而不是 null。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @return 返回 成员载体列表；returns the member carriers.
     */
    List<KnowledgeMemberDTO> listMembers(
            @NotNull AdminActor actor,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId
    );

    /**
     * 中文说明：API-013 完整替换成员集合：仅 OWNER 可为；集合内 {@code actorId} 不得重复、角色必须落在
     * {@code READER/EDITOR/OWNER} 词汇表内，owner 自身不得被移除可见性；替换以期望 revision 做 CAS，
     * 与库中现值不一致即 409 并携带现值，0 行写入不得成功。
     * English summary: API-013 fully replaces the member set, OWNER only; the {@code actorId} values may not repeat, roles
     * must stay inside the {@code READER/EDITOR/OWNER} vocabulary, the owner's own visibility may not be removed, and the
     * write is a CAS on the expected revision — a mismatch is 409 carrying the stored revision and a zero-row effect is not
     * success.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.replaceMembers(actor, kbId, command)}；
     * 返回替换后的权威成员集合。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param command 参数 成员完整替换命令；parameter the full members replacement command.
     * @return 返回 已提交的成员载体列表；returns the committed member carriers.
     */
    List<KnowledgeMemberDTO> replaceMembers(
            @NotNull AdminActor actor,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @Valid @NotNull KnowledgeMembersCommandDTO command
    );

    /**
     * 中文说明：API-014 分页读取该知识库下的活跃文档（READER 及以上），只投影
     * {@code id/kbId/fileName/activeRevisionId/latestJobId/revision} 与安全时间戳，绝不投影原文或提取文本。
     * English summary: API-014 pages the active documents of one knowledge base at READER or above, projecting only
     * {@code id/kbId/fileName/activeRevisionId/latestJobId/revision} plus the safe timestamps and never the raw file or the
     * extracted text.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.listDocuments(actor, kbId, query)}；
     * 知识库不可见即 403，空页返回 {@code []}。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param query 参数 分页查询载体；parameter the page query carrier.
     * @return 返回 文档分页投影；returns the paged document projection.
     */
    KnowledgePageVO<KnowledgeDocumentVO> listDocuments(
            @NotNull AdminActor actor,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @Valid @NotNull KnowledgePageQueryDTO query
    );

    /**
     * 中文说明：API-015 上传文档：EDITOR 及以上方可为；先校验文件名/mediaType 与字节数上限，
     * 再在同一事务内写入文档行 + {@code STAGING} revision（原始字节、{@code byteCount}、{@code contentHash}）
     * + {@code DOCUMENT_INGEST} 作业行并一次性提交，嵌入空间与维度按知识库冻结值快照写入；
     * 事务内绝不发起任何模型或向量调用（锁外调用），作业成功前旧活动 revision 保持不变，
     * 因此上传本身永远不会让检索看到半成品索引。{@code documentId} 给出时按 CAS 追加新版本，
     * 其 {@code expectedRevision} 不一致即 409；{@code idempotencyKey} 命中同一意图时复用既有作业而不重复摄取。
     * English summary: API-015 uploads a document, EDITOR or above: the file name, mediaType and byte bound are validated
     * first, then one transaction writes the document row plus the {@code STAGING} revision (raw bytes, {@code byteCount},
     * {@code contentHash}) plus the {@code DOCUMENT_INGEST} job row and commits them together, snapshotting the knowledge
     * base's frozen embedding space and dimensions. No model or vector call happens while the transaction holds locks —
     * vendor work stays outside; until the job succeeds the previous active revision is untouched, so an upload can never
     * expose a half-built index to retrieval. A supplied {@code documentId} appends a new version under CAS whose mismatch
     * is 409, and an {@code idempotencyKey} matching the same intent reuses the existing job instead of ingesting twice.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.uploadDocument(actor, kbId, command, idempotencyKey)}；
     * 返回的文档投影携带 {@code latestJobId}，客户端据此轮询 API-019/020；超出上传上限或内容非法为 422，
     * 嵌入别名不可用为 503 {@code KNOWLEDGE_MODEL_UNAVAILABLE}，且没有云端兜底。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param command 参数 上传命令；parameter the upload command.
     * @param idempotencyKey 参数 可选 {@code Idempotency-Key} 请求头值，至多 64 字符；parameter optional
     *                       {@code Idempotency-Key} header value, at most 64 characters.
     * @return 返回 已提交的文档投影；returns the committed document projection.
     */
    KnowledgeDocumentVO uploadDocument(
            @NotNull AdminActor actor,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @Valid @NotNull KnowledgeUploadCommandDTO command,
            @Size(max = 64) String idempotencyKey
    );

    /**
     * 中文说明：API-016 读取文档的某个修订（READER 及以上）：修订必须属于该文档且该文档属于该知识库，
     * 任一环节不成立都按不存在处理并 404；返回体只有元数据与状态，绝不返回 {@code rawBytes} 或 {@code extractedText}。
     * English summary: API-016 reads one revision of a document at READER or above: the revision must belong to that document
     * and that document to this knowledge base, and any broken link in that chain is treated as absent and answered with 404;
     * the response carries metadata and status only, never {@code rawBytes} or {@code extractedText}.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.getRevision(actor, kbId, documentId, revisionId)}。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param documentId 参数 文档十进制字符串 id；parameter decimal-string document id.
     * @param revisionId 参数 修订十进制字符串 id；parameter decimal-string revision id.
     * @return 返回 修订投影；returns the revision projection.
     */
    KnowledgeDocumentRevisionVO getRevision(
            @NotNull AdminActor actor,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String documentId,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String revisionId
    );

    /**
     * 中文说明：API-017 删除文档：EDITOR 及以上方可为，按期望 revision 做软删 CAS（{@code SET deleted_at} 的 update，
     * 不是物理删除），命中后返回权威投影；不命中即 409 并携带库中现值，0 行绝不返回成功。
     * 历史 revision 与作业行保留可审计，检索侧因活动行谓词立即不可见。
     * English summary: API-017 deletes a document, EDITOR or above, as a soft-delete CAS at the expected revision (an
     * {@code SET deleted_at} update, never a physical delete), returning the authoritative projection on a hit; a miss is 409
     * carrying the stored revision and a zero-row effect is never reported as success. Historical revisions and job rows stay
     * auditable while retrieval stops seeing the row immediately through its active predicate.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.deleteDocument(actor, kbId, documentId, expectedRevision)}；
     * {@code expectedRevision} 缺失或非正数按 422 拒绝，避免无条件删除。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param documentId 参数 文档十进制字符串 id；parameter decimal-string document id.
     * @param expectedRevision 参数 客户端观察到的文档 revision；parameter the client-observed document revision.
     * @return 返回 删除后的文档投影；returns the document projection after the delete.
     */
    KnowledgeDocumentVO deleteDocument(
            @NotNull AdminActor actor,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String documentId,
            Long expectedRevision
    );

    /**
     * 中文说明：API-018 为既有文档创建重投作业：EDITOR 及以上方可为，文档与其 revision 必须存在且属于该知识库，
     * 作业类型固定 {@code DOCUMENT_INGEST}、{@code sourceRevisionId} 与期望 revision 由命令携带并参与请求摘要；
     * 同一 {@code idempotencyKey} 意图命中且摘要一致时复用既有作业（返回同一 {@code jobId}），
     * 摘要不同即 409 {@code KNOWLEDGE_IDEMPOTENCY_CONFLICT}；创建作业与推进 {@code latestJobId} 同事务完成，
     * 摄取本身在 worker 侧进行，本方法绝不同步调用模型。
     * English summary: API-018 creates a reingest job for an existing document, EDITOR or above: the document and its revision
     * must exist inside this knowledge base, the job type is fixed to {@code DOCUMENT_INGEST}, and the command's
     * {@code sourceRevisionId} plus expected revision feed the request digest; the same {@code idempotencyKey} intent with an
     * equal digest reuses the existing job (returning the same {@code jobId}) while a different digest is a 409
     * {@code KNOWLEDGE_IDEMPOTENCY_CONFLICT}. Job creation and the {@code latestJobId} advance commit in one transaction, the
     * ingestion itself runs on the worker side, and this method never calls a model synchronously.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.createReindexJob(actor, kbId, documentId, command, idempotencyKey)}；
     * 新建返回 201 与作业投影，客户端随后按 API-019/020 轮询。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param documentId 参数 文档十进制字符串 id；parameter decimal-string document id.
     * @param command 参数 重投命令；parameter the reindex command.
     * @param idempotencyKey 参数 可选 {@code Idempotency-Key} 请求头值，至多 64 字符；parameter optional
     *                       {@code Idempotency-Key} header value, at most 64 characters.
     * @return 返回 已提交的作业投影；returns the committed job projection.
     */
    KnowledgeJobVO createReindexJob(
            @NotNull AdminActor actor,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String documentId,
            @Valid @NotNull KnowledgeReindexCommandDTO command,
            @Size(max = 64) String idempotencyKey
    );

    /**
     * 中文说明：API-022 接地问答：READER 及以上方可为，本方法只做权限与入参复核后把检索与生成委托给
     * {@link KnowledgeRetrievalService}，答案必须带引用且引用只来自该 actor 可见、该知识库活动 revision 的分块；
     * 检索不到可用证据时如实返回无依据结果而不是编造，模型别名不可用为 503 {@code KNOWLEDGE_MODEL_UNAVAILABLE}。
     * English summary: API-022 answers a grounded question, READER or above: this method performs the authorization and input
     * checks and then delegates retrieval plus generation to {@link KnowledgeRetrievalService}; the answer must carry
     * citations drawn only from chunks of this knowledge base's active revisions that this actor may see, an empty evidence
     * set yields an honest unsupported result instead of a fabrication, and an unavailable alias is a 503
     * {@code KNOWLEDGE_MODEL_UNAVAILABLE}.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.answer(actor, kbId, command)}；
     * 出参只含答案文本、引用与安全检索元数据，绝不含提示词、向量或原文字节。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param command 参数 问答命令；parameter the answer command.
     * @return 返回 带引用的答案投影；returns the grounded answer projection with its citations.
     */
    KnowledgeAnswerVO answer(
            @NotNull AdminActor actor,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @Valid @NotNull KnowledgeAnswerCommandDTO command
    );
}
