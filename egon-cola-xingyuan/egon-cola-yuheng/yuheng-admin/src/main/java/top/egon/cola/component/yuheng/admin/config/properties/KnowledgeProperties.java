package top.egon.cola.component.yuheng.admin.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 中文说明：{@code KnowledgeProperties} 是知识摄取与持久任务的运行边界配置（Spec §15 配置键表
 * {@code yuheng.knowledge.*}，默认值逐字为 {@code false/false/2/2/PT2M/PT30S/3/[PT5S,PT30S]}）。
 * 这些键<b>不是</b>调优建议而是安全与正确性事实：租约 120 秒与心跳 30 秒决定过期 worker 能否发布，
 * {@code claim-size} 与 {@code worker-concurrency} 决定「每次 claim 不得超过可用槽」是否成立，
 * {@code max-attempts} 与 {@code retry-delays} 决定「≤3 次总尝试、5s/30s 退避、仅对显式可重试错误」这条合同，
 * {@code max-upload-bytes} 的上界 20 MiB 就是 {@code gateway_knowledge_revision.byte_count} 的 DB CHECK，
 * {@code embed-batch-size} 的上界 64 就是 {@code KnowledgeModelClientService#embed} 的 {@code @Size(max = 64)}。
 * 本类只<b>携带默认值</b>：按用户要求配置由启动时补充，因此这里没有也不该有 YAML 文件，宽松取值一律被
 * {@code @Min}/{@code @Max}/{@code @Size}/{@code @NotNull} 与下面的跨字段自检在绑定阶段拒绝。
 * {@code ignoreUnknownFields} 刻意保持缺省（宽容）：{@code yuheng.knowledge.model-client.*}（本 Step）与
 * {@code yuheng.knowledge.wiki.*}（Step 14）是同一前缀下另立的 {@code @ConfigurationProperties} 根，
 * 收紧会让父绑定把子键报成未知字段，这与 {@code GatewayAdminProperties} 对 {@code yuheng.admin.openapi.*}
 * 的处理完全一致；子键本身的严格绑定由 {@code KnowledgeModelClientProperties} 负责。
 * English summary: {@code KnowledgeProperties} is the runtime boundary of knowledge ingestion and persistent jobs
 * (Spec §15 key table {@code yuheng.knowledge.*}, defaults verbatim {@code false/false/2/2/PT2M/PT30S/3/
 * [PT5S,PT30S]}). These keys are security and correctness facts rather than tuning advice: lease 120s against
 * heartbeat 30s decides whether an expired lease can still publish, claim size against worker concurrency decides
 * whether "a claim never exceeds the free slots" holds, max attempts against the retry delays decide the
 * "at most three attempts, 5s/30s, only explicitly retryable errors" contract, the 20 MiB upload ceiling is exactly the
 * {@code gateway_knowledge_revision.byte_count} DB CHECK, and the 64 batch ceiling is exactly
 * {@code KnowledgeModelClientService#embed}'s {@code @Size(max = 64)}. This class alone <b>carries</b> the defaults:
 * the user supplies configuration at startup, so there is deliberately no YAML here, and any permissive value is
 * refused during binding by the {@code @Min}/{@code @Max}/{@code @Size}/{@code @NotNull} bounds plus the cross-field
 * checks below. {@code ignoreUnknownFields} intentionally stays at its lenient default because
 * {@code yuheng.knowledge.model-client.*} (this Step) and {@code yuheng.knowledge.wiki.*} (Step 14) are separate
 * {@code @ConfigurationProperties} roots under the same prefix, so tightening it would make the parent report its own
 * children as unknown — exactly how {@code GatewayAdminProperties} treats {@code yuheng.admin.openapi.*}; the strict
 * sub-tree binding belongs to {@code KnowledgeModelClientProperties}.
 *
 * 用法 / Usage: 由 {@code KnowledgeConfiguration} 以 {@code @EnableConfigurationProperties} 注册，注入点用
 * {@link #BEAN_NAME} 消歧；缺省两把开关皆为假，故未显式开启时既无 worker 也无出网，管理面读写路径不受影响。
 */
@Validated
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@ConfigurationProperties(prefix = "yuheng.knowledge")
public class KnowledgeProperties {

    /** 中文说明：{@code @EnableConfigurationProperties} 生成的 bean 名（{@code prefix-全限定类名}），注入点据此消歧。
     *  English summary: the bean name {@code @EnableConfigurationProperties} generates ({@code prefix-FQCN}) that injection points use to disambiguate. */
    public static final String BEAN_NAME
            = "yuheng.knowledge-top.egon.cola.component.yuheng.admin.config.properties.KnowledgeProperties";

    /** 中文说明：知识能力总开关，缺省为假；为假时本 Step 的摄取面不产出任何模型调用。
     *  English summary: the master knowledge switch, false unless configured; while false this Step's ingestion face makes no model call at all. */
    @NotNull
    @Builder.Default
    private boolean enabled = false;

    /** 中文说明：后台 worker 开关，缺省为假；只有在 {@link #enabled} 为真时才允许为真，见 {@link #isWorkerEnabledOnlyWithFeature()}。 English summary: the background worker switch, false unless configured; it may only be true while {@link #enabled} is true, see {@link #isWorkerEnabledOnlyWithFeature()}. */
    @NotNull
    @Builder.Default
    private boolean workerEnabled = false;

    /** 中文说明：worker 并发（Spec 默认 2），同时是 {@code knowledgeJobTaskExecutor} 的核心与最大线程数，硬上界 32 与队列容量同阶。 English summary: the worker concurrency (Spec default 2), which is both the core and the maximum pool size of {@code knowledgeJobTaskExecutor}, capped at 32 to stay on the queue's order. */
    @Min(1)
    @Max(32)
    @Builder.Default
    private int workerConcurrency = 2;

    /** 中文说明：单次数据库 claim 的行数上界（Spec 默认 2），必须不大于可用槽，见 {@link #isClaimWithinWorkerCapacity()}；真实上限仍由 SQL 的 {@code LIMIT slots} 收口。 English summary: the row cap of one database claim (Spec default 2), which must not exceed the free slots, see {@link #isClaimWithinWorkerCapacity()}; the runtime bound stays the SQL {@code LIMIT slots}. */
    @Min(1)
    @Max(32)
    @Builder.Default
    private int claimSize = 2;

    /** 中文说明：租约时长（Spec 默认 PT2M）；所有状态回写都要匹配 {@code lease_token} 与未过期的它，因此它是「过期 worker 不能发布」的时间事实。 English summary: the lease duration (Spec default PT2M); every write-back must match the {@code lease_token} plus this unexpired value, which is what keeps an expired worker from publishing. */
    @NotNull
    @Builder.Default
    private Duration lease = Duration.ofMinutes(2);

    /** 中文说明：心跳周期（Spec 默认 PT30S），必须严格短于租约，见 {@link #isHeartbeatShorterThanLease()}。 English summary: the heartbeat period (Spec default PT30S), which must stay strictly inside the lease, see {@link #isHeartbeatShorterThanLease()}. */
    @NotNull
    @Builder.Default
    private Duration heartbeat = Duration.ofSeconds(30);

    /** 中文说明：一个意图的总尝试上限（Spec 默认 3），与 {@code gateway_knowledge_job} 的 {@code attempt CHECK 0..3} 同界，超出即为不可重试终态。 English summary: the total attempt cap for one intent (Spec default 3), bounded by the same {@code attempt CHECK 0..3} on {@code gateway_knowledge_job}; anything beyond it is a terminal failure. */
    @Min(1)
    @Max(3)
    @Builder.Default
    private int maxAttempts = 3;

    /** 中文说明：自动重试的退避序列（Spec 默认 [PT5S,PT30S]），只服务显式可重试错误；条目数必须覆盖 {@code maxAttempts - 1} 次重试且全部为正，见 {@link #isRetryScheduleConsistent()}。 English summary: the automatic retry back-off sequence (Spec default [PT5S,PT30S]) for explicitly retryable errors only; it must cover {@code maxAttempts - 1} retries with positive entries, see {@link #isRetryScheduleConsistent()}. */
    @NotNull
    @Size(min = 1, max = 8)
    @Builder.Default
    private List<@NotNull Duration> retryDelays
            = new ArrayList<>(List.of(Duration.ofSeconds(5), Duration.ofSeconds(30)));

    /** 中文说明：单次上传原件的字节上限（Spec 默认 20971520 = 20MiB），上界即 revision 表的 {@code byte_count} CHECK，配小了只会更早 413，配大了数据库会拒绝。 English summary: the per-upload raw byte cap (Spec default 20971520 = 20MiB) whose ceiling is the revision table's {@code byte_count} CHECK; a smaller value only 413s sooner while a larger one the database refuses. */
    @Min(1)
    @Max(20_971_520)
    @Builder.Default
    private long maxUploadBytes = 20_971_520L;

    /** 中文说明：分块预算，单位为 token（Spec 的 {@code chunking_config.chunkSize}），界 32..4096 与被复用组件 {@code RagChunkingConfigDTO.MIN/MAX_MAX_TOKENS} 一致，冻结进 revision 的 {@code chunking_config} 后同 job 重试必须可复现。 English summary: the chunk budget in tokens (the Spec's {@code chunking_config.chunkSize}) bounded by 32..4096, which is the reused component's {@code RagChunkingConfigDTO.MIN/MAX_MAX_TOKENS}; once frozen into the revision's {@code chunking_config} a re-run of the same job must reproduce it. */
    @Min(32)
    @Max(4096)
    @Builder.Default
    private int chunkSize = 512;

    /** 中文说明：相邻块的前缀重叠（Spec 的 {@code chunking_config.overlap}），单位为 token，必须严格小于 {@link #chunkSize}，见 {@link #isOverlapBelowChunkSize()}。 English summary: the prefix overlap of neighbouring chunks in tokens (the Spec's {@code chunking_config.overlap}), which must stay strictly below {@link #chunkSize}, see {@link #isOverlapBelowChunkSize()}. */
    @Min(0)
    @Max(2048)
    @Builder.Default
    private int chunkOverlap = 64;

    /** 中文说明：一次 embedding 请求的文本条数上限，硬上界 64 来自端口合同 {@code @Size(max = 64)}；摄取必须按此分批，失败批次不得污染已 stage 的计数。 English summary: the text count of one embedding request, hard-capped at 64 by the port contract's {@code @Size(max = 64)}; ingestion must batch within it and a failed batch may not pollute the staged count. */
    @Min(1)
    @Max(64)
    @Builder.Default
    private int embedBatchSize = 64;

    /**
     * 中文说明：返回 {@link #retryDelays} 的不可变快照副本，使正在排程的 worker 永不可能看到一份被并发缩短或清空的退避表。
     * English summary: Returns an immutable snapshot of {@link #retryDelays} so a scheduling worker can never observe a
     * concurrently shortened or cleared back-off table.
     */
    public List<Duration> getRetryDelays() {
        return retryDelays == null
                ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(retryDelays));
    }

    /**
     * 中文说明：写入时复制 {@link #retryDelays}，绑定与测试传入的可变列表不会与本类共享同一个底层数组。
     * English summary: Defensively copies the incoming {@link #retryDelays} so a mutable list from binding or a test never
     * shares this instance's backing array.
     */
    public KnowledgeProperties setRetryDelays(List<Duration> retryDelays) {
        this.retryDelays = retryDelays == null ? new ArrayList<>() : new ArrayList<>(retryDelays);
        return this;
    }

    /**
     * 中文说明：跨字段自检：worker 是「在没有 HTTP 请求的线程里发起模型调用」的那一方，脱离总开关单独开启它会让一个
     * 本应关闭的能力在后台出网，因此这种组合在绑定阶段即失败关闭，而不是留到运行时判断。
     * English summary: Cross-field check: the worker is the party that issues model calls from a thread with no HTTP request,
     * so enabling it apart from the master switch would let a supposedly closed capability reach the network in the
     * background; that combination therefore fails closed during binding instead of being decided at runtime.
     */
    @AssertTrue(message = "yuheng.knowledge.worker-enabled requires yuheng.knowledge.enabled to be true")
    public boolean isWorkerEnabledOnlyWithFeature() {
        return !workerEnabled || enabled;
    }

    /**
     * 中文说明：跨字段自检：Spec 要求「每次数据库 claim 不得超过可用槽」，若 {@code claim-size} 大于
     * {@code worker-concurrency}，配置本身就承诺了超发，SQL 的 {@code LIMIT slots} 也补不上这个语义漏洞。
     * English summary: Cross-field check: the Spec requires every database claim to stay within the free slots, so a
     * {@code claim-size} above {@code worker-concurrency} would have configuration itself promise over-submission, which
     * the SQL {@code LIMIT slots} cannot repair.
     */
    @AssertTrue(message = "yuheng.knowledge.claim-size must not exceed yuheng.knowledge.worker-concurrency")
    public boolean isClaimWithinWorkerCapacity() {
        return claimSize <= workerConcurrency;
    }

    /**
     * 中文说明：跨字段自检：心跳唯一的作用是续租，周期不严格短于租约就意味着两次心跳之间租约必然过期，
     * 于是合法持有者会被自己的配置判定为失去所有权，进而把在途任务让给别的 worker。
     * English summary: Cross-field check: renewing the lease is the heartbeat's only purpose, so a period that is not
     * strictly shorter than the lease guarantees the lease lapses between two beats and the lawful holder is declared
     * owner-less by its own configuration, handing running work to another worker.
     */
    @AssertTrue(message = "yuheng.knowledge.heartbeat must be shorter than yuheng.knowledge.lease")
    public boolean isHeartbeatShorterThanLease() {
        return lease != null && heartbeat != null && heartbeat.compareTo(lease) < 0;
    }

    /**
     * 中文说明：跨字段自检：{@code max-attempts} 次总尝试之间最多需要 {@code maxAttempts - 1} 次退避，序列更短就会让
     * 某一次重试没有延迟可依；任何非正条目则等价于「立即重打上游」，与有界重试的合同相反。
     * English summary: Cross-field check: {@code max-attempts} total tries need at most {@code maxAttempts - 1} back-offs, so
     * a shorter sequence leaves a retry with no delay to observe, while any non-positive entry means "hit the upstream
     * again immediately", the opposite of bounded retry.
     */
    @AssertTrue(message = "yuheng.knowledge.retry-delays must hold at least max-attempts minus one positive durations")
    public boolean isRetryScheduleConsistent() {
        if (retryDelays == null || retryDelays.size() < maxAttempts - 1) {
            return false;
        }
        return retryDelays.stream().allMatch(delay -> delay != null
                && !delay.isZero() && !delay.isNegative());
    }

    /**
     * 中文说明：跨字段自检：重叠以整块为前提，不小于块预算时切片永不推进，摄取会在同一个窗口上无限重复，
     * 并把 {@code (revision_id, chunk_index)} 的唯一意图写成自相矛盾。
     * English summary: Cross-field check: overlap presupposes a strictly smaller window, because an overlap at or above the
     * chunk budget never advances, so ingestion would repeat the same window forever and write a self-contradictory
     * intent into the {@code (revision_id, chunk_index)} uniqueness key.
     */
    @AssertTrue(message = "yuheng.knowledge.chunk-overlap must be smaller than yuheng.knowledge.chunk-size")
    public boolean isOverlapBelowChunkSize() {
        return chunkOverlap < chunkSize;
    }
}
