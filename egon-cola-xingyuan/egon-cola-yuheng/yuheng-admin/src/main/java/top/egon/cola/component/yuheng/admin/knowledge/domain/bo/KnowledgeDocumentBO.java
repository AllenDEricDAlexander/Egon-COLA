package top.egon.cola.component.yuheng.admin.knowledge.domain.bo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Instant;

/**
 * 中文说明：{@code KnowledgeDocumentBO} 是 {@code gateway_knowledge_document} 的业务载体，只承载资料行的
 * 归属与「当前有效版本 / 最近作业」两个逻辑指针；{@code activeRevisionId} 与 {@code latestJobId} 都是
 * 十进制文本的不透明 ID，null 分别表示「从未成功发布」与「尚无作业」。{@code KnowledgeDocumentVO.status}
 * 这类派生值不在本载体中出现，因为 {@code gateway_knowledge_document} 没有状态列，它由 Service 结合
 * {@code KnowledgeJobBO} 计算。
 * English summary: {@code KnowledgeDocumentBO} is the business carrier of {@code gateway_knowledge_document} and holds
 * only the ownership plus the two logical pointers "active revision" and "latest job"; both are decimal opaque ids
 * where null means "never published" and "no job yet". Derived values such as {@code KnowledgeDocumentVO.status} stay
 * out of this carrier because the table owns no status column, so the service computes them from the job.
 *
 * 用法 / Usage: 只由 {@code KnowledgeDocumentPersistenceConverter} 与 {@code KnowledgeDocumentPO} 互转，并经
 * {@code KnowledgeRepository} 端口流动；激活指针只能通过 {@code activateRevision} 的 CAS 语义变更
 * （断言 {@code revision = expectedRevision}，0 行即所有权/版本丢失而非成功），文件名不解释为路径，
 * 租户与软删条件由仓储守卫补齐。active revision 必须属于本资料且状态 READY 的跨字段规则属于 Service 边界。
 * Structural mapping happens only in the persistence converter; the pointer may move solely through the CAS
 * {@code activateRevision} path where zero rows means lost ownership, and the READY-membership invariant stays with the
 * owning service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgeDocumentBO {

    /** 不透明主键的十进制文本，插入前为 null / decimal text of the assigned key, null before an insert. */
    private String id;

    /** 所属知识库十进制 ID，创建后不改 / owning knowledge base id, immutable after creation. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String kbId;

    /** 原始文件显示名，1–255，永不作为路径 / original display file name, 1–255, never a path. */
    @NotBlank
    @Size(max = 255)
    private String fileName;

    /** 当前有效版本十进制 ID，未发布时为 null，只经 CAS 回写 / active revision id, null before the first successful publish and only moved by the CAS. */
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String activeRevisionId;

    /** 最近作业十进制 ID，从未排队时为 null / latest job id, null when nothing was ever queued. */
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String latestJobId;

    /** 调用方期望的乐观版本：创建意图为 0 哨兵值，更新为库中现值，落库后由仓储回写权威值（存储行恒 ≥1）/ caller expectation, 0 on create intent and the stored revision on update, overwritten by the authoritative value (stored rows are always ≥1) after a save. */
    @Min(0)
    private long revision;

    /** 投影自 {@code create_time} 的创建时刻 / creation instant projected from {@code create_time}. */
    private Instant createdAt;

    /** 投影自 {@code update_time} 的更新时刻 / update instant projected from {@code update_time}. */
    private Instant updatedAt;
}
