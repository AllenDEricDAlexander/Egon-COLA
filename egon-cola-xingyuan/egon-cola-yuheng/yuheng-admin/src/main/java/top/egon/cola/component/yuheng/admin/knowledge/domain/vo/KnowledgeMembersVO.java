package top.egon.cola.component.yuheng.admin.knowledge.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeMemberDTO;

/**
 * 中文说明：{@code KnowledgeMembersVO} 是 API-012/013 的权威成员快照，版本与成员集合在同一响应中返回。
 * English summary: {@code KnowledgeMembersVO} is the authoritative API-012/013 member snapshot, returning the revision
 * and member set together.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgeMembersVO {

    @NotNull
    @Size(max = 100)
    @Valid
    private List<KnowledgeMemberDTO> members;

    @NotNull
    @Min(1)
    @Schema(minimum = "1")
    private Long revision;

    public List<KnowledgeMemberDTO> getMembers() {
        return members == null ? null : Collections.unmodifiableList(new ArrayList<>(members));
    }

    public KnowledgeMembersVO setMembers(List<KnowledgeMemberDTO> members) {
        this.members = members == null ? null : new ArrayList<>(members);
        return this;
    }
}
