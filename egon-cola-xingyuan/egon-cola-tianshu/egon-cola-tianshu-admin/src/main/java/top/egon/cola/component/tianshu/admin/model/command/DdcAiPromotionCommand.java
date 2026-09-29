package top.egon.cola.component.tianshu.admin.model.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.tianshu.model.ai.DdcAiScopeDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiPromotionCommand {

    @NotBlank
    @Size(max = 19)
    @Pattern(regexp = "^[1-9][0-9]*$")
    @DecimalMax("9223372036854775807")
    private String sourceVersionNo;

    @NotNull
    @Valid
    private DdcAiScopeDTO targetScope;

    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$")
    private String targetResourceKey;

    @Size(max = 19)
    @Pattern(regexp = "^(0|[1-9][0-9]*)$")
    @DecimalMax("9223372036854775807")
    @Schema(type = "string", nullable = true, pattern = "^(0|[1-9][0-9]{0,18})$")
    private String expectedTargetRevision;

    @NotNull
    @Size(max = 512)
    private String changeNote;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static DdcAiPromotionCommand of(
            @JsonProperty(value = "sourceVersionNo", required = true) String sourceVersionNo,
            @JsonProperty(value = "targetScope", required = true) DdcAiScopeDTO targetScope,
            @JsonProperty(value = "targetResourceKey", required = true) String targetResourceKey,
            @JsonProperty(value = "expectedTargetRevision", required = true) String expectedTargetRevision,
            @JsonProperty(value = "changeNote", required = true) String changeNote) {
        return DdcAiPromotionCommand.builder().sourceVersionNo(sourceVersionNo).targetScope(targetScope)
                .targetResourceKey(targetResourceKey).expectedTargetRevision(expectedTargetRevision)
                .changeNote(changeNote).build();
    }
}
