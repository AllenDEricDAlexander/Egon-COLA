package top.egon.cola.component.yuheng.admin.knowledge.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 中文说明：{@code KnowledgeRetryCommandDTO} 是原 API-021 人工重试失败作业的 CQE Command 载体，
 * 只有一个客户端字段 {@code expectedRevision}；后继作业 ID、{@code retryOfJobId}、幂等意图、请求哈希、
 * 冻结的 payload 与租约列都由服务端从失败原作业派生，租户更不是本载体的字段。
 * English summary: {@code KnowledgeRetryCommandDTO} is the API-021 command carrier for a manual retry of a failed
 * job and holds the single client field {@code expectedRevision}; the successor id, {@code retryOfJobId}, idempotency
 * intent, request hash, frozen payload and lease columns are all server-derived from the failed predecessor, and
 * tenancy is never a field of this carrier.
 *
 * 用法 / Usage: 由 Controller 以 {@code @Valid} 绑定，路径 {@code jobId} 才是重试目标；
 * {@code expectedRevision} 必须等于原作业的当前 revision，不符返回 409 {@code KNOWLEDGE_REVISION_CONFLICT}
 * 并回带权威 revision，0 行更新一律视为失败而不是成功；只有终态失败且原作业未被后继覆盖时才可重试，
 * 该状态机判断与 ≤3 次尝试、5s/30s 退延的策略都属于 Service，注解只守住 CAS 输入。
 * Bound with {@code @Valid} at the controller while the path {@code jobId} names the retry target; the expected
 * revision must equal the predecessor's stored revision, a mismatch returns 409 with the authoritative value, zero
 * updated rows are a failure rather than a success, and the terminal-state plus attempt/delay policy stays with the
 * service while the annotations only guard the CAS input.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgeRetryCommandDTO {

    /** 必须等于原作业当前 revision 的正整数，不符返回 409 / must equal the predecessor revision, a positive integer, and a mismatch yields 409. */
    @NotNull
    @Min(1)
    private Long expectedRevision;
}
