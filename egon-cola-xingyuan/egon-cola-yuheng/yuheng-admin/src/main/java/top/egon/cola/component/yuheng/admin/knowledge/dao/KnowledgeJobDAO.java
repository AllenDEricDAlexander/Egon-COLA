package top.egon.cola.component.yuheng.admin.knowledge.dao;

import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.yuheng.admin.knowledge.domain.po.KnowledgeJobPO;

import java.time.Instant;
import java.util.List;

/**
 * 知识作业表的 MyBatis-Plus Mapper，复用 EgonColaMapper 的租户内活跃读取与版本化软删除。
 * MyBatis-Plus mapper for knowledge jobs; inherits tenant-scoped active reads and versioned soft-delete.
 * 用法 / Usage: 仅声明 Spec §11.2 访问路径所需的具名类型化查询，通用 CRUD 一律经对应持久化仓储调用。
 * 除继承的通用方法外，本接口只承载 Step 12 租约会话所需的四条具名语句（{@code selectClaimable}、{@code claimJob}、
 * {@code heartbeatJob}、{@code finishJob}）：{@code FOR UPDATE SKIP LOCKED} 与「租约令牌 + 运行态 + 租约未过期」的
 * 写回 CAS 无法由 {@code Wrapper} 表达，只能落在具名 SQL 上。写入实体参数一律命名 {@code et}，否则
 * {@code EgonColaOriginalSqlGuardInterceptor} 的 {@code WRITE_ID_MISMATCH} 与 MP 的租户/审计盖章都会失配。
 * Beyond the inherited generic methods this interface carries only the four lease statements Step 12 needs
 * ({@code selectClaimable}, {@code claimJob}, {@code heartbeatJob}, {@code finishJob}): {@code FOR UPDATE SKIP LOCKED} and the
 * "lease token + running state + unexpired lease" write-back CAS cannot be expressed by a {@code Wrapper}, so they live in
 * named SQL. A write statement's entity parameter is always named {@code et}, otherwise both the starter's
 * {@code WRITE_ID_MISMATCH} check and MyBatis-Plus' tenant/audit stamping would miss.
 */
public interface KnowledgeJobDAO extends EgonColaMapper<KnowledgeJobPO> {

    /**
     * 中文说明：锁定式取出本轮可认领的作业行——{@code QUEUED}/{@code RETRY_WAIT} 且 {@code next_attempt_at} 已到期，
     * 或 {@code RUNNING} 但租约已过期（worker 崩溃后由后来者接管）；按 {@code next_attempt_at, id} 升序取至多
     * {@code slots} 行，并以 {@code FOR UPDATE SKIP LOCKED} 让并发 worker 互相跳过已锁行而不排队等待。
     * English summary: Locks out the rows claimable in this round — a {@code QUEUED} or {@code RETRY_WAIT} row whose
     * {@code next_attempt_at} has come due, or a {@code RUNNING} row whose lease already expired (a crashed worker's job being
     * taken over) — ascending by {@code next_attempt_at, id}, at most {@code slots} rows, with {@code FOR UPDATE SKIP LOCKED}
     * so concurrent workers skip locked rows instead of queueing behind them.
     *
     * 用法 / Usage: 只由 {@code MpKnowledgeRepository.claimNext(int)} 调用，且必须与随后的 {@link #claimJob} 同事务，
     * 行锁才在认领写回提交前一直持有；租户与活跃谓词由受守卫边界追加，本方法没有租户入参。
     * Called only by {@code MpKnowledgeRepository.claimNext(int)} and, to hold the row lock until the claim write-back
     * commits, inside the same transaction as {@link #claimJob}; tenant and active-row predicates come from the guarded
     * boundary, so this method takes no tenant argument.
     * @param now 本轮认领的时间基准；the claim clock for this round.
     * @param slots 本轮认领上限（空闲槽位数），非负；claim bound for this round (free slots), non-negative.
     * @return 候选作业行；the candidate job rows.
     */
    List<KnowledgeJobPO> selectClaimable(@Param("now") Instant now, @Param("slots") int slots);

    /**
     * 中文说明：把一行观察到的候选作业真正翻到 {@code RUNNING} 并登记租约：写入 {@code lease_owner}、
     * 调用方给定的单调递增 {@code lease_token} 与 {@code lease_expires_at}，同时推进业务 {@code revision}、
     * 技术 {@code version} 与审计列；WHERE 复核观察到的 {@code id}/{@code tenant_id}/{@code status}/
     * {@code lease_token}/{@code version} 与「本轮确实可认领」的时刻条件，因此影响行数就是认领裁决：
     * 0 行表示该行已被其他 worker 抢走或状态已变，绝不能当作认领成功。
     * English summary: Actually flips an observed candidate to {@code RUNNING} and registers the lease: it stores
     * {@code lease_owner}, the caller-supplied monotonically increasing {@code lease_token} and {@code lease_expires_at}, while
     * advancing the business {@code revision}, the technical {@code version} and the audit columns. Its WHERE re-asserts the
     * observed {@code id}/{@code tenant_id}/{@code status}/{@code lease_token}/{@code version} plus the "claimable at this instant"
     * time test, so the affected row count is the verdict: zero rows means another worker won or the state moved, which is
     * never a successful claim.
     *
     * 用法 / Usage: 只由 {@code MpKnowledgeRepository.claimNext(int)} 在 {@link #selectClaimable} 之后逐行调用；
     * {@code et} 必须是刚读到的原行（携带观察到的 {@code status}/{@code leaseToken}/{@code version}），
     * 令牌由调用方按 {@code 观察值 + 1} 给出，重启接管因此单调递增而迟到写回必然 0 行。
     * Called row by row by {@code MpKnowledgeRepository.claimNext(int)} right after {@link #selectClaimable}; {@code et}
     * must be the freshly read row (carrying the observed {@code status}/{@code leaseToken}/{@code version}) and the token is
     * the caller's {@code observed + 1}, so a restart advances it monotonically and a late write-back necessarily hits zero
     * rows.
     * @param et 观察到的候选行，同时提供主键、租户、技术版本与审计盖章；the observed candidate row, also supplying key, tenant, technical version and audit stamping.
     * @param leaseOwner 本轮 worker 实例标识（varchar(128)）；the worker instance identity for this claim (varchar(128)).
     * @param leaseToken 新的租约令牌（观察值 + 1）；the new lease token (observed value plus one).
     * @param leaseExpiresAt 租约到期时刻（120 秒）；the lease expiry instant (120 seconds).
     * @param now 本轮时刻基准，用于复核可认领性；the round's clock, re-asserting claimability.
     * @return 1 表示认领成功，0 表示所有权归他人；one for a successful claim, zero meaning ownership went elsewhere.
     */
    int claimJob(@Param("et") KnowledgeJobPO et,
                 @Param("leaseOwner") String leaseOwner,
                 @Param("leaseToken") long leaseToken,
                 @Param("leaseExpiresAt") Instant leaseExpiresAt,
                 @Param("now") Instant now);

    /**
     * 中文说明：心跳顺延租约到期时刻，CAS 条件是 {@code id + tenant_id + lease_token + status='RUNNING' +
     * lease_expires_at > now}；返回 0 即所有权已丢失（租约超时被接管、令牌已推进或作业已终态），
     * 调用方必须立即停止工作并放弃发布。心跳只延长 {@code lease_expires_at}，不推进业务 {@code revision}，
     * 也不改变 {@code stage}/{@code attempt}；它仍是一条版本化 UPDATE，所以技术 {@code version} 与审计两列照守卫
     * 要求随 {@code et} 走（{@code WHERE ... version = #{et.version}}、{@code SET update_user_id/update_time}）。
     * English summary: Extends the lease expiry by heartbeat under {@code id + tenant_id + lease_token +
     * status='RUNNING' + lease_expires_at > now}; a zero result means ownership is gone (the lease lapsed and was taken over,
     * the token advanced, or the job turned terminal) and the caller must stop working and abandon publication. A heartbeat
     * only moves {@code lease_expires_at}: it never advances the business {@code revision} nor touches {@code stage}/{@code attempt},
     * yet being a versioned UPDATE it still carries the technical {@code version} and the audit pair through {@code et} exactly as
     * the guard demands ({@code WHERE ... version = #{et.version}}, {@code SET update_user_id/update_time}).
     *
     * 用法 / Usage: 只由 {@code MpKnowledgeRepository.heartbeat(String, long, Instant)} 调用；{@code et} 必须是刚读到的
     * 活跃行，其 {@code version} 提供 CAS 谓词、{@code updateUserId}/{@code updateTime} 提供守卫审计绑定，
     * 租户因此来自行本身而非业务入参。/ Called only by {@code MpKnowledgeRepository.heartbeat(String, long, Instant)};
     * {@code et} must be the freshly loaded active row, whose {@code version} feeds the CAS predicate and whose
     * {@code updateUserId}/{@code updateTime} feed the guard's audit binding, so tenancy comes from the row rather than a
     * business argument.
     * @param et 刚读到的活跃作业行，提供主键、租户、技术版本与审计盖章；the freshly loaded active job row, supplying key, tenant, technical version and audit stamping.
     * @param leaseToken 认领时获得的租约令牌；the lease token acquired at claim time.
     * @param leaseExpiresAt 顺延后的到期时刻；the extended expiry instant.
     * @param now 判定当前租约尚未过期的时刻基准；the clock proving the current lease has not lapsed.
     * @return 1 表示所有权仍成立，0 表示已丢失；one while ownership holds, zero once it is lost.
     */
    int heartbeatJob(@Param("et") KnowledgeJobPO et,
                     @Param("leaseToken") long leaseToken,
                     @Param("leaseExpiresAt") Instant leaseExpiresAt,
                     @Param("now") Instant now);

    /**
     * 中文说明：以租约令牌 CAS 写回终态（{@code status}/{@code stage}/{@code attempt}/{@code next_attempt_at}/
     * {@code error_code}/{@code result}）并推进业务 {@code revision}、技术 {@code version} 与审计列；
     * WHERE 同样要求 {@code id + tenant_id + lease_token + status='RUNNING' + lease_expires_at > now}，
     * 0 行意味着本次执行结果作废——过期租约绝不留下状态，更不触发活动版本切换。三个租约列与意图身份
     * （{@code kb_id}/{@code type}/{@code resource_id}/{@code actor_id}/{@code idempotency_key}/{@code request_hash}/
     * {@code payload}/{@code retry_of_job_id}）都不在 SET 之列：租约只由 claim/heartbeat 推进，意图一旦入队即不可改写。
     * English summary: Writes the terminal state ({@code status}/{@code stage}/{@code attempt}/{@code next_attempt_at}/
     * {@code error_code}/{@code result}) back through the lease-token CAS, advancing the business {@code revision}, the
     * technical {@code version} and the audit columns, under the same {@code id + tenant_id + lease_token + status='RUNNING' +
     * lease_expires_at > now} condition; zero rows voids this execution's outcome — a lapsed lease leaves no state behind and
     * certainly never drives an active-revision switch. Neither the three lease columns nor the intent identity
     * ({@code kb_id}/{@code type}/{@code resource_id}/{@code actor_id}/{@code idempotency_key}/{@code request_hash}/
     * {@code payload}/{@code retry_of_job_id}) appears in the SET list: the lease advances only through claim and heartbeat,
     * and an intent is immutable once queued.
     *
     * 用法 / Usage: 只由 {@code MpKnowledgeRepository.finish(KnowledgeJobBO, long)} 调用，
     * {@code et} 是刚读到的活跃行（携带观察到的 {@code version} 与审计盖章来源）。/ Called only by
     * {@code MpKnowledgeRepository.finish(KnowledgeJobBO, long)}, with {@code et} being the freshly loaded active row
     * (supplying the observed {@code version} and the audit stamping source).
     * @param et 携带终态的活跃行；the active row carrying the terminal state.
     * @param leaseToken 认领时获得的租约令牌；the lease token acquired at claim time.
     * @param now 判定当前租约尚未过期的时刻基准；the clock proving the current lease has not lapsed.
     * @return 1 表示终态由本方写入，0 表示所有权已丢失；one when this side wrote the terminal state, zero once ownership is gone.
     */
    int finishJob(@Param("et") KnowledgeJobPO et,
                  @Param("leaseToken") long leaseToken,
                  @Param("now") Instant now);
}
