package top.egon.cola.component.tianshu.ai.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 被工作区或创作监视的 Skill 资源；pinnedVersionNo 仅工作区使用，directory/autoPublish/changeNote 仅创作使用。
 * / A Skill watched by the workspace or authoring; pinnedVersionNo is workspace-only, directory/autoPublish/changeNote
 * are authoring-only.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder(toBuilder = true)
public class DdcAiWatchedResourceDTO {

    @NotBlank
    @Size(max = 128)
    private String namespaceCode;

    @NotBlank
    @Size(max = 128)
    private String resourceKey;

    @NotNull
    @Positive
    private Long resourceId;

    @Positive
    private Long pinnedVersionNo;

    @Size(max = 255)
    private String directory;

    @Builder.Default
    private boolean autoPublish = false;

    @NotNull
    @Size(max = 512)
    @Builder.Default
    private String changeNote = "";

    /** 监视索引键。 / Watch index key. */
    public String watchKey() {
        return namespaceCode + "/" + resourceKey;
    }
}
