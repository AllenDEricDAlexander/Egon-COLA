package top.egon.cola.component.yuheng.admin.knowledge.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeEgressPolicyEnum;
import top.egon.cola.component.yuheng.admin.shared.domain.validation.CreateGroup;
import top.egon.cola.component.yuheng.admin.shared.domain.validation.UpdateGroup;

/**
 * 中文说明：{@code KnowledgeBaseCommandDTO} 是复用于原 API-009 创建与 API-011 完整替换的 CQE Command 载体；
 * 两个 body 形状相同，唯一差异是 API-009 不含 {@code expectedRevision}、API-011 必须携带当前 revision。
 * English summary: {@code KnowledgeBaseCommandDTO} is the shared command carrier for API-009 create and
 * API-011 replace; the only difference is that expectedRevision is absent on create and required on update.
 *
 * 用法 / Usage: 共同字段在 Default/Create/Update 三组同时生效，
 * {@code expectedRevision} 只在 UpdateGroup 必填；owner、embeddingSpaceId 与 dimensions 由服务端派生，
 * 因此不在本载体中出现，任何 null 都不能清空必填字段。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgeBaseCommandDTO {

    /** 展示名称，trim 后 1–128 字符 / trimmed display name. */
    @NotBlank(groups = {Default.class, CreateGroup.class, UpdateGroup.class})
    @Size(max = 128, groups = {Default.class, CreateGroup.class, UpdateGroup.class})
    private String name;

    /** 描述，0–2000 字符，可空不可 null / description, empty allowed but never null. */
    @NotNull(groups = {Default.class, CreateGroup.class, UpdateGroup.class})
    @Size(max = 2_000, groups = {Default.class, CreateGroup.class, UpdateGroup.class})
    private String description;

    /** 生成上下文出域策略，embedding 始终 LOCAL / egress policy for generated context. */
    @NotNull(groups = {Default.class, CreateGroup.class, UpdateGroup.class})
    private KnowledgeEgressPolicyEnum egressPolicy;

    /** 允许的 CHAT alias，命令时服务端复核 / allowed CHAT alias, revalidated on command. */
    @NotBlank(groups = {Default.class, CreateGroup.class, UpdateGroup.class})
    @Size(max = 64, groups = {Default.class, CreateGroup.class, UpdateGroup.class})
    private String chatModel;

    /** EMBEDDING alias，已有索引时不得原地改变空间 / embedding alias, immutable space once indexed. */
    @NotBlank(groups = {Default.class, CreateGroup.class, UpdateGroup.class})
    @Size(max = 64, groups = {Default.class, CreateGroup.class, UpdateGroup.class})
    private String embeddingModel;

    /** 创建不传；替换须等于当前 revision，正整数 / absent on create, current revision on replace. */
    @Min(value = 0, groups = {Default.class, UpdateGroup.class})
    @NotNull(groups = UpdateGroup.class)
    private Long expectedRevision;
}
