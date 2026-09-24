package top.egon.cola.component.yuheng.admin.knowledge.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeJobBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgePageQueryDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeRetryCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeJobVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgePageVO;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;

import java.util.List;

/**
 * 中文说明：{@code KnowledgeJobService} 是知识作业（{@code DOCUMENT_INGEST}/{@code WIKI_GENERATE}）的业务合同，
 * 一半是管理面可见的作业查询与重试（API-019–021），另一半是 worker 侧的租约生命周期——认领、心跳与终态发布
 * （claim/heartbeat/complete/fail/retry）。它对外只暴露安全的状态字段
 * （{@code id/kbId/type/resourceId/status/stage/attempt/errorCode/retryOfJobId/revision} 与时间戳），
 * 绝不泄漏租约所有者、载荷原文、提示词、向量或结果内部结构；返回类型只有既有 VO 与业务载体，绝不返回 PO。
 * English summary: {@code KnowledgeJobService} is the business contract of the knowledge jobs
 * ({@code DOCUMENT_INGEST}/{@code WIKI_GENERATE}): the management-visible job read, list and retry (API-019–021) plus the
 * worker-side lease lifecycle of claim, heartbeat and terminal publication (claim/heartbeat/complete/fail/retry). It exposes
 * only the safe status fields ({@code id/kbId/type/resourceId/status/stage/attempt/errorCode/retryOfJobId/revision} and the
 * timestamps), never the lease owner, payload content, prompts, vectors or the internal shape of a result, and its return
 * types are the existing VOs and business carriers only — never a PO.
 *
 * 用法 / Usage: 由 {@code KnowledgeJobController} 与 {@code KnowledgeJobWorker} 以限定名
 * （bean {@code knowledgeJobServiceImpl}）注入；实现类持有 {@code knowledgeRepository}、
 * {@code knowledgeJobStrategyRegistry}（{@code Map<KnowledgeJobTypeEnum, KnowledgeJobStrategy>}）与 {@code Clock} 等限定协作者。
 * 租约不变式在这里收口：租约 120 秒、心跳 30 秒、并发 2、队列 32，每次认领不超过空闲槽位；
 * 每次写回都以 {@code id + lease_token + status = 'RUNNING' + lease_expires_at > now()} 为条件，
 * 影响 0 行表示所有权已丢失而不是成功；进程重启让令牌单调递增，因此过期租约既不能发布版本也不能写终态。
 * 至多 3 次尝试、退避 5s/30s 且只对显式可重试错误生效；重试创建后继行（{@code retryOfJobId} 指向失败作业、
 * 旧行保留完整历史、新行使用新的幂等意图）。权限与错误契约固定为：不可见身份
 * {@code 403 KNOWLEDGE_FORBIDDEN}；跨租户/软删/不存在的行 {@code 404 KNOWLEDGE_RESOURCE_NOT_FOUND}；
 * 期望修订不匹配 {@code 409 KNOWLEDGE_REVISION_CONFLICT}（携带库中现值）；幂等意图摘要分歧
 * {@code 409 KNOWLEDGE_IDEMPOTENCY_CONFLICT}；状态或字段不成立 {@code 422 KNOWLEDGE_VALIDATION_FAILED}；
 * 已认领类型在策略注册表中缺失 {@code 503 KNOWLEDGE_STRATEGY_UNAVAILABLE}（落为终态失败而不是静默丢弃）。
 * / Inject it by qualifier ({@code knowledgeJobServiceImpl}) into the controller and the worker; the implementation holds
 * {@code knowledgeRepository}, {@code knowledgeJobStrategyRegistry} (a {@code Map<KnowledgeJobTypeEnum, KnowledgeJobStrategy>})
 * and the {@code Clock} as qualified collaborators. The lease invariants are enforced here: a 120-second lease, a
 * 30-second heartbeat, concurrency 2 over a queue of 32, and never more claims than free slots; every write-back is
 * conditioned on {@code id + lease_token + status = 'RUNNING' + lease_expires_at > now()} so a zero-row effect means
 * ownership was lost, not success; a restart advances the token monotonically, which is why a stale lease can neither
 * publish a revision nor write a terminal state. At most three attempts with 5s/30s delays apply only to explicitly
 * retryable errors, and a retry creates a successor row ({@code retryOfJobId} addressing the failed job, the old row keeping
 * its history, the new row carrying a fresh idempotency intent). The authorization and error contract is fixed: an
 * unauthorized actor gets {@code 403 KNOWLEDGE_FORBIDDEN}, a cross-tenant, soft-deleted or absent row
 * {@code 404 KNOWLEDGE_RESOURCE_NOT_FOUND}, a revision mismatch {@code 409 KNOWLEDGE_REVISION_CONFLICT} carrying the stored
 * revision, a diverged idempotency digest {@code 409 KNOWLEDGE_IDEMPOTENCY_CONFLICT}, an invalid field or state
 * {@code 422 KNOWLEDGE_VALIDATION_FAILED}, and a claimed type missing from the strategy registry
 * {@code 503 KNOWLEDGE_STRATEGY_UNAVAILABLE} recorded as a terminal failure rather than a silent drop.
 */
@Validated
public interface KnowledgeJobService {

    /**
     * 中文说明：API-019 读取单个作业：先确认 actor 对该作业所属知识库可见（owner 或 members），
     * 不可见即 403；本租户不存在或已软删即 404；输出只含安全状态字段与时刻，不含租约与载荷。
     * English summary: API-019 reads one job after confirming the actor may see the knowledge base the job belongs to
     * (owner or member), answering 403 when not, 404 for a row that is absent or soft-deleted inside this tenant, and
     * projecting only the safe status fields and instants without the lease or payload.
     *
     * 用法 / Usage: {@code knowledgeJobServiceImpl.getJob(actor, jobId)}；
     * 客户端把 {@code QUEUED}/{@code RUNNING}/{@code RETRY_WAIT} 视为进行中，
     * {@code SUCCEEDED/FAILED/STALE/CANCELLED} 视为终态。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param jobId 参数 作业十进制字符串 id；parameter decimal-string job id.
     * @return 返回 作业投影；returns the job projection.
     */
    KnowledgeJobVO getJob(
            @NotNull AdminActor actor,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String jobId
    );

    /**
     * 中文说明：API-020 分页读取该知识库的作业（Spec §9.2 定为 EDITOR 及以上），支持可选状态过滤，
     * 次序固定 {@code create_time DESC, id DESC}，总数与当页同谓词，返回裸 {@code items/page/size/total}。
     * English summary: API-020 pages the jobs of one knowledge base at EDITOR or above, the level Spec §9.2 pins, with an optional status filter in the
     * fixed {@code create_time DESC, id DESC} order, the total sharing the page predicate, answering with the bare
     * {@code items/page/size/total} shape.
     *
     * 用法 / Usage: {@code knowledgeJobServiceImpl.listJobs(actor, kbId, query)}；空页返回 {@code []} 而非 null。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param query 参数 分页查询载体，可携带状态过滤；parameter the page query carrier, optionally status-filtered.
     * @return 返回 作业分页投影；returns the paged job projection.
     */
    KnowledgePageVO<KnowledgeJobVO> listJobs(
            @NotNull AdminActor actor,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @Valid @NotNull KnowledgePageQueryDTO query
    );

    /**
     * 中文说明：API-021 重试一个已到终态且允许重试的作业：EDITOR 及以上方可为；
     * 要求 {@code command.expectedRevision} 等于旧行现值（不一致 409 并携带现值），
     * 并只在失败原因属于显式可重试集合时创建后继行——后继行复用同一 {@code kbId}/{@code type}/{@code resourceId}，
     * 以 {@code retryOfJobId} 指向旧作业，使用新的幂等意图并处于 {@code QUEUED}；
     * 旧行的状态、错误码与结果原样保留以维持审计历史，绝不把旧行复活或改写。
     * {@code idempotencyKey} 命中同一意图且摘要一致时返回既有后继作业，摘要分歧即 409 幂等冲突。
     * English summary: API-021 retries a terminal, explicitly retryable job, EDITOR or above: the command's
     * {@code expectedRevision} must equal the old row's current value (otherwise 409 carrying it) and a successor row is
     * created only when the failure belongs to the retryable set — the successor reuses the same {@code kbId}, {@code type}
     * and {@code resourceId}, points at the old job through {@code retryOfJobId}, carries a fresh idempotency intent and
     * starts {@code QUEUED}; the old row's status, error code and result are preserved untouched for audit, never revived or
     * rewritten. An {@code idempotencyKey} matching the same intent with an equal digest returns the existing successor while
     * a different digest is a 409 idempotency conflict.
     *
     * 用法 / Usage: {@code knowledgeJobServiceImpl.retryJob(actor, jobId, command, idempotencyKey)}；
     * 后继创建与 CAS 推进在同一短事务内完成，0 行不得返回成功；重试本身不调用模型。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param jobId 参数 被重试作业的十进制字符串 id；parameter decimal-string id of the job being retried.
     * @param command 参数 重试命令，携带期望 revision；parameter the retry command carrying the expected revision.
     * @param idempotencyKey 参数 可选 {@code Idempotency-Key} 请求头值，至多 64 字符；parameter optional
     *                       {@code Idempotency-Key} header value, at most 64 characters.
     * @return 返回 后继作业投影；returns the successor job projection.
     */
    KnowledgeJobVO retryJob(
            @NotNull AdminActor actor,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String jobId,
            @Valid @NotNull KnowledgeRetryCommandDTO command,
            @Size(max = 64) String idempotencyKey
    );

    /**
     * 中文说明：执行 claimAvailable 操作：worker 侧的认领入口，只在有空闲槽位时被调用，
     * 以 {@code slots} 为上限经 {@code FOR UPDATE SKIP LOCKED} 领取可运行作业，
     * 返回的每个载体都已处于 {@code RUNNING} 并携带本次认领的单调递增租约令牌与到期时刻；
     * 无可认领作业时返回空列表而不是伪造一行。认领不开启长事务，也不在事务内做任何外部调用。
     * English summary: Executes the claimAvailable operation; the worker-side claim entry, called only when slots are free,
     * taking runnable jobs under {@code FOR UPDATE SKIP LOCKED} bounded by {@code slots} and returning carriers that are
     * already {@code RUNNING} each with the monotonically advanced lease token and expiry acquired by this claim; when
     * nothing is claimable it returns an empty list rather than a fabricated row. Claiming opens no long transaction and
     * performs no external call inside one.
     *
     * 用法 / Usage: {@code knowledgeJobServiceImpl.claimAvailable(slots)}；
     * 调用前必须已恢复该轮作业的可信租户与 actor 上下文（守卫决定租户，任何方法都不接受租户入参），
     * 每个返回作业随后必须周期心跳（30 秒对 120 秒租约），否则其结果作废。
     * @param slots 参数 本次认领上限，等于空闲槽位数，非负；parameter claim bound equal to the free slot count,
     *              non-negative.
     * @return 返回 已认领并持租约的作业载体列表；returns the claimed job carriers holding a lease.
     */
    List<KnowledgeJobBO> claimAvailable(
            @Min(0) int slots
    );

    /**
     * 中文说明：执行 heartbeat 操作：把该作业的租约按配置时长顺延，写回条件是
     * {@code id + lease_token + status = 'RUNNING' + lease_expires_at > now()}；
     * 返回 {@code false} 即所有权已丢失（租约被接管、令牌已推进或作业已终态），
     * 调用方必须立刻停止后续阶段并放弃发布——过期租约永远不能发布活动版本，也不能写终态。
     * English summary: Executes the heartbeat operation; it extends this job's lease by the configured duration under
     * {@code id + lease_token + status = 'RUNNING' + lease_expires_at > now()}. A {@code false} return means ownership is
     * lost (the lease was taken over, the token advanced, or the job already reached a terminal state) and the caller must
     * halt the remaining stages and abandon publication — a stale lease can never activate a revision nor write a terminal
     * state.
     *
     * 用法 / Usage: {@code knowledgeJobServiceImpl.heartbeat(job)}；入参是 {@link #claimAvailable(int)} 返回的载体实例
     * （携带权威 {@code leaseToken}），不重新读取即可判定；本方法不修改业务进度列。
     * @param job 参数 持租约的作业载体；parameter the job carrier holding the lease.
     * @return 返回 所有权是否仍然成立；returns whether ownership still holds.
     */
    boolean heartbeat(@Valid @NotNull KnowledgeJobBO job);

    /**
     * 中文说明：执行 publishTerminal 操作：把策略返回的终态（{@code SUCCEEDED/FAILED/STALE/CANCELLED} 及其
     * {@code stage}/{@code errorCode}/{@code result}）以租约 CAS 写回并推进修订，返回库中权威载体；
     * CAS 未命中时返回的载体不得被调用方当作本次写入的结果使用（终态由新持有者或 {@code STALE} 判定收口），
     * 因此发布是恰一次而非至少一次——与之相对，厂商模型调用不具幂等性，只能靠活动切换的 revision CAS 收口。
     * 超过 3 次尝试或不可重试错误必须直接落 {@code FAILED} 而不再排 {@code RETRY_WAIT}。
     * English summary: Executes the publishTerminal operation; it writes the strategy's terminal state
     * ({@code SUCCEEDED/FAILED/STALE/CANCELLED} with {@code stage}, {@code errorCode} and {@code result}) back through the
     * lease CAS and advances the revision, returning the authoritative carrier. When the CAS misses, the caller may not treat
     * the carrier as its own write — the terminal state is settled by the new holder or the {@code STALE} verdict — so
     * publishing is exactly-once, in contrast to vendor model calls, which are not idempotent and can only be fenced by the
     * revision CAS of the active-revision switch. Attempts beyond three, or a non-retryable error, must land directly on
     * {@code FAILED} instead of scheduling {@code RETRY_WAIT}.
     *
     * 用法 / Usage: {@code knowledgeJobServiceImpl.publishTerminal(job, leaseToken)}；
     * {@code leaseToken} 必须是本次认领获得的值，且调用前 {@link #heartbeat(KnowledgeJobBO)} 仍成立；
     * 本方法内不再发起任何模型调用，也不在未持租约时尝试切换活动版本。
     * @param job 参数 携带终态的作业载体；parameter the job carrier holding the terminal state.
     * @param leaseToken 参数 认领时获得的租约令牌，正整数；parameter the lease token acquired at claim time.
     * @return 返回 库中权威的作业业务载体；returns the authoritative job carrier from the database.
     */
    KnowledgeJobBO publishTerminal(@Valid @NotNull KnowledgeJobBO job, @Min(1) long leaseToken);
}
