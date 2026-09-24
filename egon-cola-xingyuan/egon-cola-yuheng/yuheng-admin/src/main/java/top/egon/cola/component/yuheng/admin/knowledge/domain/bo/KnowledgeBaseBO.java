package top.egon.cola.component.yuheng.admin.knowledge.domain.bo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeMemberDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeEgressPolicyEnum;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 中文说明：{@code KnowledgeBaseBO} 是 {@code gateway_knowledge_base} 的业务载体，承载知识库的业务事实：
 * {@code members} 列在库内是 jsonb，在这里是结构化集合（复用 {@link KnowledgeMemberDTO} 这一条顶层载体，
 * 不用 Map 代替授权数据），{@code egressPolicy} 与 {@code type} 类列在这里是具名枚举，
 * {@code revision} 与 {@code createdAt}/{@code updatedAt} 是服务端权威投影；
 * 它不继承 {@code EgonModel}，也不承载租户、操作者与软删列。
 * English summary: {@code KnowledgeBaseBO} is the business carrier of {@code gateway_knowledge_base}: the
 * {@code members} jsonb column surfaces as a structured collection here (reusing the top-level
 * {@link KnowledgeMemberDTO} carrier rather than an untyped Map), the coded columns surface as named enums, while
 * {@code revision} and the audit projections stay server-authoritative; it neither extends {@code EgonModel} nor
 * carries tenant, operator or soft-delete columns.
 *
 * 用法 / Usage: 只由 {@code KnowledgeBasePersistenceConverter} 与 {@code KnowledgeBasePO} 互转，并经
 * {@code KnowledgeRepository} 端口流动。owner 与恰好一个 OWNER、embedding 空间/维度在首次上传后冻结、
 * alias 可用性等跨字段与跨聚合一致性由命令侧注解与 Service 权限入口负责，本载体只保留单字段原生约束与
 * {@code @Valid} 嵌套校验；集合读写一律防御性快照，避免命令提交后成员被外部改写进入持久边界。
 * Structural mapping happens only in the persistence converter; the OWNER-union and frozen-space invariants stay with
 * the command contract and the owning service, while this carrier keeps single-field constraints, {@code @Valid}
 * nesting and a defensive snapshot for every collection read or write.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgeBaseBO {

    /** 不透明主键的十进制文本，插入前为 null / decimal text of the assigned key, null before an insert. */
    private String id;

    /** 展示名称，trim 后 1–128 字符 / trimmed display name, 1–128 characters. */
    @NotBlank
    @Size(max = 128)
    private String name;

    /** 描述，0–2000 字符，可空串但不可 null / description, empty allowed but never null. */
    @NotNull
    @Size(max = 2_000)
    private String description;

    /** 服务端派生的所有者主体，创建后不改 / server-derived owner subject, immutable after creation. */
    @NotBlank
    @Size(max = 128)
    private String ownerActorId;

    /** 成员授权投影，0–200 条且恰好一个 OWNER 由 Service 复核 / member grants, 0–200 records with the OWNER union checked by the service. */
    @NotNull
    @Size(max = 200)
    @Valid
    private List<KnowledgeMemberDTO> members;

    /** 生成上下文出域策略，embedding 恒为 LOCAL / egress policy for generated context, embedding stays LOCAL. */
    @NotNull
    private KnowledgeEgressPolicyEnum egressPolicy;

    /** CHAT alias，命令时服务端复核 / CHAT alias, revalidated on command. */
    @NotBlank
    @Size(max = 64)
    private String chatModel;

    /** 本地 EMBEDDING alias，已有索引时不得原地改变空间 / local embedding alias, immutable space once indexed. */
    @NotBlank
    @Size(max = 64)
    private String embeddingModel;

    /** 冻结的嵌入空间标识，首次摄取后不可变 / frozen embedding space identity, immutable after the first ingest. */
    @Size(max = 128)
    private String embeddingSpaceId;

    /** 冻结向量维度，与 alias 一致，未摄取时为 null / frozen dimensions aligned with the alias, null before ingestion. */
    @Min(1)
    @Max(16_000)
    private Integer dimensions;

    /** 调用方期望的乐观版本：创建意图为 0 哨兵值，替换为库中现值，落库后由仓储回写权威值（存储行恒 ≥1）/ caller expectation, 0 on create intent and the stored revision on replace, overwritten by the authoritative value (stored rows are always ≥1) after a save. */
    @Min(0)
    private long revision;

    /** 投影自 {@code create_time} 的创建时刻 / creation instant projected from {@code create_time}. */
    private Instant createdAt;

    /** 投影自 {@code update_time} 的更新时刻 / update instant projected from {@code update_time}. */
    private Instant updatedAt;

    /**
     * 中文说明：返回 members 的不可变快照副本，未设置时保持 null 以区分「尚无成员」。
     * English summary: Returns an immutable snapshot copy of the member grants, staying null to distinguish
     * "not projected yet" from an explicit empty set.
     */
    public List<KnowledgeMemberDTO> getMembers() {
        return members == null ? null : Collections.unmodifiableList(new ArrayList<>(members));
    }

    /**
     * 中文说明：写入时复制 members，避免命令提交后被调用方或异步任务改写已进入持久边界的数据。
     * English summary: Defensively copies the incoming member grants so an async path cannot mutate persisted state.
     */
    public KnowledgeBaseBO setMembers(List<KnowledgeMemberDTO> members) {
        this.members = members == null ? null : new ArrayList<>(members);
        return this;
    }
}
