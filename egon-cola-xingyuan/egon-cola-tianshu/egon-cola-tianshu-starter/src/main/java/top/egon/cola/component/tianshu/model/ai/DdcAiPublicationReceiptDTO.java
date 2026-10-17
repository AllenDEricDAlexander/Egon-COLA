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
 * Skill 草稿发布回执。
 * / Receipt of a Skill draft publication.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DdcAiPublicationReceiptDTO {

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long publicationId;

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long resourceId;

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long targetVersionId;

    @NotNull
    @Positive
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long targetVersionNo;

    @NotNull
    @Min(0)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long resourceRevision;

    @NotBlank
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String contentSha256;
}
