package top.egon.cola.component.tianshu.model.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 下游创作目录整树替换 Skill 草稿的上传载体；完整内容规则由 Admin 转换为草稿后校验。
 * / Whole-tree Skill draft upload from a downstream authoring directory; Admin validates full content rules after
 * converting it into a draft.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder(toBuilder = true)
public class DdcAiSkillDraftUploadDTO {

    @NotBlank
    @Size(max = 128)
    private String namespaceCode;

    @NotBlank
    @Size(max = 128)
    private String resourceKey;

    @NotNull
    @Positive
    private Long resourceId;

    /** 上次拉取或上传得到的资源修订号，用于 CAS。 / Resource revision from the last pull or upload, used for CAS. */
    @NotNull
    @Min(0)
    private Long expectedRevision;

    @NotBlank
    @Size(max = 128)
    private String displayName;

    @NotNull
    @Size(max = 2048)
    private String description;

    @NotNull
    @Size(min = 1, max = 257)
    @Valid
    private List<DdcAiNodeDTO> files;
}
