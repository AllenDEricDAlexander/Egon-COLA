package top.egon.cola.component.tianshu.admin.model.command;

import com.fasterxml.jackson.annotation.JsonFormat;
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
import top.egon.cola.component.tianshu.admin.service.ai.validation.ValidDurationRange;

import java.time.Duration;

/** API-013 完整替换优化策略。 / API-013 full replacement of an optimization policy. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder(toBuilder = true)
public class DdcAiOptimizationPolicyCommand {

    @NotBlank
    @Pattern(regexp = "^(0|[1-9][0-9]{0,18})$")
    @Schema(description = "当前策略修订；首次保存为 0", example = "0")
    private String expectedRevision;

    @NotNull
    private Boolean enabled;

    @NotNull
    @Min(1)
    @Max(100)
    private Integer scoreThreshold;

    @NotNull
    @Min(1)
    @Max(100000)
    private Integer minSamples;

    @NotNull
    @ValidDurationRange(minSeconds = 300, maxSeconds = 2592000)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", format = "duration", example = "PT24H")
    private Duration evaluationWindow;

    @NotNull
    @Min(1)
    @Max(50)
    private Integer maxIterations;

    @NotNull
    @ValidDurationRange(minSeconds = 60, maxSeconds = 604800)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", format = "duration", example = "PT1H")
    private Duration cooldown;

    @NotNull
    private Boolean autoPublish;

    @NotNull
    private Boolean judgeEnabled;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$")
    private String optimizerModelAlias;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$")
    private String judgeModelAlias;

    @NotNull
    @Size(max = 4096)
    @Pattern(regexp = "^(?:[^\\p{Cntrl}]|\\n)*$")
    private String optimizerInstruction;

    @NotNull
    private Boolean rearm;
}
