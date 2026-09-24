package top.egon.cola.component.yuheng.admin.knowledge.service.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeDocumentRevisionBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeJobBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeMemberDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgePageQueryDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeRetryCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobStageEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobStatusEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobTypeEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeMemberRoleEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeRevisionStatusEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeJobVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgePageVO;
import top.egon.cola.component.yuheng.admin.knowledge.repository.KnowledgeRepository;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeJobService;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeJobStrategy;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminNotFoundException;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;

/**
 * 中文说明：{@code KnowledgeJobServiceImpl} 是 {@link KnowledgeJobService} 的实现，同时管两件事：管理面的作业读
 * （API-019/020）与人工重试（API-021），以及 worker 侧的租约生命周期（认领、心跳、终态发布）。
 * 管理面一律先身份、再角色、再状态、最后受守卫 CAS：作业行读不到（跨租户、软删、从未存在）按
 * 404 {@code KNOWLEDGE_RESOURCE_NOT_FOUND}，行读得到但主体在该知识库上没有足够角色按
 * 403 {@code KNOWLEDGE_FORBIDDEN}，顺序固定以免用 403 泄漏存在性；对外只投影安全状态字段，
 * 租约三列、{@code payload}、{@code result} 内部结构与 actor 细节一律不外传。
 * English summary: {@code KnowledgeJobServiceImpl} implements {@link KnowledgeJobService} and owns two halves: the
 * management face's job reads (API-019/020) plus manual retry (API-021), and the worker-side lease lifecycle of claim,
 * heartbeat and terminal publication. The management half always runs identity → role → state → guarded
 * compare-and-set: a job row that cannot be read (foreign tenant, soft-deleted, never created) is a 404
 * {@code KNOWLEDGE_RESOURCE_NOT_FOUND} while a readable row whose base grants the presenting identity no sufficient role is
 * a 403 {@code KNOWLEDGE_FORBIDDEN}, in that fixed order so a 403 never leaks existence; only the safe status fields are
 * projected while the three lease columns, {@code payload}, the {@code result} shape and the actor's internals stay inside.
 *
 * 用法 / Usage: 由 {@code knowledgeJobController} 与 {@code KnowledgeJobWorker} 以限定名
 * （bean {@code knowledgeJobServiceImpl}）注入；协作者是 {@code knowledgeRepository}、
 * {@code knowledgeJobStrategyRegistry}（{@code Map<KnowledgeJobTypeEnum, KnowledgeJobStrategy>}）、
 * {@code KnowledgeProperties} 与 {@code knowledgeClock}。四条不变量在这里收口：其一，所有权只由
 * {@code id + lease_token + status = 'RUNNING' + lease_expires_at > now()} 的 CAS 证明，影响 0 行意味着所有权
 * 已丢失而绝不是成功，因此发布是恰一次（模型调用不在此列，供应商侧不具幂等性）；其二，类型路由只查注册表，
 * 已认领但注册表缺失的类型当场落 {@code FAILED} + {@code KNOWLEDGE_STRATEGY_UNAVAILABLE} 终态，既不静默丢弃也
 * 不伪造成功（Rule 9：本类任何位置都没有对 {@link KnowledgeJobTypeEnum} 的 {@code switch}/{@code if-else}）；
 * 其三，自动重试只在预算内排 {@code RETRY_WAIT}，尝试数按 5s/30s 递增且总数不超过 {@code maxAttempts}，越界一律
 * 降级 {@code FAILED}；其四，人工重试创建<b>后继行</b>（{@code retryOfJobId} 指向失败作业、新意图、{@code attempt}
 * 归零），失败历史行原样保留、从不改写。所有模型调用都发生在本类之外，事务里没有任何出网。
 * / Inject it by qualifier ({@code knowledgeJobServiceImpl}) into the controller and the worker; its collaborators are
 * {@code knowledgeRepository}, {@code knowledgeJobStrategyRegistry} (a {@code Map<KnowledgeJobTypeEnum, KnowledgeJobStrategy>}),
 * {@code KnowledgeProperties} and {@code knowledgeClock}. Four invariants close here: first, ownership is proven only by the
 * {@code id + lease_token + status = 'RUNNING' + lease_expires_at > now()} compare-and-set, a zero-row effect means ownership
 * was lost and never success, so publication is exactly-once (a vendor model call is not, and is deliberately excluded);
 * second, dispatch is a registry lookup only, and a claimed type missing from the map is closed on the spot as terminal
 * {@code FAILED} + {@code KNOWLEDGE_STRATEGY_UNAVAILABLE}, neither a silent drop nor a fabricated success (Rule 9: no
 * {@code switch}/{@code if-else} over {@link KnowledgeJobTypeEnum} exists anywhere in this class); third, an automatic retry
 * schedules {@code RETRY_WAIT} only inside the budget, attempts advancing on the 5s/30s ladder and never past
 * {@code maxAttempts}, anything beyond being downgraded to {@code FAILED}; fourth, a manual retry creates a
 * <b>successor</b> row ({@code retryOfJobId} addressing the failed job, a fresh intent, {@code attempt} reset) and leaves the
 * failed history untouched and never rewritten. Every model call happens outside this class, so no transaction holds a
 * network round trip.
 */
@Slf4j
@Validated
@Service("knowledgeJobServiceImpl")
@RequiredArgsConstructor
public class KnowledgeJobServiceImpl implements KnowledgeJobService {

    /** 中文说明：{@code gateway_knowledge_job.attempt} 的库内 CHECK 上界，配置再大也越不过去。 English summary: the stored {@code attempt} ceiling, which no larger configuration overrides. */
    private static final int ATTEMPT_CEILING = 3;

    /** 创建意图的乐观版本哨兵值，与既有管理端写法一致 / the optimistic revision sentinel meaning create intent, as in the existing management services. */
    private static final long CREATE_REVISION = 0L;

    /** 幂等意图键的最小长度，低于它一律改用确定性生成的键 / the shortest accepted idempotency intent, shorter values fall back to a generated key. */
    private static final int INTENT_KEY_MIN_LENGTH = 16;

    /** 单个文档的分块上限，与 {@code chunk_index} 的 CHECK 0..9999 及 {@code KnowledgeServiceImpl} 同口径 / the chunk ceiling per document, matching the {@code chunk_index} CHECK 0..9999 and the base service. */
    private static final int MAX_CHUNKS = 10_000;

    /** 待摄取 revision 在载荷中的固定键名，与 {@code DocumentIngestionStrategy} 的读取口径一致 / the payload key holding the revision to ingest, matching the ingestion strategy's reader. */
    private static final String REVISION_ID_FIELD = "revisionId";

    /** 来源 revision 的备选键名（重索引沿用），读取顺序与策略端一致 / the alternate source-revision key, read in the same order as the strategy does. */
    private static final String SOURCE_REVISION_ID_FIELD = "sourceRevisionId";

    /** 已认领类型缺少策略时的终态机器码，只写进 {@code error_code} 列 / the terminal machine code for a claimed type without a strategy, written into {@code error_code} only. */
    private static final String STRATEGY_UNAVAILABLE = "KNOWLEDGE_STRATEGY_UNAVAILABLE";

    /** 读不到行时的机器码 / the machine code of an unreadable row. */
    private static final String NOT_FOUND = "KNOWLEDGE_RESOURCE_NOT_FOUND";

    /** 角色不足时的机器码 / the machine code of an insufficient role. */
    private static final String FORBIDDEN = "KNOWLEDGE_FORBIDDEN";

    /** 状态或字段不成立时的机器码 / the machine code of an impossible state or field. */
    private static final String VALIDATION_FAILED = "KNOWLEDGE_VALIDATION_FAILED";

    /** 允许读取作业投影的角色：READER 及以上 / the roles allowed to read a job projection: READER or above. */
    private static final Set<KnowledgeMemberRoleEnum> READER_OR_ABOVE = EnumSet.of(
            KnowledgeMemberRoleEnum.READER,
            KnowledgeMemberRoleEnum.EDITOR,
            KnowledgeMemberRoleEnum.OWNER
    );

    /** 允许列表与重试的角色：EDITOR 及以上（API-020/021 的授权口径）/ the roles allowed to list and retry (the API-020/021 authorization). */
    private static final Set<KnowledgeMemberRoleEnum> EDITOR_OR_ABOVE = EnumSet.of(
            KnowledgeMemberRoleEnum.EDITOR,
            KnowledgeMemberRoleEnum.OWNER
    );

    /** 允许建后继行的终态：只有 {@code FAILED} 与 {@code STALE} / the terminal states a successor may be built for: {@code FAILED} and {@code STALE} only. */
    private static final Set<KnowledgeJobStatusEnum> RETRYABLE_TERMINAL = EnumSet.of(
            KnowledgeJobStatusEnum.FAILED,
            KnowledgeJobStatusEnum.STALE
    );

    /** {@code publishTerminal} 可接受的写回状态：四个终态加自动重试排程 / the write-back states {@code publishTerminal} accepts: the four terminal ones plus the automatic retry schedule. */
    private static final Set<KnowledgeJobStatusEnum> PUBLISHABLE = EnumSet.of(
            KnowledgeJobStatusEnum.SUCCEEDED,
            KnowledgeJobStatusEnum.FAILED,
            KnowledgeJobStatusEnum.STALE,
            KnowledgeJobStatusEnum.CANCELLED,
            KnowledgeJobStatusEnum.RETRY_WAIT
    );

    /** 冻结切分配置的策略名，与 {@code KnowledgeServiceImpl} 创建修订时同源 / the frozen chunking strategy name, taken from the same source as the service that creates revisions. */
    private static final String CHUNKING_STRATEGY = "FIXED_WINDOW";

    @Qualifier("knowledgeRepository")
    private final KnowledgeRepository knowledgeRepository;

    @Qualifier("knowledgeJobStrategyRegistry")
    private final Map<KnowledgeJobTypeEnum, KnowledgeJobStrategy> knowledgeJobStrategyRegistry;

    @Qualifier(KnowledgeProperties.BEAN_NAME)
    private final KnowledgeProperties knowledgeProperties;

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    @Qualifier("knowledgeClock")
    private final Clock clock;

    /**
     * 中文说明：执行 getJob 操作（API-019）；先读作业行（读不到即 404），再按该作业所属知识库判角色，
     * 无足够角色即 403；投影只含安全字段与本方法读到的同一权威行，绝不因为要判断权限而推进任何状态。
     * English summary: Executes the getJob operation (API-019): the job row is read first (an unreadable one being a 404) and
     * the role is then decided on the base that job belongs to, no sufficient role being a 403; the projection carries the
     * safe fields of that same authoritative row and no state ever advances just because authorization was needed.
     *
     * 用法 / Usage: {@code knowledgeJobServiceImpl.getJob(actor, jobId)}；
     * {@code QUEUED}/{@code RUNNING}/{@code RETRY_WAIT} 是进行中，其余是终态。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param jobId 参数 作业十进制字符串 id；parameter decimal-string job id.
     * @return 返回 作业投影；returns the job projection.
     */
    @Override
    @Transactional(readOnly = true)
    public KnowledgeJobVO getJob(
            AdminActor actor,
            String jobId) {
        KnowledgeJobBO job = requireJob(jobId);
        requireVisibleBase(actor, job.getKbId(), READER_OR_ABOVE);
        return jobView(job);
    }

    /**
     * 中文说明：执行 listJobs 操作（API-020）；EDITOR 及以上，按知识库分页读取作业，可选状态过滤只在作业
     * 词汇表内取值（资料修订独有的 {@code STAGING}/{@code READY} 与任何非法值一律 422），次序由仓储固定为
     * {@code create_time DESC, id DESC}，总数与当页共用同一状态谓词，空页返回 {@code []} 且 {@code total=0}。
     * 本方法是纯读：绝不认领、绝不续租、也绝不推进任何任务状态——列表刷新不是调度器。
     * English summary: Executes the listJobs operation (API-020), EDITOR or above, paging one base's jobs with an optional
     * status filter drawn only from the job vocabulary (a document-revision-only {@code STAGING}/{@code READY} or any illegal
     * value being a 422), in the repository's fixed {@code create_time DESC, id DESC} order, the total sharing the page's
     * status predicate and an empty page answering {@code []} with {@code total = 0}. The method is purely reading: it never
     * claims, never renews a lease and never advances any task state, because refreshing a list is not a scheduler.
     *
     * 用法 / Usage: {@code knowledgeJobServiceImpl.listJobs(actor, kbId, query)}；未声明的
     * {@code search} 字段刻意不参与任何过滤。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param query 参数 分页查询载体，可携带状态过滤；parameter the page query carrier, optionally status-filtered.
     * @return 返回 作业分页投影；returns the paged job projection.
     */
    @Override
    @Transactional(readOnly = true)
    public KnowledgePageVO<KnowledgeJobVO> listJobs(
            AdminActor actor,
            String kbId,
            KnowledgePageQueryDTO query) {
        requireVisibleBase(actor, kbId, EDITOR_OR_ABOVE);
        KnowledgeJobStatusEnum status = jobStatusFilter(query.getStatus());
        int page = query.getPage();
        int size = query.getSize();
        long total = knowledgeRepository.countJobs(kbId, status);
        List<KnowledgeJobVO> items = total == 0L
                ? List.of()
                : knowledgeRepository.listJobs(kbId, status, page, size).stream()
                        .map(KnowledgeJobServiceImpl::jobView)
                        .toList();
        return new KnowledgePageVO<>(items, page, size, total);
    }

    /**
     * 中文说明：执行 retryJob 操作（API-021）；顺序为读旧行（404）→ EDITOR 判色（403）→ 状态可重试
     * （只有 {@code FAILED}/{@code STALE}，否则 422）→ 期望 revision 预检（不符 409 并携带库中现值）→
     * 幂等意图回读（命中同摘要即返回既有后继，摘要分歧 409）→ 重新冻结有效来源 → 插入后继行。
     * “重新冻结有效来源”按载荷事实而非作业类型判断：来源修订仍是 {@code STAGING} 就原样复用（恢复可用），
     * 已经 {@code READY} 或 {@code FAILED} 则以同一段原件新插一条 {@code STAGING} 修订并把新 id 写进后继载荷，
     * 因为摄取端把非 {@code STAGING} 来源判为 {@code STALE}；没有修订键的载荷（例如未来的 Wiki 作业）原样继承。
     * 旧行的状态、错误码与结果<b>从不改写</b>，{@code expectedRevision} 因而是读侧前置条件而非被消费的版本。
     * English summary: Executes the retryJob operation (API-021) in the order read the old row (404) → EDITOR
     * authorization (403) → retryable state ({@code FAILED}/{@code STALE} only, anything else 422) → expected-revision
     * pre-check (a mismatch being 409 carrying the stored value) → idempotency re-read (an intent hit with an equal digest
     * returning the existing successor, a diverged digest 409) → re-freeze of the effective source → insert of the successor
     * row. That re-freeze is decided from payload facts rather than from the job type: a source revision still
     * {@code STAGING} is reused untouched so recovery stays possible, while one already {@code READY} or {@code FAILED} gets a
     * fresh {@code STAGING} revision built from the very same original bytes with its new id written into the successor's
     * payload, since the ingestion side judges a non-{@code STAGING} source {@code STALE}; a payload without any revision key
     * (a future wiki job, for instance) is inherited as it stands. The old row's status, error code and result are
     * <b>never</b> rewritten, so {@code expectedRevision} is a read-side precondition rather than a consumed version.
     *
     * 用法 / Usage: {@code knowledgeJobServiceImpl.retryJob(actor, jobId, command, idempotencyKey)}；
     * 后继创建在单个短事务内完成，本方法不调用任何模型；上游可能再次计费，控制器据返回的 {@code jobId}
     * 回 202 与 {@code Location}。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param jobId 参数 被重试作业的十进制字符串 id；parameter decimal-string id of the job being retried.
     * @param command 参数 重试命令，携带期望 revision；parameter the retry command carrying the expected revision.
     * @param idempotencyKey 参数 可选 {@code Idempotency-Key}，不足 16 字符时改用派生键；parameter the optional
     *                       {@code Idempotency-Key}, replaced below 16 characters by a derived intent.
     * @return 返回 后继作业投影；returns the successor job projection.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeJobVO retryJob(
            AdminActor actor,
            String jobId,
            KnowledgeRetryCommandDTO command,
            String idempotencyKey) {
        KnowledgeJobBO predecessor = requireJob(jobId);
        KnowledgeBaseBO base = requireVisibleBase(actor, predecessor.getKbId(), EDITOR_OR_ABOVE);
        if (!RETRYABLE_TERMINAL.contains(predecessor.getStatus())) {
            throw new CommonException(
                    422,
                    VALIDATION_FAILED,
                    "only a FAILED or STALE job can be retried, this one is " + predecessor.getStatus()
            );
        }
        assertRevision(predecessor.getRevision(), requireExpectedRevision(command.getExpectedRevision()));
        String intentKey = intentKey(
                idempotencyKey,
                actor.actorId(),
                predecessor.getKbId(),
                predecessor.getType().wireValue(),
                predecessor.getId()
        );
        String requestHash = sha256Hex(canonicalRetryCommand(predecessor).toString());
        KnowledgeJobBO intent = knowledgeRepository
                .findJobByIntent(
                        predecessor.getKbId(),
                        actor.actorId(),
                        predecessor.getType(),
                        intentKey
                )
                .orElse(null);
        if (intent != null) {
            assertSameIntent(intent, requestHash);
            log.info("KNOWLEDGE_JOB_RETRY_REPLAYED jobId={} successorId={} kbId={} actorId={}",
                    jobId, intent.getId(), intent.getKbId(), actor.actorId());
            return jobView(intent);
        }
        KnowledgeJobBO successor = knowledgeRepository.insertJob(successorOf(
                predecessor, base, actor, intentKey, requestHash));
        log.info("KNOWLEDGE_JOB_RETRY_CREATED predecessorId={} successorId={} kbId={} documentId={} actorId={}",
                predecessor.getId(), successor.getId(), successor.getKbId(), successor.getResourceId(),
                actor.actorId());
        return jobView(successor);
    }

    /**
     * 中文说明：执行 claimAvailable 操作；只在有空闲槽位时被调用，认领上限取 {@code slots} 与部署配置
     * {@code claim-size} 的较小值，因此“每次数据库 claim 不得超过可用槽”在配置与调用两侧同时成立。
     * 返回的每个载体都已由仓储以 {@code FOR UPDATE SKIP LOCKED} 翻到 {@code RUNNING} 并携带本次认领的单调递增
     * 租约令牌；无可认领作业即返回空列表，绝不伪造一行。认领之后立刻用注册表查类型：注册表里没有该类型的
     * 实现时，本次认领当场以 {@code FAILED} + {@code KNOWLEDGE_STRATEGY_UNAVAILABLE} 收口（沿用同一个租约令牌
     * CAS，写不进去也只说明所有权已易手），并从返回集合中剔除——既不静默丢弃，也不留下永远无人认领的
     * {@code RUNNING} 行。除此之外本方法不做任何业务工作，更不出网。
     * English summary: Executes the claimAvailable operation, called only while slots are free, the claim bound being the
     * smaller of {@code slots} and the deployed {@code claim-size} so "a database claim never exceeds the free slots" holds on
     * both the configuration and the call side. Every returned carrier has already been flipped to {@code RUNNING} by the
     * repository under {@code FOR UPDATE SKIP LOCKED} and carries the monotonically advanced token of this claim; nothing
     * claimable answers an empty list and never a fabricated row. Right after claiming, the type is looked up in the registry:
     * when the registry holds no implementation for it, this very claim is closed as {@code FAILED} +
     * {@code KNOWLEDGE_STRATEGY_UNAVAILABLE} (through the same token CAS, a miss only meaning ownership already moved) and
     * dropped from the returned set — neither a silent drop nor a {@code RUNNING} row nobody will ever serve. Beyond that this
     * method performs no work at all and certainly no egress.
     *
     * 用法 / Usage: {@code knowledgeJobServiceImpl.claimAvailable(slots)}；调用方（worker）必须已在守卫上下文里
     * 恢复该轮作业的可信租户与 actor，本方法不接受租户入参；返回的作业按同一注册表取到策略后执行，
     * 并且必须按 30 秒心跳维持租约，否则其结果作废。
     * @param slots 参数 本次认领上限，等于空闲槽位数，非负；parameter claim bound equal to the free slot count,
     *              non-negative.
     * @return 返回 已认领、持租约且确有策略可用的作业载体；returns the claimed, leased carriers that really have a strategy.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<KnowledgeJobBO> claimAvailable(int slots) {
        if (slots <= 0) {
            return List.of();
        }
        int batch = Math.min(slots, Math.max(1, knowledgeProperties.getClaimSize()));
        List<KnowledgeJobBO> claimed = knowledgeRepository.claimNext(batch);
        List<KnowledgeJobBO> served = new ArrayList<>(claimed.size());
        for (KnowledgeJobBO job : claimed) {
            if (job != null && knowledgeJobStrategyRegistry.containsKey(job.getType())) {
                served.add(job);
                continue;
            }
            closeWithoutStrategy(job);
        }
        log.info("KNOWLEDGE_JOB_CLAIMED requested={} claimed={} served={}", batch, claimed.size(), served.size());
        return List.copyOf(served);
    }

    /**
     * 中文说明：执行 heartbeat 操作；用载体自带的权威租约令牌把到期时刻按配置 {@code lease} 顺延，
     * 不重新读取行、也不触碰任何业务进度列。写回条件由仓储收口为
     * {@code id + lease_token + status = 'RUNNING' + lease_expires_at > now()}，因此返回 {@code false} 只有三种可能：
     * 令牌已推进、租约被他人接管、作业已终态——三者都意味着所有权已丢失，调用方必须立即停止后续阶段并
     * 放弃发布。配置里的续租窗口非正同样按失去所有权的<b>预防</b>处理：无凭据可写时绝不让工作继续。
     * English summary: Executes the heartbeat operation, extending the expiry by the configured {@code lease} with the
     * authoritative token already on the carrier, re-reading nothing and touching no business progress column. The repository
     * closes the write under {@code id + lease_token + status = 'RUNNING' + lease_expires_at > now()}, so a {@code false} has
     * exactly three causes: the token advanced, another worker took the lease over, or the job already reached a terminal
     * state — every one of them meaning ownership is gone and the caller must halt the remaining stages and abandon
     * publication. A non-positive renewal window is treated preventively as lost ownership too: without a credential to
     * write with, work must not continue either.
     *
     * 用法 / Usage: {@code knowledgeJobServiceImpl.heartbeat(job)}；入参必须是
     * {@link #claimAvailable(int)} 返回的载体实例（30 秒心跳对 120 秒租约），长工作在阶段边界补心跳。
     * @param job 参数 持租约的作业载体；parameter the job carrier holding a lease.
     * @return 返回 所有权是否仍然成立；returns whether ownership still holds.
     */
    @Override
    public boolean heartbeat(KnowledgeJobBO job) {
        if (job == null || StringUtils.isBlank(job.getId()) || !holdsLease(job)) {
            return false;
        }
        Duration lease = knowledgeProperties.getLease();
        if (lease == null || lease.isZero() || lease.isNegative()) {
            log.warn("KNOWLEDGE_JOB_HEARTBEAT_REFUSED jobId={} lease={} reason=no renewal window", job.getId(), lease);
            return false;
        }
        boolean held = knowledgeRepository.heartbeat(job.getId(), job.getLeaseToken(), clock.instant().plus(lease));
        if (!held) {
            log.info("KNOWLEDGE_JOB_LEASE_LOST jobId={} token={} stage={} phase=heartbeat",
                    job.getId(), job.getLeaseToken(), job.getStage());
        }
        return held;
    }

    /**
     * 中文说明：执行 publishTerminal 操作；先读权威行（读不到即 404，绝不凭本地副本落笔），读只用于取回
     * 现值以构造写回载体，所有权本身一律交给 {@code lease_token + status = 'RUNNING' +
     * lease_expires_at > now()} 这条 CAS 裁决，因此过期或被接管的租约既不能发布版本也不能写终态。
     * 可写状态限于四个终态与 {@code RETRY_WAIT}，其余（含 {@code QUEUED}/{@code RUNNING}）按 422 拒绝：
     * 本方法只收尾，不排队也不重启。尝试数在这里统一递增并受 {@code maxAttempts}（与库内 {@code attempt
     * CHECK 0..3}）双重封顶：自动重试若已用完预算，就把 {@code RETRY_WAIT} 降级成 {@code FAILED}，
     * 于是“至多 3 次总尝试、5s/30s 退避、仅对显式可重试错误”在写回处收口，而不依赖每个策略自我约束。
     * CAS 命中后返回重读的权威载体；未命中即判定所有权已丢失：本次结论作废、绝不覆盖新持有者，也不在
     * 提交之后重试，只把库中现值如实交回。
     * English summary: Executes the publishTerminal operation: the authoritative row is read first (an unreadable one being a
     * 404, never a write from a local copy) while the read only supplies the current values the write-back carrier is
     * built from: ownership itself is decided solely by the CAS under {@code lease_token + status = 'RUNNING' +
     * lease_expires_at > now()}, which is why a lapsed lease can neither publish a revision nor write a terminal state. The
     * writable states are the four terminal ones plus {@code RETRY_WAIT}; anything else (including {@code QUEUED}/
     * {@code RUNNING}) is a 422 because this method only settles work, it neither enqueues nor restarts it. Attempts are
     * incremented centrally here and doubly capped by {@code maxAttempts} next to the stored {@code attempt CHECK 0..3}: a
     * {@code RETRY_WAIT} whose budget is spent is downgraded to {@code FAILED}, so "at most three attempts, 5s/30s back-off,
     * explicitly retryable errors only" closes at the write-back instead of relying on every strategy to police itself. A
     * hit returns the re-read authoritative carrier; a miss declares ownership lost: this outcome is void, it never
     * overwrites the new holder and is never retried after commit, only the stored value is handed back honestly.
     *
     * 用法 / Usage: {@code knowledgeJobServiceImpl.publishTerminal(job, leaseToken)}；{@code leaseToken} 必须是
     * {@link #claimAvailable(int)} 给出的值且此前 {@link #heartbeat(KnowledgeJobBO)} 仍成立；本方法内既无模型调用，
     * 也不会在未持租约时尝试切换活动版本。
     * @param job 参数 携带终态的作业载体；parameter the job carrier holding the terminal state.
     * @param leaseToken 参数 认领时获得的租约令牌，正整数；parameter the lease token acquired at claim time.
     * @return 返回 库中权威的作业业务载体；returns the authoritative job carrier from the database.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeJobBO publishTerminal(
            KnowledgeJobBO job,
            long leaseToken) {
        if (job == null || StringUtils.isBlank(job.getId())) {
            throw new CommonException(
                    422,
                    VALIDATION_FAILED,
                    "publishing a terminal state requires the id of a persisted job"
            );
        }
        KnowledgeJobBO current = requireJob(job.getId());
        KnowledgeJobBO writeBack = terminalCarrier(job, current);
        if (!knowledgeRepository.finish(writeBack, leaseToken)) {
            log.info("KNOWLEDGE_JOB_PUBLISH_LOST jobId={} token={} status={} outcome=void",
                    current.getId(), leaseToken, writeBack.getStatus());
            return knowledgeRepository.findJob(current.getId()).orElse(writeBack);
        }
        log.info("KNOWLEDGE_JOB_PUBLISHED jobId={} status={} stage={} attempt={} errorCode={}",
                current.getId(), writeBack.getStatus(), writeBack.getStage(), writeBack.getAttempt(),
                writeBack.getErrorCode());
        return knowledgeRepository.findJob(current.getId()).orElse(writeBack);
    }

    /**
     * 中文说明：读取作业行，读不到（含跨租户与软删）按 404 {@code KNOWLEDGE_RESOURCE_NOT_FOUND}；
     * 本方法从不因权限判定而改写任何状态。
     * English summary: Reads the job row, an unreadable one (foreign tenant or soft-deleted included) being a 404
     * {@code KNOWLEDGE_RESOURCE_NOT_FOUND}; nothing is ever rewritten because authorization needed deciding.
     * @param jobId 参数 作业十进制字符串 id；parameter decimal-string job id.
     * @return 返回 作业业务载体；returns the job carrier.
     */
    private KnowledgeJobBO requireJob(String jobId) {
        return knowledgeRepository.findJob(StringUtils.trimToEmpty(jobId))
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        NOT_FOUND + " knowledge job was not found"
                ));
    }

    /**
     * 中文说明：读取知识库并判定角色，是三个管理面方法共用的守卫：行不可见按 404，
     * 行可见但角色不在允许集合内按 403，顺序固定以免用 403 泄漏存在性。
     * English summary: Reads the base and decides the role, the guard shared by the three management methods: an invisible row
     * is a 404 while a visible row whose role sits outside the allowed set is a 403, always in that order so a 403 leaks no
     * existence.
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
                        NOT_FOUND + " knowledge base was not found"
                ));
        KnowledgeMemberRoleEnum role = deriveRole(base, actor.actorId());
        if (role == null || !allowed.contains(role)) {
            throw new CommonException(
                    403,
                    FORBIDDEN,
                    "the presenting identity holds no sufficient role on this knowledge base"
            );
        }
        return base;
    }

    /**
     * 中文说明：派生当前主体的角色：{@code ownerActorId} 命中即 OWNER，否则取 {@code members} 中该主体
     * 角色最高的一条，都不匹配返回 {@code null} 表示完全无权限；与 {@code KnowledgeServiceImpl} 同一口径。
     * English summary: Derives the presenting identity's role: {@code ownerActorId} yields OWNER, otherwise the highest
     * matching {@code members} entry wins, and no match at all returns {@code null} meaning no access; the same rule
     * {@code KnowledgeServiceImpl} applies.
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
        List<KnowledgeMemberDTO> members = base.getMembers() == null ? List.of() : base.getMembers();
        KnowledgeMemberRoleEnum derived = null;
        for (KnowledgeMemberDTO member : members) {
            if (member == null || !StringUtils.equals(member.getActorId(), actorId) || member.getRole() == null) {
                continue;
            }
            if (derived == null || member.getRole().getCode() > derived.getCode()) {
                derived = member.getRole();
            }
        }
        return derived;
    }

    /**
     * 中文说明：把可选状态过滤规范化为作业枚举：空值表示不过滤；作业词汇表之外的值（包括资料修订独有的
     * {@code STAGING}/{@code READY}）按 422 如实拒绝，而不是悄悄当成“无过滤”。
     * English summary: Normalizes the optional status filter into the job enum: blank means unfiltered while a value outside
     * the job vocabulary (the document-revision-only {@code STAGING}/{@code READY} included) is honestly a 422 rather than a
     * silently ignored filter.
     * @param status 参数 声明的状态字符串，可为空；parameter the declared status string, optional.
     * @return 返回 状态过滤或 null；returns the status filter or null.
     */
    private static KnowledgeJobStatusEnum jobStatusFilter(String status) {
        if (StringUtils.isBlank(status)) {
            return null;
        }
        try {
            return KnowledgeJobStatusEnum.fromWire(StringUtils.trim(status));
        } catch (IllegalArgumentException unsupported) {
            throw new CommonException(
                    422,
                    VALIDATION_FAILED,
                    "status must name a knowledge job state"
            );
        }
    }

    /**
     * 中文说明：构造后继作业载体：复用同一 {@code kbId}/{@code type}/{@code resourceId} 与重新冻结的来源载荷，
     * 以 {@code retryOfJobId} 指向失败行，意图键与请求摘要都是本次新算的，状态 {@code QUEUED}、
     * {@code stage=QUEUED}、{@code attempt=0}、{@code nextAttemptAt} 不晚于当前时刻、租约三列清空、
     * {@code errorCode}/{@code result} 不带过来，revision 用创建哨兵 0；失败行的历史因此完整保留。
     * English summary: Builds the successor carrier: the same {@code kbId}, {@code type} and {@code resourceId} plus the
     * re-frozen source payload are reused, {@code retryOfJobId} addresses the failed row, the intent key and request digest
     * are freshly derived, and the row starts {@code QUEUED} with {@code stage = QUEUED}, {@code attempt = 0},
     * {@code nextAttemptAt} no later than now, the three lease columns cleared, neither {@code errorCode} nor {@code result}
     * carried over and revision at the create sentinel, which is how the failed row's history stays complete.
     * @param predecessor 参数 失败或失效的原作业；parameter the failed or stale predecessor.
     * @param base 参数 已复核的知识库载体（提供冻结的嵌入空间与维度）；parameter the revalidated base carrying the
     *             frozen embedding space and dimensions.
     * @param actor 参数 发起重试的主体；parameter the retrying actor.
     * @param intentKey 参数 本次重试的幂等意图；parameter this retry's idempotency intent.
     * @param requestHash 参数 重试意图的规范化摘要；parameter the canonical digest of the retry intent.
     * @return 返回 待插入的后继载体；returns the successor carrier to insert.
     */
    private KnowledgeJobBO successorOf(
            KnowledgeJobBO predecessor,
            KnowledgeBaseBO base,
            AdminActor actor,
            String intentKey,
            String requestHash) {
        return KnowledgeJobBO.builder()
                .kbId(predecessor.getKbId())
                .type(predecessor.getType())
                .resourceId(predecessor.getResourceId())
                .actorId(actor.actorId())
                .payload(reFrozenPayload(predecessor, base))
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
                .retryOfJobId(predecessor.getId())
                .revision(CREATE_REVISION)
                .build();
    }

    /**
     * 中文说明：重新冻结有效来源，判定只看载荷里的事实而<b>不看作业类型</b>（Rule 9）：读
     * {@code revisionId}/{@code sourceRevisionId}，键缺失或取值不是十进制 id 即原样继承前身的载荷；
     * 来源修订仍是 {@code STAGING} 也原样继承，让自动恢复与人工重试共用同一份暂存数据；
     * 已是 {@code READY} 或 {@code FAILED} 则以同一段原件新插一条 {@code STAGING} 修订（嵌入空间与维度取知识库
     * 的冻结值、切分配置按当前部署值重新冻结、{@code chunkCount} 归零），并把新 id 写进载荷副本——
     * 摄取端对非 {@code STAGING} 来源一律判 {@code STALE}，不重做这一步人工重试就必然空转。
     * 来源修订行完全读不到时按 404 拒绝重试，绝不凭空造一个作业。
     * English summary: Re-freezes the effective source from payload facts alone and never from the job type (Rule 9):
     * {@code revisionId}/{@code sourceRevisionId} is read, and a missing key or a non-decimal value inherits the predecessor's
     * payload verbatim; a source revision still {@code STAGING} is inherited too, so automatic recovery and a manual retry
     * share the same staged data. Once it is {@code READY} or {@code FAILED}, a fresh {@code STAGING} revision is inserted from
     * the very same original bytes (embedding space and dimensions from the base's frozen values, the chunking configuration
     * re-frozen from the current deployment values and {@code chunkCount} reset) and its new id replaces the payload copy —
     * the ingestion side judges every non-{@code STAGING} source {@code STALE}, so without this step a manual retry would spin
     * for nothing. A source revision row that cannot be read at all refuses the retry with a 404 rather than inventing a job.
     * @param predecessor 参数 失败或失效的原作业；parameter the failed or stale predecessor.
     * @param base 参数 已复核的知识库载体；parameter the revalidated knowledge base carrier.
     * @return 返回 重新冻结后的载荷；returns the re-frozen payload.
     */
    private JsonNode reFrozenPayload(
            KnowledgeJobBO predecessor,
            KnowledgeBaseBO base) {
        String sourceRevisionId = frozenRevisionId(predecessor.getPayload());
        if (sourceRevisionId == null) {
            return predecessor.getPayload();
        }
        KnowledgeDocumentRevisionBO source = knowledgeRepository
                .findRevision(predecessor.getKbId(), sourceRevisionId)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        NOT_FOUND + " source revision of the retried job was not found"
                ));
        if (source.getStatus() == KnowledgeRevisionStatusEnum.STAGING) {
            return predecessor.getPayload();
        }
        KnowledgeDocumentRevisionBO staged = knowledgeRepository.insertRevision(stagingRevision(source, base));
        return withRevisionId(predecessor.getPayload(), staged.getId());
    }

    /**
     * 中文说明：以来源修订的原件重造一条 {@code STAGING} 行：字节、摘要、文件名与 media type 原样继承，
     * 嵌入空间与维度取知识库的冻结值（与既有向量空间不一致会被摄取端判 {@code STALE}），
     * 抽取文本一并带上以免重复解析，{@code chunkCount} 归零等待重新分块。
     * English summary: Rebuilds a {@code STAGING} row from the source revision's original: bytes, digest, file name and media
     * type are inherited as they stand, the embedding space and dimensions come from the base's frozen values (a mismatch
     * with the existing vector space being judged {@code STALE} by the ingestion side), the extracted text comes along so
     * nothing is re-parsed needlessly, and {@code chunkCount} resets to await fresh chunking.
     * @param source 参数 来源修订；parameter the source revision.
     * @param base 参数 知识库载体；parameter the knowledge base carrier.
     * @return 返回 待插入的暂存修订；returns the staging revision to insert.
     */
    private KnowledgeDocumentRevisionBO stagingRevision(
            KnowledgeDocumentRevisionBO source,
            KnowledgeBaseBO base) {
        if (StringUtils.isBlank(base.getEmbeddingSpaceId()) || base.getDimensions() == null) {
            throw new CommonException(
                    503,
                    "KNOWLEDGE_MODEL_UNAVAILABLE",
                    "the knowledge base has no frozen LOCAL embedding space to re-index into"
            );
        }
        byte[] rawBytes = source.getRawBytes();
        return KnowledgeDocumentRevisionBO.builder()
                .kbId(source.getKbId())
                .documentId(source.getDocumentId())
                .fileName(source.getFileName())
                .mediaType(source.getMediaType())
                .rawBytes(rawBytes)
                .byteCount(rawBytes == null ? null : (long) rawBytes.length)
                .contentHash(source.getContentHash())
                .extractedText(source.getExtractedText())
                .embeddingSpaceId(base.getEmbeddingSpaceId())
                .dimensions(base.getDimensions())
                .chunkingConfig(frozenChunkingConfig())
                .status(KnowledgeRevisionStatusEnum.STAGING)
                .chunkCount(0)
                .revision(CREATE_REVISION)
                .build();
    }

    /**
     * 中文说明：冻结切分配置，键与库内 {@code chunking_config} 的约定一致（{@code strategy}/{@code chunkSize}/
     * {@code overlap}/{@code maxChunks}），值取自部署配置，使重放与自动重试得到完全相同的分块口径。
     * English summary: Freezes the chunking configuration under the column's agreed keys ({@code strategy},
     * {@code chunkSize}, {@code overlap}, {@code maxChunks}) with values from the deployment configuration, so a replay and an
     * automatic retry derive exactly the same chunking basis.
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
     * 中文说明：读取载荷中冻结的来源修订 id：优先 {@code revisionId}，回退 {@code sourceRevisionId}，
     * 取值必须是十进制 id 且长度合法，否则返回 {@code null} 表示这份载荷与修订无关（例如未来的 Wiki 作业）；
     * 读取顺序与 {@code DocumentIngestionStrategy} 完全一致，两处任何一处改动都会让重试找错来源。
     * English summary: Reads the revision id frozen into a payload, preferring {@code revisionId} and falling back to
     * {@code sourceRevisionId}, and returns {@code null} unless the value is a well-formed decimal id, meaning the payload has
     * nothing to do with revisions (a future wiki job, for instance); the reading order is exactly the ingestion strategy's,
     * because changing either side would send a retry to the wrong source.
     * @param payload 参数 作业载荷，可为空；parameter the job payload, optional.
     * @return 返回 修订 id 或 null；returns the revision id or null.
     */
    private static String frozenRevisionId(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            return null;
        }
        String candidate = payload.path(REVISION_ID_FIELD).isTextual()
                ? payload.path(REVISION_ID_FIELD).asText()
                : payload.path(SOURCE_REVISION_ID_FIELD).isTextual()
                        ? payload.path(SOURCE_REVISION_ID_FIELD).asText()
                        : null;
        candidate = StringUtils.trimToNull(candidate);
        return candidate != null && candidate.matches("^[1-9][0-9]{0,19}$") ? candidate : null;
    }

    /**
     * 中文说明：把新的暂存修订 id 写进载荷副本：只改 {@code revisionId} 一个键，其余冻结事实原样继承，
     * 绝不就地改动前身载体持有的节点（前身行还要维持完整审计）。
     * English summary: Writes the new staging revision id into a copy of the payload, touching only the {@code revisionId} key
     * while every other frozen fact is inherited as it stands, and never mutating the node the predecessor carrier still
     * holds because that row has to keep a complete audit.
     * @param payload 参数 前身载荷；parameter the predecessor payload.
     * @param revisionId 参数 新暂存修订 id；parameter the new staging revision id.
     * @return 返回 后继载荷；returns the successor payload.
     */
    private static JsonNode withRevisionId(
            JsonNode payload,
            String revisionId) {
        if (payload == null || !payload.isObject()) {
            return payload;
        }
        JsonNode copy = payload.deepCopy();
        ((ObjectNode) copy).put(REVISION_ID_FIELD, revisionId);
        return copy;
    }

    /**
     * 中文说明：构造重试意图的规范化节点：被重试作业 id、其所属知识库、其类型与其原始摘要共同决定
     * “同 key 同原 job 返回同一后继、同 key 不同原 job 判 409”；{@code operation} 前缀让同一 {@code Idempotency-Key}
     * 不会在上传、重投与重试之间互相冒充。
     * English summary: Builds the canonical retry node: the retried job id, its base, its type and its own original digest
     * together decide "the same key and the same original job return the same successor while the same key on another job is
     * a 409", and the {@code operation} prefix keeps one {@code Idempotency-Key} from impersonating itself across an upload, a
     * reindex and a retry.
     * @param predecessor 参数 被重试的原作业；parameter the retried predecessor.
     * @return 返回 规范化节点；returns the canonical node.
     */
    private JsonNode canonicalRetryCommand(KnowledgeJobBO predecessor) {
        ObjectNode canonical = objectMapper.createObjectNode();
        canonical.put("jobId", StringUtils.trimToEmpty(predecessor.getId()));
        canonical.put("kbId", predecessor.getKbId());
        canonical.put("operation", "RETRY");
        canonical.put("requestHash", StringUtils.trimToEmpty(predecessor.getRequestHash()));
        canonical.put("type", predecessor.getType() == null ? "" : predecessor.getType().wireValue());
        return canonical;
    }

    /**
     * 中文说明：构造 {@code RETRY_WAIT} 或终态写回载体：状态、阶段、错误码与结果都取自策略返回的载体，
     * 尝试数则一律以库中权威值为基准递增并被 {@code maxAttempts} 与库内 CHECK 双重封顶；预算已用完的
     * {@code RETRY_WAIT} 降级为 {@code FAILED} 并保留其错误码，因此“超过 3 次尝试不再排 RETRY_WAIT”在写回处成立。
     * 载荷、意图与租约列都不由本载体决定，{@code finish} 也只覆写终态六列。
     * English summary: Builds the {@code RETRY_WAIT} or terminal write-back carrier: status, stage, error code and result come
     * from the carrier the strategy returned, while the attempt count always advances from the stored authoritative value and
     * is doubly capped by {@code maxAttempts} next to the column CHECK; a {@code RETRY_WAIT} whose budget is spent is
     * downgraded to {@code FAILED} keeping its error code, which is why "no RETRY_WAIT beyond three attempts" holds at the
     * write-back. The payload, the intent and the lease columns are not decided by this carrier, and {@code finish} overwrites
     * the six terminal columns only.
     * @param proposed 参数 策略返回的载体；parameter the carrier the strategy returned.
     * @param current 参数 库中权威的 {@code RUNNING} 行；parameter the authoritative {@code RUNNING} row.
     * @return 返回 写回载体；returns the write-back carrier.
     */
    private KnowledgeJobBO terminalCarrier(
            KnowledgeJobBO proposed,
            KnowledgeJobBO current) {
        KnowledgeJobStatusEnum status = proposed.getStatus();
        if (status == null || !PUBLISHABLE.contains(status)) {
            throw new CommonException(
                    422,
                    VALIDATION_FAILED,
                    "only a terminal state or RETRY_WAIT can be published, this one is " + status
            );
        }
        if (proposed.getStage() == null) {
            throw new CommonException(
                    422,
                    VALIDATION_FAILED,
                    "publishing a job state requires the stage it reached"
            );
        }
        int consumed = consumedAttempts(current.getAttempt(), proposed.getAttempt());
        if (status == KnowledgeJobStatusEnum.RETRY_WAIT && consumed >= effectiveMaxAttempts()) {
            log.info("KNOWLEDGE_JOB_RETRY_BUDGET_EXHAUSTED jobId={} attempt={} maxAttempts={} downgraded=FAILED",
                    current.getId(), consumed, effectiveMaxAttempts());
            return copyJob(current)
                    .setStatus(KnowledgeJobStatusEnum.FAILED)
                    .setStage(proposed.getStage())
                    .setAttempt(consumed)
                    .setNextAttemptAt(nextAttemptAt(KnowledgeJobStatusEnum.FAILED, proposed, current, consumed))
                    .setErrorCode(proposed.getErrorCode())
                    .setResult(proposed.getResult());
        }
        return copyJob(current)
                .setStatus(status)
                .setStage(proposed.getStage())
                .setAttempt(consumed)
                .setNextAttemptAt(nextAttemptAt(status, proposed, current, consumed))
                .setErrorCode(proposed.getErrorCode())
                .setResult(proposed.getResult());
    }

    /**
     * 中文说明：写回后的排程时刻：{@code RETRY_WAIT} 优先采纳策略给出的时刻（它才知道自己为何可重试），
     * 缺失才按 {@code retryDelays} 补算；真正的终态则继承策略的时刻，两者都为空时保留库中原值，
     * 因为终态行不再被认领，这一列只是审计而不是承诺。
     * English summary: The instant scheduled by a write-back: a {@code RETRY_WAIT} adopts the strategy's own value first (it
     * alone knows why the error was retryable) and only falls back to the {@code retryDelays} ladder, while a genuine terminal
     * state inherits the strategy's instant and, when that too is absent, keeps the stored value because a terminal row is
     * never claimed again and the column is audit rather than a promise.
     * @param status 参数 即将写入的状态；parameter the status about to be written.
     * @param proposed 参数 策略返回的载体；parameter the carrier the strategy returned.
     * @param current 参数 库中权威行；parameter the authoritative row.
     * @param consumed 参数 已消耗的尝试数；parameter the attempts consumed so far.
     * @return 返回 排程时刻；returns the scheduled instant.
     */
    private Instant nextAttemptAt(
            KnowledgeJobStatusEnum status,
            KnowledgeJobBO proposed,
            KnowledgeJobBO current,
            int consumed) {
        if (proposed.getNextAttemptAt() != null) {
            return proposed.getNextAttemptAt();
        }
        return status == KnowledgeJobStatusEnum.RETRY_WAIT
                ? retryNotBefore(consumed)
                : Objects.requireNonNullElseGet(current.getNextAttemptAt(), clock::instant);
    }

    /**
     * 中文说明：本次写回后已消耗的尝试数：以库中现值加一为准（每一次 RUNNING 都算消耗了一次），
     * 策略若显式给出更大的值则采纳，最后被配置上限与库内 CHECK 0..3 双重封顶。
     * English summary: The attempts consumed by this write-back: the stored value plus one leads (every {@code RUNNING} round
     * consumed one), a larger value the strategy states explicitly is adopted, and the configuration ceiling next to the
     * column CHECK 0..3 caps the result.
     * @param stored 参数 库中现值；parameter the stored count.
     * @param proposed 参数 策略给出的计数，可为空；parameter the strategy's count, optional.
     * @return 返回 封顶后的消耗数；returns the capped consumed count.
     */
    private int consumedAttempts(
            Integer stored,
            Integer proposed) {
        int observed = (stored == null ? 0 : stored) + 1;
        if (proposed != null && proposed > observed) {
            observed = proposed;
        }
        return Math.min(Math.max(observed, 0), Math.min(ATTEMPT_CEILING, effectiveMaxAttempts()));
    }

    /**
     * 中文说明：配置声明的总尝试上限，至少 1 且不超过库内 {@code attempt} 列的 CHECK 上界。
     * English summary: The attempt ceiling the configuration declares, at least one and never past the column CHECK ceiling.
     * @return 返回 有效上限；returns the effective ceiling.
     */
    private int effectiveMaxAttempts() {
        return Math.max(1, Math.min(ATTEMPT_CEILING, knowledgeProperties.getMaxAttempts()));
    }

    /**
     * 中文说明：{@code RETRY_WAIT} 的补算排程：策略没有给出时刻时才按 {@code retryDelays} 里已用尝试数对应的
     * 一档取值，越界取最后一档，序列为空或非法即当下可认领；5s/30s 两档与 {@code maxAttempts} 一起构成
     * “至多 3 次总尝试、5s/30s 退避”的时间事实。
     * English summary: The fallback schedule of a {@code RETRY_WAIT}: only when the strategy named no instant is the
     * {@code retryDelays} entry for the attempts already used taken, the last entry covering an overflow and an empty or
     * illegal sequence meaning claimable right now; the 5s/30s ladder next to {@code maxAttempts} is what makes "at most
     * three attempts with 5s/30s back-off" a temporal fact.
     * @param consumed 参数 已消耗的尝试数；parameter the attempts consumed so far.
     * @return 返回 排程时刻；returns the scheduled instant.
     */
    private Instant retryNotBefore(int consumed) {
        List<Duration> delays = knowledgeProperties.getRetryDelays();
        if (delays == null || delays.isEmpty()) {
            return clock.instant();
        }
        Duration delay = delays.get(Math.min(Math.max(consumed - 1, 0), delays.size() - 1));
        return delay == null || delay.isNegative() ? clock.instant() : clock.instant().plus(delay);
    }

    /**
     * 中文说明：把注册表里查不到策略的已认领作业收口为终态 {@code FAILED} +
     * {@code KNOWLEDGE_STRATEGY_UNAVAILABLE}：沿用本次认领的租约令牌写回，因此写不进去只说明所有权已易手，
     * 绝不用无凭据的整行替换去“强行收尾”；作业也因此不会永远停在没人认领的 {@code RUNNING}。
     * English summary: Closes a claimed job whose type the registry does not serve as terminal {@code FAILED} +
     * {@code KNOWLEDGE_STRATEGY_UNAVAILABLE}, writing through the token this claim acquired so a miss only means ownership
     * already moved and a full-row replace without a credential is never used to force it shut; the job therefore cannot sit
     * in {@code RUNNING} forever with nobody to serve it.
     * @param job 参数 已认领的作业载体，可能为空对象；parameter the claimed carrier, possibly null.
     */
    private void closeWithoutStrategy(KnowledgeJobBO job) {
        if (job == null || StringUtils.isBlank(job.getId())) {
            return;
        }
        if (!holdsLease(job)) {
            log.warn("KNOWLEDGE_JOB_STRATEGY_UNAVAILABLE jobId={} type={} outcome=no credential",
                    job.getId(), job.getType());
            return;
        }
        KnowledgeJobBO closed = copyJob(job)
                .setStatus(KnowledgeJobStatusEnum.FAILED)
                .setStage(job.getStage() == null ? KnowledgeJobStageEnum.QUEUED : job.getStage())
                .setAttempt(consumedAttempts(job.getAttempt(), null))
                .setNextAttemptAt(Objects.requireNonNullElseGet(job.getNextAttemptAt(), clock::instant))
                .setErrorCode(STRATEGY_UNAVAILABLE)
                .setResult(null);
        boolean written = knowledgeRepository.finish(closed, job.getLeaseToken());
        log.warn("KNOWLEDGE_JOB_STRATEGY_UNAVAILABLE jobId={} kbId={} type={} attempt={} written={}",
                job.getId(), job.getKbId(), job.getType(), closed.getAttempt(), written);
    }

    /**
     * 中文说明：判断载体是否真的持有一份可用租约：id 存在、令牌为正且状态为 {@code RUNNING}；
     * 三者任缺其一，任何写回都只是无凭据的猜测。
     * English summary: Decides whether a carrier really holds a usable lease: an id, a positive token and {@code RUNNING}
     * status, any one missing making a write-back an uncredentialed guess.
     * @param job 参数 待判定载体；parameter the candidate carrier.
     * @return 返回 是否持租约；returns whether a lease is held.
     */
    private static boolean holdsLease(KnowledgeJobBO job) {
        return job.getLeaseToken() != null
                && job.getLeaseToken() >= 1L
                && job.getStatus() == KnowledgeJobStatusEnum.RUNNING;
    }

    /**
     * 中文说明：作业载体的完整复制，配合链式 setter 使用，避免就地改动调用方持有的权威实例。
     * English summary: A full copy of a job carrier, used with the chained setters so the authoritative instance the caller
     * holds is never mutated in place.
     * @param job 参数 来源载体；parameter the source carrier.
     * @return 返回 副本载体；returns the copy.
     */
    private static KnowledgeJobBO copyJob(KnowledgeJobBO job) {
        return KnowledgeJobBO.builder()
                .id(job.getId())
                .kbId(job.getKbId())
                .type(job.getType())
                .resourceId(job.getResourceId())
                .actorId(job.getActorId())
                .payload(job.getPayload())
                .idempotencyKey(job.getIdempotencyKey())
                .requestHash(job.getRequestHash())
                .status(job.getStatus())
                .stage(job.getStage())
                .attempt(job.getAttempt())
                .nextAttemptAt(job.getNextAttemptAt())
                .leaseOwner(job.getLeaseOwner())
                .leaseToken(job.getLeaseToken())
                .leaseExpiresAt(job.getLeaseExpiresAt())
                .errorCode(job.getErrorCode())
                .result(job.getResult())
                .retryOfJobId(job.getRetryOfJobId())
                .revision(job.getRevision())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    /**
     * 中文说明：作业投影，只输出安全状态字段与时刻，不含租约三列、载荷、结果内部结构与 actor 细节；
     * 与 {@code KnowledgeServiceImpl} 的投影口径逐字一致。
     * English summary: Projects a job, emitting the safe status fields and instants only, without the three lease columns, the
     * payload, the result shape or the actor's internals, and character for character the projection
     * {@code KnowledgeServiceImpl} applies.
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
     * 中文说明：幂等意图键的规范化：调用方给出的键长度足够且为可打印 ASCII 即用其 trim 值，否则把给定
     * 片段折成 SHA-256 十六进制（恰 64 字符）作为确定性键，使重放命中同一意图而不产生第二个后继；
     * 载体约束是 16–64，短键必须替换而不是让校验在持久边界才失败。
     * English summary: Normalizes the idempotency intent: a supplied key that is long enough and printable ASCII is used
     * trimmed, otherwise the given fragments are folded into a SHA-256 hex digest (exactly 64 characters) as a deterministic
     * key so a replay hits the same intent instead of producing a second successor; the carrier bound is 16–64, so a short
     * key is replaced rather than failing validation at the persistence edge.
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
     * English summary: Compares the stored job's canonical digest against this request's: equal reuses the intent, different
     * is a 409 {@code KNOWLEDGE_IDEMPOTENCY_CONFLICT}, and a stored job is never overwritten under the same intent key.
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
                    "the idempotency key was reused with a different retry target"
            );
        }
    }

    /**
     * 中文说明：把期望 revision 规范化为正值：null 或非正按 422 拒绝，避免退化成无条件写。
     * English summary: Normalizes the expected revision to a positive value, a null or non-positive one being a 422 so the
     * write may never degrade into an unconditional one.
     * @param expectedRevision 参数 调用方期望值；parameter the caller expectation.
     * @return 返回 规范化后的期望值；returns the normalized expectation.
     */
    private static long requireExpectedRevision(Long expectedRevision) {
        if (expectedRevision == null || expectedRevision < 1L) {
            throw new CommonException(
                    422,
                    VALIDATION_FAILED,
                    "expectedRevision is required and must be positive"
            );
        }
        return expectedRevision;
    }

    /**
     * 中文说明：乐观版本预检：现值与期望不一致即抛 409 并携带库中权威 {@code currentRevision}，因此沿用
     * {@link GatewayAdminRevisionConflictException} 以便既有 409 响应体保留现值；后继行的创建完全依赖
     * 意图唯一键，本预检只是将“客户端看到的版本已过期”如实反馈出去。
     * English summary: Applies the optimistic revision pre-check: a mismatch raises 409 carrying the stored authoritative
     * {@code currentRevision}, which is why {@link GatewayAdminRevisionConflictException} is kept so the existing 409 body
     * preserves that value; creating the successor rests entirely on the intent's unique key, this pre-check only reporting
     * honestly that the revision the client saw has since moved.
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
     * 中文说明：SHA-256 小写十六进制摘要，用于幂等意图与请求摘要。
     * English summary: The lowercase SHA-256 hex digest behind idempotency intents and request digests.
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
