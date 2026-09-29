package top.egon.cola.component.tianshu.admin.model.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiDeleteCommand {

    @NotBlank
    @Size(max = 19)
    @Pattern(regexp = "^(0|[1-9][0-9]*)$")
    private String expectedRevision;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static DdcAiDeleteCommand of(
            @JsonProperty(value = "expectedRevision", required = true) String expectedRevision) {
        return DdcAiDeleteCommand.builder().expectedRevision(expectedRevision).build();
    }
}
