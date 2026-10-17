package top.egon.cola.component.tianshu.ai.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * 下游 AI Starter 根配置：使用上报、Skill 工作区与 Skill 创作上传。
 * / Root configuration of the downstream AI starter: usage reporting, Skill workspace and Skill authoring upload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@Validated
@ConfigurationProperties(DdcAiStarterProperties.PREFIX)
public class DdcAiStarterProperties {

    public static final String PREFIX = "egon.cola.component.tianshu.ai";

    @NotNull
    @Valid
    @Builder.Default
    @NestedConfigurationProperty
    private DdcAiFeedbackProperties feedback = new DdcAiFeedbackProperties();

    @NotNull
    @Valid
    @Builder.Default
    private Skill skill = new Skill();

    /** Skill 下行工作区与上行创作。 / Skill downstream workspace and upstream authoring. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class Skill {

        @NotNull
        @Valid
        @Builder.Default
        @NestedConfigurationProperty
        private DdcAiWorkspaceProperties workspace = new DdcAiWorkspaceProperties();

        @NotNull
        @Valid
        @Builder.Default
        @NestedConfigurationProperty
        private DdcAiAuthoringProperties authoring = new DdcAiAuthoringProperties();
    }
}
