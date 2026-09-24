package top.egon.cola.component.yuheng.admin.knowledge.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.yuheng.admin.config.GatewayPersistenceContextComponent;
import top.egon.cola.component.yuheng.admin.config.properties.KnowledgeProperties;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeJobBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobStatusEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobTypeEnum;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeJobService;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeJobStrategy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 中文说明：{@code KnowledgeJobWorker} 是知识持久作业的唯一后台入口：一个 {@code @Scheduled} 轮询在心跳周期上醒来，
 * <b>先续租、后按空闲槽认领</b>，再把每条认领到的作业投进有界执行器交给类型策略。它自己<b>不做</b>解析、切分、
 * 嵌入或发布——那些是 {@link KnowledgeJobStrategy} 的职责；它也只负责三件租约事实：认领（claim）、续租（heartbeat）、
 * 发布终态（publishTerminal），全部经 {@link KnowledgeJobService} 的租约 CAS 完成。
 *
 * 四条不可让的事实落在这里。<b>其一，认领不得超过空闲槽</b>：槽位数由执行器自己的
 * {@code maxPoolSize + queueCapacity} 减去 {@code activeCount + queueSize} 得出，再被
 * {@code yuheng.knowledge.claim-size} 二次收紧，所以「bounded 2 workers / 32 queue / claim &lt;= slots」
 * 是运行时算出来的，不是注释里的承诺；{@code getQueueSize()/getActiveCount()} 在池未初始化时返回 0，
 * 因此这里刻意不调用会抛 {@link IllegalStateException} 的 {@code getThreadPoolExecutor()}。
 * <b>其二，可信上下文只复用既有机制</b>：worker 线程没有 HTTP 请求，于是每条 SQL 都必须在
 * {@link GatewayPersistenceContextComponent#call(String, java.util.Callable)} 内执行——它写入的租户只可能来自部署配置
 * {@code yuheng.persistence.tenant-id}，审计主体来自 {@code service-user-id}，并在 finally 原样恢复先前 MDC；
 * 声明租户传 {@code null} 表示「后台任务不带身份声明」，由单企业安装域承接。
 * {@code TraceContext.root(jobId).open()} 负责 trace/请求号并同样在关闭时恢复先前 MDC，
 * 这里<b>没有</b>第二套上下文实现，也没有从 header/body/job 里"取租户"的路径。
 * 业务发起人 {@code actorId} 与 worker 的审计身份被刻意分开：前者只是被复核的数据，不会因执行而获得任何权限，
 * 成员复核在策略内部于发起时与产出前各做一次。<b>其三，长任务必须续租</b>：嵌入阶段可以远超 30 秒，
 * 策略只在阶段边界续租，因此轮询对每条在途作业补一次 {@code heartbeat}；返回假即所有权已丢（令牌被推进、
 * 租约被接管或作业已终态），此时只是停止续租并在日志记一条事实——策略自己的阶段边界 CAS 会先落地，
 * worker 绝不伪造发布。<b>其四，发布是恰一次</b>：终态只经 {@code publishTerminal} 的 {@code id + lease_token +
 * status='RUNNING' + lease_expires_at > now} CAS 写回，0 行不是成功；厂商模型调用不具幂等性，
 * 因此活动版本切换由 revision CAS 收口，而不是靠 worker 重试。
 *
 * 分发只用注册表：{@code knowledgeJobStrategyRegistry} 是 {@code Map<KnowledgeJobTypeEnum, KnowledgeJobStrategy>}，
 * 查不到即按 {@code 503 KNOWLEDGE_STRATEGY_UNAVAILABLE} 落终态失败（例如 Step 14 之前的 {@code WIKI_GENERATE}），
 * 绝不静默丢弃、也写不出 {@code switch}。类型化异常由策略自己收束为终态载体，因此 worker 的
 * {@code catch} 只覆盖基础设施故障（数据库不可用、租约丢失、身份声明不符），一律如实失败并交回租约 CAS，
 * 不编造成功、不改道云端。日志纪律与前一面一致：只有稳定 id、类型、状态、阶段、机器码与计数，
 * 没有载荷原文、正文、提示词、向量、租约值或凭据。
 *
 * English summary: {@code KnowledgeJobWorker} is the only background entry for persistent knowledge jobs: one
 * {@code @Scheduled} tick wakes on the heartbeat period, <b>renews first and only then claims within the free slots</b>,
 * and hands each claimed job to the bounded executor for its typed strategy. It performs <b>no</b> parsing, chunking,
 * embedding or publication itself — that is {@link KnowledgeJobStrategy}'s work — and owns exactly three lease facts:
 * claim, heartbeat and terminal publication, all through the {@link KnowledgeJobService} lease CAS.
 *
 * Four non-negotiables land here. <b>First, a claim never exceeds the free slots</b>: the slot count is computed from
 * the executor itself as {@code maxPoolSize + queueCapacity} minus {@code activeCount + queueSize} and then clamped
 * again by {@code yuheng.knowledge.claim-size}, so "bounded 2 workers / 32 queue / claim &lt;= slots" is arithmetic
 * rather than a comment; {@code getQueueSize()/getActiveCount()} return zero before the pool is initialized, which is
 * why this class deliberately avoids {@code getThreadPoolExecutor()}, whose IllegalStateException a shutdown race would
 * otherwise turn into a poll failure. <b>Second, the trusted context only reuses the mechanism the module already
 * has</b>: a worker thread carries no HTTP request, so every statement must run inside
 * {@link GatewayPersistenceContextComponent#call(String, java.util.Callable)}, whose tenant can only come from
 * {@code yuheng.persistence.tenant-id} and whose audit principal from {@code service-user-id}, restoring the previous MDC
 * in a finally block; a {@code null} claimed tenant means "a background task with no identity claim", served by the single
 * enterprise installation domain. {@code TraceContext.root(jobId).open()} supplies the trace and request id and likewise
 * restores the previous MDC on close, so there is <b>no</b> parallel context implementation and no path that reads a tenant
 * from a header, body or job. The submitting {@code actorId} stays separate from the worker's audit identity: the former is
 * only data that gets re-checked, never a right the worker inherits, and the membership re-check happens twice inside the
 * strategy. <b>Third, long work must be renewed</b>: embedding can outlive 30 seconds, a strategy only renews at phase
 * boundaries, so the tick issues one {@code heartbeat} per in-flight job; a {@code false} means ownership is gone (the token
 * advanced, the lease was taken over, or the job is already terminal), which stops the renewal and logs the fact only —
 * the strategy's own boundary CAS lands first and the worker never fabricates a publication. <b>Fourth, publishing is
 * exactly-once</b>: a terminal state travels only through the {@code publishTerminal} CAS on {@code id + lease_token +
 * status='RUNNING' + lease_expires_at > now}, where zero rows is not success; vendor model calls are not idempotent, so the
 * active-version switch is fenced by the revision CAS rather than by a worker retry.
 *
 * Dispatch uses only the registry: {@code knowledgeJobStrategyRegistry} is a
 * {@code Map<KnowledgeJobTypeEnum, KnowledgeJobStrategy>} and a miss becomes the terminal
 * {@code 503 KNOWLEDGE_STRATEGY_UNAVAILABLE} (for instance {@code WIKI_GENERATE} before Step 14) rather than a silent drop,
 * so no {@code switch} can be written. Typed errors are already closed into terminal carriers by the strategy, so this
 * class's {@code catch} covers infrastructure only — an unavailable database, a lost lease, an identity mismatch — and fails
 * honestly through the same CAS without inventing a success or rerouting to cloud. Logging keeps the same discipline as the
 * request face: stable ids, type, status, stage, machine codes and counts, never payload text, document content, prompts,
 * vectors, lease values or credentials.
 *
 * 用法 / Usage: 由组件扫描注册为 bean {@code knowledgeJobWorker}，且<b>只</b>由
 * {@code @ConditionalOnProperty(name = {"yuheng.knowledge.enabled", "yuheng.knowledge.worker-enabled"},
 * havingValue = "true", matchIfMissing = false)} 这一条开关把守——两把键都必须显式为真，缺键即不创建 bean，
 * 因此方法体内没有第二个静默守卫；周期取自同一个键 {@code yuheng.knowledge.heartbeat}（默认 PT30S），
 * 与 {@code KnowledgeProperties#isHeartbeatShorterThanLease()} 保证的「心跳严格短于租约」共同构成续租窗口。
 * 它依赖 A7 的 {@code knowledgeJobTaskExecutor}（2 线程 / 32 队列）与 {@code knowledgeJobStrategyRegistry}，
 * 二者只在同一对开关为真时存在，因此 bean 依赖关系与条件天然一致。
 * / Registered by component scanning as bean {@code knowledgeJobWorker} and guarded by <b>exactly one</b>
 * {@code @ConditionalOnProperty} over the two keys with explicit {@code havingValue = "true"} and
 * {@code matchIfMissing = false}, so a missing key creates no bean and no second silent guard is needed inside the method;
 * the period comes from the same key {@code yuheng.knowledge.heartbeat} (PT30S by default), which together with
 * {@code KnowledgeProperties#isHeartbeatShorterThanLease()} defines the renewal window. It depends on A7's
 * {@code knowledgeJobTaskExecutor} (2 threads / 32 queue) and {@code knowledgeJobStrategyRegistry}, both of which exist
 * only while the same two keys are true, so the bean dependencies and the conditions match by construction.
 */
@Slf4j
@Validated
@Service("knowledgeJobWorker")
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"yuheng.knowledge.enabled", "yuheng.knowledge.worker-enabled"},
        havingValue = "true", matchIfMissing = false)
public class KnowledgeJobWorker {

    /** 中文说明：机器码：已认领的类型在策略注册表里没有实现（Rule 9 的唯一失败收口），必须落终态而不是静默丢弃。 English summary: machine code: a claimed type has no strategy in the registry (the single Rule 9 closure), so it lands terminal instead of being dropped. */
    static final String STRATEGY_UNAVAILABLE = "KNOWLEDGE_STRATEGY_UNAVAILABLE";

    /** 中文说明：机器码：worker 侧基础设施异常（数据库、租约、身份上下文）收口为依赖不可用，绝不改道云端也不伪造成功。 English summary: machine code: a worker-side infrastructure failure (database, lease, identity context) closes as an unavailable dependency, never rerouting to cloud or fabricating success. */
    static final String DEPENDENCY_UNAVAILABLE = "YUHENG_DEPENDENCY_UNAVAILABLE";

    /** 中文说明：轮询自身的 trace 请求号（不认领任何作业时用它），稳定且不含业务语义，便于把续租日志归到同一条链上。 English summary: the stable, semantics-free trace request id of the poll itself, used while no job is claimed so the renewal lines stay on one chain. */
    private static final String POLL_REQUEST_ID = "knowledge-job-poll";

    /** 中文说明：{@code error_code} 列的宽度：机器码本就远短于此，这条截断只为「日志与写回都不因长度限制整体失败」兜底。 English summary: the width of the {@code error_code} column, far above every machine code, kept only so that neither the log nor the write-back can fail on length. */
    private static final int MAX_ERROR_CODE_CHARACTERS = 64;

    /** 中文说明：同实例防重入标志，与 {@code GatewayOpenApiSyncReconciler} 同一手法：一轮没走完就不会再起一轮，避免 claim 与在途表被并发推进。 English summary: the same-instance re-entrancy flag, the same device {@code GatewayOpenApiSyncReconciler} uses: a tick cannot overlap itself, so the claim and the in-flight map are never advanced concurrently. */
    private final AtomicBoolean ticking = new AtomicBoolean();

    /** 中文说明：在途作业表：键是作业 id，值携带认领载体与本次租约令牌；认领线程放入、执行线程在 finally 移除，续租扫描只读它，因此「长任务续租」不依赖任何额外定时线程。 English summary: the in-flight table: keyed by job id, valued with the claimed carrier and this lease token; the claiming thread inserts, the executing thread removes in a finally, and only the renewal sweep reads it, which is why long-work renewal needs no extra timer thread. */
    private final ConcurrentMap<String, InFlightJob> inFlight = new ConcurrentHashMap<>();

    /** 中文说明：作业业务合同（认领/心跳/终态发布三件事的唯一入口），按名注入实现类而不依赖类型：容器里同一接口只有一个实现，但本模块的既有纪律是每个注入点都显式限定。 English summary: the job business contract, the only entry for claim, heartbeat and terminal publication, injected by implementation name rather than by type: one implementation exists, but this module's discipline is an explicit qualifier on every injection point. */
    @Qualifier("knowledgeJobServiceImpl")
    private final KnowledgeJobService knowledgeJobService;

    /** 中文说明：类型到策略的只读注册表（Rule 9 的唯一分发事实来源）；键不是 {@code String}，因此 Spring 走「按名取单个 bean」而不是将多元素注入收窄，这与 {@code LlmApiController} 的 {@code llmProtocolStrategyRegistry} 同构。 English summary: the immutable type to strategy registry, the single source of truth for dispatch under Rule 9; because the key is not {@code String}, Spring resolves the named bean instead of narrowing a multi-element injection, exactly as {@code LlmApiController}'s {@code llmProtocolStrategyRegistry} does. */
    @Qualifier("knowledgeJobStrategyRegistry")
    private final Map<KnowledgeJobTypeEnum, KnowledgeJobStrategy> knowledgeJobStrategyRegistry;

    /** 中文说明：有界执行器（核心与最大线程数 {@code worker-concurrency}、队列 32），空闲槽只按它自己报告的计数计算，因此「claim 不超过可用槽」与真实提交能力同源；不在构造期读取，避免与池的初始化顺序耦合。 English summary: the bounded executor (core and maximum pool size {@code worker-concurrency}, queue 32) whose own counters define the free slots, so "a claim never exceeds the free slots" shares one source with the real submission capacity; it is read per tick rather than at construction to stay decoupled from the pool's initialization order. */
    @Qualifier("knowledgeJobTaskExecutor")
    private final ThreadPoolTaskExecutor knowledgeJobTaskExecutor;

    /** 中文说明：模块唯一的持久化身份上下文：在真正执行 SQL 的线程上写入部署租户与服务审计主体并在 finally 恢复；worker 不自建第二套 MDC 机制，也不接受任何来自数据行的租户值。 English summary: the module's single persistence identity context, which installs the deployment tenant and service audit principal on the SQL-executing thread and restores them in a finally; the worker builds no second MDC mechanism and never accepts a tenant value coming from a data row. */
    @Qualifier("gatewayPersistenceContextComponent")
    private final GatewayPersistenceContextComponent persistenceContext;

    /** 中文说明：知识运行边界，只用 {@code claimSize} 给单次认领封顶（心跳/租约的界由绑定阶段与策略各自守住，时刻也只由策略自己在阶段边界取用）。 English summary: the knowledge runtime boundary, used only to cap one claim by {@code claimSize} — the lease and heartbeat bounds stay in binding and in the strategy, which is also the only party reading instants. */
    @Qualifier(KnowledgeProperties.BEAN_NAME)
    private final KnowledgeProperties knowledgeProperties;

    /**
     * 中文说明：一轮心跳周期的轮询：先在可信持久化上下文内续租在途作业，再按空闲槽认领并投递。
     * 顺序是刻意的——<b>续租先于认领</b>：若反过来，一次慢认领会把在途作业的续租窗口挤掉，
     * 合法持有者会被别的 worker 判为过期并接管，而这正是租约会话最不该发生的自我伤害。
     * 整个方法体不外抛任何异常：调度线程一旦抛出就会被 Spring 记为该任务的失败并继续下一轮，
     * 但这里选择把异常收成一含机器码的 warn，以免任何载荷信息顺着默认处理链泄漏出去。
     * 无空闲槽时直接返回且不认领（日志留白，30 秒一轮的 warn 会是噪音）；
     * {@link TaskRejectedException} 只可能来自「算完空闲槽之后又被别的提交吃掉」的窗口，
     * 处理方式是<b>不再认领</b>并 warn 一次：已认领的这条不回滚、不改状态、也不再续租，
     * 于是 120 秒后租约自然过期、行重新可认领，既没有丢作业也没有第二次发布。
     * 本方法内没有任何模型调用，也没有长事务：出网只发生在被投递的策略线程里。
     * English summary: One poll per heartbeat period: it first renews the in-flight jobs inside the trusted persistence
     * context and only then claims and submits within the free slots.
     * The order is deliberate — <b>renewal before claiming</b>: reversed, a slow claim would squeeze out the renewal window
     * of the in-flight jobs, so a lawful holder would be declared expired and taken over by another worker, which is the
     * exact self-harm a lease session must avoid.
     * Nothing escapes this method: a throw on a scheduler thread is recorded by Spring as a failed execution and the next
     * tick still runs, but closing it into one machine-coded warn here keeps payload information out of the default
     * handling chain. With no free slot it returns without claiming (the log stays silent, since a warn every 30 seconds
     * would be noise); a {@link TaskRejectedException} can only come from the window where capacity was consumed after the
     * slots were computed, and the response is to <b>stop claiming</b> and warn once: the rejected job is neither rolled
     * back, rewritten nor renewed again, so its lease lapses after 120 seconds and the row becomes claimable — no job is
     * lost and nothing is published twice. This method issues no model call and opens no long transaction; egress happens
     * only on the submitted strategy thread.
     *
     * 用法 / Usage: 由 {@code @EnableScheduling}（{@code bootstrap/GatewayAdminConfiguration}）驱动，
     * {@code fixedDelayString}/{@code initialDelayString} 都绑定 {@code yuheng.knowledge.heartbeat}（默认 PT30S）；
     * 运维只通过两个开关键控制它是否运行，没有独立的运行期守卫。
     * @throws IllegalStateException 上下文或注册表在运行期被替换为不可用状态；an unavailable context or registry at runtime.
     */
    @Scheduled(fixedDelayString = "${yuheng.knowledge.heartbeat:PT30S}",
            initialDelayString = "${yuheng.knowledge.heartbeat:PT30S}")
    public void poll() {
        if (!ticking.compareAndSet(false, true)) {
            return;
        }
        try {
            persistenceContext.call(null, () -> {
                renewHeldLeases();
                claimWithinFreeSlots();
                return null;
            });
        } catch (Exception failure) {
            log.warn("knowledge job poll closed without publishing code={} detail={}",
                    DEPENDENCY_UNAVAILABLE, failure.getClass().getSimpleName());
        } finally {
            ticking.set(false);
        }
    }

    /** 中文说明：在途作业的续租扫描：只对尚未进入收尾的作业发一次 {@code heartbeat}；返回假即令牌被推进、租约被接管或作业已终态，此刻唯一正确的动作是停止续租并记一条事实，而不是替策略发布。 English summary: the in-flight renewal sweep: one {@code heartbeat} per job that has not started closing; a false means the token advanced, the lease was taken over or the job is already terminal, and the only right answer then is to stop renewing and record the fact rather than publish on the strategy's behalf. */
    private void renewHeldLeases() {
        for (InFlightJob held : inFlight.values()) {
            if (held.closing()) {
                continue;
            }
            boolean stillHeld;
            try {
                stillHeld = knowledgeJobService.heartbeat(held.job());
            } catch (RuntimeException failure) {
                log.warn("knowledge job lease renewal failed job={} kb={} code={} detail={}",
                        held.job().getId(), held.job().getKbId(), DEPENDENCY_UNAVAILABLE,
                        failure.getClass().getSimpleName());
                continue;
            }
            if (!stillHeld) {
                held.markClosing();
                log.warn("knowledge job lease no longer held job={} kb={} leaseToken={}",
                        held.job().getId(), held.job().getKbId(), held.leaseToken());
            }
        }
    }

    /** 中文说明：按空闲槽认领：槽数为零就不 claim；一次 claim 的行数同时被 SQL 的 {@code LIMIT slots}、执行器真实余量与 {@code claim-size} 三重收口，因此「bounded 2 workers / 32 queue / claim &lt;= slots」在运行期成立。 English summary: claiming within the free slots: zero slots means no claim at all; one claim is bounded three times over — the SQL {@code LIMIT slots}, the executor's real headroom and {@code claim-size} — which is what makes "bounded 2 workers / 32 queue / claim &lt;= slots" true at runtime. */
    private void claimWithinFreeSlots() {
        int slots = freeSlots();
        if (slots <= 0) {
            return;
        }
        List<KnowledgeJobBO> claimed = knowledgeJobService.claimAvailable(slots);
        if (claimed == null || claimed.isEmpty()) {
            return;
        }
        log.info("knowledge jobs claimed slots={} count={}", slots, claimed.size());
        for (KnowledgeJobBO job : claimed) {
            submit(job);
        }
    }

    /** 中文说明：空闲槽 = 执行器总容量（最大线程 + 队列容量）− 已占用（活动线程 + 队列深度），再按 {@code claimSize} 取小并夹到非负；只用 Spring 的空安全计数 getter，因此停机竞态里读到 0 也只会让本轮不认领，而不是把异常抛进调度线程。 English summary: free slots are the executor's total capacity (maximum pool size plus queue capacity) minus what is occupied (active threads plus queue depth), clamped against {@code claimSize} and to a non-negative floor; only Spring's null-safe counters are read, so a shutdown race that yields zero simply skips this tick instead of throwing into the scheduler thread. */
    private int freeSlots() {
        int capacity = knowledgeJobTaskExecutor.getMaxPoolSize() + knowledgeJobTaskExecutor.getQueueCapacity();
        int occupied = knowledgeJobTaskExecutor.getActiveCount() + knowledgeJobTaskExecutor.getQueueSize();
        int headroom = Math.max(0, capacity - occupied);
        return Math.min(headroom, Math.max(0, knowledgeProperties.getClaimSize()));
    }

    /** 中文说明：投递一条已认领的作业：先在途表登记（否则本轮之后的续租扫描看不到它），再提交执行器；{@link TaskRejectedException}（或池已关停）时立刻撤销登记并 warn，此后既不再续租也不写回，让租约自己到期回到可认领集合。 English summary: submitting one claimed job: the in-flight registration happens first, otherwise the next renewal sweep could not see it, and the executor call follows; a {@link TaskRejectedException} (or a shut-down pool) undoes the registration and warns, after which the job is neither renewed nor written back, so its lease lapses and the row returns to the claimable set. */
    private void submit(KnowledgeJobBO job) {
        Long leaseToken = job == null ? null : job.getLeaseToken();
        String jobId = job == null ? null : job.getId();
        if (jobId == null || leaseToken == null || leaseToken.longValue() < 1L) {
            log.warn("knowledge job claim carries no usable lease job={} leaseToken={}", jobId, leaseToken);
            return;
        }
        InFlightJob held = new InFlightJob(job, leaseToken.longValue());
        if (inFlight.putIfAbsent(jobId, held) != null) {
            log.warn("knowledge job already in flight job={} kb={}", jobId, job.getKbId());
            return;
        }
        try {
            knowledgeJobTaskExecutor.execute(() -> execute(held));
        } catch (TaskRejectedException saturated) {
            inFlight.remove(jobId, held);
            log.warn("knowledge job submission rejected job={} kb={} type={} code={}",
                    jobId, job.getKbId(), job.getType(), DEPENDENCY_UNAVAILABLE);
        } catch (IllegalStateException shuttingDown) {
            inFlight.remove(jobId, held);
            log.warn("knowledge job submission refused job={} kb={} type={} detail={}",
                    jobId, job.getKbId(), job.getType(), shuttingDown.getClass().getSimpleName());
        }
    }

    /**
     * 中文说明：一条作业在执行线程上的完整生命周期：开启以作业 id 为请求号的 trace（{@code close()} 在 finally 恢复先前 MDC）
     * → 在可信持久化上下文内查注册表分发 → 拿策略返回的终态载体经 {@code publishTerminal} 的租约 CAS 写回 →
     * 从在途表移除。分发只查表，无 {@code switch}；查不到实现即以 {@link #STRATEGY_UNAVAILABLE} 落终态失败，
     * 因为一条「已经认领却永远不会执行」的作业被静默丢掉比失败更糟（幂等意图会被永久占住）。
     * 进入发布前一律先 {@code markClosing()}，让续租扫描从此刻停止发心跳——策略可能已经在阶段边界自己续过租，
     * 心跳与发布之间的竞态因此只会落在「CAS 命中或 0 行」两种可判定结果上。
     * {@code catch} 只处理基础设施异常：策略已把类型化错误收束成载体，能抛到这里的是数据库、租约或身份上下文，
     * 处理方式是一次 best-effort 的 {@link #abandon} 写回（同样受租约 CAS 保护，0 行即已被他人收口），
     * 然后照常从在途表移除；本类绝不把未知结果当成成功，也绝不改道云端。
     * English summary: The whole life of one job on an execution thread: open a trace whose request id is the job id (its
     * {@code close()} restores the previous MDC in a finally) → dispatch through the registry inside the trusted persistence
     * context → write the strategy's terminal carrier back through the {@code publishTerminal} lease CAS → remove it from the
     * in-flight table. Dispatch only reads the map and contains no {@code switch}; a type with no implementation lands the
     * terminal {@link #STRATEGY_UNAVAILABLE} failure, because silently dropping a job that was already claimed is worse than
     * failing it — the idempotency intent would stay occupied forever. Every path calls {@code markClosing()} before
     * publication so the renewal sweep stops beating from that instant: the strategy may already have renewed at a phase
     * boundary, and the only remaining races between heartbeat and publication are the two decidable outcomes of a CAS hit or
     * zero rows. The {@code catch} covers infrastructure only — a strategy already closes every typed error into a carrier, so
     * anything reaching here is database, lease or identity context — and the response is one best-effort {@link #abandon}
     * write-back, itself fenced by the lease CAS so zero rows means somebody else already settled the row; the job is then
     * removed from the in-flight table as usual. An unknown outcome is never treated as success and never rerouted to cloud.
     * @param held 参数 在途记录（认领载体 + 本次租约令牌）；parameter the in-flight record (claimed carrier plus this lease token).
     */
    private void execute(InFlightJob held) {
        KnowledgeJobBO job = held.job();
        long leaseToken = held.leaseToken();
        try {
            persistenceContext.call(null, () -> {
                KnowledgeJobStrategy strategy = job.getType() == null
                        ? null : knowledgeJobStrategyRegistry.get(job.getType());
                if (strategy == null) {
                    log.warn("knowledge job strategy unavailable job={} kb={} type={} code={}",
                            job.getId(), job.getKbId(), job.getType(), STRATEGY_UNAVAILABLE);
                    held.markClosing();
                    knowledgeJobService.publishTerminal(undispatched(job), leaseToken);
                    return null;
                }
                KnowledgeJobBO terminal = strategy.run(job, leaseToken);
                held.markClosing();
                if (terminal == null || terminal.getId() == null) {
                    log.warn("knowledge job produced no terminal carrier job={} kb={} leaseToken={}",
                            job.getId(), job.getKbId(), leaseToken);
                    return null;
                }
                knowledgeJobService.publishTerminal(terminal, leaseToken);
                log.info("knowledge job finished job={} kb={} type={} status={} stage={} code={}",
                        terminal.getId(), terminal.getKbId(), job.getType(), terminal.getStatus(),
                        terminal.getStage(), terminal.getErrorCode());
                return null;
            });
        } catch (Exception failure) {
            held.markClosing();
            log.error("knowledge job execution failed job={} kb={} type={} code={} detail={}",
                    job.getId(), job.getKbId(), job.getType(), DEPENDENCY_UNAVAILABLE,
                    failure.getClass().getSimpleName());
            abandon(held);
        } finally {
            inFlight.remove(job.getId(), held);
        }
    }

    /** 中文说明：异常后的 best-effort 终态写回：把作业记为 {@code FAILED + YUHENG_DEPENDENCY_UNAVAILABLE}（阶段沿用认领时的权威值，不猜进度），仍走同一条租约 CAS；写不进去就是所有权已丢，交给新持有者或租约过期收口，本方法只补一条 warn，绝不重试或抛回调度线程。 English summary: the best-effort terminal write-back after an exception: the job is recorded as {@code FAILED + YUHENG_DEPENDENCY_UNAVAILABLE} (keeping the claimed stage rather than guessing progress) through the very same lease CAS; a miss means ownership is gone and is settled by the new holder or the expiring lease, so this method adds one warn and never retries or throws back into the scheduler thread. */
    private void abandon(InFlightJob held) {
        try {
            persistenceContext.call(null, () -> {
                knowledgeJobService.publishTerminal(failed(held.job()), held.leaseToken());
                return null;
            });
        } catch (Exception failure) {
            log.warn("knowledge job abandonment not published job={} kb={} detail={}",
                    held.job().getId(), held.job().getKbId(), failure.getClass().getSimpleName());
        }
    }

    /** 中文说明：把认领载体复制成「已认领但无法分发」的终态载体：只改状态、错误码（64 字符内截断）与结果，载荷、租约三列、attempt、revision 与时间戳原样保留，因为写回语句按整行 {@code et} 比对 CAS；attempt 沿用认领值——没有真正执行过任何一次尝试，不能虚增。 English summary: copies the claimed carrier into the terminal carrier for "claimed but undispatchable": only status and the error code (truncated to 64 characters) change, while the payload, the three lease columns, attempt, revision and timestamps carry over verbatim because the write-back compares the whole loaded row; the claimed attempt stays as is, since no attempt actually ran and none may be invented. */
    private KnowledgeJobBO undispatched(KnowledgeJobBO job) {
        return copyOf(job)
                .setStatus(KnowledgeJobStatusEnum.FAILED)
                .setErrorCode(machineCode(STRATEGY_UNAVAILABLE));
    }

    /** 中文说明：把执行异常收束成终态失败载体：状态 {@code FAILED}、阶段保持认领值、错误码固定为依赖不可用，不写任何异常消息（消息可能带载荷事实）也不虚增 attempt。 English summary: closes an execution exception into a terminal failure carrier: {@code FAILED} with the claimed stage preserved and the fixed dependency machine code, writing no exception message at all (a message can carry payload facts) and never inflating the attempt. */
    private KnowledgeJobBO failed(KnowledgeJobBO job) {
        return copyOf(job)
                .setStatus(KnowledgeJobStatusEnum.FAILED)
                .setErrorCode(machineCode(DEPENDENCY_UNAVAILABLE));
    }

    /** 中文说明：机器码长度收口：{@code error_code} 列有限宽，超出即截断而不是丢弃整次写回；截断后仍可读，且不引入任何动态文案。 English summary: bounds a machine code to the column width: an over-long code is truncated rather than abandoning the whole write-back, staying readable and never introducing dynamic text. */
    private static String machineCode(String code) {
        return code.length() <= MAX_ERROR_CODE_CHARACTERS ? code : code.substring(0, MAX_ERROR_CODE_CHARACTERS);
    }

    /** 中文说明：载体整体复制：终态由本类构造时也必须交出全部权威列，供守卫按整行比对（同 {@code DocumentIngestionStrategy} 的 {@code copyJob} 口径），只改写状态与错误码。 English summary: a full copy of the carrier: whenever this class builds the terminal state it must still hand over every authoritative column so the guard can compare the whole row (the same shape {@code DocumentIngestionStrategy}'s {@code copyJob} uses), changing only status and error code. */
    private KnowledgeJobBO copyOf(KnowledgeJobBO job) {
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
     * 中文说明：在途记录：把「认领载体 + 本次租约令牌 + 是否进入收尾」绑成一个不可拆的单元，
     * 这样续租扫描绝不会再对一条正在发布的作业发心跳。令牌在认领那一刻就固定，
     * 之后被接管只会推进库中现值并让本 worker 的 CAS 归零，本类不会用新令牌重发。
     * English summary: the in-flight record, binding the claimed carrier, this lease token and the closing flag into one
     * unit so the renewal sweep can never beat a job that is being published. The token is pinned at claim time: a later
     * takeover only advances the stored value and zeroes this worker's CAS, and the class never republishes with a new token.
     */
    private static final class InFlightJob {

        private final KnowledgeJobBO job;

        private final long leaseToken;

        private final AtomicBoolean closing = new AtomicBoolean();

        private InFlightJob(KnowledgeJobBO job, long leaseToken) {
            this.job = job;
            this.leaseToken = leaseToken;
        }

        private KnowledgeJobBO job() {
            return job;
        }

        private long leaseToken() {
            return leaseToken;
        }

        private boolean closing() {
            return closing.get();
        }

        private void markClosing() {
            closing.set(true);
        }
    }
}
