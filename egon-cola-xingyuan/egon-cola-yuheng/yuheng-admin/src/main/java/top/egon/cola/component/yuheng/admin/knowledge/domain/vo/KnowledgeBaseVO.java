package top.egon.cola.component.yuheng.admin.knowledge.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeEgressPolicyEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeMemberRoleEnum;

/**
 * 中文说明：{@code KnowledgeBaseVO} 是原 API-008–011 的完整知识库投影：字符串 id、当前主体角色、
 * 创建时冻结的嵌入空间与维度以及乐观 revision 全部来自本节合同。
 * English summary: {@code KnowledgeBaseVO} is the complete API-008–011 knowledge-base projection carrying the
 * decimal-string id, the caller's current role, the frozen embedding space/dimensions and the optimistic revision.
 *
 * 用法 / Usage: ownerActorId、embeddingSpaceId、dimensions 与 revision 均为服务端派生，
 * 因此本载体不接受命令写入；id 始终是 1–20 位十进制字符串，前端不得转 number。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgeBaseVO {

    /** 服务端 Snowflake 十进制字符串 / server-assigned decimal string id. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String id;

    /** 展示名称 / trimmed display name. */
    @NotBlank
    @Size(max = 128)
    private String name;

    /** 描述，0–2000 字符，可空字符串但不可 null / description, empty allowed but never null. */
    @NotNull
    @Size(max = 2_000)
    private String description;

    /** 创建时可信身份派生的 owner，不接受 body 自报 / owner derived from the trusted identity at creation. */
    @NotBlank
    @Size(max = 128)
    private String ownerActorId;

    /** 当前主体在 KB 中的角色 / current subject role inside this KB. */
    @NotNull
    private KnowledgeMemberRoleEnum myRole;

    /** 生成上下文出域策略 / egress policy for generated context. */
    @NotNull
    private KnowledgeEgressPolicyEnum egressPolicy;

    /** 允许的 CHAT alias / allowed CHAT alias. */
    @NotBlank
    @Size(max = 64)
    private String chatModel;

    /** EMBEDDING alias / embedding alias. */
    @NotBlank
    @Size(max = 64)
    private String embeddingModel;

    /** 创建时冻结的不可变嵌入空间标识 / embedding space identity frozen at creation. */
    @NotBlank
    @Size(max = 128)
    private String embeddingSpaceId;

    /** 冻结的正维度 D，不随模型默认变化 / frozen positive dimension. */
    @NotNull
    @Min(1)
    @Max(16_000)
    private Integer dimensions;

    /** 已提交乐观版本，正整数 / committed optimistic revision, positive. */
    @NotNull
    @Min(1)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Long revision;
}
