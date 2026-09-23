package top.egon.cola.component.yuheng.admin.knowledge.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeMemberRoleEnum;

/**
 * 中文说明：{@code KnowledgeMemberDTO} 是原 API-012/013 成员记录的一层顶层载体，
 * 与 gateway_knowledge_base.members JSONB 元素一一映射，不使用任意 Map。
 * English summary: {@code KnowledgeMemberDTO} is the top-level member record carrier of API-012/013 mapped
 * one-to-one to a KB members JSONB element instead of an untyped Map.
 *
 * 用法 / Usage: 由成员命令载体以 {@code @Valid} 逐个校验；主体唯一、恰好一个 OWNER 等跨记录规则
 * 由 Service 在同一事务内复核，actorId 只承载已验证企业身份 subject，不捏造身份目录接口。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgeMemberDTO {

    /** 企业身份 subject，配置时校验稳定语法 / verified enterprise identity subject. */
    @NotBlank
    @Size(max = 128)
    private String actorId;

    /** READER/EDITOR/OWNER，本版不支持 owner 转移 / member role; owner transfer is out of scope. */
    @NotNull
    private KnowledgeMemberRoleEnum role;
}
