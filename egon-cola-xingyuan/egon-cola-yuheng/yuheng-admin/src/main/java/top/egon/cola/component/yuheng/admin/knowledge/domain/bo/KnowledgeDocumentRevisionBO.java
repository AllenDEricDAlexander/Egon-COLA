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
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeRevisionStatusEnum;

import java.time.Instant;

/**
 * 中文说明：{@code KnowledgeDocumentRevisionBO} 是 {@code gateway_knowledge_revision} 的业务载体，
 * 一次上传即一条不可变修订：原件字节、抽取正文、冻结的嵌入空间/维度与冻结的分块配置都在这条记录上，
 * {@code chunkingConfig} 列在库内是 jsonb，在这里保持 {@link JsonNode} 顶层类型而不退化为任意 Map 字段集合，
 * {@code status} 是具名枚举 {@link KnowledgeRevisionStatusEnum}，{@code revision} 与
 * {@code createdAt}/{@code updatedAt} 是服务端权威投影；它不继承 {@code EgonModel}，也不承载租户与软删列。
 * English summary: {@code KnowledgeDocumentRevisionBO} is the business carrier of
 * {@code gateway_knowledge_revision}: one upload is one immutable revision holding the original bytes, the extracted
 * text, the frozen embedding space/dimensions and the frozen chunking configuration; {@code chunkingConfig} keeps the
 * {@link JsonNode} top level type instead of degrading into loose Map fields, {@code status} is the named
 * {@link KnowledgeRevisionStatusEnum}, and {@code revision} with the audit instants stay server-authoritative; it
 * neither extends {@code EgonModel} nor carries tenant or soft-delete columns.
 *
 * 用法 / Usage: 只由 {@code KnowledgeDocumentRevisionPersistenceConverter} 与
 * {@code KnowledgeDocumentRevisionPO} 互转，并经 {@code KnowledgeRepository} 端口流动。原件字节在 setter 与
 * getter 上各复制一次，阻断摄取 worker 异步读取期间被篡改；抽取正文只由 PARSE 阶段写入（PARSE 前为 null），
 * {@code contentHash} 是 {@code rawBytes} 的小写 SHA-256，而 {@code byteCount == rawBytes.length}、
 * {@code chunkCount == 实际暂存 chunk 数}、只有 READY 可被激活等跨字段一致性属于 Service/Strategy 职责，
 * 本载体只保留单字段原生边界。
 * Structural mapping happens only in the persistence converter; the original bytes are copied on both the setter and
 * the getter so an async worker cannot observe tampering, the extracted text is written solely by the PARSE stage,
 * and the hash/byte-count/READY invariants stay with the owning strategy while this carrier keeps single-field bounds.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgeDocumentRevisionBO {

    /** 不透明主键的十进制文本，插入前为 null / decimal text of the assigned key, null before an insert. */
    private String id;

    /** 所属知识库十进制 ID，与 documentId 组成复合边界 / owning knowledge base id, paired with documentId as the composite boundary. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String kbId;

    /** 所属资料十进制 ID，创建后不改 / owning document id, immutable after creation. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String documentId;

    /** 该版本原始文件名，1–255，不作为路径 / original file name of this revision, 1–255, never a path. */
    @NotBlank
    @Size(max = 255)
    private String fileName;

    /** 服务端联合验证过的媒体类型，1–128 / server-validated media type, 1–128. */
    @NotBlank
    @Size(max = 128)
    private String mediaType;

    /** 原件字节，1–20971520（20MiB），只在命令与 PARSE 边界内流动 / original bytes, 1–20MiB, never leaves the command and PARSE boundary. */
    @NotNull
    @Size(min = 1, max = 20_971_520)
    private byte[] rawBytes;

    /** 原件字节数，须等于 rawBytes 长度，1–20971520 / original byte count, must equal the raw length, 1–20MiB. */
    @NotNull
    @Min(1)
    @Max(20_971_520)
    private Long byteCount;

    /** 原件小写 SHA-256 十六进制 64 字符，Wiki sourceHash 复用同值 / lowercase SHA-256 hex of the original bytes, reused as the wiki source hash. */
    @NotBlank
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String contentHash;

    /** PARSE 阶段抽取正文，PARSE 前为 null，上限 2000000 字符 / text extracted by the PARSE stage, null before it, bounded by 2000000 characters. */
    @Size(max = 2_000_000)
    private String extractedText;

    /** 冻结的本地嵌入空间标识，与 KB 一致 / frozen local embedding space identity aligned with the base. */
    @NotBlank
    @Size(max = 128)
    private String embeddingSpaceId;

    /** 冻结向量维度 D，与 KB/alias 一致 / frozen dimensions D aligned with the base and the alias. */
    @NotNull
    @Min(1)
    @Max(16_000)
    private Integer dimensions;

    /** 冻结的 RAG 分块规则（strategy/chunkSize/overlap/maxChunks），jsonb / frozen RAG chunking rule held as jsonb. */
    @NotNull
    private JsonNode chunkingConfig;

    /** STAGING/READY/FAILED，仅 READY 可被激活 / staging, ready or failed; only a ready revision may be activated. */
    @NotNull
    private KnowledgeRevisionStatusEnum status;

    /** 成功发布前核对的完整 chunk 数，0..10000 / complete chunk count verified before publishing, 0..10000. */
    @NotNull
    @Min(0)
    @Max(10_000)
    private Integer chunkCount;

    /** 调用方期望的乐观版本：创建意图为 0 哨兵值，更新为库中现值，落库后由仓储回写权威值（存储行恒 ≥1）/ caller expectation, 0 on create intent and the stored revision on update, overwritten by the authoritative value (stored rows are always ≥1) after a save. */
    @Min(0)
    private long revision;

    /** 投影自 {@code create_time} 的创建时刻 / creation instant projected from {@code create_time}. */
    private Instant createdAt;

    /** 投影自 {@code update_time} 的最后状态变更时刻 / update instant projected from {@code update_time}. */
    private Instant updatedAt;

    /**
     * 中文说明：返回 rawBytes 的副本，调用方修改数组不会影响已进入摄取作业的对象；未设置时保持 null。
     * English summary: Returns a copy of the stored bytes so a later mutation cannot reach the ingest job, staying
     * null before an upload is bound.
     */
    public byte[] getRawBytes() {
        return rawBytes == null ? null : rawBytes.clone();
    }

    /**
     * 中文说明：写入时复制 rawBytes，阻断异步摄取期间的输入篡改。
     * English summary: Defensively copies the incoming bytes to block tampering while an async ingest task runs.
     */
    public KnowledgeDocumentRevisionBO setRawBytes(byte[] rawBytes) {
        this.rawBytes = rawBytes == null ? null : rawBytes.clone();
        return this;
    }
}
