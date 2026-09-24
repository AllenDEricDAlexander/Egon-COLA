package top.egon.cola.component.yuheng.admin.knowledge.service.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.yuheng.admin.config.properties.KnowledgeProperties;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeBaseBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeDocumentBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeDocumentRevisionBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeJobBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeAnswerCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeBaseCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeMemberDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeMembersCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgePageQueryDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeReindexCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeUploadCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobStatusEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobStageEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobTypeEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeMemberRoleEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeRevisionStatusEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeAnswerVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeBaseVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeDocumentRevisionVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeDocumentVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeJobVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgePageVO;
import top.egon.cola.component.yuheng.admin.knowledge.repository.KnowledgeRepository;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeRetrievalService;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeService;
import top.egon.cola.component.yuheng.admin.llm.domain.bo.LlmChannelBO;
import top.egon.cola.component.yuheng.admin.llm.domain.bo.LlmModelBO;
import top.egon.cola.component.yuheng.admin.llm.domain.dto.LlmRouteBindingDTO;
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmDeploymentEnum;
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmModelKindEnum;
import top.egon.cola.component.yuheng.admin.llm.repository.LlmConfigurationRepository;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.shared.domain.bo.IdempotencyBO;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminNotFoundException;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;
import top.egon.cola.component.yuheng.admin.shared.repository.IdempotencyRepository;

/**
 * 中文说明：{@code KnowledgeServiceImpl} 是 {@link KnowledgeService} 的实现，逐字落实 API-008–018 与 API-022
 * 的业务顺序：注解校验 → 身份与角色 → 资源可见性（404/403 的分工）→ 状态与业务 {@code revision} → 受守卫 CAS 写入，
 * 外部模型调用永远留在事务与锁之外。角色判定只在本层发生：OWNER 取 {@code base.ownerActorId} 或 {@code members} 中
 * 角色为 OWNER 的条目，EDITOR/READER 只来自 {@code members}，无任何匹配角色即 403
 * {@code KNOWLEDGE_FORBIDDEN}；租户守卫已经把跨租户与软删行藏成“不存在”，因此读不到行一律 404
 * {@code KNOWLEDGE_RESOURCE_NOT_FOUND}，绝不借 403 泄漏存在性。
 * English summary: {@code KnowledgeServiceImpl} implements {@link KnowledgeService} and keeps the original order of
 * API-008–018 and API-022: annotation validation → identity and role → resource visibility (the 404/403 split) → state and
 * business {@code revision} → guarded compare-and-set writes, while a model call always stays outside any transaction and
 * lock. Only this layer decides roles: OWNER is {@code base.ownerActorId} or a {@code members} entry whose role is OWNER,
 * EDITOR/READER come only from {@code members}, and with no matching role at all the answer is 403
 * {@code KNOWLEDGE_FORBIDDEN}; because the tenant guard already hides foreign-tenant and soft-deleted rows, a row that
 * cannot be read is 404 {@code KNOWLEDGE_RESOURCE_NOT_FOUND} and never a 403 that leaks existence.
 *
 * 用法 / Usage: 由 {@code knowledgeController}、{@code knowledgeDocumentController} 以限定名
 * （bean {@code knowledgeServiceImpl}）注入；写路径持 {@code @Transactional(rollbackFor = Exception.class)}，
 * 读路径持 {@code readOnly = true}，{@link #answer} 刻意不开事务以免把连接横跨一次生成调用。三个不变量在这里收口：
 * 其一，{@link #uploadDocument} 是“文档 + STAGING 修订（原始字节、SHA-256 摘要）+ {@code DOCUMENT_INGEST} 作业”
 * 的单个原子事务，绝不同步调用模型，命中同一幂等意图时复用既有作业而不产生第二个，摘要分歧按 409
 * {@code KNOWLEDGE_IDEMPOTENCY_CONFLICT}；其二，{@code KnowledgeDocumentBO} 没有状态列，文档投影的
 * {@code status} 由最新作业状态派生（{@code SUCCEEDED}→READY、{@code FAILED}→FAILED、其余 PROCESSING）；
 * 其三，嵌入空间与维度在创建时按本地 EMBEDDING 别名冻结、在首次上传时快照进修订，之后不得原地改变，
 * 且永远没有云端嵌入兜底。0 行写入永远不是成功：CAS 失败即重读现值并如实抛 409（携带 {@code currentRevision}）或 404。
 * / Inject it by qualifier ({@code knowledgeServiceImpl}); writes carry
 * {@code @Transactional(rollbackFor = Exception.class)}, reads carry {@code readOnly = true}, and {@link #answer}
 * deliberately opens no transaction so a connection is never held across a generation call. Three invariants close here:
 * first, {@link #uploadDocument} is one atomic transaction over the document plus the {@code STAGING} revision (raw bytes
 * and the SHA-256 digest) plus the {@code DOCUMENT_INGEST} job, never calling a model synchronously, reusing the existing
 * job when the same idempotency intent is hit instead of producing a second one and rejecting a diverged digest with 409
 * {@code KNOWLEDGE_IDEMPOTENCY_CONFLICT}; second, {@code KnowledgeDocumentBO} has no status column so the document
 * projection derives {@code status} from the latest job ({@code SUCCEEDED}→READY, {@code FAILED}→FAILED, otherwise
 * PROCESSING); third, the embedding space and dimensions are frozen from the LOCAL EMBEDDING alias at creation and
 * snapshotted into the revision at first upload, never changed in place afterwards and never served by a cloud fallback.
 * A zero-row effect is never success: a CAS miss re-reads the stored value and honestly raises 409 (keeping
 * {@code currentRevision}) or 404.
 */
@Slf4j
@Validated
@Service("knowledgeServiceImpl")
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    /** 创建意图的乐观版本哨兵值，与既有管理端写法一致 / the optimistic revision sentinel meaning create intent, as in the existing management services. */
    private static final long CREATE_REVISION = 0L;

    /** 幂等意图键的最小长度，低于它一律改用确定性生成的键 / the shortest accepted idempotency intent, shorter values fall back to a generated key. */
    private static final int INTENT_KEY_MIN_LENGTH = 16;

    /** 知识库成员上限，与库内 {@code members} jsonb 的 CHECK 同口径 / the member ceiling that matches the stored {@code members} jsonb CHECK. */
    private static final int MAX_MEMBERS = 100;

    /** 单个文档的分块上限，与 {@code chunk_index} 的 CHECK 0..9999 同口径 / the chunk ceiling per document, matching the {@code chunk_index} CHECK 0..9999. */
    private static final int MAX_CHUNKS = 10_000;

    /** 知识库创建幂等记录的作用域类型，复用既有 {@code gateway_idempotency_record} / the scope type of a knowledge base create record, reusing the existing idempotency table. */
    private static final String KB_CREATE_SCOPE = "KNOWLEDGE_BASE_CREATE";

    /** 创建知识库所需的功能权限标签：逻辑 {@code KB_CREATE} 或知识写能力 / the authority a creator needs: the logical {@code KB_CREATE} label or the knowledge write capability. */
    private static final Set<String> CREATE_AUTHORITIES = Set.of(
            "KB_CREATE",
            "CAP_yuheng:knowledge:write"
    );

    /** READER 及以上可见 / the roles that may read. */
    private static final Set<KnowledgeMemberRoleEnum> READER_OR_ABOVE = EnumSet.of(
            KnowledgeMemberRoleEnum.READER,
            KnowledgeMemberRoleEnum.EDITOR,
            KnowledgeMemberRoleEnum.OWNER
    );

    /** EDITOR 及以上可写 / the roles that may write. */
    private static final Set<KnowledgeMemberRoleEnum> EDITOR_OR_ABOVE = EnumSet.of(
            KnowledgeMemberRoleEnum.EDITOR,
            KnowledgeMemberRoleEnum.OWNER
    );

    /** 仅 OWNER 可管理成员与配置 / the roles that may manage members and configuration. */
    private static final Set<KnowledgeMemberRoleEnum> OWNER_ONLY = EnumSet.of(
            KnowledgeMemberRoleEnum.OWNER
    );

    /** 文档投影的状态派生表，缺项即 PROCESSING / the derivation table of the document projection, an absent entry means PROCESSING. */
    private static final Map<KnowledgeJobStatusEnum, String> DOCUMENT_STATUS = Map.of(
            KnowledgeJobStatusEnum.SUCCEEDED, "READY",
            KnowledgeJobStatusEnum.FAILED, "FAILED"
    );

    /** 已声明的解析类型（media type 口径）/ the declared parseable media types. */
    private static final Set<String> SUPPORTED_MEDIA_TYPES = Set.of(
            "text/markdown",
            "text/plain",
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    /** 已声明的扩展名到解析类型的推导表，保证落库的 mediaType 永不为空 / extension to media type derivation, keeping the persisted mediaType non-blank. */
    private static final Map<String, String> SUPPORTED_FILE_EXTENSIONS = Map.of(
            "md", "text/markdown",
            "txt", "text/plain",
            "pdf", "application/pdf",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    /** 切分配置冻结时的策略名 / the strategy name frozen into the chunking configuration. */
    private static final String CHUNKING_STRATEGY = "FIXED_WINDOW";

    @Qualifier("knowledgeRepository")
    private final KnowledgeRepository knowledgeRepository;

    @Qualifier("knowledgeRetrievalService")
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    @Qualifier(KnowledgeProperties.BEAN_NAME)
    private final KnowledgeProperties knowledgeProperties;

    @Qualifier("llmConfigurationRepository")
    // Spec §9.2.9–011 与 §8 要求服务端自行解析并复核 chat/embedding 别名，再据此冻结 embeddingSpaceId 与维度
    // （二者在修订载体上是必填的，缺了就写不出可摄取的修订）。没有任何已声明端口能读别名，因此复用 Step 10 的
    // LlmConfigurationRepository。 English: Spec §9.2.9-011 makes the server resolve and re-validate the chat and embedding
    // aliases and freeze embeddingSpaceId/dimensions from them, and no declared port can read an alias, so the Step-10
    // repository is reused.
    private final LlmConfigurationRepository llmConfigurationRepository;

    @Qualifier("idempotencyRepository")
    // Spec §4（API-009）明令复用既有 gateway_idempotency_record（scopeType=KNOWLEDGE_BASE_CREATE）而不是新表：
    // 没有它，重放的 Idempotency-Key 会静默建出第二个知识库。 English: Spec §4 mandates reusing the existing
    // gateway_idempotency_record for API-009 instead of a second table; without it a replayed Idempotency-Key would create
    // another knowledge base.
    private final IdempotencyRepository idempotencyRepository;

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    @Qualifier("knowledgeClock")
    private final Clock clock;

    /**
     * 中文说明：执行 createBase 操作；先复核创建者具备 {@code KB_CREATE} 功能权限（创建时知识库尚不存在，
     * 无法做成员判定），再按 {@code scopeType=KNOWLEDGE_BASE_CREATE}/{@code scopeId=SHA-256(actorId)}/
     * {@code key=Idempotency-Key} 回读既有意图：命中且命令摘要一致即重新校验可读性后返回原知识库，摘要分歧按 409；
     * 未命中才在同一个写事务内落 {@code revision=1} 的知识库行（owner 取自可信身份、members 初始化为仅该 OWNER 一条），
     * 并按本地 EMBEDDING 别名冻结嵌入空间与维度。整个流程不发出任何模型请求。
     * English summary: Executes the createBase operation; the creator's {@code KB_CREATE} authority is checked first (a
     * knowledge base does not exist yet, so membership cannot decide), then the stored intent
     * {@code scopeType=KNOWLEDGE_BASE_CREATE}/{@code scopeId=SHA-256(actorId)}/{@code key=Idempotency-Key} is re-read: a hit
     * with an equal command digest re-validates readability and returns the original base, a diverged digest is a 409. Only
     * on a miss does one write transaction store the {@code revision = 1} knowledge base row (the owner comes from the
     * trusted identity and members seed to exactly that OWNER entry) and freeze the embedding space and dimensions from the
     * LOCAL EMBEDDING alias. No model request is issued anywhere in this flow.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.createBase(actor, command, idempotencyKey)}；控制器回 201，
     * 响应体的 {@code revision} 恒为 1。/ The controller answers 201 and the projection's {@code revision} is always 1.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param command 参数 创建命令；parameter the create command.
     * @param idempotencyKey 参数 可选 {@code Idempotency-Key}，不足 16 字符时改用确定性生成的键；parameter the optional
     *                       {@code Idempotency-Key}, replaced by a deterministically generated intent below 16 characters.
     * @return 返回 已提交的知识库投影；returns the committed knowledge base projection.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseVO createBase(
            AdminActor actor,
            KnowledgeBaseCommandDTO command,
            String idempotencyKey) {
        assertCreateAuthority(actor);
        String scopeId = sha256Hex(actor.actorId());
        String commandHash = sha256Hex(canonicalCreateCommand(command).toString());
        String intentKey = intentKey(idempotencyKey, actor.actorId(), commandHash);
        IdempotencyBO replay = idempotencyRepository
                .find(KB_CREATE_SCOPE, scopeId, intentKey)
                .orElse(null);
        if (replay != null) {
            return replayedBase(actor, replay, commandHash);
        }
        LlmModelBO chatModel = resolveChatAlias(command.getChatModel());
        LlmModelBO embeddingModel = resolveEmbeddingAlias(command.getEmbeddingModel());
        KnowledgeBaseBO committed = knowledgeRepository.insertBase(KnowledgeBaseBO.builder()
                .name(StringUtils.trim(command.getName()))
                .description(command.getDescription())
                .ownerActorId(actor.actorId())
                .members(List.of(member(actor.actorId(), KnowledgeMemberRoleEnum.OWNER)))
                .egressPolicy(command.getEgressPolicy())
                .chatModel(StringUtils.trim(chatModel.getModelKey()))
                .embeddingModel(StringUtils.trim(embeddingModel.getModelKey()))
                .embeddingSpaceId(embeddingModel.getEmbeddingSpaceId())
                .dimensions(embeddingModel.getDimensions())
                .revision(CREATE_REVISION)
                .build());
        idempotencyRepository.save(IdempotencyBO.builder()
                .scopeType(KB_CREATE_SCOPE)
                .scopeId(scopeId)
                .key(intentKey)
                .payloadSha256(commandHash)
                .resourceId(committed.getId())
                .response(Map.of("kbId", String.valueOf(committed.getId())))
                .createdAt(clock.instant())
                .expiresAt(null)
                .build());
        log.info(
                "KNOWLEDGE_BASE_CREATED kbId={} ownerActorId={} egressPolicy={} embeddingSpaceId={} dimensions={}",
                committed.getId(),
                actor.actorId(),
                committed.getEgressPolicy(),
                committed.getEmbeddingSpaceId(),
                committed.getDimensions()
        );
        return baseView(committed, KnowledgeMemberRoleEnum.OWNER);
    }

    /**
     * 中文说明：执行 listBases 操作；只读短事务内按同一谓词先取总数再取当页，每行的 {@code myRole}
     * 由当次身份实时派生（owner 优先，其次 members 中最高角色），空页返回 {@code []}。
     * English summary: Executes the listBases operation; one read-only transaction takes the matched total and then the page
     * under the same predicate, deriving each row's {@code myRole} from the presenting identity (the owner wins, otherwise
     * the highest matching member role), and an empty page answers {@code []}.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.listBases(actor, query)}；page 从 1 起、size 1–100 由载体注解保证。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param query 参数 分页查询载体；parameter the page query carrier.
     * @return 返回 知识库分页投影；returns the paged knowledge base projection.
     */
    @Override
    @Transactional(readOnly = true)
    public KnowledgePageVO<KnowledgeBaseVO> listBases(
            AdminActor actor,
            KnowledgePageQueryDTO query) {
        long total = knowledgeRepository.countBasesOfActor(actor.actorId());
        List<KnowledgeBaseVO> items = knowledgeRepository
                .listBasesOfActor(actor.actorId(), query.getPage(), query.getSize())
                .stream()
                .map(base -> baseView(base, deriveRole(base, actor.actorId())))
                .toList();
        return new KnowledgePageVO<>(items, query.getPage(), query.getSize(), total);
    }

    /**
     * 中文说明：执行 getBase 操作；READER 及以上可见，读不到行是 404、看得见行但无角色是 403，
     * 两者严格分开，因为守卫已经把跨租户与软删行折叠成“不存在”。
     * English summary: Executes the getBase operation; visible at READER or above, where an unreadable row is 404 and a row
     * that exists without a role is 403 — the two never blur, since the guard already collapses foreign-tenant and
     * soft-deleted rows into “absent”.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.getBase(actor, kbId)}；返回体携带权威 {@code revision} 供替换使用。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @return 返回 知识库投影；returns the knowledge base projection.
     */
    @Override
    @Transactional(readOnly = true)
    public KnowledgeBaseVO getBase(
            AdminActor actor,
            String kbId) {
        KnowledgeBaseBO base = requireVisibleBase(actor, kbId, READER_OR_ABOVE);
        return baseView(base, deriveRole(base, actor.actorId()));
    }

    /**
     * 中文说明：执行 replaceBase 操作；仅 OWNER 可为，顺序是角色 → 期望 revision → 别名复核 → 冻结值不变式 → CAS：
     * 命令里的 chat/embedding 别名必须仍然存在、已启用且 kind 匹配，EMBEDDING 的全部路由渠道必须仍是 LOCAL；
     * 一旦该知识库已有文档（索引已存在），{@code embeddingSpaceId} 与 {@code dimensions} 不得被换到另一个空间，
     * 违反按 422 拒绝而不是静默放宽。整行替换只覆盖五个业务字段，owner 与 members 原样保留。
     * English summary: Executes the replaceBase operation, OWNER only, in the order role → expected revision → alias
     * re-validation → frozen-value invariant → CAS: the command's chat and embedding aliases must still exist, be enabled
     * and match their kind, every routed channel of an EMBEDDING alias must still be LOCAL, and once the base holds
     * documents the {@code embeddingSpaceId} and {@code dimensions} may not move to another space — a violation is a 422
     * field failure rather than a silent widening. The full replace covers only the five business fields and keeps the owner
     * and the members untouched.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.replaceBase(actor, kbId, command)}；CAS 未命中即重读现值抛 409，
     * 行消失则 404，返回体是推进后的权威投影。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param command 参数 完整替换命令；parameter the full replace command.
     * @return 返回 已提交的知识库投影；returns the committed knowledge base projection.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseVO replaceBase(
            AdminActor actor,
            String kbId,
            KnowledgeBaseCommandDTO command) {
        KnowledgeBaseBO current = requireVisibleBase(actor, kbId, OWNER_ONLY);
        long expectedRevision = requireExpectedRevision(command.getExpectedRevision(), kbId);
        assertRevision(current.getRevision(), expectedRevision);
        LlmModelBO chatModel = resolveChatAlias(command.getChatModel());
        LlmModelBO embeddingModel = resolveEmbeddingAlias(command.getEmbeddingModel());
        if (knowledgeRepository.countDocuments(kbId) > 0L
                && (!Objects.equals(current.getEmbeddingSpaceId(), embeddingModel.getEmbeddingSpaceId())
                || !Objects.equals(current.getDimensions(), embeddingModel.getDimensions()))) {
            throw new CommonException(
                    422,
                    "KNOWLEDGE_VALIDATION_FAILED",
                    "embeddingSpaceId and dimensions are immutable once the knowledge base is indexed"
            );
        }
        KnowledgeBaseBO candidate = baseCarrier(current)
                .setName(StringUtils.trim(command.getName()))
                .setDescription(command.getDescription())
                .setEgressPolicy(command.getEgressPolicy())
                .setChatModel(StringUtils.trim(chatModel.getModelKey()))
                .setEmbeddingModel(StringUtils.trim(embeddingModel.getModelKey()))
                .setEmbeddingSpaceId(embeddingModel.getEmbeddingSpaceId())
                .setDimensions(embeddingModel.getDimensions())
                .setRevision(expectedRevision);
        KnowledgeBaseBO committed = casBase(candidate, expectedRevision, kbId);
        log.info(
                "KNOWLEDGE_BASE_REPLACED kbId={} revision={} egressPolicy={} embeddingSpaceId={} actorId={}",
                kbId,
                committed.getRevision(),
                committed.getEgressPolicy(),
                committed.getEmbeddingSpaceId(),
                actor.actorId()
        );
        return baseView(committed, KnowledgeMemberRoleEnum.OWNER);
    }

    /**
     * 中文说明：执行 listMembers 操作；Spec §9.2 定 API-012 为 OWNER，故仅 owner 可读，只输出 {@code actorId} 与 typed {@code role}
     * 的防御性副本，顺序与库内 {@code members} 一致，绝不携带任何知识库内容。
     * English summary: Executes the listMembers operation, which Spec §9.2 reserves for OWNER, returning a defensive copy of the
     * {@code actorId} and typed {@code role} pairs in the stored order and never any knowledge base content.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.listMembers(actor, kbId)}；无附加成员时返回空列表而不是 null。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @return 返回 成员载体列表；returns the member carriers.
     */
    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeMemberDTO> listMembers(
            AdminActor actor,
            String kbId) {
        KnowledgeBaseBO base = requireVisibleBase(actor, kbId, OWNER_ONLY);
        return copyMembers(membersOf(base));
    }

    /**
     * 中文说明：执行 replaceMembers 操作；仅 OWNER 可为，集合规则为“至多 100 条、actorId 唯一、
     * 有且仅有一个 OWNER 且其 actorId 等于 {@code ownerActorId}”（首版不支持 owner 转移），
     * 违反按 422 拒绝；随后以期望 revision 做整行 CAS，0 行即重读现值抛 409。成员集合与 KB 配置同列存储，
     * 因此替换 members 会推进 {@code revision}，正在执行的输出需按新 revision 重新授权。
     * English summary: Executes the replaceMembers operation, OWNER only, under the set rules “at most 100 entries, unique
     * actorIds, exactly one OWNER whose actorId equals {@code ownerActorId}” (owner transfer is out of scope for this
     * release), each violation being a 422; the write is then a full-row compare-and-set at the expected revision and a
     * zero-row effect re-reads the stored value and raises 409. Members share their column with the base configuration, so
     * a replacement advances {@code revision} and an in-flight output must be re-authorized at the new value.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.replaceMembers(actor, kbId, command)}；
     * 返回替换后的权威成员集合。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param command 参数 成员完整替换命令；parameter the full members replacement command.
     * @return 返回 已提交的成员载体列表；returns the committed member carriers.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<KnowledgeMemberDTO> replaceMembers(
            AdminActor actor,
            String kbId,
            KnowledgeMembersCommandDTO command) {
        KnowledgeBaseBO current = requireVisibleBase(actor, kbId, OWNER_ONLY);
        long expectedRevision = requireExpectedRevision(command.getExpectedRevision(), kbId);
        assertRevision(current.getRevision(), expectedRevision);
        List<KnowledgeMemberDTO> members = assertMemberSet(current, command.getMembers());
        KnowledgeBaseBO candidate = baseCarrier(current)
                .setMembers(new ArrayList<>(members))
                .setRevision(expectedRevision);
        KnowledgeBaseBO committed = casBase(candidate, expectedRevision, kbId);
        log.info(
                "KNOWLEDGE_MEMBERS_REPLACED kbId={} revision={} members={} actorId={}",
                kbId,
                committed.getRevision(),
                members.size(),
                actor.actorId()
        );
        return copyMembers(membersOf(committed));
    }

    /**
     * 中文说明：执行 listDocuments 操作；READER 及以上可读，只投影文件名、活动 revision 指针、
     * 最新作业指针、权威 revision 与安全时刻，绝不投影原文字节或提取文本；
     * {@code status} 是按最新作业状态派生的显示态，不是新持久列。
     * English summary: Executes the listDocuments operation; readable at READER or above, projecting only the file name, the
     * active revision pointer, the latest job pointer, the authoritative revision and the safe instants, never the raw bytes
     * or the extracted text; {@code status} is a display state derived from the latest job rather than a new persisted
     * column.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.listDocuments(actor, kbId, query)}；
     * 当页每行按 {@code latestJobId} 回查一次作业状态，因此空指针直接落 PROCESSING。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param query 参数 分页查询载体；parameter the page query carrier.
     * @return 返回 文档分页投影；returns the paged document projection.
     */
    @Override
    @Transactional(readOnly = true)
    public KnowledgePageVO<KnowledgeDocumentVO> listDocuments(
            AdminActor actor,
            String kbId,
            KnowledgePageQueryDTO query) {
        requireVisibleBase(actor, kbId, READER_OR_ABOVE);
        long total = knowledgeRepository.countDocuments(kbId);
        List<KnowledgeDocumentVO> items = knowledgeRepository
                .listDocuments(kbId, query.getPage(), query.getSize())
                .stream()
                .map(this::documentView)
                .toList();
        return new KnowledgePageVO<>(items, query.getPage(), query.getSize(), total);
    }

    /**
     * 中文说明：执行 uploadDocument 操作；EDITOR 及以上方可为。顺序：角色 → 文件名校验 → mediaType/扩展名联合校验
     * → 字节数与 {@code maxUploadBytes} 上限 → SHA-256 内容摘要 → 幂等意图回读 → 单事务写入。
     * 意图命中且 {@code requestHash} 一致时返回既有作业所在文档，绝不产生第二个作业；摘要分歧按 409
     * {@code KNOWLEDGE_IDEMPOTENCY_CONFLICT}。写入单元在同一事务里落 revision（{@code STAGING}、原始字节、
     * {@code byteCount}、小写十六进制 {@code contentHash}、按知识库冻结值快照的嵌入空间与维度、切分配置）
     * 与 {@code DOCUMENT_INGEST} 作业，再把 {@code latestJobId} CAS 推进到文档行，三行一起提交或一起回滚，
     * 因此失败绝不留下孤立作业；旧活动 revision 在作业成功前保持不动。本方法内没有任何模型或向量调用。
     * English summary: Executes the uploadDocument operation, EDITOR or above, in the order role → file name → joint
     * mediaType/extension check → byte bound against {@code maxUploadBytes} → SHA-256 content digest → idempotency intent
     * re-read → one-transaction write. An intent hit with an equal {@code requestHash} returns the document of the existing
     * job and never produces a second job, while a diverged digest is a 409 {@code KNOWLEDGE_IDEMPOTENCY_CONFLICT}. The
     * write unit stores the revision ({@code STAGING}, the raw bytes, {@code byteCount}, the lowercase hex
     * {@code contentHash}, the embedding space and dimensions snapshotted from the knowledge base, and the chunking
     * configuration) plus the {@code DOCUMENT_INGEST} job, then CAS-advances {@code latestJobId} onto the document row, so
     * the rows commit or roll back together and a failure cannot leave an orphan job; the previous active revision stays
     * untouched until the job succeeds. No model or vector call happens inside this method.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.uploadDocument(actor, kbId, command, idempotencyKey)}；
     * 返回的文档投影携带 {@code latestJobId}，客户端据此轮询 API-019/020；越界为 413/415/422，
     * 嵌入别名不可用为 503 {@code KNOWLEDGE_MODEL_UNAVAILABLE}，且没有云端兜底。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param command 参数 上传命令；parameter the upload command.
     * @param idempotencyKey 参数 可选 {@code Idempotency-Key}，不足 16 字符时改用 actor+kb+内容摘要派生的确定性键；
     *                       parameter the optional {@code Idempotency-Key}, replaced below 16 characters by a deterministic
     *                       key derived from actor, base and content digest.
     * @return 返回 已提交的文档投影；returns the committed document projection.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentVO uploadDocument(
            AdminActor actor,
            String kbId,
            KnowledgeUploadCommandDTO command,
            String idempotencyKey) {
        KnowledgeBaseBO base = requireVisibleBase(actor, kbId, EDITOR_OR_ABOVE);
        String fileName = assertFileName(command.getFileName());
        String mediaType = assertMediaType(fileName, command.getMediaType());
        byte[] rawBytes = assertUploadSize(command.getContent());
        String contentHash = sha256Hex(rawBytes);
        String intentKey = intentKey(idempotencyKey, actor.actorId(), kbId, contentHash);
        KnowledgeDocumentBO existing = StringUtils.isBlank(command.getDocumentId())
                ? null
                : requireDocument(kbId, command.getDocumentId());
        String requestHash = sha256Hex(canonicalUploadCommand(
                kbId, command.getDocumentId(), fileName, mediaType, contentHash).toString());
        Optional<KnowledgeJobBO> intent = knowledgeRepository
                .findJobByIntent(kbId, actor.actorId(), KnowledgeJobTypeEnum.DOCUMENT_INGEST, intentKey);
        if (intent.isPresent()) {
            return replayedUpload(existing, intent.get(), requestHash);
        }
        KnowledgeDocumentBO document = existing == null
                ? knowledgeRepository.insertDocument(KnowledgeDocumentBO.builder()
                        .kbId(kbId)
                        .fileName(fileName)
                        .revision(CREATE_REVISION)
                        .build())
                : appendVersionTo(existing, command, fileName);
        KnowledgeDocumentRevisionBO revision = knowledgeRepository.insertRevision(revisionCarrier(
                kbId, document.getId(), fileName, mediaType, rawBytes, contentHash, base));
        KnowledgeJobBO job = knowledgeRepository.insertJob(queuedJob(
                kbId,
                actor,
                document.getId(),
                intentKey,
                requestHash,
                ingestPayload(kbId, revision.getId(), fileName, mediaType, contentHash, base)));
        KnowledgeDocumentBO bound = bindLatestJob(document, job.getId());
        log.info(
                "KNOWLEDGE_DOCUMENT_UPLOADED kbId={} documentId={} revisionId={} jobId={} byteCount={} actorId={}",
                kbId,
                bound.getId(),
                revision.getId(),
                job.getId(),
                revision.getByteCount(),
                actor.actorId()
        );
        return documentView(bound);
    }

    /**
     * 中文说明：执行 getRevision 操作；READER 及以上可读，revision 必须属于该文档且该文档属于该知识库，
     * 链条上任一环节不成立都按 404 处理（不泄漏存在性）；投影只给元数据与状态，绝不返回 {@code rawBytes}，
     * 也不返回服务器路径或作业内部状态。
     * English summary: Executes the getRevision operation; readable at READER or above, where the revision must belong to that
     * document and that document to this base, any broken link being a 404 that leaks no existence; the projection carries
     * metadata and status only, never {@code rawBytes}, a server path, or internal job state.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.getRevision(actor, kbId, documentId, revisionId)}。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param documentId 参数 文档十进制字符串 id；parameter decimal-string document id.
     * @param revisionId 参数 修订十进制字符串 id；parameter decimal-string revision id.
     * @return 返回 修订投影；returns the revision projection.
     */
    @Override
    @Transactional(readOnly = true)
    public KnowledgeDocumentRevisionVO getRevision(
            AdminActor actor,
            String kbId,
            String documentId,
            String revisionId) {
        requireVisibleBase(actor, kbId, READER_OR_ABOVE);
        requireDocument(kbId, documentId);
        KnowledgeDocumentRevisionBO revision = knowledgeRepository.findRevision(kbId, revisionId)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "KNOWLEDGE_RESOURCE_NOT_FOUND knowledge document revision was not found"
                ));
        if (!Objects.equals(revision.getDocumentId(), documentId)) {
            throw new GatewayAdminNotFoundException(
                    "KNOWLEDGE_RESOURCE_NOT_FOUND knowledge document revision was not found"
            );
        }
        return revisionView(revision);
    }

    /**
     * 中文说明：执行 deleteDocument 操作；EDITOR 及以上方可为，必须携带正 {@code expectedRevision}
     * （缺失或非正按 422 拒绝，避免无条件删除），与库中现值不一致按 409 携带现值。
     * 本方法从不物理删除：历史 revision 与作业行原样保留可审计，只把活动 revision 指针撤下，
     * 使新检索与依赖它的 Wiki 立即失效；重复删除仍成功（幂等地再次推进 revision）。
     * English summary: Executes the deleteDocument operation, EDITOR or above, which requires a positive
     * {@code expectedRevision} (absent or non-positive is a 422 so an unconditional delete is impossible) and whose
     * mismatch is a 409 carrying the stored value. Nothing is ever physically deleted: historical revisions and job rows
     * stay auditable and only the active revision pointer is withdrawn, which makes new retrievals and the wikis depending on
     * them immediately invalid; a repeated delete still succeeds by advancing the revision once more.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.deleteDocument(actor, kbId, documentId, expectedRevision)}；
     * 控制器按契约回 204，本方法仍返回权威投影供审计。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param documentId 参数 文档十进制字符串 id；parameter decimal-string document id.
     * @param expectedRevision 参数 客户端观察到的文档 revision；parameter the client-observed document revision.
     * @return 返回 删除后的文档投影；returns the document projection after the delete.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocumentVO deleteDocument(
            AdminActor actor,
            String kbId,
            String documentId,
            Long expectedRevision) {
        requireVisibleBase(actor, kbId, EDITOR_OR_ABOVE);
        KnowledgeDocumentBO document = requireDocument(kbId, documentId);
        long expected = requireExpectedRevision(expectedRevision, documentId);
        assertRevision(document.getRevision(), expected);
        // API-017 的下架是带版本的逻辑删除：只有 deleted_at 与 version 变化，revision/chunk/job 历史全部留在册内。
        if (!knowledgeRepository.softDeleteDocument(kbId, documentId, expected)) {
            throw revisionConflictOf(kbId, documentId);
        }
        KnowledgeDocumentBO withdrawn = documentCarrier(document)
                .setActiveRevisionId(document.getActiveRevisionId())
                .setRevision(expected + 1);
        log.info(
                "KNOWLEDGE_DOCUMENT_DELETED kbId={} documentId={} revision={} actorId={}",
                kbId,
                documentId,
                withdrawn.getRevision(),
                actor.actorId()
        );
        return documentView(withdrawn);
    }

    /**
     * 中文说明：执行 createReindexJob 操作；EDITOR 及以上方可为，文档与来源 revision 必须存在且属于该知识库
     * （任一环节不成立按 404）；作业类型固定 {@code DOCUMENT_INGEST}。重投<b>不</b>把作业指向已有的
     * {@code READY} 活动版本——那会被摄取端的 {@code STAGING} 前提判为 {@code STALE}——而是在同一事务内按来源
     * 原件的字节与摘要新插一条 {@code STAGING} 修订（切分配置按当前部署值重新冻结），作业载荷指向这条新修订。
     * 载荷只冻结 revision id、来源摘要、模型别名与嵌入空间（不含密钥、正文或上游响应），请求摘要只取调用方
     * 事实，故同一意图命中且摘要一致时返回同一 {@code jobId}，摘要分歧按 409；新修订、作业行与
     * {@code latestJobId} 的推进在同一事务内一起提交，摄取本身只在 worker 侧发生，旧活动版本在此之前始终可检索。
     * English summary: Executes the createReindexJob operation, EDITOR or above: the document and its source revision must
     * exist inside this base (any break being a 404) and the job type is fixed to {@code DOCUMENT_INGEST}. A reindex does
     * <b>not</b> point the job at the existing {@code READY} active revision — the ingestion side would judge that
     * {@code STALE} against its {@code STAGING} precondition — instead inserting a fresh {@code STAGING} revision from the
     * source's own bytes and digest within the same transaction, its chunking configuration re-frozen from the current
     * deployment values, and the job payload addresses that new revision. The payload freezes only the revision id, the
     * source digest, the model alias and the embedding space (no secret, body or upstream response) while the request digest
     * covers caller-supplied facts alone, so the same intent with an equal digest returns the same {@code jobId} and a
     * diverged digest is a 409; the new revision, the job row and the {@code latestJobId} advance commit together, ingestion
     * itself only ever runs on the worker side, and the previous active revision stays retrievable until then.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.createReindexJob(actor, kbId, documentId, command, idempotencyKey)}；
     * 本方法绝不同步调用模型，控制器据 {@code jobId} 回 202 与 {@code Location}。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param documentId 参数 文档十进制字符串 id；parameter decimal-string document id.
     * @param command 参数 重投命令；parameter the reindex command.
     * @param idempotencyKey 参数 可选 {@code Idempotency-Key}，不足 16 字符时改用派生键；parameter the optional
     *                       {@code Idempotency-Key}, replaced by a derived intent below 16 characters.
     * @return 返回 已提交的作业投影；returns the committed job projection.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeJobVO createReindexJob(
            AdminActor actor,
            String kbId,
            String documentId,
            KnowledgeReindexCommandDTO command,
            String idempotencyKey) {
        KnowledgeBaseBO base = requireVisibleBase(actor, kbId, EDITOR_OR_ABOVE);
        KnowledgeDocumentBO document = requireDocument(kbId, documentId);
        long expected = requireExpectedRevision(command.getExpectedRevision(), documentId);
        assertRevision(document.getRevision(), expected);
        String sourceRevisionId = StringUtils.defaultIfBlank(
                command.getSourceRevisionId(),
                document.getActiveRevisionId()
        );
        if (StringUtils.isBlank(sourceRevisionId)) {
            throw new CommonException(
                    422,
                    "KNOWLEDGE_VALIDATION_FAILED",
                    "sourceRevisionId is required while the document has no active revision"
            );
        }
        KnowledgeDocumentRevisionBO source = knowledgeRepository.findRevision(kbId, sourceRevisionId)
                .filter(revision -> Objects.equals(revision.getDocumentId(), documentId))
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "KNOWLEDGE_RESOURCE_NOT_FOUND knowledge document revision was not found"
                ));
        String intentKey = intentKey(
                idempotencyKey,
                actor.actorId(),
                kbId,
                documentId,
                sourceRevisionId
        );
        String requestHash = sha256Hex(canonicalReindexCommand(
                kbId, documentId, sourceRevisionId, source.getContentHash()).toString());
        Optional<KnowledgeJobBO> intent = knowledgeRepository
                .findJobByIntent(kbId, actor.actorId(), KnowledgeJobTypeEnum.DOCUMENT_INGEST, intentKey);
        if (intent.isPresent()) {
            assertSameIntent(intent.get(), requestHash);
            return jobView(intent.get());
        }
        KnowledgeDocumentRevisionBO staging = knowledgeRepository.insertRevision(revisionCarrier(
                kbId,
                documentId,
                source.getFileName(),
                source.getMediaType(),
                source.getRawBytes(),
                source.getContentHash(),
                base));
        KnowledgeJobBO job = knowledgeRepository.insertJob(queuedJob(
                kbId,
                actor,
                documentId,
                intentKey,
                requestHash,
                ingestPayload(
                        kbId,
                        staging.getId(),
                        source.getFileName(),
                        source.getMediaType(),
                        source.getContentHash(),
                        base)));
        KnowledgeDocumentBO bound = bindLatestJob(document, job.getId());
        log.info(
                "KNOWLEDGE_REINDEX_JOB_CREATED kbId={} documentId={} revisionId={} sourceRevisionId={} jobId={}"
                        + " revision={} actorId={}",
                kbId,
                documentId,
                staging.getId(),
                source.getId(),
                job.getId(),
                bound.getRevision(),
                actor.actorId()
        );
        return jobView(job);
    }

    /**
     * 中文说明：执行 answer 操作；READER 及以上方可为，本方法只负责可见性复核与入参透传，
     * 检索与生成整体委托给 {@link KnowledgeRetrievalService}（Step 13 提供 bean）；
     * 因此这里既不开事务也不持锁，模型调用不占用任何数据库事务。
     * English summary: Executes the answer operation, READER or above: this method only re-validates visibility and passes
     * the command through, delegating retrieval plus generation wholesale to {@link KnowledgeRetrievalService} (whose bean
     * Step 13 provides), so it opens neither a transaction nor a lock and a model call never occupies a database
     * transaction.
     *
     * 用法 / Usage: {@code knowledgeServiceImpl.answer(actor, kbId, command)}；
     * 出参只含答案文本、引用与安全检索元数据，绝不含提示词、向量或原文字节。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param command 参数 问答命令；parameter the answer command.
     * @return 返回 带引用的答案投影；returns the grounded answer projection with its citations.
     */
    @Override
    public KnowledgeAnswerVO answer(
            AdminActor actor,
            String kbId,
            KnowledgeAnswerCommandDTO command) {
        requireVisibleBase(actor, kbId, READER_OR_ABOVE);
        return knowledgeRetrievalService.answer(actor, kbId, command);
    }

    /**
     * 中文说明：读取知识库并判定角色，是全部方法的统一守卫：行不可见（跨租户、软删、从未存在）按
     * 404 {@code KNOWLEDGE_RESOURCE_NOT_FOUND}；行可见但角色不在允许集合内按
     * 403 {@code KNOWLEDGE_FORBIDDEN}，两者顺序固定，避免把存在性泄漏给无权限身份。
     * English summary: Reads the knowledge base and decides its role, the shared guard of every method: an invisible row
     * (foreign tenant, soft-deleted, never created) is a 404 {@code KNOWLEDGE_RESOURCE_NOT_FOUND} and a visible row whose
     * role is outside the allowed set a 403 {@code KNOWLEDGE_FORBIDDEN}, always in that order so existence never leaks to an
     * unauthorized actor.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param allowed 参数 允许的角色集合；parameter the accepted role set.
     * @return 返回 知识库业务载体；returns the knowledge base carrier.
     */
    private KnowledgeBaseBO requireVisibleBase(
            AdminActor actor,
            String kbId,
            Set<KnowledgeMemberRoleEnum> allowed) {
        KnowledgeBaseBO base = knowledgeRepository.findBase(kbId)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "KNOWLEDGE_RESOURCE_NOT_FOUND knowledge base was not found"
                ));
        KnowledgeMemberRoleEnum role = deriveRole(base, actor.actorId());
        if (role == null || !allowed.contains(role)) {
            throw new CommonException(
                    403,
                    "KNOWLEDGE_FORBIDDEN",
                    "the presenting identity holds no sufficient role on this knowledge base"
            );
        }
        return base;
    }

    /**
     * 中文说明：读取文档并保证它属于该知识库，跨知识库的同 id 文档按 404 处理；
     * 这一判定本身也是越权屏障，调用前必须已完成知识库角色复核。
     * English summary: Reads a document and keeps it inside this knowledge base, a same-id document under another base being
     * a 404; the read is itself the cross-base barrier and the caller must already have settled the base role.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param documentId 参数 文档十进制字符串 id；parameter decimal-string document id.
     * @return 返回 文档业务载体；returns the document carrier.
     */
    private KnowledgeDocumentBO requireDocument(
            String kbId,
            String documentId) {
        return knowledgeRepository.findDocument(kbId, documentId)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "KNOWLEDGE_RESOURCE_NOT_FOUND knowledge document was not found"
                ));
    }

    /**
     * 中文说明：派生当前主体的角色：{@code ownerActorId} 命中即 OWNER，否则在 {@code members} 中取该主体
     * 角色最高的一条；都不匹配返回 {@code null} 表示完全无权限。
     * English summary: Derives the presenting identity's role: {@code ownerActorId} yields OWNER, otherwise the highest
     * matching {@code members} entry wins, and no match at all returns {@code null} meaning no access.
     * @param base 参数 知识库载体；parameter the knowledge base carrier.
     * @param actorId 参数 主体稳定标识；parameter the stable actor identifier.
     * @return 返回 角色或 null；returns the role or null.
     */
    private static KnowledgeMemberRoleEnum deriveRole(
            KnowledgeBaseBO base,
            String actorId) {
        if (StringUtils.equals(base.getOwnerActorId(), actorId)) {
            return KnowledgeMemberRoleEnum.OWNER;
        }
        KnowledgeMemberRoleEnum derived = null;
        for (KnowledgeMemberDTO member : membersOf(base)) {
            if (!StringUtils.equals(member.getActorId(), actorId) || member.getRole() == null) {
                continue;
            }
            if (derived == null || member.getRole().getCode() > derived.getCode()) {
                derived = member.getRole();
            }
        }
        return derived;
    }

    /**
     * 中文说明：读取成员的空安全视图，未投影时返回空列表而不是 null，便于逐条判定。
     * English summary: Reads a null-safe member view, answering an empty list rather than null when nothing is projected.
     * @param base 参数 知识库载体；parameter the knowledge base carrier.
     * @return 返回 成员列表；returns the members.
     */
    private static List<KnowledgeMemberDTO> membersOf(KnowledgeBaseBO base) {
        List<KnowledgeMemberDTO> members = base.getMembers();
        return members == null ? List.of() : members;
    }

    /**
     * 中文说明：复核创建知识库所需的功能权限：{@code KB_CREATE} 逻辑标签或知识写能力，
     * 二者都不具备按 403 拒绝（创建时还没有成员集合可用，成员判定无从谈起）。
     * English summary: Re-checks the authority a knowledge base creation needs: the logical {@code KB_CREATE} label or the
     * knowledge write capability, holding neither being a 403 (no member set exists yet, so membership cannot decide).
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     */
    private static void assertCreateAuthority(AdminActor actor) {
        Set<String> granted = new LinkedHashSet<>(actor.scopes());
        granted.addAll(actor.roles());
        for (String authority : CREATE_AUTHORITIES) {
            if (granted.contains(authority)) {
                return;
            }
        }
        throw new CommonException(
                403,
                "KNOWLEDGE_FORBIDDEN",
                "creating a knowledge base requires the KB_CREATE authority"
        );
    }

    /**
     * 中文说明：幂等意图键的规范化：调用方给出的键长度足够即用其 trim 值；否则用给定片段拼成的
     * SHA-256 十六进制（恰 64 字符）作为确定性键，使重放命中同一意图。载体约束是 16–64，
     * 短键必须替换而不是让校验在持久边界才失败。
     * English summary: Normalizes the idempotency intent: a supplied key of sufficient length is used trimmed, otherwise the
     * given fragments are folded into a SHA-256 hex digest (exactly 64 characters) as a deterministic key so a replay hits
     * the same intent. The carrier bound is 16–64, so a short key is replaced rather than failing validation at the
     * persistence edge.
     * @param supplied 参数 调用方键，可为空；parameter the supplied key, optional.
     * @param seedParts 参数 确定性派生片段；parameter the fragments seeding the deterministic key.
     * @return 返回 16–64 字符的意图键；returns the intent key inside 16–64 characters.
     */
    private static String intentKey(
            String supplied,
            String... seedParts) {
        String trimmed = StringUtils.trimToEmpty(supplied);
        if (trimmed.length() >= INTENT_KEY_MIN_LENGTH && isPrintableAscii(trimmed)) {
            return trimmed;
        }
        StringBuilder seed = new StringBuilder();
        for (String part : seedParts) {
            seed.append('|').append(StringUtils.trimToEmpty(part));
        }
        return sha256Hex(seed.toString());
    }

    /**
     * 中文说明：意图键只接受可打印 ASCII，避免把控制字符带进唯一键。
     * English summary: Only printable ASCII is accepted for an intent key so control characters never reach a unique column.
     * @param value 参数 待判定文本；parameter the candidate text.
     * @return 返回 是否全部可打印 ASCII；returns whether every character is printable ASCII.
     */
    private static boolean isPrintableAscii(String value) {
        for (int index = 0; index < value.length(); index = index + 1) {
            char current = value.charAt(index);
            if (current < 0x21 || current > 0x7E) {
                return false;
            }
        }
        return true;
    }

    /**
     * 中文说明：比较既有作业与本次请求的规范化摘要：一致即复用意图，不一致按
     * 409 {@code KNOWLEDGE_IDEMPOTENCY_CONFLICT}，绝不用同一意图键覆盖既有作业。
     * English summary: Compares the stored job's canonical digest with this request's: equal reuses the intent, different is
     * a 409 {@code KNOWLEDGE_IDEMPOTENCY_CONFLICT}, and a stored job is never overwritten under the same intent key.
     * @param existing 参数 命中的既有作业；parameter the matched existing job.
     * @param requestHash 参数 本次请求摘要；parameter this request's digest.
     */
    private static void assertSameIntent(
            KnowledgeJobBO existing,
            String requestHash) {
        if (!Objects.equals(existing.getRequestHash(), requestHash)) {
            throw new CommonException(
                    409,
                    "KNOWLEDGE_IDEMPOTENCY_CONFLICT",
                    "the idempotency key was reused with a different payload"
            );
        }
    }

    /**
     * 中文说明：上传重放的返回：意图命中时按 {@code resourceId} 回到既有文档并带上 {@code latestJobId}，
     * 不重复建文档、不重复建 revision、不重复建作业；调用方因此看到与首次完全一致的投影。
     * English summary: Answers an upload replay: the intent hit resolves back to the existing document by {@code resourceId}
     * carrying its {@code latestJobId}, creating no second document, revision or job, so the caller sees exactly the first
     * projection.
     * @param known 参数 已读取的文档载体，尚未读取时为 null；parameter the already-read document carrier, null when unread.
     * @param existing 参数 命中的既有作业；parameter the matched existing job.
     * @param requestHash 参数 本次请求摘要；parameter this request's digest.
     * @return 返回 文档投影；returns the document projection.
     */
    private KnowledgeDocumentVO replayedUpload(
            KnowledgeDocumentBO known,
            KnowledgeJobBO existing,
            String requestHash) {
        assertSameIntent(existing, requestHash);
        KnowledgeDocumentBO document = known == null
                ? requireDocument(existing.getKbId(), existing.getResourceId())
                : known;
        log.info(
                "KNOWLEDGE_UPLOAD_REPLAYED kbId={} documentId={} jobId={} status={}",
                document.getKbId(),
                document.getId(),
                existing.getId(),
                existing.getStatus()
        );
        return documentView(document);
    }

    /**
     * 中文说明：知识库创建意图重放：摘要一致后必须重新按当前成员判定可读性，
     * 撤权窗口不能靠缓存绕过；行已不可见按 404。
     * English summary: Replays a knowledge base create intent: after the digests agree, readability is re-decided from the
     * current membership because a revocation window may not be bypassed from a cached record, and an invisible row is a 404.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param replay 参数 既有幂等记录；parameter the stored idempotency record.
     * @param commandHash 参数 本次命令摘要；parameter this command's digest.
     * @return 返回 知识库投影；returns the knowledge base projection.
     */
    private KnowledgeBaseVO replayedBase(
            AdminActor actor,
            IdempotencyBO replay,
            String commandHash) {
        if (!Objects.equals(replay.getPayloadSha256(), commandHash)) {
            throw new CommonException(
                    409,
                    "KNOWLEDGE_IDEMPOTENCY_CONFLICT",
                    "the idempotency key was reused with a different payload"
            );
        }
        KnowledgeBaseBO base = knowledgeRepository.findBase(replay.getResourceId())
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "KNOWLEDGE_RESOURCE_NOT_FOUND knowledge base was not found"
                ));
        KnowledgeMemberRoleEnum role = deriveRole(base, actor.actorId());
        if (role == null || !READER_OR_ABOVE.contains(role)) {
            throw new CommonException(
                    403,
                    "KNOWLEDGE_FORBIDDEN",
                    "the stored resource is no longer readable by this identity"
            );
        }
        return baseView(base, role);
    }

    /**
     * 中文说明：按别名读取并复核 chat 模型：必须存在、已启用且 kind 为 CHAT；不发任何网络请求，
     * 存在性与启用状态都属于命令时的服务端复核。
     * English summary: Reads and re-checks the chat alias: it must exist, be enabled and be of kind CHAT, with no network
     * call at all, since existence and enablement belong to the command-time server-side review.
     * @param modelKey 参数 chat 别名；parameter the chat alias.
     * @return 返回 模型业务载体；returns the model carrier.
     */
    private LlmModelBO resolveChatAlias(String modelKey) {
        LlmModelBO model = resolveAlias(modelKey, "chatModel");
        if (LlmModelKindEnum.CHAT != model.getKind()) {
            throw invalidAlias("chatModel must reference a CHAT model alias");
        }
        return model;
    }

    /**
     * 中文说明：按别名读取并复核 embedding 模型：必须存在、已启用、kind 为 EMBEDDING、
     * 已冻结 {@code embeddingSpaceId} 与 {@code dimensions}，且其全部路由渠道仍是 LOCAL 部署
     * ——文档向量永远没有云端兜底路径。
     * English summary: Reads and re-checks the embedding alias: it must exist, be enabled, be of kind EMBEDDING, already
     * freeze {@code embeddingSpaceId} and {@code dimensions}, and keep every routed channel on a LOCAL deployment — documents
     * have no cloud embedding path at all.
     * @param modelKey 参数 embedding 别名；parameter the embedding alias.
     * @return 返回 模型业务载体；returns the model carrier.
     */
    private LlmModelBO resolveEmbeddingAlias(String modelKey) {
        LlmModelBO model = resolveAlias(modelKey, "embeddingModel");
        if (LlmModelKindEnum.EMBEDDING != model.getKind()) {
            throw invalidAlias("embeddingModel must reference an EMBEDDING model alias");
        }
        if (StringUtils.isBlank(model.getEmbeddingSpaceId()) || model.getDimensions() == null) {
            throw invalidAlias("the embedding alias must already freeze embeddingSpaceId and dimensions");
        }
        assertEmbeddingRoutesLocal(model);
        return model;
    }

    /**
     * 中文说明：读取别名行：不存在或已停用按 422 拒绝（字段指向了不可用的别名），
     * 不当作 404 也不静默改用其它别名。
     * English summary: Reads the alias row: absent or disabled is a 422 (the field points at an unusable alias), neither a
     * 404 nor a silent substitution of another alias.
     * @param modelKey 参数 别名；parameter the alias.
     * @param field 参数 字段名；parameter the field name.
     * @return 返回 模型业务载体；returns the model carrier.
     */
    private LlmModelBO resolveAlias(
            String modelKey,
            String field) {
        String alias = StringUtils.trim(modelKey);
        LlmModelBO model = llmConfigurationRepository.findModel(alias)
                .orElseThrow(() -> invalidAlias(field + " references an unknown model alias"));
        if (!Boolean.TRUE.equals(model.getEnabled())) {
            throw invalidAlias(field + " references a disabled model alias");
        }
        return model;
    }

    /**
     * 中文说明：EMBEDDING 模型的本地性复核：一次性批量读取路由渠道，任一渠道未知、停用或非 LOCAL
     * 都按 422 拒绝，绝不把文档向量改道云端。
     * English summary: Re-checks the locality of an EMBEDDING model by reading its routed channels in one batch: an unknown,
     * disabled or non-LOCAL channel is a 422 and document vectors are never rerouted to a cloud deployment.
     * @param model 参数 EMBEDDING 模型载体；parameter the embedding model carrier.
     */
    private void assertEmbeddingRoutesLocal(LlmModelBO model) {
        List<LlmRouteBindingDTO> routes = model.getRoutes();
        if (routes == null || routes.isEmpty()) {
            throw invalidAlias("the embedding alias has no routed channel to embed through");
        }
        Set<String> keys = new LinkedHashSet<>();
        for (LlmRouteBindingDTO route : routes) {
            keys.add(route.getChannelKey());
        }
        Map<String, LlmChannelBO> channels = new LinkedHashMap<>();
        for (LlmChannelBO channel : llmConfigurationRepository.findChannelsByKeys(keys)) {
            channels.put(channel.getChannelKey(), channel);
        }
        for (String key : keys) {
            LlmChannelBO channel = channels.get(key);
            if (channel == null) {
                throw invalidAlias("the embedding alias routes at an unknown channel");
            }
            if (!Boolean.TRUE.equals(channel.getEnabled())
                    || LlmDeploymentEnum.LOCAL != channel.getDeployment()) {
                throw invalidAlias("the embedding alias may only route at enabled LOCAL channels");
            }
        }
    }

    /**
     * 中文说明：别名复核失败的统一 422 载体。
     * English summary: The shared 422 carrier for a failed alias review.
     * @param reason 参数 可定位的字段原因；parameter the addressable field reason.
     * @return 返回 待抛出的异常；returns the exception to throw.
     */
    private static CommonException invalidAlias(String reason) {
        return new CommonException(422, "KNOWLEDGE_VALIDATION_FAILED", reason);
    }

    /**
     * 中文说明：成员集合规则复核：非空、至多 {@value #MAX_MEMBERS} 条、actorId 唯一且非空、
     * 角色全部落在 {@code READER/EDITOR/OWNER} 词汇表内、有且仅有一个 OWNER 且其 actorId 等于
     * {@code ownerActorId}（不能删除或转移当前 owner）；违反一律 422。
     * English summary: Re-checks the member set rules: non-empty, at most {@value #MAX_MEMBERS} entries, unique non-blank
     * actorIds, roles inside the {@code READER/EDITOR/OWNER} vocabulary, and exactly one OWNER whose actorId equals
     * {@code ownerActorId} (the current owner may be neither removed nor transferred); every violation is a 422.
     * @param base 参数 当前知识库载体；parameter the current knowledge base carrier.
     * @param members 参数 待替换的成员集合；parameter the incoming member set.
     * @return 返回 校验后的成员副本；returns the validated copy.
     */
    private static List<KnowledgeMemberDTO> assertMemberSet(
            KnowledgeBaseBO base,
            List<KnowledgeMemberDTO> members) {
        if (members == null || members.isEmpty() || members.size() > MAX_MEMBERS) {
            throw invalidAlias("members must hold between 1 and " + MAX_MEMBERS + " entries");
        }
        Set<String> seen = new LinkedHashSet<>();
        List<KnowledgeMemberDTO> copies = new ArrayList<>(members.size());
        int owners = 0;
        String ownerMember = null;
        for (KnowledgeMemberDTO member : members) {
            if (member == null || StringUtils.isBlank(member.getActorId()) || member.getRole() == null) {
                throw invalidAlias("every member needs an actorId and a role");
            }
            if (!seen.add(member.getActorId())) {
                throw invalidAlias("member actorIds must be unique");
            }
            if (KnowledgeMemberRoleEnum.OWNER == member.getRole()) {
                owners++;
                ownerMember = member.getActorId();
            }
            copies.add(member(member.getActorId(), member.getRole()));
        }
        if (owners != 1 || !StringUtils.equals(base.getOwnerActorId(), ownerMember)) {
            throw invalidAlias("members must carry exactly one OWNER entry matching ownerActorId");
        }
        return copies;
    }

    /**
     * 中文说明：成员载体的防御性构造，避免调用方在异步序列化期间改写已进入持久边界的数据。
     * English summary: Builds a defensive member carrier so a caller cannot mutate data that already reached the persistence
     * boundary while it is being serialized.
     * @param actorId 参数 成员主体；parameter the member actor.
     * @param role 参数 成员角色；parameter the member role.
     * @return 返回 成员载体；returns the member carrier.
     */
    private static KnowledgeMemberDTO member(
            String actorId,
            KnowledgeMemberRoleEnum role) {
        return KnowledgeMemberDTO.builder()
                .actorId(StringUtils.trim(actorId))
                .role(role)
                .build();
    }

    /**
     * 中文说明：成员副本，逐条重建载体而不共享引用。
     * English summary: Copies the member list, rebuilding each carrier instead of sharing references.
     * @param members 参数 来源成员；parameter the source members.
     * @return 返回 副本列表；returns the copy.
     */
    private static List<KnowledgeMemberDTO> copyMembers(List<KnowledgeMemberDTO> members) {
        List<KnowledgeMemberDTO> copies = new ArrayList<>(members.size());
        for (KnowledgeMemberDTO member : members) {
            copies.add(member(member.getActorId(), member.getRole()));
        }
        return copies;
    }

    /**
     * 中文说明：复核文件名：trim 后非空、不超过 255 且携带扩展名，违反按 422。
     * English summary: Re-checks the file name: non-blank after trimming, at most 255 characters and carrying an extension,
     * each violation being a 422.
     * @param fileName 参数 原始文件名；parameter the incoming file name.
     * @return 返回 规范化文件名；returns the normalized file name.
     */
    private static String assertFileName(String fileName) {
        String trimmed = StringUtils.trimToEmpty(fileName);
        if (trimmed.isEmpty() || trimmed.length() > 255 || StringUtils.indexOf(trimmed, '.') < 0) {
            throw new CommonException(
                    422,
                    "KNOWLEDGE_VALIDATION_FAILED",
                    "fileName must be a non-blank name of at most 255 characters carrying an extension"
            );
        }
        return trimmed;
    }

    /**
     * 中文说明：mediaType 与扩展名联合复核：声明类型命中已解析集合即采纳；否则由扩展名推导（MD/TXT/PDF/DOCX/XLSX），
     * 保证持久化的 mediaType 永不为空；两者都不成立按 415 {@code KNOWLEDGE_MEDIA_UNSUPPORTED}。
     * English summary: Checks mediaType and extension jointly: a declared type inside the parseable set wins, otherwise the
     * extension derives it (MD/TXT/PDF/DOCX/XLSX) so the persisted mediaType is never blank, and a joint miss is a 415
     * {@code KNOWLEDGE_MEDIA_UNSUPPORTED}.
     * @param fileName 参数 已规范化的文件名；parameter the normalized file name.
     * @param mediaType 参数 声明的 media type；parameter the declared media type.
     * @return 返回 规范化 media type；returns the normalized media type.
     */
    private static String assertMediaType(
            String fileName,
            String mediaType) {
        String declared = StringUtils.trimToEmpty(mediaType).toLowerCase(Locale.ROOT);
        if (SUPPORTED_MEDIA_TYPES.contains(declared)) {
            return declared;
        }
        String derived = SUPPORTED_FILE_EXTENSIONS.get(StringUtils.substringAfterLast(fileName, ".")
                .toLowerCase(Locale.ROOT));
        if (derived != null) {
            return derived;
        }
        throw new CommonException(
                415,
                "KNOWLEDGE_MEDIA_UNSUPPORTED",
                "the parser supports markdown, plain text, pdf, docx and xlsx only"
        );
    }

    /**
     * 中文说明：字节数复核：非空、至少 1 字节、且不超过部署配置 {@code maxUploadBytes}（合同上限 20MiB），
     * 越界按 413 {@code KNOWLEDGE_FILE_TOO_LARGE}；空文件按 422 拒绝。
     * English summary: Re-checks the byte count: non-null, at least one byte, and within the deployed
     * {@code maxUploadBytes} (20MiB contract ceiling), an overflow being a 413 {@code KNOWLEDGE_FILE_TOO_LARGE} and an empty
     * upload a 422.
     * @param content 参数 原始字节；parameter the raw bytes.
     * @return 返回 原始字节的防御性副本；returns a defensive copy of the raw bytes.
     */
    private byte[] assertUploadSize(byte[] content) {
        if (content == null || content.length == 0) {
            throw new CommonException(
                    422,
                    "KNOWLEDGE_VALIDATION_FAILED",
                    "an uploaded document must carry at least one byte"
            );
        }
        long ceiling = Math.min(20_971_520L, knowledgeProperties.getMaxUploadBytes());
        if (content.length > ceiling) {
            throw new CommonException(
                    413,
                    "KNOWLEDGE_FILE_TOO_LARGE",
                    "the uploaded document exceeds the configured size ceiling"
            );
        }
        return content.clone();
    }

    /**
     * 中文说明：把期望 revision 规范化为正值：null 或非正按 422 拒绝，避免退化成无条件写。
     * English summary: Normalizes the expected revision to a positive value, a null or non-positive one being a 422 so the
     * write may never degrade into an unconditional one.
     * @param expectedRevision 参数 调用方期望值；parameter the caller expectation.
     * @param resourceId 参数 资源 id，用于可定位消息；parameter the resource id used to address the message.
     * @return 返回 规范化后的期望值；returns the normalized expectation.
     */
    private static long requireExpectedRevision(
            Long expectedRevision,
            String resourceId) {
        if (expectedRevision == null || expectedRevision < 1L) {
            throw new CommonException(
                    422,
                    "KNOWLEDGE_VALIDATION_FAILED",
                    "expectedRevision is required and must be positive for " + resourceId
            );
        }
        return expectedRevision;
    }

    /**
     * 中文说明：乐观版本预检：现值与期望不一致即抛 409 并携带库中权威 {@code currentRevision}，
     * 因此沿用 {@link GatewayAdminRevisionConflictException} 以便既有 409 响应体保留现值。
     * English summary: Applies the optimistic revision pre-check: a mismatch raises 409 carrying the stored authoritative
     * {@code currentRevision}, which is why {@link GatewayAdminRevisionConflictException} is kept so the existing 409 body
     * preserves that value.
     * @param storedRevision 参数 库中现值；parameter the stored revision.
     * @param expectedRevision 参数 调用方期望值；parameter the caller expectation.
     */
    private static void assertRevision(
            long storedRevision,
            long expectedRevision) {
        if (storedRevision != expectedRevision) {
            throw new GatewayAdminRevisionConflictException(storedRevision);
        }
    }

    /**
     * 中文说明：CAS 未命中后的裁决：重读现值，行仍在即 409 携带新现值，行已消失即 404；
     * 0 行永远不返回成功。
     * English summary: Adjudicates a CAS miss by re-reading the row: still present means 409 carrying the fresh value, gone
     * means 404, and a zero-row effect is never returned as success.
     * @param kbId 参数 知识库 id；parameter the knowledge base id.
     * @param documentId 参数 文档 id；parameter the document id.
     * @return 返回 待抛出的异常；returns the exception to throw.
     */
    private RuntimeException revisionConflictOf(
            String kbId,
            String documentId) {
        return knowledgeRepository.findDocument(kbId, documentId)
                .map(document -> (RuntimeException) new GatewayAdminRevisionConflictException(document.getRevision()))
                .orElseGet(() -> new GatewayAdminNotFoundException(
                        "KNOWLEDGE_RESOURCE_NOT_FOUND knowledge document was not found"
                ));
    }

    /**
     * 中文说明：以期望 revision 执行知识库整行 CAS，未命中即重读现值抛 409（行消失则 404）。
     * English summary: Performs the full-row knowledge base compare-and-set at the expected revision, a miss re-reading the
     * stored value for a 409 (or a 404 once the row is gone).
     * @param candidate 参数 完整替换载体；parameter the full replacement carrier.
     * @param expectedRevision 参数 期望 revision；parameter the expected revision.
     * @param kbId 参数 知识库 id；parameter the knowledge base id.
     * @return 返回 已提交载体；returns the committed carrier.
     */
    private KnowledgeBaseBO casBase(
            KnowledgeBaseBO candidate,
            long expectedRevision,
            String kbId) {
        if (knowledgeRepository.replaceBase(candidate, expectedRevision)) {
            return knowledgeRepository.findBase(kbId)
                    .orElseThrow(() -> new GatewayAdminNotFoundException(
                            "KNOWLEDGE_RESOURCE_NOT_FOUND knowledge base was not found"
                    ));
        }
        throw knowledgeRepository.findBase(kbId)
                .map(base -> (RuntimeException) new GatewayAdminRevisionConflictException(base.getRevision()))
                .orElseGet(() -> new GatewayAdminNotFoundException(
                        "KNOWLEDGE_RESOURCE_NOT_FOUND knowledge base was not found"
                ));
    }

    /**
     * 中文说明：在既有文档上追加新版本：只复核期望 revision 与文件名，不创建第二个文档行；
     * 返回携带权威 id 与现值的载体供后续绑定 {@code latestJobId}。
     * English summary: Appends a version onto an existing document, re-checking only the expected revision and the file name
     * without creating a second document row, and returns the carrier holding the authoritative id and revision for the
     * later {@code latestJobId} binding.
     * @param document 参数 已读取的文档载体；parameter the read document carrier.
     * @param command 参数 上传命令；parameter the upload command.
     * @param fileName 参数 规范化文件名；parameter the normalized file name.
     * @return 返回 文档载体；returns the document carrier.
     */
    private KnowledgeDocumentBO appendVersionTo(
            KnowledgeDocumentBO document,
            KnowledgeUploadCommandDTO command,
            String fileName) {
        long expected = requireExpectedRevision(command.getExpectedRevision(), document.getId());
        assertRevision(document.getRevision(), expected);
        return documentCarrier(document).setFileName(fileName).setRevision(expected);
    }

    /**
     * 中文说明：把新作业的 id 绑定到文档的 {@code latestJobId}：这是同事务内的第二次 CAS，
     * 因此上传后的文档投影一定携带作业指针，客户端可据此轮询；未命中即 409 并整体回滚三行写入。
     * English summary: Binds the fresh job's id onto the document's {@code latestJobId} — the second compare-and-set of the
     * same transaction — so an uploaded document projection always carries the job pointer the client polls with, a miss
     * being a 409 that rolls all three rows back.
     * @param document 参数 文档载体；parameter the document carrier.
     * @param jobId 参数 新作业 id；parameter the fresh job id.
     * @return 返回 绑定后的权威文档载体；returns the bound authoritative document carrier.
     */
    private KnowledgeDocumentBO bindLatestJob(
            KnowledgeDocumentBO document,
            String jobId) {
        long expected = Math.max(1L, document.getRevision());
        KnowledgeDocumentBO candidate = documentCarrier(document)
                .setLatestJobId(jobId)
                .setRevision(expected);
        if (!knowledgeRepository.updateDocument(candidate, expected)) {
            throw revisionConflictOf(document.getKbId(), document.getId());
        }
        return requireDocument(document.getKbId(), document.getId());
    }

    /**
     * 中文说明：构造 {@code STAGING} 修订载体：原始字节、{@code byteCount} 与小写十六进制 SHA-256
     * 摘要在同一次写入中固化，嵌入空间与维度取知识库的冻结值（缺失即 503，因为没有本地嵌入别名可用、
     * 也绝不允许云端兜底），切分配置按部署参数冻结，{@code extractedText} 在 PARSE 之前保持 null，
     * {@code chunkCount} 以 0 起始。
     * English summary: Builds the {@code STAGING} revision carrier, freezing the raw bytes, {@code byteCount} and the
     * lowercase SHA-256 hex digest in one write, taking the embedding space and dimensions from the knowledge base's frozen
     * values (their absence is a 503, since no LOCAL embedding alias would be configured and a cloud fallback is never
     * permitted), freezing the chunking configuration from the deployment parameters, keeping {@code extractedText} null
     * until PARSE, and starting {@code chunkCount} at zero.
     * @param kbId 参数 知识库 id；parameter the knowledge base id.
     * @param documentId 参数 文档 id；parameter the document id.
     * @param fileName 参数 文件名；parameter the file name.
     * @param mediaType 参数 media type；parameter the media type.
     * @param rawBytes 参数 原始字节副本；parameter the defensive copy of the raw bytes.
     * @param contentHash 参数 内容摘要；parameter the content digest.
     * @param base 参数 知识库载体；parameter the knowledge base carrier.
     * @return 返回 修订载体；returns the revision carrier.
     */
    private KnowledgeDocumentRevisionBO revisionCarrier(
            String kbId,
            String documentId,
            String fileName,
            String mediaType,
            byte[] rawBytes,
            String contentHash,
            KnowledgeBaseBO base) {
        if (StringUtils.isBlank(base.getEmbeddingSpaceId()) || base.getDimensions() == null) {
            throw new CommonException(
                    503,
                    "KNOWLEDGE_MODEL_UNAVAILABLE",
                    "the knowledge base has no frozen LOCAL embedding space to index into"
            );
        }
        return KnowledgeDocumentRevisionBO.builder()
                .kbId(kbId)
                .documentId(documentId)
                .fileName(fileName)
                .mediaType(mediaType)
                .rawBytes(rawBytes)
                .byteCount((long) rawBytes.length)
                .contentHash(contentHash)
                .extractedText(null)
                .embeddingSpaceId(base.getEmbeddingSpaceId())
                .dimensions(base.getDimensions())
                .chunkingConfig(frozenChunkingConfig())
                .status(KnowledgeRevisionStatusEnum.STAGING)
                .chunkCount(0)
                .revision(CREATE_REVISION)
                .build();
    }

    /**
     * 中文说明：冻结切分配置，键与库内 {@code chunking_config} 约定一致
     * （{@code strategy}/{@code chunkSize}/{@code overlap}/{@code maxChunks}），值取自部署配置，
     * 使同一作业重放得到完全相同的分块口径。
     * English summary: Freezes the chunking configuration under the columns' agreed keys ({@code strategy},
     * {@code chunkSize}, {@code overlap}, {@code maxChunks}) with values from the deployment configuration, so a replay of
     * the same job derives exactly the same chunking basis.
     * @return 返回 冻结配置节点；returns the frozen configuration node.
     */
    private JsonNode frozenChunkingConfig() {
        return objectMapper.createObjectNode()
                .put("strategy", CHUNKING_STRATEGY)
                .put("chunkSize", knowledgeProperties.getChunkSize())
                .put("overlap", knowledgeProperties.getChunkOverlap())
                .put("maxChunks", MAX_CHUNKS);
    }

    /**
     * 中文说明：构造摄取作业载荷：冻结待处理 {@code STAGING} revision id 与来源事实（知识库 id、文件名、
     * media type、内容摘要、模型别名、嵌入空间与维度），绝不写入密钥、正文或上游响应体。
     * {@code revisionId} 是 {@code KnowledgeJobStrategy} 唯一的摄取入口，缺失即无从定位待向量化对象，
     * 因此载荷只在 revision 行插入之后构造；请求摘要与之解耦（见 {@link #canonicalUploadCommand}），
     * 令重放判定在任何写入发生之前完成。
     * English summary: Builds the ingestion payload, freezing the {@code STAGING} revision id to be processed next to the
     * source facts (base id, file name, media type, content digest, model alias, embedding space and dimensions) and never a
     * secret, body or upstream response. {@code revisionId} is the ingestion strategy's single entry point — without it there
     * is nothing to locate and embed — so the payload is built only once the revision row exists, while the request digest
     * stays decoupled from it (see {@link #canonicalUploadCommand}) so the replay decision completes before any write.
     * @param kbId 参数 知识库 id；parameter the knowledge base id.
     * @param revisionId 参数 待摄取的 STAGING revision id；parameter the staging revision id to ingest.
     * @param fileName 参数 文件名；parameter the file name.
     * @param mediaType 参数 media type；parameter the media type.
     * @param contentHash 参数 内容摘要；parameter the content digest.
     * @param base 参数 知识库载体；parameter the knowledge base carrier.
     * @return 返回 载荷节点；returns the payload node.
     */
    private JsonNode ingestPayload(
            String kbId,
            String revisionId,
            String fileName,
            String mediaType,
            String contentHash,
            KnowledgeBaseBO base) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("baseId", kbId);
        payload.put("contentHash", contentHash);
        payload.put("dimensions", base.getDimensions());
        payload.put("embeddingSpace", base.getEmbeddingSpaceId());
        payload.put("fileName", fileName);
        payload.put("mediaType", mediaType);
        payload.put("model", base.getEmbeddingModel());
        payload.put("revisionId", revisionId);
        return payload;
    }

    /**
     * 中文说明：构造 {@code QUEUED} 作业载体：{@code stage=QUEUED}、{@code attempt=0}、
     * {@code nextAttemptAt} 不晚于当前时刻、{@code leaseToken=0}、创建意图 revision 为 0 哨兵值，
     * 幂等意图与请求摘要一并落库供复用判定。租约三列在认领前保持空。
     * English summary: Builds the {@code QUEUED} job carrier: {@code stage = QUEUED}, {@code attempt = 0},
     * {@code nextAttemptAt} no later than now, {@code leaseToken = 0} and the create-intent revision sentinel, storing the
     * idempotency intent together with the request digest for the reuse decision. The three lease columns stay empty until
     * a claim.
     * @param kbId 参数 知识库 id；parameter the knowledge base id.
     * @param actor 参数 发起主体；parameter the submitting actor.
     * @param documentId 参数 目标文档 id，即 {@code resourceId}；parameter the target document, the {@code resourceId}.
     * @param intentKey 参数 幂等意图键；parameter the idempotency intent.
     * @param requestHash 参数 规范化请求摘要；parameter the canonical request digest.
     * @param payload 参数 冻结载荷；parameter the frozen payload.
     * @return 返回 作业载体；returns the job carrier.
     */
    private KnowledgeJobBO queuedJob(
            String kbId,
            AdminActor actor,
            String documentId,
            String intentKey,
            String requestHash,
            JsonNode payload) {
        return KnowledgeJobBO.builder()
                .kbId(kbId)
                .type(KnowledgeJobTypeEnum.DOCUMENT_INGEST)
                .resourceId(documentId)
                .actorId(actor.actorId())
                .payload(payload)
                .idempotencyKey(intentKey)
                .requestHash(requestHash)
                .status(KnowledgeJobStatusEnum.QUEUED)
                .stage(KnowledgeJobStageEnum.QUEUED)
                .attempt(0)
                .nextAttemptAt(clock.instant())
                .leaseOwner(null)
                .leaseToken(0L)
                .leaseExpiresAt(null)
                .errorCode(null)
                .result(null)
                .retryOfJobId(null)
                .revision(CREATE_REVISION)
                .build();
    }

    /**
     * 中文说明：把知识库载体上需要保留的业务列复制成替换载体，避免整行替换意外清空 owner、
     * members 或冻结的嵌入空间。
     * English summary: Copies the knowledge base columns that must survive a full replace so the replacement cannot
     * accidentally clear the owner, the members, or the frozen embedding space.
     * @param current 参数 当前载体；parameter the current carrier.
     * @return 返回 复制后的载体；returns the copy.
     */
    private static KnowledgeBaseBO baseCarrier(KnowledgeBaseBO current) {
        return KnowledgeBaseBO.builder()
                .id(current.getId())
                .name(current.getName())
                .description(current.getDescription())
                .ownerActorId(current.getOwnerActorId())
                .members(new ArrayList<>(membersOf(current)))
                .egressPolicy(current.getEgressPolicy())
                .chatModel(current.getChatModel())
                .embeddingModel(current.getEmbeddingModel())
                .embeddingSpaceId(current.getEmbeddingSpaceId())
                .dimensions(current.getDimensions())
                .revision(current.getRevision())
                .build();
    }

    /**
     * 中文说明：复制文档载体，保留活动 revision 指针与文件名，只让调用方覆盖本次要推进的列。
     * English summary: Copies a document carrier, keeping the active revision pointer and the file name so only the columns
     * this call advances are overwritten.
     * @param current 参数 当前载体；parameter the current carrier.
     * @return 返回 复制后的载体；returns the copy.
     */
    private static KnowledgeDocumentBO documentCarrier(KnowledgeDocumentBO current) {
        return KnowledgeDocumentBO.builder()
                .id(current.getId())
                .kbId(current.getKbId())
                .fileName(current.getFileName())
                .activeRevisionId(current.getActiveRevisionId())
                .latestJobId(current.getLatestJobId())
                .revision(current.getRevision())
                .build();
    }

    /**
     * 中文说明：知识库投影，{@code myRole} 由调用方传入的当次派生结果决定。
     * English summary: Projects a knowledge base, taking {@code myRole} from the caller's freshly derived value.
     * @param base 参数 知识库载体；parameter the knowledge base carrier.
     * @param role 参数 当次派生角色；parameter the role derived for this presentation.
     * @return 返回 知识库投影；returns the knowledge base projection.
     */
    private static KnowledgeBaseVO baseView(
            KnowledgeBaseBO base,
            KnowledgeMemberRoleEnum role) {
        return KnowledgeBaseVO.builder()
                .id(base.getId())
                .name(base.getName())
                .description(base.getDescription())
                .ownerActorId(base.getOwnerActorId())
                .myRole(role)
                .egressPolicy(base.getEgressPolicy())
                .chatModel(base.getChatModel())
                .embeddingModel(base.getEmbeddingModel())
                .embeddingSpaceId(base.getEmbeddingSpaceId())
                .dimensions(base.getDimensions())
                .revision(base.getRevision())
                .build();
    }

    /**
     * 中文说明：文档投影，{@code status} 按最新作业状态派生显示态（无最新作业即 PROCESSING），
     * 绝不读取或输出原文字节与提取文本。
     * English summary: Projects a document, deriving {@code status} from the latest job's state (no latest job means
     * PROCESSING) and never reading or emitting raw bytes or extracted text.
     * @param document 参数 文档载体；parameter the document carrier.
     * @return 返回 文档投影；returns the document projection.
     */
    private KnowledgeDocumentVO documentView(KnowledgeDocumentBO document) {
        return KnowledgeDocumentVO.builder()
                .id(document.getId())
                .kbId(document.getKbId())
                .fileName(document.getFileName())
                .activeRevisionId(document.getActiveRevisionId())
                .latestJobId(document.getLatestJobId())
                .revision(document.getRevision())
                .status(deriveDocumentStatus(document))
                .createdAt(document.getCreatedAt())
                .build();
    }

    /**
     * 中文说明：派生文档显示态：{@code SUCCEEDED→READY}、{@code FAILED→FAILED}，
     * 其余（含 RUNNING/RETRY_WAIT/STALE/CANCELLED 与作业缺失）一律 PROCESSING；
     * 文档的活动状态与作业状态分开，摄取失败不会把已有活动版本判为不可用。
     * English summary: Derives the document's display state: {@code SUCCEEDED→READY}, {@code FAILED→FAILED} and everything
     * else (RUNNING, RETRY_WAIT, STALE, CANCELLED and a missing job included) as PROCESSING; the document's active state and
     * the job state stay separate, so a failed ingestion never marks an existing active version unusable.
     * @param document 参数 文档载体；parameter the document carrier.
     * @return 返回 显示态字符串；returns the display state.
     */
    private String deriveDocumentStatus(KnowledgeDocumentBO document) {
        if (StringUtils.isBlank(document.getLatestJobId())) {
            return "PROCESSING";
        }
        return knowledgeRepository.findJob(document.getLatestJobId())
                .map(KnowledgeJobBO::getStatus)
                .map(DOCUMENT_STATUS::get)
                .orElse("PROCESSING");
    }

    /**
     * 中文说明：修订投影，只输出元数据与状态；{@code rawBytes} 与服务器路径不在任何字段里，
     * {@code text} 是 PARSE 之后才有的提取文本。
     * English summary: Projects a revision, emitting metadata and status only: {@code rawBytes} and any server path are absent
     * from every field, and {@code text} is the extracted text that only exists after PARSE.
     * @param revision 参数 修订载体；parameter the revision carrier.
     * @return 返回 修订投影；returns the revision projection.
     */
    private static KnowledgeDocumentRevisionVO revisionView(KnowledgeDocumentRevisionBO revision) {
        return KnowledgeDocumentRevisionVO.builder()
                .id(revision.getId())
                .documentId(revision.getDocumentId())
                .kbId(revision.getKbId())
                .fileName(revision.getFileName())
                .mediaType(revision.getMediaType())
                .byteCount(revision.getByteCount())
                .text(revision.getExtractedText())
                .hash(revision.getContentHash())
                .status(revision.getStatus())
                .createdAt(revision.getCreatedAt())
                .build();
    }

    /**
     * 中文说明：作业投影，只输出安全状态字段与时刻，不含租约、载荷、actor 内部字段或结果结构。
     * English summary: Projects a job, emitting the safe status fields and instants only, without the lease, the payload, the
     * internal actor fields, or the result shape.
     * @param job 参数 作业载体；parameter the job carrier.
     * @return 返回 作业投影；returns the job projection.
     */
    private static KnowledgeJobVO jobView(KnowledgeJobBO job) {
        return KnowledgeJobVO.builder()
                .id(job.getId())
                .kbId(job.getKbId())
                .type(job.getType())
                .resourceId(job.getResourceId())
                .status(job.getStatus())
                .stage(job.getStage())
                .revision(job.getRevision())
                .attempt(job.getAttempt())
                .errorCode(job.getErrorCode())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    /**
     * 中文说明：构造创建命令的规范化节点，键按字典序写入，使同一命令在重放时得到稳定摘要。
     * English summary: Builds the canonical node of a create command with alphabetically written keys, so replaying the same
     * command yields a stable digest.
     * @param command 参数 创建命令；parameter the create command.
     * @return 返回 规范化节点；returns the canonical node.
     */
    private JsonNode canonicalCreateCommand(KnowledgeBaseCommandDTO command) {
        ObjectNode canonical = objectMapper.createObjectNode();
        canonical.put("chatModel", StringUtils.trimToEmpty(command.getChatModel()));
        canonical.put("description", StringUtils.trimToEmpty(command.getDescription()));
        canonical.put("egressPolicy", String.valueOf(command.getEgressPolicy()));
        canonical.put("embeddingModel", StringUtils.trimToEmpty(command.getEmbeddingModel()));
        canonical.put("name", StringUtils.trimToEmpty(command.getName()));
        return canonical;
    }

    /**
     * 中文说明：构造上传命令的规范化节点：只取调用方给出的事实（知识库、可选目标文档、文件名、
     * 规范化 media type 与原件摘要），键按字典序写入。它刻意<b>不</b>含服务端生成的 id，
     * 因此摘要能在任何行写入之前算出，重放与 409 判定也就无需先写后读。
     * English summary: Builds the canonical upload node from caller-supplied facts only (base, optional target document,
     * file name, normalized media type and the original's digest) with alphabetically written keys. It deliberately holds
     * <b>no</b> server-generated id, which is why the digest is available before any row exists and the replay plus 409
     * decision needs no write-then-read.
     * @param kbId 参数 知识库 id；parameter the knowledge base id.
     * @param documentId 参数 可选目标文档 id（同文档新版本）；parameter the optional target document id.
     * @param fileName 参数 已规范化文件名；parameter the normalized file name.
     * @param mediaType 参数 已规范化 media type；parameter the normalized media type.
     * @param contentHash 参数 原件摘要；parameter the digest of the original bytes.
     * @return 返回 规范化节点；returns the canonical node.
     */
    private JsonNode canonicalUploadCommand(
            String kbId,
            String documentId,
            String fileName,
            String mediaType,
            String contentHash) {
        ObjectNode canonical = objectMapper.createObjectNode();
        canonical.put("contentHash", contentHash);
        canonical.put("documentId", StringUtils.trimToEmpty(documentId));
        canonical.put("fileName", fileName);
        canonical.put("kbId", kbId);
        canonical.put("mediaType", mediaType);
        canonical.put("operation", "UPLOAD");
        return canonical;
    }

    /**
     * 中文说明：构造重索引命令的规范化节点：目标文档、来源 revision 与来源摘要构成不可变意图，
     * 键按字典序写入；{@code operation} 前缀保证同一 {@code Idempotency-Key} 在上传与重投之间不会互相冒充。
     * English summary: Builds the canonical reindex node whose immutable intent is the target document, the source revision
     * and the source digest, written with alphabetical keys; the {@code operation} prefix keeps one {@code Idempotency-Key}
     * from impersonating itself between an upload and a reindex.
     * @param kbId 参数 知识库 id；parameter the knowledge base id.
     * @param documentId 参数 目标文档 id；parameter the target document id.
     * @param sourceRevisionId 参数 来源 revision id；parameter the source revision id.
     * @param contentHash 参数 来源内容摘要；parameter the source content digest.
     * @return 返回 规范化节点；returns the canonical node.
     */
    private JsonNode canonicalReindexCommand(
            String kbId,
            String documentId,
            String sourceRevisionId,
            String contentHash) {
        ObjectNode canonical = objectMapper.createObjectNode();
        canonical.put("contentHash", StringUtils.trimToEmpty(contentHash));
        canonical.put("documentId", documentId);
        canonical.put("kbId", kbId);
        canonical.put("operation", "REINDEX");
        canonical.put("sourceRevisionId", sourceRevisionId);
        return canonical;
    }

    /**
     * 中文说明：SHA-256 小写十六进制摘要，用于内容摘要、请求摘要与幂等 scope。
     * English summary: The lowercase SHA-256 hex digest behind content hashes, request digests and idempotency scopes.
     * @param value 参数 待摘要文本；parameter the text to digest.
     * @return 返回 64 字符摘要；returns the 64-character digest.
     */
    private static String sha256Hex(String value) {
        return sha256Hex(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * 中文说明：字节版 SHA-256 摘要；算法必然存在，缺失属于运行时环境故障而不是业务分支。
     * English summary: The byte-array SHA-256 digest; the algorithm is guaranteed, so its absence is an environment failure
     * rather than a business branch.
     * @param value 参数 待摘要字节；parameter the bytes to digest.
     * @return 返回 64 字符摘要；returns the 64-character digest.
     */
    private static String sha256Hex(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
