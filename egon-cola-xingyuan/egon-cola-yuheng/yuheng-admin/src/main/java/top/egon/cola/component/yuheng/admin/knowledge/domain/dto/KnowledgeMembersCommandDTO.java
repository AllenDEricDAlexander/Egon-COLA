package top.egon.cola.component.yuheng.admin.knowledge.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 中文说明：{@code KnowledgeMembersCommandDTO} 是原 API-013 完整替换知识库成员的 CQE Command 载体，
 * 只承载 {@code members/expectedRevision} 两个客户端可控字段；owner、租户与操作者都是服务端派生值，
 * 因此不在本载体中出现，任何 null 都不能清空必填字段。
 * English summary: {@code KnowledgeMembersCommandDTO} is the API-013 command carrier that replaces the whole member
 * set of a knowledge base; it holds only the client-owned {@code members/expectedRevision} pair, so server-derived
 * owner, tenant and operator values never appear here and no null can clear a mandatory field.
 *
 * 用法 / Usage: 由 Controller 以 {@code @Valid} 绑定，元素逐个通过嵌套 {@code @Valid} 校验；
 * {@code members} 是完整集合而非增量，且至少一条（通常是所有者本人），空数组不是「移除全部授权」的表达；
 * 主体唯一、恰好一个 OWNER、不得移除最后编辑者等跨记录规则由 Service 在同一事务内按 CAS 复核，
 * 集合在 setter 与 getter 上各自复制一次，避免校验通过后被调用方改写。
 * Bound with {@code @Valid} at the controller and nested-validated per element; the list is the complete replacement
 * set holding at least one record (normally the owner), so an empty array never means "remove every grant", while the
 * uniqueness and OWNER-union rules stay with the service inside one CAS transaction and the collection is copied on
 * both the setter and the getter so a caller cannot mutate an already validated command.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgeMembersCommandDTO {

    /** 完整成员集合，1–200 条，缺少字段与空数组都被拒绝 / complete member set, 1–200 records; both a missing field and an empty array are rejected. */
    @NotEmpty
    @Size(max = 200)
    @Valid
    private List<KnowledgeMemberDTO> members;

    /** 必须等于库中当前 revision 的正整数，不符返回 409 / must equal the stored revision, a positive integer, and a mismatch yields 409. */
    @NotNull
    @Min(1)
    private Long expectedRevision;

    /**
     * 中文说明：返回 members 的不可变快照副本，未传时为 null 交由 {@code @NotEmpty} 拒绝。
     * English summary: Returns an immutable snapshot copy of the members, staying null so {@code @NotEmpty} rejects it.
     */
    public List<KnowledgeMemberDTO> getMembers() {
        return members == null ? null : Collections.unmodifiableList(new ArrayList<>(members));
    }

    /**
     * 中文说明：写入时复制 members，阻断校验通过后的调用方原地修改。
     * English summary: Defensively copies the incoming members so the caller cannot mutate a validated command.
     */
    public KnowledgeMembersCommandDTO setMembers(List<KnowledgeMemberDTO> members) {
        this.members = members == null ? null : new ArrayList<>(members);
        return this;
    }
}
