package top.egon.cola.component.tianshu.ai.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Skill 创作目录监视与草稿上传配置。 / Skill authoring directory watch and draft upload configuration. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiAuthoringProperties {

    private boolean enabled;

    private Path root;

    @NotNull
    @Builder.Default
    private Duration pollInterval = Duration.ofSeconds(5);

    @NotNull
    @Builder.Default
    private Duration debounce = Duration.ofSeconds(2);

    @NotNull
    @Valid
    @Builder.Default
    private List<DdcAiWatchedResourceDTO> resources = new ArrayList<>();
}
