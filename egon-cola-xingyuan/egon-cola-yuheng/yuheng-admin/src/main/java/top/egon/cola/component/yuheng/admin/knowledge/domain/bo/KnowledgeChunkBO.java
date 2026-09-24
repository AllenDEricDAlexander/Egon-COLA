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

import java.time.Instant;

/**
 * 中文说明：{@code KnowledgeChunkBO} 是 {@code gateway_knowledge_chunk} 的业务载体，一条记录即某条
 * revision 内一个有界分块：{@code chunkIndex} 是版本内唯一定位（CHECK 0..9999），{@code metadata} 列在库内是
 * jsonb，在这里保持 {@link JsonNode} 顶层类型而不退化为任意 Map 字段集合，{@code embedding} 是
 * {@code vector} 列的 {@code float[]} 投影；它不继承 {@code EgonModel}，也不承载租户与软删列。
 * English summary: {@code KnowledgeChunkBO} is the business carrier of {@code gateway_knowledge_chunk}: one row is one
 * bounded chunk of a revision, {@code chunkIndex} is the unique in-revision position (CHECK 0..9999),
 * {@code metadata} keeps the {@link JsonNode} top level type instead of degrading into loose Map fields, and
 * {@code embedding} is the {@code float[]} projection of the vector column; it neither extends {@code EgonModel} nor
 * carries tenant or soft-delete columns.
 *
 * 用法 / Usage: 只由 {@code KnowledgeChunkPersistenceConverter} 与 {@code KnowledgeChunkPO} 互转，并经
 * {@code KnowledgeRepository.stageChunks} 批量写入。向量在 setter 与 getter 上各复制一次，避免批次内
 * 复用数组的模型客户端在 CAS 发布前改写已暂存结果；{@code embedding.length == dimensions}、全部有限且非零、
 * 条数等于 {@code chunkCount} 以及 {@code contentHash} 为正文 UTF-8 SHA-256 等一致性由
 * {@code KnowledgeModelClientService} 与摄取 Strategy 在调用与发布前复核，本载体只声明单字段边界；
 * {@code metadata} 只承载解析器可信定位，不得携带调用方 ACL。
 * Structural mapping happens only in the persistence converter and batches enter through
 * {@code KnowledgeRepository.stageChunks}; the vector is copied on both the setter and the getter so a reused client
 * buffer cannot mutate staged results before the CAS publish, while the length/finiteness/non-zero/count and
 * content-hash invariants stay with the model client and the ingest strategy, and metadata never carries caller ACL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgeChunkBO {

    /** 不透明主键的十进制文本，插入前为 null；同 revision/chunkIndex 重试保留旧 ID / decimal text of the assigned key, null before an insert; a retry keeps the same id for a revision and index. */
    private String id;

    /** 所属知识库十进制 ID，与 revisionId 组成复合 FK / owning knowledge base id, paired with revisionId in the composite FK. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String kbId;

    /** 所属不可变 revision 十进制 ID，创建后不改 / owning immutable revision id, immutable after creation. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String revisionId;

    /** 版本内唯一序号，CHECK 0..9999 / unique index inside the revision, CHECK 0..9999. */
    @NotNull
    @Min(0)
    @Max(9_999)
    private Integer chunkIndex;

    /** 分块正文，保留原始空白，上限 2000000 字符 / chunk body preserving whitespace, bounded by 2000000 characters. */
    @NotBlank
    @Size(max = 2_000_000)
    private String content;

    /** 解析器可信定位（页码/标题/偏移），jsonb，默认 {@code {}} / trusted parser locator (page, heading, offset) as jsonb, {@code {}} by default. */
    @NotNull
    private JsonNode metadata;

    /** 分块正文 UTF-8 小写 SHA-256 十六进制 64 字符，用于向量重建完整性 / lowercase SHA-256 hex of the chunk body, used for vector rebuild integrity. */
    @NotBlank
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String contentHash;

    /** 嵌入空间标识，必须与所属 revision 相同 / embedding space identity, must equal the owning revision's. */
    @NotBlank
    @Size(max = 128)
    private String embeddingSpaceId;

    /** 向量维度，与 KB/revision 一致 / vector dimensions aligned with the base and the revision. */
    @NotNull
    @Min(1)
    @Max(16_000)
    private Integer dimensions;

    /** 本地模型产出的向量，长度须等于 dimensions 且有限非零 / local model vector, exactly dimensions long, finite and non-zero. */
    @NotNull
    @Size(min = 1, max = 16_000)
    private float[] embedding;

    /** 调用方期望的乐观版本：创建意图为 0 哨兵值，更新为库中现值，落库后由仓储回写权威值（存储行恒 ≥1）/ caller expectation, 0 on create intent and the stored revision on update, overwritten by the authoritative value (stored rows are always ≥1) after a save. */
    @Min(0)
    private long revision;

    /** 投影自 {@code create_time} 的创建时刻 / creation instant projected from {@code create_time}. */
    private Instant createdAt;

    /** 投影自 {@code update_time} 的最后状态变更时刻 / update instant projected from {@code update_time}. */
    private Instant updatedAt;

    /**
     * 中文说明：返回 embedding 的副本，模型客户端复用缓冲区也不会污染已暂存向量；未暂存时保持 null。
     * English summary: Returns a copy of the stored vector so a reused client buffer cannot poison staged chunks,
     * staying null before an embedding exists.
     */
    public float[] getEmbedding() {
        return embedding == null ? null : embedding.clone();
    }

    /**
     * 中文说明：写入时复制 embedding，阻断批次重试期间对已暂存向量的原地改写。
     * English summary: Defensively copies the incoming vector to block in-place mutation during a batch retry.
     */
    public KnowledgeChunkBO setEmbedding(float[] embedding) {
        this.embedding = embedding == null ? null : embedding.clone();
        return this;
    }
}
