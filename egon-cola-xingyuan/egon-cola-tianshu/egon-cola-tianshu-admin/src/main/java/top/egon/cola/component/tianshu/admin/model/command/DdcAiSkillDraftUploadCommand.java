package top.egon.cola.component.tianshu.admin.model.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiValidationGroups;

/**
 * RPC-005：下游创作目录整树替换 Skill 草稿。资源身份三元组必须与持久化资源一致。
 * / RPC-005: a downstream authoring tree replaces a Skill draft. The resource identity triple must match the stored
 * resource.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiSkillDraftUploadCommand {

    @NotBlank
    @Size(max = 128)
    private String namespaceCode;

    @NotBlank
    @Size(max = 128)
    private String resourceKey;

    @NotNull
    @Positive
    private Long resourceId;

    @NotBlank
    @Size(max = 19)
    @Pattern(regexp = "^(0|[1-9][0-9]*)$")
    private String expectedRevision;

    @NotBlank
    @Size(max = 128)
    private String displayName;

    @NotNull
    @Size(max = 2048)
    private String description;

    @NotNull
    @Valid
    @ConvertGroup(from = Default.class, to = DdcAiValidationGroups.Save.class)
    private DdcAiContentDTO content;
}
