package top.egon.cola.component.tianshu.model.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 草稿上传后的资源修订与草稿摘要。
 * / Resource revision and draft digest after a draft upload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DdcAiRevisionDTO {

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long resourceId;

    @NotNull
    @Min(0)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long resourceRevision;

    @NotBlank
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String draftSha256;

    /** 草稿内容是否与当前发布版本一致。 / Whether the draft content equals the current published version. */
    @NotNull
    private Boolean draftMatchesCurrent;
}
