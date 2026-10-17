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

/** 已发布 Skill 的本地工作区落盘配置。 / Local workspace configuration for published Skills. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiWorkspaceProperties {

    private boolean enabled;

    private Path root;

    private boolean subscriptionEnabled;

    @NotNull
    @Builder.Default
    private Duration reconcileInterval = Duration.ofSeconds(60);

    @NotNull
    @Valid
    @Builder.Default
    private List<DdcAiWatchedResourceDTO> resources = new ArrayList<>();
}
