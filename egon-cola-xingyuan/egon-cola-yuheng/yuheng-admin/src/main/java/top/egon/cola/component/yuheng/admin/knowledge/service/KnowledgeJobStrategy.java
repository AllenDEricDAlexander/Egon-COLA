package top.egon.cola.component.yuheng.admin.knowledge.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeJobBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobTypeEnum;

/**
 * 中文说明：{@code KnowledgeJobStrategy} 是作业类型到执行逻辑的唯一路由机制（Rule 9）：
 * {@code KnowledgeConfiguration} 注入所有实现并以 {@code EnumMap} 建立
 * {@code knowledgeJobStrategyRegistry}（{@code Map<KnowledgeJobTypeEnum, KnowledgeJobStrategy>}），
 * 同一 {@code type()} 出现两个实现即在启动期 {@code IllegalStateException} 失败，
 * 已认领类型在注册表中缺失则落 {@code 503 KNOWLEDGE_STRATEGY_UNAVAILABLE} 终态而不是静默丢弃。
 * 因此 worker、{@code KnowledgeJobServiceImpl} 以及任何协作者都不得对 {@code KnowledgeJobTypeEnum}
 * 写 {@code switch}/{@code if-else} 分支来判断类型，只能按类型查注册表；新增类型只新增一个实现。
 * English summary: {@code KnowledgeJobStrategy} is the only routing mechanism from a job type to its execution logic
 * (Rule 9): {@code KnowledgeConfiguration} injects every implementation and builds {@code knowledgeJobStrategyRegistry}
 * ({@code Map<KnowledgeJobTypeEnum, KnowledgeJobStrategy>}) as an {@code EnumMap}, two implementations sharing one
 * {@code type()} failing startup with an {@code IllegalStateException} and a claimed type missing from the map becoming a
 * terminal {@code 503 KNOWLEDGE_STRATEGY_UNAVAILABLE} rather than a silent drop. Consequently neither the worker, nor
 * {@code KnowledgeJobServiceImpl}, nor any collaborator may branch on {@code KnowledgeJobTypeEnum} with a
 * {@code switch}/{@code if-else}; the registry is looked up by type and a new type only adds a new implementation.
 *
 * 用法 / Usage: 由 {@code KnowledgeJobServiceImpl} 在持有租约的作业上调用（bean 例：
 * {@code documentIngestionStrategy} 对应 {@code DOCUMENT_INGEST}，Wiki 生成对应 {@code WIKI_GENERATE}）；
 * 接口不带 {@code @Service}/Lombok 注解，实现类自己声明限定 DI 与 {@code @Slf4j}。
 * 实现的执行纪律：绝不在数据库事务或锁内调用模型；文档只经 LOCAL 嵌入 alias 产生向量，
 * 没有云端兜底；只有全部向量校验通过（数量等于分块数、每条长度等于冻结维度、全部有限且非全零）并
 * {@code stageChunks} 成功后，才允许用活动 revision 的 CAS 发布，任一步失败旧活动版本原样保留；
 * 每一次写回都通过 {@code KnowledgeJobService#publishTerminal} 的租约 CAS 收口，
 * 心跳失败或影响 0 行意味着所有权已丢失，本策略的结果必须被丢弃而不是重试覆盖新持有者。
 * / The implementation is invoked by {@code KnowledgeJobServiceImpl} on a job it holds the lease for (beans such as
 * {@code documentIngestionStrategy} for {@code DOCUMENT_INGEST} and the wiki generator for {@code WIKI_GENERATE}); the
 * interface carries no {@code @Service} or Lombok annotation while each implementation declares its own qualified
 * dependencies and {@code @Slf4j}. Execution discipline: never call a model inside a database transaction or lock;
 * documents embed only through the LOCAL embedding alias with no cloud fallback; publication is allowed only after every
 * vector validates (count equal to the chunk count, each exactly the frozen dimensions long, all finite and not all zero)
 * and {@code stageChunks} succeeds, so a partial failure leaves the previous active revision untouched; every write-back
 * closes through the lease CAS of {@code KnowledgeJobService#publishTerminal}, where a failed heartbeat or a zero-row
 * effect means ownership was lost and this strategy's outcome must be discarded rather than overwriting the new holder.
 */
@Validated
public interface KnowledgeJobStrategy {

    /**
     * 中文说明：执行 type 操作；声明本策略唯一负责的作业类型，注册表以它为键，
     * 因此取值必须是 {@code KnowledgeJobTypeEnum} 的具体常量且全应用内不得重复。
     * English summary: Executes the type operation; declares the single job type this strategy owns, which is the registry
     * key, so the value must be a concrete {@code KnowledgeJobTypeEnum} constant and may not repeat across the application.
     *
     * 用法 / Usage: {@code strategy.type()}；必须是常量时间返回，不得依赖注入状态或当前作业推导。
     * @return 返回 本策略负责的作业类型；returns the job type this strategy owns.
     */
    KnowledgeJobTypeEnum type();

    /**
     * 中文说明：执行 run 操作；在给定的已认领作业上完成该类型的全部业务工作，并返回要写回的终态载体
     * （{@code SUCCEEDED}，或带 {@code errorCode} 的 {@code FAILED}/{@code STALE}/{@code CANCELLED}）；
     * 入参载体携带本次认领的租约令牌，实现不得在租约之外发布，也不得绕过 CAS 直接改动作业行——
     * 只有本端口返回的载体才会经 {@code KnowledgeJobService#publishTerminal(KnowledgeJobBO, long)}
     * 以 {@code id + lease_token + status = 'RUNNING' + lease_expires_at > now()} 写回，
     * 因此过期租约不可能发布结果；影响 0 行是所有权丢失而非成功。
     * 可重试性判定也在这里做出：只有显式可重试错误才允许进入 {@code RETRY_WAIT} 排程（5s/30s 退避，总尝试 ≤ 3）。
     * English summary: Executes the run operation; performs all the work of this job type on the claimed job and returns the
     * terminal carrier to write back ({@code SUCCEEDED}, or {@code FAILED}/{@code STALE}/{@code CANCELLED} with an
     * {@code errorCode}). The given carrier holds the lease token of this claim; an implementation may not publish outside
     * that lease nor touch the job row around the CAS, because only the carrier returned here is written back through
     * {@code KnowledgeJobService#publishTerminal(KnowledgeJobBO, long)} under
     * {@code id + lease_token + status = 'RUNNING' + lease_expires_at > now()} — a stale lease therefore cannot publish, and
     * a zero-row effect means ownership was lost rather than success. Retryability is also decided here: only explicitly
     * retryable errors may schedule {@code RETRY_WAIT} (5s/30s backoff, at most three attempts in total).
     *
     * 用法 / Usage: {@code strategy.run(job, leaseToken)}；
     * 长工作要在阶段边界配合心跳，向量与外部调用都在事务之外，日志只记 id/机器码/耗时，
     * 不记原文、提示词、向量或密钥；不可用的别名或策略缺失分别以
     * {@code 503 KNOWLEDGE_MODEL_UNAVAILABLE}/{@code 503 KNOWLEDGE_STRATEGY_UNAVAILABLE} 收口为终态。
     * @param job 参数 已认领并持租约的作业载体；parameter the claimed job carrier holding a lease.
     * @param leaseToken 参数 认领时获得的租约令牌，正整数；parameter the lease token acquired at claim time.
     * @return 返回 待经租约 CAS 写回的终态作业载体；returns the terminal job carrier to write back through the lease CAS.
     */
    KnowledgeJobBO run(@Valid @NotNull KnowledgeJobBO job, @Min(1) long leaseToken);
}
