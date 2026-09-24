package top.egon.cola.component.yuheng.admin.knowledge.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 中文说明：{@code KnowledgeReindexCommandDTO} 是原 API-018 为既有资料重新排队摄取作业的 CQE Command 载体，
 * 只承载 {@code sourceRevisionId/expectedRevision}；作业 ID、幂等意图与请求哈希都由服务端派生，
 * 租户更不是本载体的字段。
 * English summary: {@code KnowledgeReindexCommandDTO} is the API-018 command carrier that re-queues ingestion for an
 * existing document; it holds only {@code sourceRevisionId/expectedRevision}, while the job id, idempotency intent
 * and request hash stay server-derived and tenancy is never a field of this carrier.
 *
 * 用法 / Usage: 由 Controller 以 {@code @Valid} 绑定；{@code sourceRevisionId} 缺失表示以资料当前
 * active revision 为源，存在时必须是十进制字符串 ID 且属于路径上的 {@code documentId}（该归属与状态
 * READY 的复核属于 Service，注解不伪造跨资源检查）；{@code expectedRevision} 是资料行的乐观版本哨兵，
 * 与库中现值不符返回 409 {@code KNOWLEDGE_REVISION_CONFLICT} 并回带当前 revision。命令进入 Service 后
 * 视为不可变，任何重新赋值都必须重新校验。
 * Bound with {@code @Valid} at the controller; a missing source means "reingest the current active revision", an
 * supplied one must be a decimal id owned by the path document (ownership and READY state are service checks, the
 * annotation never fakes a cross-resource lookup), and {@code expectedRevision} is the document CAS sentinel whose
 * mismatch returns 409 with the authoritative revision. Treat the command as immutable once it reaches the service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgeReindexCommandDTO {

    /** 重新摄取的来源版本十进制 ID，缺失即使用当前 active revision / source revision id for the reingest, absent meaning the current active revision. */
    @Schema(nullable = true)
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String sourceRevisionId;

    /** 必须等于资料当前 revision 的正整数，不符返回 409 / must equal the document revision, a positive integer, and a mismatch yields 409. */
    @NotNull
    @Min(1)
    private Long expectedRevision;
}
