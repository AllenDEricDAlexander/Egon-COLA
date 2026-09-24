package top.egon.cola.component.yuheng.admin.knowledge.domain.bo;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobStageEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobStatusEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobTypeEnum;

import java.time.Instant;

/**
 * 中文说明：{@code KnowledgeJobBO} 是 {@code gateway_knowledge_job} 的业务载体，文档摄取与 Wiki 生成共用
 * 这一张持久任务表：{@code type}/{@code status}/{@code stage} 是具名枚举而不是自由字符串，
 * {@code payload} 与 {@code result} 两列在库内是 jsonb，在这里保持 {@link JsonNode} 顶层类型而不退化为任意
 * Map 字段集合，租约三列（{@code leaseOwner}/{@code leaseToken}/{@code leaseExpiresAt}）与
 * {@code attempt}/{@code nextAttemptAt} 共同构成 claim-heartbeat-finish 的全部状态；{@code revision} 与
 * {@code createdAt}/{@code updatedAt} 是服务端权威投影。它不继承 {@code EgonModel}，不承载租户与软删列，
 * 也不携带任何消息中间件句柄。
 * English summary: {@code KnowledgeJobBO} is the business carrier of {@code gateway_knowledge_job}, the single
 * durable task table shared by document ingestion and wiki generation: {@code type}/{@code status}/{@code stage} are
 * named enums rather than free strings, {@code payload} and {@code result} keep the {@link JsonNode} top level type
 * instead of degrading into loose Map fields, the three lease columns together with {@code attempt} and
 * {@code nextAttemptAt} form the whole claim-heartbeat-finish state, while {@code revision} and the audit instants
 * stay server-authoritative. It neither extends {@code EgonModel} nor carries tenant, soft-delete or broker handles.
 *
 * 用法 / Usage: 只由 {@code KnowledgeJobPersistenceConverter} 与 {@code KnowledgeJobPO} 互转，并经
 * {@code KnowledgeRepository}/{@code KnowledgeJobService} 端口流动。每一次状态回写都必须携带
 * {@code id + leaseToken + status RUNNING + 未过期} 的 CAS 断言，返回 0 行意味着所有权已丢失而不是成功；
 * {@code leaseToken} 在重启后单调递增，{@code attempt} 只在显式可重试错误时按 5s/30s 递增且总数 ≤3，
 * {@code retryOfJobId} 指向失败的原作业而不是覆盖历史。这些时序与租约不变量由
 * {@code KnowledgeJobServiceImpl} 与 {@code KnowledgeJobStrategy} 保持，本载体只声明单字段边界；
 * {@code payload}/{@code result} 内不得写入密钥、正文或上游响应体。
 * Structural mapping happens only in the persistence converter; every write-back must assert
 * {@code id + leaseToken + RUNNING + unexpired} and zero rows means lost ownership rather than success, the token
 * increases monotonically across restarts, attempts stay ≤3 with 5s/30s delays for explicitly retryable errors, and
 * {@code retryOfJobId} points at the failed predecessor instead of overwriting history — those timing and lease
 * invariants belong to the job service and strategy while this carrier keeps single-field bounds and never holds
 * secrets, bodies or upstream responses in its jsonb.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgeJobBO {

    /** 不透明主键的十进制文本，插入前为 null / decimal text of the assigned key, null before an insert. */
    private String id;

    /** 所属知识库十进制 ID，worker 与查询都以 KB 为边界 / owning knowledge base id, the boundary for workers and queries alike. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String kbId;

    /** DOCUMENT_INGEST 或 WIKI_GENERATE，只经 Strategy 注册表路由 / ingestion or wiki generation, routed only through the strategy registry. */
    @NotNull
    private KnowledgeJobTypeEnum type;

    /** 目标资源十进制 ID（资料/页面/KB），依 type 由应用层复核 / targeted resource id (document, page or base), revalidated per type by the application. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String resourceId;

    /** 发起主体，运行时重新校验成员资格且不赋予系统超权 / originating subject, membership rechecked at runtime without granting extra system authority. */
    @NotBlank
    @Size(max = 128)
    private String actorId;

    /** 冻结的作业输入（revision IDs、source hashes、模型/空间、basePageRevision），jsonb / frozen job inputs (revision ids, source hashes, model and space, base page revision) as jsonb. */
    @NotNull
    private JsonNode payload;

    /** 幂等意图键，可打印 ASCII 16–64，按 actor+kb+type 作用域唯一 / idempotency intent, printable ASCII 16–64, unique per actor, base and type. */
    @NotBlank
    @Size(min = 16, max = 64)
    private String idempotencyKey;

    /** 规范化命令的小写 SHA-256 十六进制 64 字符，同键不同 hash 判 409 / lowercase SHA-256 hex of the canonical command, a mismatch on the same key is a conflict. */
    @NotBlank
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String requestHash;

    /** QUEUED/RUNNING/RETRY_WAIT/SUCCEEDED/FAILED/STALE/CANCELLED / the full persisted job status vocabulary. */
    @NotNull
    private KnowledgeJobStatusEnum status;

    /** QUEUED/PARSE/EMBED/GENERATE/PUBLISH/DONE，claim 不改变阶段 / the execution stage, unchanged by a claim. */
    @NotNull
    private KnowledgeJobStageEnum stage;

    /** 已消耗的执行次数，CHECK 0..3 / consumed attempts, CHECK 0..3. */
    @NotNull
    @Min(0)
    @Max(3)
    private Integer attempt;

    /** 最早可被 claim 的 UTC 时刻，首次为 now，明确重试按 5s/30s 推迟 / earliest claimable instant, now on creation and delayed by 5s/30s for an explicit retry. */
    @NotNull
    private Instant nextAttemptAt;

    /** 持有租约的 worker 实例，无运行任务时为 null / owning worker instance, null while no attempt runs. */
    @Size(max = 128)
    private String leaseOwner;

    /** 每次 claim 单调 +1 的租约令牌，回写必须匹配，初始 0 / lease token increasing monotonically per claim and required by every write-back, starting at 0. */
    @NotNull
    @Min(0)
    private Long leaseToken;

    /** 租约到期时刻，RUNNING 必填（120s），其余状态为 null / lease expiry, mandatory while RUNNING (120s) and null otherwise. */
    private Instant leaseExpiresAt;

    /** 仅安全机器码，1..128 字符，不存上游响应正文 / a safe machine code only, 1..128 characters, never an upstream body. */
    @Size(max = 128)
    private String errorCode;

    /** 成功输出的资源/版本 ID，jsonb，限额 64KiB，未终态时为 null / produced resource and revision ids as jsonb, capped at 64KiB and null before a terminal state. */
    private JsonNode result;

    /** 人工重试所指向的历史作业十进制 ID，原作业不被覆盖；非重试为 null / predecessor job id of a manual retry, keeping the original row; null unless this is a retry. */
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String retryOfJobId;

    /** 调用方期望的乐观版本：创建意图为 0 哨兵值，回写为库中现值，落库后由仓储回写权威值（存储行恒 ≥1）/ caller expectation, 0 on create intent and the stored revision on write-back, overwritten by the authoritative value (stored rows are always ≥1) after a save. */
    @Min(0)
    private long revision;

    /** 投影自 {@code create_time} 的入队时刻 / creation instant projected from {@code create_time}. */
    private Instant createdAt;

    /** 投影自 {@code update_time} 的最后状态变更时刻 / update instant projected from {@code update_time}. */
    private Instant updatedAt;
}
